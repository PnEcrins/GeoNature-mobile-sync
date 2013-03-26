package com.makina.ecrins.sync.tasks;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;
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
import com.makina.ecrins.sync.adb.ADBCommandException;
import com.makina.ecrins.sync.service.Status;
import com.makina.ecrins.sync.settings.LoadSettingsCallable;

/**
 * {@link AbstractTaskRunnable} implementation about mobile applications update :
 * <ul>
 * <li>Retrieve the list of mobile applications to install or to update from the server</li>
 * <li>for each application, check if the mobile application is installed to the connected device</li>
 * <li>for each application, check if mobile application updates are available from the server for the connected device</li>
 * </ul>
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class UpdateApplicationsFromServerTaskRunnable extends AbstractTaskRunnable
{
	private static final Logger LOG = Logger.getLogger(UpdateApplicationsFromServerTaskRunnable.class);
	
	private final List<ApkInfo> apks = new ArrayList<ApkInfo>();
	private int apkIndex;
	private int progress;
	
	private boolean result = true;
	
	@Override
	public void run()
	{
		apkIndex = 0;
		progress = 0;
		apks.clear();
		
		setTaskStatus(new TaskStatus(-1, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_PENDING));
		
		// gets all available applications informations from the server
		if (fetchLastAppsVersionsFromServer(10))
		{
			int ratio = 100 - progress;
			
			for (ApkInfo apkinfo : apks)
			{
				try
				{
					// checks if the mobile application is already installed or not
					if (checkInstalledAppFromDevice(apkinfo, ratio, 10))
					{
						// gets application informations from device
						if (fetchAppVersionFromDevice(apkinfo, ratio, 10, 10))
						{
							// check if a newer version is available or not
							if (checkInstalledAppVersion(apkinfo, ratio, 10, 20))
							{
								if (downloadLastAppFromServer(apkinfo, ratio, 30, 30))
								{
									if (installAppToDevice(apkinfo, ratio, 10, 60, true))
									{
										// everything is OK, now check if installation was successful
										if (fetchAppVersionFromDevice(apkinfo, ratio, 10, 70))
										{
											if (checkInstalledAppVersion(apkinfo, ratio, 10, 80))
											{
												
											}
										}
									}
								}
							}
						}
					}
					else
					{
						// the mobile application was not found on the connected device, so install it
						LOG.info(apkinfo.getPackageName() + " is not installed");
						
						if (downloadLastAppFromServer(apkinfo, ratio, 50, 10))
						{
							if (installAppToDevice(apkinfo, ratio, 10, 60, false))
							{
								// everything is OK, now check if installation was successful
								if (fetchAppVersionFromDevice(apkinfo, ratio, 10, 70))
								{
									if (checkInstalledAppVersion(apkinfo, ratio, 10, 80))
									{
										
									}
								}
							}
						}
					}
				}
				catch (TaskException te)
				{
					LOG.error(te.getLocalizedMessage(), te);
					
					progress = computeProgress(apkIndex, apks.size(), 1, 1, ratio, 100, 0);
					result = false;
					setTaskStatus(new TaskStatus(progress, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FAILED));
				}
				
				apkIndex++;
			}
		}
		else
		{
			LOG.warn("nothing to check");
		}
		
		progress = 100;
		
		if (result)
		{
			setTaskStatus(new TaskStatus(progress, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FINISH));
		}
		else
		{
			setTaskStatus(new TaskStatus(progress, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FAILED));
		}
	}
	
	private boolean fetchLastAppsVersionsFromServer(final int factor)
	{
		final DefaultHttpClient httpClient = new DefaultHttpClient();
		final HttpParams httpParameters = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, LoadSettingsCallable.getInstance().getSyncSettings().getServerTimeout());
		//HttpConnectionParams.setSoTimeout(httpParameters, 5000);
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		nameValuePairs.add(new BasicNameValuePair("token", LoadSettingsCallable.getInstance().getSyncSettings().getServerToken()));
		
		try
		{
			HttpPost httpPost = new HttpPost(
					LoadSettingsCallable.getInstance().getSyncSettings().getServerUrl() +
					LoadSettingsCallable.getInstance().getSyncSettings().getAppUpdateVersionUrl());
			
			HttpResponse httpResponse = null;
			
			try
			{
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				httpResponse = httpClient.execute(httpPost);
				
				// checks if server response is valid
				StatusLine status = httpResponse.getStatusLine();
				
				if (status.getStatusCode() == HttpStatus.SC_OK)
				{
					// pulls content stream from response
					final HttpEntity entity = httpResponse.getEntity();
					InputStream inputStream = entity.getContent();
					
					FileOutputStream fos = new FileOutputStream(new File(TaskManager.getInstance().getTemporaryDirectory(), "versions.json"));
					
					IOUtils.copy(inputStream, new CountingOutputStream(fos)
					{
						@Override
						protected void afterWrite(int n) throws IOException
						{
							super.afterWrite(n);
							
							progress = computeProgress(0, 1, getCount(), entity.getContentLength(), 100, factor, 0);
							setTaskStatus(new TaskStatus(progress, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_PENDING));
						}
					});
					
					inputStream.close();
					fos.close();
					
					apks.addAll(ApkUtils.getApkInfosFromJson(new File(TaskManager.getInstance().getTemporaryDirectory(), "versions.json")));
					
					progress = factor;
					setTaskStatus(new TaskStatus(progress, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_PENDING));
				}
				else
				{
					LOG.error("unable to download file from URL '" + httpPost.getURI().toString() + "', HTTP status : " + status.getStatusCode());
					
					progress = 100;
					this.result = false;
					setTaskStatus(new TaskStatus(progress, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FAILED));
				}
			}
			catch (IOException ioe)
			{
				LOG.error(ioe.getMessage(), ioe);
				
				httpPost.abort();
				progress = 100;
				this.result = false;
				setTaskStatus(new TaskStatus(progress, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FAILED));
			}
			finally
			{
				HttpClientUtils.closeQuietly(httpResponse);
			}
		}
		finally
		{
			httpClient.getConnectionManager().shutdown();
		}
		
		return !apks.isEmpty();
	}
	
	/**
	 * Returns <code>true</code> if the given {@link ApkInfo#getPackageName()} is currently installed or not.
	 * @param apkInfo {@link ApkInfo} instance to check
	 * @param factor factor as percentage to apply for the current progress
	 * @return <code>true</code> if the given {@link ApkInfo#getPackageName()} is currently installed, <code>false</code> otherwise
	 * @throws TaskException
	 */
	private boolean checkInstalledAppFromDevice(ApkInfo apkInfo, int ratio, int factor) throws TaskException
	{
		try
		{
			List<String> result = ADBCommand.getInstance().executeCommand("pm list packages | grep " + apkInfo.getPackageName());
			progress = computeProgress(apkIndex, apks.size(), 1, 1, ratio, factor, 0);
			setTaskStatus(new TaskStatus(progress, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_PENDING));
			
			return !result.isEmpty() && StringUtils.substringAfter(result.get(0), ":").equals(apkInfo.getPackageName());
		}
		catch (ADBCommandException ace)
		{
			throw new TaskException(ace.getLocalizedMessage(), ace);
		}
	}
	
	private boolean fetchAppVersionFromDevice(ApkInfo apkInfo, int ratio, int factor, int offset) throws TaskException
	{
		try
		{
			// about flag -f 32, see: http://developer.android.com/reference/android/content/Intent.html#FLAG_INCLUDE_STOPPED_PACKAGES
			if (!ADBCommand.getInstance().executeCommand("am broadcast -a " + apkInfo.getPackageName() + ".INTENT_PACKAGE_INFO -f 32").isEmpty())
			{
				ADBCommand.getInstance().pull(ApkUtils.getExternalStorageDirectory() + "Android/data/" + apkInfo.getSharedUserId() + "/version.json", TaskManager.getInstance().getTemporaryDirectory().getAbsolutePath());
				JSONObject versionJson = new JSONObject(FileUtils.readFileToString(new File(TaskManager.getInstance().getTemporaryDirectory(), "version.json")));
				
				progress = computeProgress(apkIndex, apks.size(), 1, 1, ratio, factor, offset);
				setTaskStatus(new TaskStatus(progress, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_PENDING));
				
				return versionJson.has("package") && versionJson.has("versionCode");
			}
			else
			{
				return false;
			}
		}
		catch (ADBCommandException ace)
		{
			throw new TaskException(ace.getLocalizedMessage(), ace);
		}
		catch (FileNotFoundException fnfe)
		{
			throw new TaskException(fnfe.getLocalizedMessage(), fnfe);
		}
		catch (IOException ioe)
		{
			throw new TaskException(ioe.getLocalizedMessage(), ioe);
		}
		catch (JSONException je)
		{
			throw new TaskException(je.getLocalizedMessage(), je);
		}
	}
	
	private boolean checkInstalledAppVersion(ApkInfo apkInfo, int ratio, int factor, int offset) throws TaskException
	{
		try
		{
			ApkInfo apkInfoFromDevice = new ApkInfo(new JSONObject(FileUtils.readFileToString(new File(TaskManager.getInstance().getTemporaryDirectory(), "version.json"))));
			
			LOG.info("installed mobile application : " + apkInfoFromDevice.getPackageName() + ", version : " + apkInfoFromDevice.getVersionName() + " (" + apkInfoFromDevice.getVersionCode() + ")");
			
			if (apkInfoFromDevice.getVersionCode() < apkInfo.getVersionCode())
			{
				LOG.info("found a new version of " + apkInfoFromDevice.getPackageName());
				
				progress = computeProgress(apkIndex, apks.size(), 1, 1, ratio, factor, offset);
				setTaskStatus(new TaskStatus(progress, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_PENDING));
				
				return true;
			}
			else
			{
				LOG.info(apkInfoFromDevice.getPackageName() + " is up to date");
				
				progress = computeProgress(apkIndex, apks.size(), 1, 1, ratio, factor, offset);
				setTaskStatus(new TaskStatus(progress, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_PENDING));
				
				return false;
			}
		}
		catch (FileNotFoundException fnfe)
		{
			throw new TaskException(fnfe.getLocalizedMessage(), fnfe);
		}
		catch (JSONException je)
		{
			throw new TaskException(je.getLocalizedMessage(), je);
		}
		catch (IOException ioe)
		{
			throw new TaskException(ioe.getLocalizedMessage(), ioe);
		}
	}
	
	private boolean downloadLastAppFromServer(final ApkInfo apkInfo, final int ratio, final int factor, final int offset)
	{
		boolean result = true;
		
		final DefaultHttpClient httpClient = new DefaultHttpClient();
		final HttpParams httpParameters = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, LoadSettingsCallable.getInstance().getSyncSettings().getServerTimeout());
		//HttpConnectionParams.setSoTimeout(httpParameters, 5000);
		
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
		nameValuePairs.add(new BasicNameValuePair("token", LoadSettingsCallable.getInstance().getSyncSettings().getServerToken()));
		
		try
		{
			HttpPost httpPost = new HttpPost(
					LoadSettingsCallable.getInstance().getSyncSettings().getServerUrl() +
					LoadSettingsCallable.getInstance().getSyncSettings().getAppUpdateDownloadUrl() +
					"/" + apkInfo.getApkName() + "/");
			
			HttpResponse httpResponse = null;
			
			try
			{
				httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
				httpResponse = httpClient.execute(httpPost);

				// checks if server response is valid
				StatusLine status = httpResponse.getStatusLine();
				
				if (status.getStatusCode() == HttpStatus.SC_OK)
				{
					// pulls content stream from response
					final HttpEntity entity = httpResponse.getEntity();
					
					InputStream inputStream = entity.getContent();
					FileOutputStream fos = new FileOutputStream(new File(TaskManager.getInstance().getTemporaryDirectory(), apkInfo.getApkName()));
					
					IOUtils.copy(inputStream, new CountingOutputStream(fos)
					{
						@Override
						protected void afterWrite(int n) throws IOException
						{
							super.afterWrite(n);
							
							progress = computeProgress(apkIndex, apks.size(), getCount(), entity.getContentLength(), ratio, factor, offset);
							setTaskStatus(new TaskStatus(progress, MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.download.text"), apkInfo.getApkName()), Status.STATUS_PENDING));
						}
					});
					
					inputStream.close();
					fos.close();
				}
				else
				{
					LOG.error("unable to download file from URL '" + httpPost.getURI().toString() + "', HTTP status : " + status.getStatusCode());
					
					progress = computeProgress(apkIndex, apks.size(), 1, 1, ratio, factor, offset);
					setTaskStatus(new TaskStatus(progress, MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.download.text"), apkInfo.getApkName()), Status.STATUS_FAILED));
					this.result = false;
					result = false;
				}
			}
			catch (IOException ioe)
			{
				LOG.error(ioe.getMessage(), ioe);
				
				progress = computeProgress(apkIndex, apks.size(), 1, 1, ratio, factor, offset);
				setTaskStatus(new TaskStatus(progress, MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.download.text"), apkInfo.getApkName()), Status.STATUS_FAILED));
				this.result = false;
				result = false;
			}
			finally
			{
				HttpClientUtils.closeQuietly(httpResponse);
			}
		}
		finally
		{
			httpClient.getConnectionManager().shutdown();
		}
		
		return result;
	}
	
	private boolean installAppToDevice(ApkInfo apkInfo, int ratio, int factor, int offset, boolean keepData)
	{
		File apkFile = new File(TaskManager.getInstance().getTemporaryDirectory(), apkInfo.getApkName());
		
		if (apkFile.exists())
		{
			progress = computeProgress(apkIndex, apks.size(), 0, 1, ratio, factor, offset);
			setTaskStatus(new TaskStatus(progress, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_PENDING));
			
			try
			{
				boolean result = ADBCommand.getInstance().install(apkFile.getAbsolutePath(), keepData);
				
				if (result)
				{
					LOG.info(apkInfo.getApkName() + " successfully installed");
					
					progress = computeProgress(apkIndex, apks.size(), 1, 1, ratio, factor, offset);
					setTaskStatus(new TaskStatus(progress, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_PENDING));
				}
				else
				{
					LOG.error("failed to install " + apkInfo.getApkName());
					
					progress = computeProgress(apkIndex, apks.size(), 1, 1, ratio, factor, offset);
					setTaskStatus(new TaskStatus(progress, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_FAILED));
					this.result = false;
				}
				
				return result;
			}
			catch (ADBCommandException ace)
			{
				LOG.error(ace.getMessage(), ace);
				
				progress = computeProgress(apkIndex, apks.size(), 1, 1, ratio, factor, offset);
				setTaskStatus(new TaskStatus(progress, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_FAILED));
				this.result = false;
				
				return false;
			}
		}
		else
		{
			LOG.error(apkFile.getAbsolutePath() + " not found");
			
			progress = computeProgress(apkIndex, apks.size(), 1, 1, ratio, factor, offset);
			setTaskStatus(new TaskStatus(progress, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_FAILED));
			this.result = false;
			
			return false;
		}
	}
	
	/**
	 * Computes the current progress (as percentage) according to given parameters.
	 * @param mainProgress
	 * @param mainProgressSize
	 * @param currentProgress
	 * @param currentProgressSize
	 * @param ratio current ratio to apply for the current progress
	 * @param factor as percentage for the current progress
	 * @param offset as percentage for the current progress
	 * @return the progress as percentage
	 */
	private int computeProgress(final int mainProgress, final int mainProgressSize, final long currentProgress, final long currentProgressSize, final int ratio, final int factor, final int offset)
	{
		return (int) ((((double) factor / 100) * ((double) ratio / (double) mainProgressSize)) * 
				(Long.valueOf(currentProgress).doubleValue() / Long.valueOf(currentProgressSize).doubleValue()) +
				(100 - ratio) +
				(((double) mainProgress / (double) mainProgressSize) * ratio) +
				(((double) offset / 100) * ((double) ratio / (double) mainProgressSize)));
	}
}
