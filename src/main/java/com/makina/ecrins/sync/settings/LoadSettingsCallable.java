package com.makina.ecrins.sync.settings;

import java.io.InputStream;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.makina.ecrins.sync.util.FileUtils;

/**
 * <code>Callable</code> implementation for loading application global settings as JSON file.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class LoadSettingsCallable implements Callable<JSONObject>
{
	private static final Logger LOG = Logger.getLogger(LoadSettingsCallable.class);
	
	public static final String KEY_SERVER_URL = "url";
	public static final String KEY_TOKEN = "token";
	public static final String KEY_STATUS_URL = "status_url";
	
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
		
		InputStream is = null;
		
		try
		{
			is = ClassLoader.getSystemResourceAsStream("settings.json");
			
			if (is == null)
			{
				LOG.warn("failed to load 'settings.json'");
			}
			else
			{
				jsonSettings = new JSONObject(FileUtils.readInputStreamAsString(is));
				
				LOG.debug("'settings.json' loaded");
			}
		}
		finally
		{
			if (is != null)
			{
				is.close();
			}
		}
		
		return jsonSettings;
	}
	
	private static class LoadSettingsCallableHolder
	{
		private final static LoadSettingsCallable instance = new LoadSettingsCallable();
	}
}
