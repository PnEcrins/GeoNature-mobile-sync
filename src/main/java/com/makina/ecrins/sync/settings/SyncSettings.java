package com.makina.ecrins.sync.settings;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Synchronization settings loaded from a JSON file.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class SyncSettings
{
    private static final String KEY_STATUS_URL = "status_url";
    protected static final String KEY_SETTINGS_URL = "settings_url";
    private static final String KEY_IMPORT_URL = "import_url";
    private static final String KEY_APP_UPDATE = "app_update";
    private static final String KEY_EXPORTS = "exports";

    private String statusUrl;
    private String settingsUrl;
    private String importUrl;
    private AppUpdateSettings appUpdateSettings;
    private final List<ExportSettings> exportsSettings = new ArrayList<ExportSettings>();

    public SyncSettings(JSONObject json) throws
                                         JSONException
    {
        this.statusUrl = json.getString(KEY_STATUS_URL);
        this.settingsUrl = json.getString(KEY_SETTINGS_URL);
        this.importUrl = json.getString(KEY_IMPORT_URL);
        this.appUpdateSettings = new AppUpdateSettings(json.getJSONObject(KEY_APP_UPDATE));

        final JSONArray exportsJsonArray = json.getJSONArray(KEY_EXPORTS);

        for (int i = 0; i < exportsJsonArray.length(); i++)
        {
            exportsSettings.add(new ExportSettings(exportsJsonArray.getJSONObject(i)));
        }
    }

    public String getStatusUrl()
    {
        return statusUrl;
    }

    public String getSettingsUrl()
    {
        return settingsUrl;
    }

    public String getImportUrl()
    {
        return importUrl;
    }

    public AppUpdateSettings getAppUpdateSettings()
    {
        return appUpdateSettings;
    }

    public List<ExportSettings> getExportsSettings()
    {
        return exportsSettings;
    }
}
