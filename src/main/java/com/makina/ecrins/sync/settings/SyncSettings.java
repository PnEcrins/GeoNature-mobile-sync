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
public class SyncSettings
{
	private static final String KEY_SYNC = "sync";
	private static final String KEY_SERVER_URL = "url";
	private static final String KEY_TOKEN = "token";
	private static final String KEY_TIMEOUT = "timeout";
	private static final String KEY_STATUS_URL = "status_url";
	private static final String KEY_IMPORT_URL = "import_url";
	private static final String KEY_APP_UPDATE = "app_update";
	private static final String KEY_APP_UPDATE_VERSION_URL = "version_url";
	private static final String KEY_APP_UPDATE_DOWNLOAD_URL = "download_url";
	private static final String KEY_EXPORTS = "exports";
	
	private String serverUrl;
	private String serverToken;
	private int serverTimeout;
	private String statusUrl;
	private String importUrl;
	private String appUpdateVersionUrl;
	private String appUpdateDownloadUrl;
	private List<ExportSettings> exportsSettings = new ArrayList<ExportSettings>();
	
	public SyncSettings(JSONObject json) throws JSONException
	{
		this.serverUrl = json.getJSONObject(KEY_SYNC).getString(KEY_SERVER_URL);
		this.serverToken = json.getJSONObject(KEY_SYNC).getString(KEY_TOKEN);
		this.serverTimeout = json.getJSONObject(KEY_SYNC).getInt(KEY_TIMEOUT);
		this.statusUrl = json.getJSONObject(KEY_SYNC).getString(KEY_STATUS_URL);
		this.importUrl = json.getJSONObject(KEY_SYNC).getString(KEY_IMPORT_URL);
		this.appUpdateVersionUrl = json.getJSONObject(KEY_SYNC).getJSONObject(KEY_APP_UPDATE).getString(KEY_APP_UPDATE_VERSION_URL);
		this.appUpdateDownloadUrl = json.getJSONObject(KEY_SYNC).getJSONObject(KEY_APP_UPDATE).getString(KEY_APP_UPDATE_DOWNLOAD_URL);
		
		JSONArray exportsJsonArray = json.getJSONObject(KEY_SYNC).getJSONArray(KEY_EXPORTS);
		
		for (int i = 0; i < exportsJsonArray.length(); i++)
		{
			exportsSettings.add(new ExportSettings(exportsJsonArray.getJSONObject(i)));
		}
	}

	public String getServerUrl()
	{
		return serverUrl;
	}

	public String getServerToken()
	{
		return serverToken;
	}

	public int getServerTimeout()
	{
		return serverTimeout;
	}

	public String getStatusUrl()
	{
		return statusUrl;
	}

	public String getImportUrl()
	{
		return importUrl;
	}

	public String getAppUpdateVersionUrl()
	{
		return appUpdateVersionUrl;
	}

	public String getAppUpdateDownloadUrl()
	{
		return appUpdateDownloadUrl;
	}

	public List<ExportSettings> getExportsSettings()
	{
		return exportsSettings;
	}
}
