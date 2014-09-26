package com.makina.ecrins.sync.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import com.makina.ecrins.sync.adb.ADBCommand;
import com.makina.ecrins.sync.adb.ADBCommand.Prop;
import com.makina.ecrins.sync.adb.ADBCommandException;
import com.makina.ecrins.sync.server.WebAPIClientUtils;
import com.makina.ecrins.sync.server.WebAPIClientUtils.HTTPCallback;
import com.makina.ecrins.sync.service.Status;
import com.makina.ecrins.sync.settings.AndroidSettings;
import com.makina.ecrins.sync.settings.DeviceSettings;
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
	private DeviceSettings deviceSettings;
	
	@Override
	public void run()
	{
		setTaskStatus(new TaskStatus(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.PENDING));
		
		if (getApkInfo())
		{
			loadDeviceSettings();
			
			if (downloadDataFromServer())
			{
				setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.FINISH));
			}
			else
			{
				setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.FAILED));
			}
		}
		else
		{
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.FAILED));
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
	
	private boolean checkFileLastModified(String headerLastModified, String remoteName)
	{
		boolean check = false;
		
		if (StringUtils.isNotBlank(headerLastModified))
		{
			try
			{
				final Date remoteLastModified = DateUtils.parseDate(headerLastModified);
				final Date localFileLastModified = ADBCommand.getInstance().getFileLastModified(getDeviceFilePath(remoteName, false));
				
				LOG.debug("remoteName: " + remoteName + ", localFileLastModified: " + localFileLastModified + ", remoteLastModified: " + remoteLastModified);
				
				check = (remoteLastModified == null) || (!remoteLastModified.after(localFileLastModified));
			}
			catch (ADBCommandException ace)
			{
				LOG.debug(ace.getMessage());
			}
		}
		
		return check;
	}
	
	private boolean downloadDataFromServer()
	{
		final AtomicBoolean result = new AtomicBoolean(true);
		
		HttpClient httpClient = WebAPIClientUtils.getHttpClient(LoadSettingsCallable.getInstance().getSettings().getSyncSettings().getServerTimeout());
		
		final List<ExportSettings> exportsSettings = LoadSettingsCallable.getInstance().getSettings().getSyncSettings().getExportsSettings();
		final AtomicInteger increment= new AtomicInteger();
		
		for (final ExportSettings exportSettings : exportsSettings)
		{
			setTaskStatus(new TaskStatus((int) (((double) increment.get() / (double) exportsSettings.size()) * 100), MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.PENDING));
			LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()));
			
			WebAPIClientUtils.httpPost(httpClient,
					LoadSettingsCallable.getInstance().getSettings().getSyncSettings().getServerUrl() +
					exportSettings.getExportUrl(),
					LoadSettingsCallable.getInstance().getSettings().getSyncSettings().getServerToken(),
					true,
					new HTTPCallback()
					{
						@Override
						public void onResponse(HttpRequestBase httpRequestBase, HttpResponse httpResponse)
						{
							// checks if server response is valid
							StatusLine status = httpResponse.getStatusLine();
							
							if (status.getStatusCode() == HttpStatus.SC_OK)
							{
								try
								{
									// check the last modified date between the remote file and the local file
									if (httpResponse.containsHeader("Last-Modified") && checkFileLastModified(httpResponse.getFirstHeader("Last-Modified").getValue(), exportSettings.getExportFile()))
									{
										LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.uptodate.text"), exportSettings.getExportFile()));
										
										httpRequestBase.abort();
									}
									else
									{
										// pulls content stream from response
										HttpEntity entity = httpResponse.getEntity();
										InputStream inputStream = entity.getContent();
										
										File localFile = new File(TaskManager.getInstance().getTemporaryDirectory(), exportSettings.getExportFile());
										FileUtils.touch(localFile);
										
										if (copyInputStream(exportSettings.getExportFile(), inputStream, new FileOutputStream(localFile), entity.getContentLength(), increment.get(), exportsSettings.size()))
										{
											setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.copytodevice.text"), exportSettings.getExportFile()), Status.PENDING));
											copyFileToDevice(localFile, exportSettings.getExportFile());
											setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.finish.text"), exportSettings.getExportFile()), Status.PENDING));
											LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.finish.text"), exportSettings.getExportFile()));
										}
										else
										{
											setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.FAILED));
											LOG.error(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.fail.text"), exportSettings.getExportFile()));
											result.set(false);
										}
										
										// ensure that the response body is fully consumed
										EntityUtils.consume(entity);
									}
								}
								catch (NumberFormatException nfe)
								{
									LOG.error(nfe.getLocalizedMessage());
									
									result.set(false);
									setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.FAILED));
								}
								catch (IllegalStateException ise)
								{
									LOG.error(ise.getLocalizedMessage());
									
									result.set(false);
									setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.FAILED));
								}
								catch (FileNotFoundException fnfe)
								{
									LOG.error(fnfe.getLocalizedMessage());
									
									result.set(false);
									setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.FAILED));
								}
								catch (ADBCommandException ace)
								{
									LOG.error(ace.getLocalizedMessage());
									
									result.set(false);
									setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.FAILED));
								}
								catch (IOException ioe)
								{
									LOG.error(ioe.getLocalizedMessage());
									
									result.set(false);
									setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.FAILED));
								}
							}
							else
							{
								LOG.error(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()) + " (URL '" + httpRequestBase.getURI().toString() + "', HTTP status : " + status.getStatusCode() + ")");
								setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.FAILED));
								
								result.set(false);
							}
						}
						
						@Override
						public void onError(Exception e)
						{
							LOG.error(e.getLocalizedMessage());
							
							result.set(false);
							setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.FAILED));
						}
					});
			
			increment.addAndGet(1);
		}
		
		return result.get();
	}
	
	private String getDeviceFilePath(String remoteName, boolean useDefaultExternalStorage)
	{
		if (useDefaultExternalStorage)
		{
			return DeviceUtils.getDefaultExternalStorageDirectory(deviceSettings) + "/" + ApkUtils.getRelativeSharedPath(apkInfo) + remoteName;
		}
		else
		{
			return DeviceUtils.getExternalStorageDirectory(deviceSettings) + "/" + ApkUtils.getRelativeSharedPath(apkInfo) + remoteName;
		}
	}
	
	private void copyFileToDevice(File localFile, String remoteName) throws ADBCommandException
	{
		LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.copytodevice.text"), localFile.getName()));
		
		if (DeviceUtils.getExternalStorageDirectory(deviceSettings).equals(DeviceUtils.getDefaultExternalStorageDirectory(deviceSettings)))
		{
			ADBCommand.getInstance().push(localFile.getAbsolutePath(), getDeviceFilePath(remoteName, false));
		}
		else
		{
			// uses specific service from mobile application to move the given file to the right place (use the external storage path)
			// The direct copy to the external storage path may fail for some devices (i.e. Permission denied) 
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
				
				setTaskStatus(new TaskStatus((int) (((double) currentExport / (double) numberOfExports) * 100) + (currentProgress / numberOfExports), MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), inputName), Status.PENDING));
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
