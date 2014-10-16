package com.makina.ecrins.sync.settings;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Global server settings for this application loaded from a JSON file.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ServerSettings
{
    private static final String KEY_SERVER_URL = "url";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_TIMEOUT = "timeout";

    private String serverUrl;
    private String serverToken;
    private int serverTimeout;

    public ServerSettings(JSONObject json) throws
                                   JSONException
    {
        this.serverUrl = json.getString(KEY_SERVER_URL);
        this.serverToken = json.getString(KEY_TOKEN);
        this.serverTimeout = json.getInt(KEY_TIMEOUT);
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
}
