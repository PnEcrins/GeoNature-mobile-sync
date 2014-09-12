package com.makina.ecrins.sync.settings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import com.makina.ecrins.sync.server.WebAPIClientUtils;
import com.makina.ecrins.sync.server.WebAPIClientUtils.HTTPCallback;

/**
 * <code>Callable</code> implementation for loading application global settings as {@link SyncSettings}.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class LoadSettingsCallable implements Callable<Settings>
{
	public static final String SETTINGS_FILE = "settings.json";
	
	private static final Logger LOG = Logger.getLogger(LoadSettingsCallable.class);
	
	private Settings settings;
	
	private LoadSettingsCallable()
	{
		settings = null;
	}
	
	public static LoadSettingsCallable getInstance()
	{
		return LoadSettingsCallableHolder.instance;
	}
	
	public Settings getSettings()
	{
		return settings;
	}

	@Override
	public Settings call() throws Exception
	{
		LOG.info(
				MessageFormat.format(
						ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.loading.text"),
						SETTINGS_FILE));
		
		File rootDirectory = new File(URLDecoder.decode(getClass().getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8")).getParentFile();
		
		LOG.info(
				MessageFormat.format(
						ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.path.text"),
						rootDirectory.getAbsolutePath()));
		
		File jsonSettingsFile = new File(rootDirectory, SETTINGS_FILE);
		
		// load 'settings.json' from app installation directory or load the default 'settings.json' in case of errors
		loadJsonSettings(jsonSettingsFile);
		// try to update 'settings.json' from server
		updateJsonSettingsFromServer(jsonSettingsFile);
		// reload 'settings.json' from app installation directory or load the default 'settings.json' in case of errors
		loadJsonSettings(new File(rootDirectory, SETTINGS_FILE));
		
		LOG.info(
				MessageFormat.format(
						ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.server.text"),
						LoadSettingsCallable.getInstance().getSettings().getSyncSettings().getServerUrl()));
		
		return settings;
	}
	
	private void updateJsonSettingsFromServer(final File jsonSettingsFile)
	{
		final File tempDir = new File(FileUtils.getTempDirectory(), "sync_settings_" + Long.toString(System.currentTimeMillis()));
		tempDir.mkdir();
		
		final HttpClient httpClient = WebAPIClientUtils.getHttpClient(LoadSettingsCallable.getInstance().getSettings().getSyncSettings().getServerTimeout());
		WebAPIClientUtils.httpPost(
				httpClient,
				LoadSettingsCallable.getInstance().getSettings().getSyncSettings().getServerUrl() +
				LoadSettingsCallable.getInstance().getSettings().getSyncSettings().getSettingsUrl(),
				LoadSettingsCallable.getInstance().getSettings().getSyncSettings().getServerToken(),
				new HTTPCallback()
				{
					@Override
					public void onResponse(HttpRequestBase httpRequestBase, HttpResponse httpResponse)
					{
						// checks if server response is valid
						final StatusLine status = httpResponse.getStatusLine();
						
						if (status.getStatusCode() == HttpStatus.SC_OK)
						{
							// pulls content stream from response
							final HttpEntity entity = httpResponse.getEntity();
							
							try
							{
								FileUtils.copyInputStreamToFile(entity.getContent(), new File(tempDir, "settings.json"));
								
								// do nothing if we have the same file
								if (FileUtils.contentEquals(new File(tempDir, "settings.json"), jsonSettingsFile))
								{
									LOG.info(
											MessageFormat.format(
													ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.update.uptodate.text"),
													SETTINGS_FILE));
								}
								else
								{
									FileUtils.copyFile(new File(tempDir, "settings.json"), jsonSettingsFile);
									
									LOG.info(
											MessageFormat.format(
													ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.update.success.text"),
													SETTINGS_FILE));
								}
							}
							catch (IOException ioe)
							{
								LOG.error(
										MessageFormat.format(
												ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.update.failed.text"),
												SETTINGS_FILE) + ": " + ioe.getMessage());
							}
						}
						else
						{
							LOG.error(
									MessageFormat.format(
											ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.update.failed.text"),
											SETTINGS_FILE) + " (" + status.getStatusCode() + ")");
						}
						
						WebAPIClientUtils.shutdownHttpClient(httpClient);
						
						if (tempDir != null)
						{
							FileDeleteStrategy.FORCE.deleteQuietly(tempDir);
						}
					}
					
					@Override
					public void onError(Exception e)
					{
						LOG.error(
								MessageFormat.format(
										ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.update.failed.text"),
										SETTINGS_FILE) + ": " + e.getMessage());
						
						WebAPIClientUtils.shutdownHttpClient(httpClient);
						
						if (tempDir != null)
						{
							FileDeleteStrategy.FORCE.deleteQuietly(tempDir);
						}
					}
				});
	}
	
	private void loadJsonSettings(File jsonSettingsFile) throws IOException, JSONException
	{
		if (jsonSettingsFile.exists())
		{
			try
			{
				settings = new Settings(new JSONObject(FileUtils.readFileToString(jsonSettingsFile)));
				
				LOG.info(
						MessageFormat.format(
								ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.loaded.text"),
								SETTINGS_FILE));
			}
			catch (Exception e)
			{
				LOG.warn(
						MessageFormat.format(
								ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.load.failed.text"),
								SETTINGS_FILE) + ": " + e.getMessage());
				
				copyAndLoadDefaultJsonSettingsToFile(jsonSettingsFile);
			}
		}
		else
		{
			copyAndLoadDefaultJsonSettingsToFile(jsonSettingsFile);
		}
	}
	
	private void copyAndLoadDefaultJsonSettingsToFile(File jsonSettingsFile) throws IOException, JSONException
	{
		InputStream is = null;
		
		try
		{
			is = Thread.currentThread().getContextClassLoader().getResourceAsStream("settings.json");
			
			if (is == null)
			{
				LOG.error(
						MessageFormat.format(
								ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.load.default.failed.text"),
								SETTINGS_FILE));
			}
			else
			{
				IOUtils.copy(is, FileUtils.openOutputStream(jsonSettingsFile));
				settings = new Settings(new JSONObject(FileUtils.readFileToString(jsonSettingsFile)));
				
				LOG.info(
						MessageFormat.format(
								ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.loaded.default.text"),
								SETTINGS_FILE));
			}
		}
		finally
		{
			if (is != null)
			{
				is.close();
			}
		}
	}
	
	private static class LoadSettingsCallableHolder
	{
		private final static LoadSettingsCallable instance = new LoadSettingsCallable();
	}
}
