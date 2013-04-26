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
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Logger;

import com.makina.ecrins.sync.adb.ADBCommand;
import com.makina.ecrins.sync.adb.ADBCommandException;
import com.makina.ecrins.sync.service.Status;
import com.makina.ecrins.sync.settings.ExportSettings;
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
	
	private boolean getApkInfo()
	{
		List<ApkInfo> apks = ApkUtils.getApkInfosFromJson(new File(TaskManager.getInstance().getTemporaryDirectory(), "versions.json"));
		
		if (apks.isEmpty())
		{
			return false;
		}
		else
		{
			// uses the first mobile application declaration
			apkInfo = apks.get(0);
			
			return true;
		}
	}
	
	private boolean checkFileSize(long remoteFileSize, String remoteName) throws ADBCommandException
	{
		return ADBCommand.getInstance().getFileSize(getDeviceFilePath(remoteName, false)) == remoteFileSize;
	}
	
	private boolean downloadDataFromServer()
	{
		boolean result = true;
		
		final DefaultHttpClient httpClient = new DefaultHttpClient();
		final HttpParams httpParameters = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, LoadSettingsCallable.getInstance().getSyncSettings().getServerTimeout());
		//HttpConnectionParams.setSoTimeout(httpParameters, 5000);
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		nameValuePairs.add(new BasicNameValuePair("token", LoadSettingsCallable.getInstance().getSyncSettings().getServerToken()));
		
		List<ExportSettings> exportsSettings = LoadSettingsCallable.getInstance().getSyncSettings().getExportsSettings();
		int i = 0;
		
		try
		{
			for (ExportSettings exportSettings : exportsSettings)
			{
				setTaskStatus(new TaskStatus((int) (((double) i / (double) exportsSettings.size()) * 100), MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.STATUS_PENDING));
				
				HttpPost httpPost = new HttpPost(
						LoadSettingsCallable.getInstance().getSyncSettings().getServerUrl() +
						exportSettings.getExportUrl());
				
				HttpResponse httpResponse = null;
				
				try
				{
					httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
					httpResponse = httpClient.execute(httpPost);
					
					// checks if server response is valid
					StatusLine status = httpResponse.getStatusLine();
					
					if (status.getStatusCode() == HttpStatus.SC_OK)
					{
						// compare file sizes between the remote file and the local file
						if (checkFileSize(Long.valueOf(httpResponse.getFirstHeader("Content-Length").getValue()), exportSettings.getExportFile()))
						{
							LOG.info("'" + exportSettings.getExportFile() + "' is already downloaded and installed");
							
							httpPost.abort();
						}
						else
						{
							// pulls content stream from response
							HttpEntity entity = httpResponse.getEntity();
							InputStream inputStream = entity.getContent();
							
							File localFile = new File(TaskManager.getInstance().getTemporaryDirectory(), exportSettings.getExportFile());
							FileUtils.touch(localFile);
							
							if (copyInputStream(exportSettings.getExportFile(), inputStream, new FileOutputStream(localFile), entity.getContentLength(), i, exportsSettings.size()))
							{
								copyFileToDevice(localFile, exportSettings.getExportFile());
								
								setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.finish.text"), exportSettings.getExportFile()), Status.STATUS_PENDING));
							}
							else
							{
								setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.STATUS_FAILED));
								result = false;
							}
						}
					}
					else
					{
						LOG.error("unable to download file from URL '" + httpPost.getURI().toString() + "', HTTP status : " + status.getStatusCode());
						setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.STATUS_FAILED));
						
						result = false;
					}
				}
				catch (ADBCommandException ace)
				{
					LOG.error(ace.getLocalizedMessage());
					
					httpPost.abort();
					result = false;
					setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.STATUS_FAILED));
				}
				catch (IOException ioe)
				{
					LOG.error(ioe.getLocalizedMessage());
					
					httpPost.abort();
					result = false;
					setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.STATUS_FAILED));
				}
				finally
				{
					HttpClientUtils.closeQuietly(httpResponse);
				}
				
				i++;
			}
		}
		finally
		{
			httpClient.getConnectionManager().shutdown();
		}
		
		return result;
	}
	
	private String getDeviceFilePath(String remoteName, boolean useDefaultExternalStorage)
	{
		if (useDefaultExternalStorage)
		{
			return ApkUtils.getDefaultExternalStorageDirectory() + "/" + ApkUtils.getRelativeSharedPath(apkInfo) + remoteName;
		}
		else
		{
			return ApkUtils.getExternalStorageDirectory() + "/" + ApkUtils.getRelativeSharedPath(apkInfo) + remoteName;
		}
	}
	
	private void copyFileToDevice(File localFile, String remoteName) throws ADBCommandException
	{
		if ((ADBCommand.getInstance().getBuildVersion() > 15) || ApkUtils.getExternalStorageDirectory().equals(ApkUtils.getDefaultExternalStorageDirectory()))
		{
			ADBCommand.getInstance().push(localFile.getAbsolutePath(), getDeviceFilePath(remoteName, false));
		}
		else
		{
			// uses specific service from mobile application to move the given file to the right place (use the external storage path)
			ADBCommand.getInstance().push(localFile.getAbsolutePath(), getDeviceFilePath(remoteName, true));
			ADBCommand.getInstance().executeCommand("am broadcast -a " + apkInfo.getPackageName() + ".INTENT_MOVE_FILE_TO_EXTERNAL_STORAGE -e " + apkInfo.getPackageName() + ".file " + remoteName + " -f 32");
		}
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
