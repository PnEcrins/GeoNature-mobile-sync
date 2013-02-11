package com.makina.ecrins.sync.settings;

import java.io.File;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

/**
 * <code>Callable</code> implementation for loading application global settings as JSON file.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class LoadSettingsCallable implements Callable<JSONObject>
{
	private static final Logger LOG = Logger.getLogger(LoadSettingsCallable.class);
	
	public static final String KEY_SYNC = "sync";
	public static final String KEY_SERVER_URL = "url";
	public static final String KEY_TOKEN = "token";
	public static final String KEY_STATUS_URL = "status_url";
	public static final String KEY_IMPORT_URL = "import_url";
	public static final String KEY_APP_UPDATE = "app_update";
	public static final String KEY_APP_UPDATE_VERSION_URL = "version_url";
	public static final String KEY_APP_UPDATE_DOWNLOAD_URL = "download_url";
	public static final String KEY_EXPORTS = "exports";
	public static final String KEY_EXPORTS_URL = "url";
	public static final String KEY_EXPORTS_FILE = "file";
	
	private JSONObject jsonSettings;
	
	private LoadSettingsCallable()
	{
		jsonSettings = new JSONObject();
	}
	
	public static LoadSettingsCallable getInstance()
	{
		return LoadSettingsCallableHolder.instance;
	}
	
	public JSONObject getJsonSettings()
	{
		return jsonSettings;
	}

	
	@Override
	public JSONObject call() throws Exception
	{
		LOG.debug("loading 'settings.json' ...");
		
		File rootDirectory = new File(URLDecoder.decode(getClass().getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8")).getParentFile();
		
		LOG.info("installation directory : " + rootDirectory.getAbsolutePath());
		
		File settingsFile = new File(rootDirectory, "settings.json");
		
		if (!settingsFile.exists())
		{
			InputStream is = null;
			
			try
			{
				is = Thread.currentThread().getContextClassLoader().getResourceAsStream("settings.json");
				
				if (is == null)
				{
					LOG.error("failed to load default 'settings.json'");
				}
				else
				{
					IOUtils.copy(is, FileUtils.openOutputStream(settingsFile));
					
					LOG.debug("default 'settings.json' copied");
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
		
		jsonSettings = new JSONObject(FileUtils.readFileToString(settingsFile));

		if (jsonSettings.length() > 0)
		{
			LOG.info("'settings.json' loaded");
		}
		else
		{
			LOG.error("failed to load 'settings.json'");
		}
		
		return jsonSettings;
	}
	
	private static class LoadSettingsCallableHolder
	{
		private final static LoadSettingsCallable instance = new LoadSettingsCallable();
	}
}
