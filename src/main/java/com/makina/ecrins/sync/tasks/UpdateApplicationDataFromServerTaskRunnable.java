package com.makina.ecrins.sync.tasks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.makina.ecrins.sync.adb.ADBCommand;
import com.makina.ecrins.sync.service.Status;
import com.makina.ecrins.sync.settings.LoadSettingsCallable;

/**
 * {@link AbstractTaskRunnable} implementation for fetching all data to be updated from the server to the connected device.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class UpdateApplicationDataFromServerTaskRunnable extends AbstractTaskRunnable
{
	private static final Logger LOG = Logger.getLogger(UpdateApplicationDataFromServerTaskRunnable.class);
	
	private ApkInfo apkInfo;
	
	@Override
	public void run()
	{
		setTaskStatus(new TaskStatus(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.STATUS_PENDING));
		
		try
		{
			if (getApkInfo())
			{
				if (downloadDataFromServer())
				{
					setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.STATUS_FINISH));
				}
				else
				{
					setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.STATUS_FAILED));
				}
			}
			else
			{
				setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.STATUS_FAILED));
			}
		}
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.STATUS_FAILED));
		}
		catch (JSONException je)
		{
			LOG.error(je.getMessage(), je);
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.STATUS_FAILED));
		}
		catch (InterruptedException ie)
		{
			LOG.error(ie.getMessage(), ie);
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.STATUS_FAILED));
		}
	}
	
	private boolean getApkInfo()
	{
		List<ApkInfo> apks = ApkUtils.getApkInfosFromJson(new File(TaskManager.getInstance().getTemporaryDirectory(), "versions.json"));
		
		if (apks.isEmpty())
		{
			return false;
		}
		else
		{
			apkInfo = apks.get(0);
			
			return true;
		}
	}
	
	private boolean downloadDataFromServer() throws IOException, JSONException, InterruptedException
	{
		final DefaultHttpClient httpClient = new DefaultHttpClient();
		final HttpParams httpParameters = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
		HttpConnectionParams.setSoTimeout(httpParameters, 5000);
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		nameValuePairs.add(new BasicNameValuePair("token", LoadSettingsCallable.getInstance().getJsonSettings().getJSONObject(LoadSettingsCallable.KEY_SYNC).getString(LoadSettingsCallable.KEY_TOKEN)));
		
		JSONArray exportsSettings = LoadSettingsCallable.getInstance().getJsonSettings().getJSONObject(LoadSettingsCallable.KEY_SYNC).getJSONArray(LoadSettingsCallable.KEY_EXPORTS);
		
		for (int i = 0; i < exportsSettings.length(); i++)
		{
			JSONObject exportSettings = exportsSettings.getJSONObject(i);
			
			setTaskStatus(new TaskStatus((int) (((double) i / (double) exportsSettings.length()) * 100), MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getString(LoadSettingsCallable.KEY_EXPORTS_FILE)), Status.STATUS_PENDING));
			
			HttpPost httpPost = new HttpPost(LoadSettingsCallable.getInstance().getJsonSettings().getJSONObject(LoadSettingsCallable.KEY_SYNC).getString(LoadSettingsCallable.KEY_SERVER_URL) + exportSettings.getString(LoadSettingsCallable.KEY_EXPORTS_URL));
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse httpResponse = httpClient.execute(httpPost);
			
			// checks if server response is valid
			StatusLine status = httpResponse.getStatusLine();
			
			if (status.getStatusCode() == HttpStatus.SC_OK)
			{
				// pulls content stream from response
				HttpEntity entity = httpResponse.getEntity();
				InputStream inputStream = entity.getContent();
				
				File localFile = new File(TaskManager.getInstance().getTemporaryDirectory(), exportSettings.getString(LoadSettingsCallable.KEY_EXPORTS_FILE));
				FileUtils.touch(localFile);
				
				if (copyInputStream(exportSettings.getString(LoadSettingsCallable.KEY_EXPORTS_FILE), inputStream, new FileOutputStream(localFile), entity.getContentLength(), i, exportsSettings.length()))
				{
					copyFileToDevice(localFile, exportSettings.getString(LoadSettingsCallable.KEY_EXPORTS_FILE));
					
					setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.finish.text"), exportSettings.getString(LoadSettingsCallable.KEY_EXPORTS_FILE)), Status.STATUS_PENDING));
				}
				else
				{
					setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getString(LoadSettingsCallable.KEY_EXPORTS_FILE)), Status.STATUS_FAILED));
					return false;
				}
			}
			else
			{
				LOG.error("unable to download file from URL '" + httpPost.getURI().toString() + "', HTTP status : " + status.getStatusCode());
				setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getString(LoadSettingsCallable.KEY_EXPORTS_FILE)), Status.STATUS_FAILED));
				
				return false;
			}
		}
		
		return true;
	}
	
	private void copyFileToDevice(File localFile, String remoteName) throws InterruptedException, IOException
	{
		ADBCommand.getInstance().push(localFile.getAbsolutePath(), ApkUtils.getExternalStorageDirectory(apkInfo) + "Android/data/" + apkInfo.getSharedUserId() + remoteName);
	}
	
	private boolean copyInputStream(String inputName, InputStream in, OutputStream out, long contentLength, int currentExport, int numberOfExports)
	{
		byte[] buffer = new byte[1024];
		
		int len;
		long totalBytesRead = 0;
		
		try
		{
			while ((len = in.read(buffer)) >= 0)
			{
				out.write(buffer, 0, len);
				totalBytesRead += len;
				
				int currentProgress =  (int) (((double) totalBytesRead / (double) contentLength) * 100);
				
				setTaskStatus(new TaskStatus((int) (((double) currentExport / (double) numberOfExports) * 100) + (currentProgress / numberOfExports), MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), inputName), Status.STATUS_PENDING));
			}
			
			out.flush();
			
			in.close();
			out.close();
		}
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
			return false;
		}
		
		return true;
	}
}
