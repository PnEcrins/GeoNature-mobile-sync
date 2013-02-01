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
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.makina.ecrins.sync.adb.ADBCommand;
import com.makina.ecrins.sync.service.Status;
import com.makina.ecrins.sync.settings.LoadSettingsCallable;

/**
 * {@link AbstractTaskRunnable} implementation about mobile application update :
 * <ul>
 * <li>check if the mobile application is installed to the connected device</li>
 * <li>check if mobile application updates are available from the server for the connected device</li>
 * </ul>
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class UpdateApplicationFromServerTaskRunnable extends AbstractTaskRunnable
{
	private static final Logger LOG = Logger.getLogger(UpdateApplicationFromServerTaskRunnable.class);
	
	private File tempDir;
	private String apkName;
	
	@Override
	public void run()
	{
		setTaskStatus(new TaskStatus(-1, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_PENDING));
		
		this.tempDir = new File(FileUtils.getTempDirectory(), "sync_update" + Long.toString(System.currentTimeMillis()));
		this.tempDir.mkdir();
		
		try
		{
			FileUtils.forceDeleteOnExit(tempDir);
			
			// checks if the mobile application is already installed or not
			if (checkInstalledAppFromDevice())
			{
				setTaskStatus(new TaskStatus(10, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_PENDING));
				
				if (fetchAppVersionFromDevice())
				{
					setTaskStatus(new TaskStatus(15, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_PENDING));
					
					if (fetchLastAppVersionFromServer(15))
					{
						setTaskStatus(new TaskStatus(30, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_PENDING));
						
						// check if a newer version is available or not
						if (checkInstalledAppVersion())
						{
							setTaskStatus(new TaskStatus(40, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_PENDING));
							
							if (downloadLastAppFromServer(40))
							{
								setTaskStatus(new TaskStatus(80, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_PENDING));
								
								if (installAppToDevice(true))
								{
									// everything is ok, now check if installation was successful
									setTaskStatus(new TaskStatus(90, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_PENDING));
									
									if (fetchAppVersionFromDevice())
									{
										setTaskStatus(new TaskStatus(95, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_PENDING));
										
										if (checkInstalledAppVersion())
										{
											setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_FAILED));
										}
										else
										{
											setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.finish.text"), Status.STATUS_FINISH));
										}
									}
									else
									{
										setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_FAILED));
									}
								}
								else
								{
									setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_FAILED));
								}
							}
						}
						else
						{
							setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FINISH));
						}
					}
				}
				else
				{
					setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FAILED));
				}
			}
			else
			{
				setTaskStatus(new TaskStatus(10, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_PENDING));
				
				// the mobile application was not found on the connected device, so install it
				if (fetchLastAppVersionFromServer(10))
				{
					setTaskStatus(new TaskStatus(20, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_PENDING));
					
					if (downloadLastAppFromServer(20))
					{
						setTaskStatus(new TaskStatus(40, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_PENDING));
						
						if (installAppToDevice(false))
						{
							// everything is ok, now check if installation was successful
							setTaskStatus(new TaskStatus(50, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_PENDING));
							
							if (fetchAppVersionFromDevice())
							{
								setTaskStatus(new TaskStatus(60, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_PENDING));
								
								if (checkInstalledAppVersion())
								{
									setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_FAILED));
								}
								else
								{
									setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.finish.text"), Status.STATUS_FINISH));
								}
							}
							else
							{
								setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_FAILED));
							}
						}
						else
						{
							setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_FAILED));
						}
					}
				}
			}
		}
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FAILED));
		}
		catch (InterruptedException ie)
		{
			LOG.error(ie.getMessage(), ie);
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FAILED));
		}
		finally
		{
			FileUtils.deleteQuietly(this.tempDir);
		}
	}
	
	private boolean checkInstalledAppFromDevice() throws IOException, InterruptedException
	{
		List<String> result = ADBCommand.getInstance().executeCommand("pm list packages | grep com.makina.ecrins.poc");
		
		return !result.isEmpty() && StringUtils.substringAfter(result.get(0), ":").equals("com.makina.ecrins.poc");
	}
	
	private boolean fetchAppVersionFromDevice()
	{
		try
		{
			// about flag -f 32, see: http://developer.android.com/reference/android/content/Intent.html#FLAG_INCLUDE_STOPPED_PACKAGES
			if (!ADBCommand.getInstance().executeCommand("am broadcast -a com.makina.ecrins.poc.INTENT_PACKAGE_INFO -f 32").isEmpty())
			{
				ADBCommand.getInstance().pull(com.makina.ecrins.sync.adb.FileUtils.getExternalStorageDirectory() + "Android/data/" + "com.makina.ecrins.poc" + "/version.json", this.tempDir.getAbsolutePath());
				JSONObject versionJson = new JSONObject(FileUtils.readFileToString(new File(this.tempDir, "version.json")));
				
				return versionJson.has("package") && versionJson.has("versionCode");
			}
			else
			{
				return false;
			}
		}
		catch (FileNotFoundException fnfe)
		{
			LOG.error(fnfe.getMessage(), fnfe);
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FAILED));
			
			return false;
		}
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FAILED));
			
			return false;
		}
		catch (InterruptedException ie)
		{
			LOG.error(ie.getMessage(), ie);
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FAILED));
			
			return false;
		}
		catch (JSONException je)
		{
			LOG.error(je.getMessage(), je);
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FAILED));
			
			return false;
		}
	}
	
	private boolean fetchLastAppVersionFromServer(final int currentProgress)
	{
		final DefaultHttpClient httpClient = new DefaultHttpClient();
		final HttpParams httpParameters = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
		HttpConnectionParams.setSoTimeout(httpParameters, 5000);
		
		try
		{
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			nameValuePairs.add(new BasicNameValuePair("token", LoadSettingsCallable.getInstance().getJsonSettings().getJSONObject(LoadSettingsCallable.KEY_SYNC).getString(LoadSettingsCallable.KEY_TOKEN)));
			
			String urlVersion = LoadSettingsCallable.getInstance().getJsonSettings().getJSONObject(LoadSettingsCallable.KEY_SYNC).getString(LoadSettingsCallable.KEY_SERVER_URL) +
								LoadSettingsCallable.getInstance().getJsonSettings().getJSONObject(LoadSettingsCallable.KEY_SYNC).getJSONObject(LoadSettingsCallable.KEY_APP_UPDATE).get(LoadSettingsCallable.KEY_APP_UPDATE_VERSION_URL);
			HttpPost httpPost = new HttpPost(urlVersion);
			
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse httpResponse = httpClient.execute(httpPost);
			
			// checks if server response is valid
			StatusLine status = httpResponse.getStatusLine();
			
			if (status.getStatusCode() == HttpStatus.SC_OK)
			{
				// pulls content stream from response
				HttpEntity entity = httpResponse.getEntity();
				InputStream inputStream = entity.getContent();
				
				FileOutputStream fos = new FileOutputStream(new File(this.tempDir, "last_version.json"));
				final long contentLength = entity.getContentLength();
				
				IOUtils.copy(inputStream, new CountingOutputStream(fos)
				{
					@Override
					protected void afterWrite(int n) throws IOException
					{
						super.afterWrite(n);
						
						setTaskStatus(new TaskStatus(currentProgress + (int) (((double) getCount() / (double) contentLength) * currentProgress), ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_PENDING));
					}
				});
				
				inputStream.close();
				fos.close();
				
				JSONObject appLastVersionJson = new JSONObject(FileUtils.readFileToString(new File(this.tempDir, "last_version.json")));
				apkName = appLastVersionJson.getString("package") + "-" + appLastVersionJson.getString("versionName") + ".apk"; 
				
				return appLastVersionJson.has("package") && appLastVersionJson.has("versionCode");
			}
			else
			{
				LOG.error("unable to download file from URL '" + httpPost.getURI().toString() + "', HTTP status : " + status.getStatusCode());
				setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FAILED));
				
				return false;
			}
		}
		catch (FileNotFoundException fnfe)
		{
			LOG.error(fnfe.getMessage(), fnfe);
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FAILED));
			
			return false;
		}
		catch (JSONException je)
		{
			LOG.error(je.getMessage(), je);
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FAILED));
			
			return false;
		}
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FAILED));
			
			return false;
		}
	}
	
	private boolean checkInstalledAppVersion()
	{
		try
		{
			JSONObject appVersionJson = new JSONObject(FileUtils.readFileToString(new File(this.tempDir, "version.json")));
			JSONObject appLastVersionJson = new JSONObject(FileUtils.readFileToString(new File(this.tempDir, "last_version.json")));
			
			LOG.info("installed mobile application : " + appVersionJson.getString("package") + ", version : " + appVersionJson.getString("versionName") + " (" + appVersionJson.getInt("versionCode") + ")");
			
			if (appVersionJson.getInt("versionCode") < appLastVersionJson.getInt("versionCode"))
			{
				LOG.info("found a new version of " + appVersionJson.getString("package"));
				
				return true;
			}
			else
			{
				LOG.info(appVersionJson.getString("package") + " is up to date");
				
				return false;
			}
		}
		catch (FileNotFoundException fnfe)
		{
			LOG.error(fnfe.getMessage(), fnfe);
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FAILED));
			
			return false;
		}
		catch (JSONException je)
		{
			LOG.error(je.getMessage(), je);
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FAILED));
			
			return false;
		}
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
			setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.check.update.text"), Status.STATUS_FAILED));
			
			return false;
		}
	}
	
	private boolean downloadLastAppFromServer(final int currentProgress)
	{
		final DefaultHttpClient httpClient = new DefaultHttpClient();
		final HttpParams httpParameters = httpClient.getParams();
		HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
		HttpConnectionParams.setSoTimeout(httpParameters, 5000);
		
		try
		{
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			nameValuePairs.add(new BasicNameValuePair("token", LoadSettingsCallable.getInstance().getJsonSettings().getJSONObject(LoadSettingsCallable.KEY_SYNC).getString(LoadSettingsCallable.KEY_TOKEN)));
			
			String urlVersion = LoadSettingsCallable.getInstance().getJsonSettings().getJSONObject(LoadSettingsCallable.KEY_SYNC).getString(LoadSettingsCallable.KEY_SERVER_URL) +
					LoadSettingsCallable.getInstance().getJsonSettings().getJSONObject(LoadSettingsCallable.KEY_SYNC).getJSONObject(LoadSettingsCallable.KEY_APP_UPDATE).get(LoadSettingsCallable.KEY_APP_UPDATE_DOWNLOAD_URL);
			
			HttpPost httpPost = new HttpPost(urlVersion);
			
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			HttpResponse httpResponse = httpClient.execute(httpPost);

			// checks if server response is valid
			StatusLine status = httpResponse.getStatusLine();
			
			if (status.getStatusCode() == HttpStatus.SC_OK)
			{
				// pulls content stream from response
				HttpEntity entity = httpResponse.getEntity();
				InputStream inputStream = entity.getContent();
				
				FileOutputStream fos = new FileOutputStream(new File(this.tempDir, apkName));
				
				final long contentLength = entity.getContentLength();
				
				IOUtils.copy(inputStream, new CountingOutputStream(fos)
				{
					@Override
					protected void afterWrite(int n) throws IOException
					{
						super.afterWrite(n);
						
						setTaskStatus(new TaskStatus(currentProgress + (int) (((double) getCount() / (double) contentLength) * currentProgress), MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.download.text"), apkName), Status.STATUS_PENDING));
					}
				});
				
				inputStream.close();
				fos.close();
				
				return true;
			}
			else
			{
				LOG.error("unable to download file from URL '" + httpPost.getURI().toString() + "', HTTP status : " + status.getStatusCode());
				setTaskStatus(new TaskStatus(100, MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.download.text"), apkName), Status.STATUS_FAILED));
				
				return false;
			}
		}
		catch (JSONException je)
		{
			LOG.error(je.getMessage(), je);
			setTaskStatus(new TaskStatus(100, MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.download.text"), apkName), Status.STATUS_FAILED));
			
			return false;
		}
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
			setTaskStatus(new TaskStatus(100, MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.download.text"), apkName), Status.STATUS_FAILED));
			
			return false;
		}
	}
	
	private boolean installAppToDevice(boolean keepData)
	{
		File apkFile = new File(this.tempDir, apkName);
		
		if (apkFile.exists())
		{
			try
			{
				return ADBCommand.getInstance().install(apkFile.getAbsolutePath(), keepData);
			}
			catch (InterruptedException ie)
			{
				LOG.error(ie.getMessage(), ie);
				setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_FAILED));
				
				return false;
			}
			catch (IOException ioe)
			{
				LOG.error(ioe.getMessage(), ioe);
				setTaskStatus(new TaskStatus(100, ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.update.text"), Status.STATUS_FAILED));
				
				return false;
			}
		}
		else
		{
			LOG.error(apkFile.getAbsolutePath() + " not found");
			
			return false;
		}
	}
}
