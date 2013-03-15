package com.makina.ecrins.sync.tasks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
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
import org.json.JSONException;
import org.json.JSONObject;

import com.makina.ecrins.sync.adb.ADBCommand;
import com.makina.ecrins.sync.adb.ADBCommandException;
import com.makina.ecrins.sync.service.Status;
import com.makina.ecrins.sync.settings.LoadSettingsCallable;

/**
 * {@link AbstractTaskRunnable} implementation for fetching all inputs to be imported from a connected to device.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ImportInputsFromDeviceTaskRunnable extends AbstractTaskRunnable
{
	private static final Logger LOG = Logger.getLogger(ImportInputsFromDeviceTaskRunnable.class);
	
	private File inputsTempDir;
	private ApkInfo apkInfo;
	
	@Override
	public void run()
	{
		setTaskStatus(new TaskStatus(-1, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.STATUS_PENDING));
		
		try
		{
			fetchInputsFromDevice();
			
			if (uploadInputs())
			{
				setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.STATUS_FINISH));
			}
			else
			{
				setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.STATUS_FAILED));
			}
		}
		catch (InterruptedException ie)
		{
			LOG.error(ie.getMessage(), ie);
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.STATUS_FAILED));
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
		catch (ADBCommandException ace)
		{
			LOG.error(ace.getMessage(), ace);
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.STATUS_FAILED));
		}
		finally
		{
			FileUtils.deleteQuietly(this.inputsTempDir);
		}
	}
	
	private boolean fetchInputsFromDevice() throws InterruptedException, IOException
	{
		this.inputsTempDir = new File(TaskManager.getInstance().getTemporaryDirectory(), "inputs");
		this.inputsTempDir.mkdir();
		FileUtils.forceDeleteOnExit(inputsTempDir);
		
		List<ApkInfo> apks = ApkUtils.getApkInfosFromJson(new File(TaskManager.getInstance().getTemporaryDirectory(), "versions.json"));
		
		if (apks.isEmpty())
		{
			return false;
		}
		else
		{
			apkInfo = apks.get(0);
			ADBCommand.getInstance().pull(ApkUtils.getExternalStorageDirectory(apkInfo) + "Android/data/" + apks.get(0).getSharedUserId() + "/inputs/", this.inputsTempDir.getAbsolutePath());
			
			return this.inputsTempDir.list().length > 0;
		}
	}
	
	private void deleteInputFromDevice(File inputJson) throws IOException, InterruptedException, ADBCommandException
	{
		ADBCommand.getInstance().executeCommand("rm " + ApkUtils.getExternalStorageDirectory(apkInfo) + "Android/data/" + "com.makina.ecrins" + "/inputs/" + inputJson.getParent() + "/" + inputJson.getName());
	}
	
	private boolean uploadInputs() throws JSONException, IOException, InterruptedException, ADBCommandException
	{
		final DefaultHttpClient httpClient = new DefaultHttpClient();
		final HttpParams httpParameters = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
		HttpConnectionParams.setSoTimeout(httpParameters, 5000);
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("token", LoadSettingsCallable.getInstance().getJsonSettings().getJSONObject(LoadSettingsCallable.KEY_SYNC).getString(LoadSettingsCallable.KEY_TOKEN)));
		nameValuePairs.add(new BasicNameValuePair("data", "{}"));
		
		String urlImport = LoadSettingsCallable.getInstance().getJsonSettings().getJSONObject(LoadSettingsCallable.KEY_SYNC).getString(LoadSettingsCallable.KEY_SERVER_URL) + LoadSettingsCallable.getInstance().getJsonSettings().getJSONObject(LoadSettingsCallable.KEY_SYNC).getString(LoadSettingsCallable.KEY_IMPORT_URL);
		HttpPost httpPost = new HttpPost(urlImport);
		
		int currentInput = 0;
		
		// finds all inputs as json file
		Collection<File> inputFiles = FileUtils.listFiles(this.inputsTempDir, new IOFileFilter()
		{
			@Override
			public boolean accept(File dir, String name)
			{
				if (dir.getAbsolutePath().equals(inputsTempDir.getAbsolutePath()))
				{
					return name.startsWith("input_") && name.endsWith(".json");
				}
				else
				{
					return false;
				}
			}
			
			@Override
			public boolean accept(File file)
			{
				return file.getName().startsWith("input_") && file.getName().endsWith(".json");
			}
		},
		TrueFileFilter.INSTANCE);
		
		for (File inputFile : inputFiles)
		{
			LOG.info("synchronizing '" + inputFile.getName() + "' ...");
			
			setTaskStatus(new TaskStatus((int) (((double) currentInput / (double) inputFiles.size()) * 100), MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.text"), inputFile.getName()), Status.STATUS_PENDING));
			
			// reads input as JSON file
			nameValuePairs.set(1, new BasicNameValuePair("data", FileUtils.readFileToString(inputFile)));
			
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse httpResponse = httpClient.execute(httpPost);
			
			// checks if server response is valid
			StatusLine status = httpResponse.getStatusLine();
			
			if (status.getStatusCode() == HttpStatus.SC_OK)
			{
				// pulls content stream from response
				HttpEntity entity = httpResponse.getEntity();
				InputStream inputStream = entity.getContent();
				
				if (readInputStreamAsJson(inputFile.getName(), inputStream, entity.getContentLength(), currentInput, inputFiles.size()))
				{
					setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.finish.text"), inputFile.getName()), Status.STATUS_PENDING));
					deleteInputFromDevice(inputFile);
					LOG.info("'" + inputFile.getName() + "' synchronized");
				}
				else
				{
					setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.text"), inputFile.getName()), Status.STATUS_FAILED));
					return false;
				}
			}
			else
			{
				LOG.error("unable to upload input from URL '" + httpPost.getURI().toString() + "', HTTP status : " + status.getStatusCode());
				setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.text"), inputFile.getName()), Status.STATUS_FAILED));
				
				return false;
			}
			
			currentInput++;
		}
		
		if (inputFiles.size() > 1)
		{
			LOG.info(inputFiles.size() + " inputs synchronized");
		}
		else if (inputFiles.size() == 1)
		{
			LOG.info(inputFiles.size() + " input synchronized");
		}
		else
		{
			LOG.info("no input to synchronize");
		}
		
		return true;
	}
	
	private boolean readInputStreamAsJson(String inputName, InputStream in, long contentLength, int currentInput, int numberOfInputs)
	{
		OutputStream out = new OutputStream()
		{
			private StringBuilder string = new StringBuilder();
			
			@Override
			public void write(int b) throws IOException
			{
				this.string.append((char) b);
			}
			
			@Override
			public String toString()
			{
				return this.string.toString();
			}
		};
		
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
				
				setTaskStatus(new TaskStatus((int) (((double) currentInput / (double) numberOfInputs) * 100) + (currentProgress / numberOfInputs), MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.text"), inputName), Status.STATUS_PENDING));
			}
			
			out.flush();
			in.close();
			out.close();
			
			// Try to build the server response as JSON and check the status code
			JSONObject jsonResponse = new JSONObject(out.toString());
			int status = jsonResponse.getInt("status_code");
			
			return status == 0;
		}
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
			return false;
		}
		catch (JSONException je)
		{
			LOG.error(je.getMessage(), je);
			return false;
		}
	}
}
