package com.makina.ecrins.sync.settings;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Android mount points settings used by {@link DeviceSettings}.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class MountsSettings
{
	private static final String KEY_DEFAULT = "default";
	private static final String KEY_EXTERNAL = "external";
	
	private final String defaultMountPoint;
	private final String externalMountPoint;
	
	public MountsSettings(JSONObject json) throws JSONException
	{
		defaultMountPoint = json.getString(KEY_DEFAULT);
		
		if (json.has(KEY_EXTERNAL))
		{
			externalMountPoint = json.getString(KEY_EXTERNAL);
		}
		else
		{
			externalMountPoint = null;
		}
	}

	public String getDefaultMountPoint()
	{
		return defaultMountPoint;
	}

	public String getExternalMountPoint()
	{
		return externalMountPoint;
	}

	@Override
	public String toString()
	{
		return "MountsSettings [defaultMountPoint=" + defaultMountPoint
				+ ", externalMountPoint=" + externalMountPoint + "]";
	}
}
