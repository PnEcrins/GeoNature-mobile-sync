package com.makina.ecrins.sync.settings;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Global settings for this application loaded from a JSON file.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class Settings
{
	private static final String KEY_SYNC = "sync";
	private static final String KEY_DEVICES = "devices";
	
	private SyncSettings syncSettings;
	private final List<DeviceSettings> devicesSettings = new ArrayList<DeviceSettings>();
	
	public Settings(JSONObject json) throws JSONException
	{
		syncSettings = new SyncSettings(json.getJSONObject(KEY_SYNC));
		
		final JSONArray devicesJsonArray = json.getJSONArray(KEY_DEVICES);
		
		for (int i = 0; i < devicesJsonArray.length(); i++)
		{
			devicesSettings.add(new DeviceSettings(devicesJsonArray.getJSONObject(i)));
		}
	}

	public SyncSettings getSyncSettings()
	{
		return syncSettings;
	}

	public List<DeviceSettings> getDevicesSettings()
	{
		return devicesSettings;
	}
}
