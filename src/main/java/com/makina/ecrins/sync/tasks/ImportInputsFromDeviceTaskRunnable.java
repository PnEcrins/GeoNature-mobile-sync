package com.makina.ecrins.sync.tasks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.io.filefilter.RegexFileFilter;
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
import org.json.JSONException;
import org.json.JSONObject;

import com.makina.ecrins.sync.adb.ADBCommand;
import com.makina.ecrins.sync.adb.ADBCommand.Prop;
import com.makina.ecrins.sync.adb.ADBCommandException;
import com.makina.ecrins.sync.service.Status;
import com.makina.ecrins.sync.settings.AndroidSettings;
import com.makina.ecrins.sync.settings.DeviceSettings;
import com.makina.ecrins.sync.settings.LoadSettingsCallable;

/**
 * {@link AbstractTaskRunnable} implementation for fetching all inputs to be imported from a connected device.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ImportInputsFromDeviceTaskRunnable extends AbstractTaskRunnable
{
	private static final Logger LOG = Logger.getLogger(ImportInputsFromDeviceTaskRunnable.class);
	
	private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
	
	private File inputsTempDir;
	private DeviceSettings deviceSettings;
	
	private List<ApkInfo> apks = new ArrayList<ApkInfo>();
	
	@Override
	public void run()
	{
		setTaskStatus(new TaskStatus(-1, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.PENDING));
		
		try
		{
			loadDeviceSettings();
			fetchInputsFromDevice();
			
			if (uploadInputs())
			{
				setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.FINISH));
			}
			else
			{
				setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.FAILED));
			}
		}
		catch (ADBCommandException ace)
		{
			LOG.error(ace.getMessage(), ace);
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.FAILED));
		}
		finally
		{
			FileUtils.deleteQuietly(this.inputsTempDir);
		}
	}
	
	private void loadDeviceSettings()
	{
		try
		{
			deviceSettings = DeviceUtils.findLoadedDeviceSettings(
					new DeviceSettings(
							ADBCommand.getInstance().getProp(Prop.RO_PRODUCT_MANUFACTURER),
							ADBCommand.getInstance().getProp(Prop.RO_PRODUCT_MODEL),
							ADBCommand.getInstance().getProp(Prop.RO_PRODUCT_NAME),
							new AndroidSettings(
									ADBCommand.getInstance().getProp(Prop.RO_BUILD_VERSION_RELEASE),
									ADBCommand.getInstance().getBuildVersion())));
			
			LOG.debug("loadDeviceSettings: " + deviceSettings);
		}
		catch (ADBCommandException ace)
		{
			LOG.warn(ace.getMessage());
			
			deviceSettings = null;
		}
	}
	
	private boolean fetchInputsFromDevice() throws ADBCommandException
	{
		this.inputsTempDir = new File(TaskManager.getInstance().getTemporaryDirectory(), "inputs");
		this.inputsTempDir.mkdir();
		
		apks = ApkUtils.getApkInfosFromJson(new File(TaskManager.getInstance().getTemporaryDirectory(), "versions.json"));
		
		if (apks.isEmpty())
		{
			return false;
		}
		else
		{
			ADBCommand.getInstance().pull(DeviceUtils.getExternalStorageDirectory(deviceSettings) + "/" + ApkUtils.getRelativeSharedPath(apks.get(0)) + "inputs/", this.inputsTempDir.getAbsolutePath());
			
			return FileUtils.listFiles(this.inputsTempDir, new RegexFileFilter("^input_\\d+.json$"), new PrefixFileFilter(apks.get(0).getSharedUserId())).size() > 0;
		}
	}
	
	private void deleteInputFromDevice(File inputJson) throws ADBCommandException
	{
		if (ADBCommand.getInstance().getBuildVersion() > 15)
		{
			ADBCommand.getInstance().executeCommand("rm " + DeviceUtils.getExternalStorageDirectory(deviceSettings) + "/" + ApkUtils.getRelativeSharedPath(apks.get(0)) + "inputs/" + inputJson.getParentFile().getName() + "/" + inputJson.getName());
		}
		else
		{
			// uses specific service from mobile application to delete the given input
			ADBCommand.getInstance().executeCommand("am broadcast -a " + inputJson.getParentFile().getName() + ".INTENT_DELETE_INPUT -e " + inputJson.getParentFile().getName() + ".file " + inputJson.getName() + " -f 32");
		}
	}
	
	private void copyInputToUserDir(File inputJson, boolean isSynchronized)
	{
		if (inputJson.exists())
		{
			try
			{
				Input input = InputUtils.getInputFromJson(inputJson);
				File inputFile = FileUtils.getFile(TaskManager.getInstance().getUserDir(), inputJson.getParentFile().getParentFile().getName(), inputJson.getParentFile().getName(), dateFormat.format(input.getDate()), ((isSynchronized)?"ok_":"ko_") + inputJson.getName());
				FileUtils.copyFile(inputJson, inputFile);
				
				if (isSynchronized)
				{
					LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.success.text"), inputJson.getName()));
				}
				else
				{
					LOG.warn(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.failed.text"), inputJson.getName()));
				}
			}
			catch (JSONException je)
			{
				LOG.warn(je.getLocalizedMessage());
			}
			catch (ParseException pe)
			{
				LOG.warn(pe.getLocalizedMessage());
			}
			catch (IOException ioe)
			{
				LOG.warn(ioe.getLocalizedMessage());
			}
		}
		else
		{
			LOG.warn(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.notfound.text"), inputJson.getAbsolutePath()));
		}
	}
	
	private boolean uploadInputs()
	{
		boolean result = true;
		
		LOG.debug("uploadInputs : start");
		
		final DefaultHttpClient httpClient = new DefaultHttpClient();
		final HttpParams httpParameters = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, LoadSettingsCallable.getInstance().getSettings().getSyncSettings().getServerTimeout());
		//HttpConnectionParams.setSoTimeout(httpParameters, 5000);
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
		nameValuePairs.add(new BasicNameValuePair("token", LoadSettingsCallable.getInstance().getSettings().getSyncSettings().getServerToken()));
		nameValuePairs.add(new BasicNameValuePair("data", "{}"));
		
		String urlImport = 	LoadSettingsCallable.getInstance().getSettings().getSyncSettings().getServerUrl() +
				LoadSettingsCallable.getInstance().getSettings().getSyncSettings().getImportUrl();
		
		HttpPost httpPost = new HttpPost(urlImport);
		HttpResponse httpResponse = null;
		
		try
		{
			int currentInput = 0;
			
			List<String> packageNames = new ArrayList<String>();
			
			for (ApkInfo apkInfo : apks)
			{
				packageNames.add(apkInfo.getPackageName());
			}
			
			// finds all inputs as json file
			Collection<File> inputFiles = FileUtils.listFiles(this.inputsTempDir, new RegexFileFilter("^input_\\d+.json$"), new NameFileFilter(packageNames));
			int inputsSynchronized = 0;
			int inputsFailed = 0;
			
			for (File inputFile : inputFiles)
			{
				setTaskStatus(new TaskStatus((int) (((double) currentInput / (double) inputFiles.size()) * 100), MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.text"), inputFile.getName()), Status.PENDING));
				LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.text"), inputFile.getName()));
				
				try
				{
					// reads input as JSON file
					nameValuePairs.set(1, new BasicNameValuePair("data", FileUtils.readFileToString(inputFile)));
					
					httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
					httpResponse = httpClient.execute(httpPost);
					
					// checks if server response is valid
					StatusLine status = httpResponse.getStatusLine();
					
					if (status.getStatusCode() == HttpStatus.SC_OK)
					{
						// pulls content stream from response
						HttpEntity entity = httpResponse.getEntity();
						InputStream inputStream = entity.getContent();
						
						boolean isSynchronized = readInputStreamAsJson(inputFile.getName(), inputStream, entity.getContentLength(), currentInput, inputFiles.size());
						
						if (isSynchronized)
						{
							setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.finish.text"), inputFile.getName()), Status.PENDING));
							inputsSynchronized++;
						}
						else
						{
							setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.text"), inputFile.getName()), Status.FAILED));
							result = false;
							inputsFailed++;
						}
						
						deleteInputFromDevice(inputFile);
						copyInputToUserDir(inputFile, isSynchronized);
					}
					else
					{
						LOG.error(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.failed.text"), inputFile.getName()) + " (URL '" + httpPost.getURI().toString() + "', HTTP status : " + status.getStatusCode() + ")");
						
						// pulls content stream from response
						HttpEntity entity = httpResponse.getEntity();
						InputStream inputStream = entity.getContent();
						readInputStreamAsJson(inputFile.getName(), inputStream, entity.getContentLength(), currentInput, inputFiles.size());
						
						setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.text"), inputFile.getName()), Status.FAILED));
						result = false;
						inputsFailed++;
					}
				}
				catch (IOException ioe)
				{
					LOG.error(ioe.getLocalizedMessage());
					
					httpPost.abort();
					result = false;
					setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.text"), inputFile.getName()), Status.FAILED));
				}
				catch (ADBCommandException ace)
				{
					LOG.error(ace.getLocalizedMessage());
					
					httpPost.abort();
					result = false;
					setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.text"), inputFile.getName()), Status.FAILED));
				}
				finally
				{
					HttpClientUtils.closeQuietly(httpResponse);
				}
				
				currentInput++;
			}
			
			if (inputFiles.size() > 0)
			{
				if (inputsSynchronized == 1)
				{
					LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.size.one.text"), inputsSynchronized));
				}
				else if (inputsSynchronized > 1)
				{
					LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.size.other.text"), inputsSynchronized));
				}
				else
				{
					LOG.warn(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.size.none.text"));
				}
				
				if (inputsFailed == 1)
				{
					LOG.warn(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.failed.size.one.text"), inputsFailed));
				}
				else if (inputsFailed > 1)
				{
					LOG.warn(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.failed.size.other.text"), inputsFailed));
				}
			}
			else
			{
				LOG.info(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.size.none.text"));
			}
		}
		finally
		{
			httpClient.getConnectionManager().shutdown();
		}
		
		LOG.debug("uploadInputs: finish");
		
		return result;
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
				
				if (contentLength > 0)
				{
					int currentProgress =  (int) (((double) totalBytesRead / (double) contentLength) * 100);
					
					setTaskStatus(new TaskStatus((int) (((double) currentInput / (double) numberOfInputs) * 100) + (currentProgress / numberOfInputs), MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.upload.text"), inputName), Status.PENDING));
				}
			}
			
			out.flush();
			in.close();
			out.close();
			
			// Try to build the server response as JSON and check the status code
			JSONObject jsonResponse = new JSONObject(out.toString());
			int status = jsonResponse.getInt("status_code");
			String messageStatus = jsonResponse.getString("status_message");
			
			if (status != 0)
			{
				LOG.error("failed to synchronize input '" + inputName + "' [message : " + messageStatus + "]");
			}
			
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
