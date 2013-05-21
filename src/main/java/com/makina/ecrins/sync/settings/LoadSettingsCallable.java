package com.makina.ecrins.sync.settings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <code>Callable</code> implementation for loading application global settings as {@link SyncSettings}.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class LoadSettingsCallable implements Callable<SyncSettings>
{
	public static final String SETTINGS_FILE = "settings.json";
	
	private static final Logger LOG = Logger.getLogger(LoadSettingsCallable.class);
	
	private SyncSettings syncSettings;
	
	private LoadSettingsCallable()
	{
		syncSettings = null;
	}
	
	public static LoadSettingsCallable getInstance()
	{
		return LoadSettingsCallableHolder.instance;
	}
	
	public SyncSettings getSyncSettings()
	{
		return syncSettings;
	}

	@Override
	public SyncSettings call() throws Exception
	{
		LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.loading.text"), SETTINGS_FILE));
		
		File rootDirectory = new File(URLDecoder.decode(getClass().getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8")).getParentFile();
		
		LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.path.text"), rootDirectory.getAbsolutePath()));
		
		File jsonSettingsFile = new File(rootDirectory, SETTINGS_FILE);
		
		if (jsonSettingsFile.exists())
		{
			try
			{
				syncSettings = new SyncSettings(new JSONObject(FileUtils.readFileToString(jsonSettingsFile)));
				
				LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.loaded.text"), SETTINGS_FILE));
			}
			catch (JSONException je)
			{
				LOG.warn(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.load.failed.text"), SETTINGS_FILE));
				
				copyDefaultJsonSettingsToFile(jsonSettingsFile);
				syncSettings = new SyncSettings(new JSONObject(FileUtils.readFileToString(jsonSettingsFile)));
				
				LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.loaded.default.text"), SETTINGS_FILE));
			}
		}
		else
		{
			copyDefaultJsonSettingsToFile(jsonSettingsFile);
			syncSettings = new SyncSettings(new JSONObject(FileUtils.readFileToString(jsonSettingsFile)));
			
			LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.loaded.default.text"), SETTINGS_FILE));
		}
		
		LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.server.text"), LoadSettingsCallable.getInstance().getSyncSettings().getServerUrl()));
		
		return syncSettings;
	}
	
	private void copyDefaultJsonSettingsToFile(File jsonSettingsFile) throws IOException
	{
		InputStream is = null;
		
		try
		{
			is = Thread.currentThread().getContextClassLoader().getResourceAsStream("settings.json");
			
			if (is == null)
			{
				LOG.error(MessageFormat.format(ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.load.default.failed.text"), SETTINGS_FILE));
			}
			else
			{
				IOUtils.copy(is, FileUtils.openOutputStream(jsonSettingsFile));
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
