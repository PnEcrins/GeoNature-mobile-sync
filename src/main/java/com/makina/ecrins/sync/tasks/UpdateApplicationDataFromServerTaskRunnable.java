package com.makina.ecrins.sync.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.log4j.Logger;

import com.makina.ecrins.sync.adb.ADBCommand;
import com.makina.ecrins.sync.adb.ADBCommandException;
import com.makina.ecrins.sync.server.WebAPIClientUtils;
import com.makina.ecrins.sync.server.WebAPIClientUtils.HTTPCallback;
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
		final AtomicBoolean result = new AtomicBoolean(true);
		
		HttpClient httpClient = WebAPIClientUtils.getHttpClient(LoadSettingsCallable.getInstance().getSyncSettings().getServerTimeout());
		
		final List<ExportSettings> exportsSettings = LoadSettingsCallable.getInstance().getSyncSettings().getExportsSettings();
		final AtomicInteger increment= new AtomicInteger();
		
		for (final ExportSettings exportSettings : exportsSettings)
		{
			setTaskStatus(new TaskStatus((int) (((double) increment.get() / (double) exportsSettings.size()) * 100), MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.STATUS_PENDING));
			
			WebAPIClientUtils.httpPost(httpClient,
					LoadSettingsCallable.getInstance().getSyncSettings().getServerUrl() +
					exportSettings.getExportUrl(),
					LoadSettingsCallable.getInstance().getSyncSettings().getServerToken(),
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
									// compare file sizes between the remote file and the local file
									if (checkFileSize(Long.valueOf(httpResponse.getFirstHeader("Content-Length").getValue()), exportSettings.getExportFile()))
									{
										LOG.info("'" + exportSettings.getExportFile() + "' is already downloaded and installed");
										
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
											copyFileToDevice(localFile, exportSettings.getExportFile());
											
											setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.finish.text"), exportSettings.getExportFile()), Status.STATUS_PENDING));
										}
										else
										{
											setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.STATUS_FAILED));
											result.set(false);
										}
									}
								}
								catch (NumberFormatException nfe)
								{
									LOG.error(nfe.getLocalizedMessage());
									
									result.set(false);
									setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.STATUS_FAILED));
								}
								catch (IllegalStateException ise)
								{
									LOG.error(ise.getLocalizedMessage());
									
									result.set(false);
									setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.STATUS_FAILED));
								}
								catch (FileNotFoundException fnfe)
								{
									LOG.error(fnfe.getLocalizedMessage());
									
									result.set(false);
									setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.STATUS_FAILED));
								}
								catch (ADBCommandException ace)
								{
									LOG.error(ace.getLocalizedMessage());
									
									result.set(false);
									setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.STATUS_FAILED));
								}
								catch (IOException ioe)
								{
									LOG.error(ioe.getLocalizedMessage());
									
									result.set(false);
									setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.STATUS_FAILED));
								}
							}
							else
							{
								LOG.error("unable to download file from URL '" + httpRequestBase.getURI().toString() + "', HTTP status : " + status.getStatusCode());
								setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.STATUS_FAILED));
								
								result.set(false);
							}
						}
						
						@Override
						public void onError(Exception e)
						{
							LOG.error(e.getLocalizedMessage());
							
							result.set(false);
							setTaskStatus(new TaskStatus(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.download.text"), exportSettings.getExportFile()), Status.STATUS_FAILED));
						}
					});
			
			increment.addAndGet(1);
		}
		
		WebAPIClientUtils.shutdownHttpClient(httpClient);
		
		return result.get();
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
