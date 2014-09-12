package com.makina.ecrins.sync.settings;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Mobile applications update settings used by {@link SyncSettings}.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class AppUpdateSettings
{
	private static final String KEY_VERSION_URL = "version_url";
	private static final String KEY_DOWNLOAD_URL = "download_url";
	
	private String versionUrl;
	private String downloadUrl;
	
	public AppUpdateSettings(JSONObject json) throws JSONException
	{
		versionUrl = json.getString(KEY_VERSION_URL);
		downloadUrl = json.getString(KEY_DOWNLOAD_URL);
	}

	public String getVersionUrl()
	{
		return versionUrl;
	}

	public String getDownloadUrl()
	{
		return downloadUrl;
	}
}
