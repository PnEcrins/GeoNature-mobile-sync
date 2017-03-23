package com.makina.ecrins.sync.settings;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Global server settings for this application loaded from a JSON file.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ServerSettings {

    private static final String KEY_SERVER_URL = "url";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_TIMEOUT = "timeout";
    private static final String KEY_ORGANISM = "organism";

    private String serverUrl;
    private String serverToken;
    private int serverTimeout;
    private String organism;

    ServerSettings(JSONObject json) throws
                                    JSONException {
        this.serverUrl = json.getString(KEY_SERVER_URL);
        this.serverToken = json.getString(KEY_TOKEN);
        this.serverTimeout = json.optInt(KEY_TIMEOUT,
                                         10000);
        this.organism = json.optString(KEY_ORGANISM);
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public String getServerToken() {
        return serverToken;
    }

    public int getServerTimeout() {
        return serverTimeout;
    }

    public String getOrganism() {
        return organism;
    }
}
