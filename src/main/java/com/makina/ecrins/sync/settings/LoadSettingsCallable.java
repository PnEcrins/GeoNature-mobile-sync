package com.makina.ecrins.sync.settings;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
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
		LOG.debug("loading 'settings.json' ...");
		
		File rootDirectory = new File(URLDecoder.decode(getClass().getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8")).getParentFile();
		
		LOG.info("installation directory : " + rootDirectory.getAbsolutePath());
		
		File jsonSettingsFile = new File(rootDirectory, "settings.json");
		
		if (jsonSettingsFile.exists())
		{
			try
			{
				syncSettings = new SyncSettings(new JSONObject(FileUtils.readFileToString(jsonSettingsFile)));
				
				LOG.info("'settings.json' loaded");
			}
			catch (JSONException je)
			{
				LOG.warn("failed to load 'settings.json'");
				
				copyDefaultJsonSettingsToFile(jsonSettingsFile);
				syncSettings = new SyncSettings(new JSONObject(FileUtils.readFileToString(jsonSettingsFile)));
				
				LOG.info("default 'settings.json' loaded");
			}
		}
		else
		{
			copyDefaultJsonSettingsToFile(jsonSettingsFile);
			syncSettings = new SyncSettings(new JSONObject(FileUtils.readFileToString(jsonSettingsFile)));
			
			LOG.info("default 'settings.json' loaded");
		}
		
		LOG.info("server sync : " + LoadSettingsCallable.getInstance().getSyncSettings().getServerUrl());
		
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
				LOG.error("failed to load default 'settings.json'");
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
