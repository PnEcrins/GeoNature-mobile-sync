package com.makina.ecrins.sync.settings;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Export settings used by {@link SyncSettings}.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ExportSettings
{
	private static final String KEY_URL = "url";
	private static final String KEY_FILE = "file";
	
	private String exportUrl;
	private String exportFile;
	
	public ExportSettings(JSONObject json) throws JSONException
	{
		exportUrl = json.getString(KEY_URL);
		exportFile = json.getString(KEY_FILE);
	}

	public String getExportUrl()
	{
		return exportUrl;
	}

	public String getExportFile()
	{
		return exportFile;
	}
}
