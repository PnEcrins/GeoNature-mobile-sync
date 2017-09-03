package com.makina.ecrins.sync.settings;

import com.makina.ecrins.sync.server.WebAPIClientUtils;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.concurrent.Callable;

/**
 * <code>Callable</code> implementation for loading application global settings as {@link SyncSettings}.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class LoadSettingsCallable
        implements Callable<Settings>
{
    public static final String SETTINGS_FILE = "settings.json";

    private static final String SERVER_SETTINGS_FILE = "server.json";
    private static final Logger LOG = Logger.getLogger(LoadSettingsCallable.class);

    private ServerSettings serverSettings;
    private Settings settings;

    private LoadSettingsCallable()
    {
        serverSettings = null;
        settings = null;
    }

    public static LoadSettingsCallable getInstance()
    {
        return LoadSettingsCallableHolder.instance;
    }

    public ServerSettings getServerSettings()
    {
        return serverSettings;
    }

    public Settings getSettings()
    {
        return settings;
    }

    @Override
    public Settings call() throws
                           Exception
    {
        LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages")
                                                    .getString("MainWindow.shell.settings.loading.text"),
                                      SETTINGS_FILE));

        File rootDirectory = new File(URLDecoder.decode(getClass().getProtectionDomain()
                                                                  .getCodeSource()
                                                                  .getLocation()
                                                                  .getPath(),
                                                        "UTF-8")).getParentFile();

        File userDir = new File(FileUtils.getUserDirectory(),
                                ".sync");

        // noinspection ResultOfMethodCallIgnored
        userDir.mkdir();

        LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages")
                                                    .getString("MainWindow.shell.settings.app.path.text"),
                                      rootDirectory.getAbsolutePath()));

        LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages")
                                                    .getString("MainWindow.shell.settings.user.path.text"),
                                      userDir.getAbsolutePath()));

        final File jsonServerSettingsFile = new File(rootDirectory,
                                                     SERVER_SETTINGS_FILE);
        final File jsonSettingsFile = new File(userDir,
                                               SETTINGS_FILE);

        // load 'server.json' from app installation directory or load the default 'server.json' in case of errors
        loadJsonServerSettings(jsonServerSettingsFile);
        // load 'settings.json' from user directory or load the default 'settings.json' in case of errors
        loadJsonSettings(jsonSettingsFile);

        if (StringUtils.isBlank(serverSettings.getServerUrl()))
        {
            LOG.error(ResourceBundle.getBundle("messages")
                                    .getString("MainWindow.shell.settings.server.notdefined.text"));
        }
        else
        {
            // try to update 'settings.json' from server
            if (!updateJsonSettingsFromServer(jsonSettingsFile,
                                              WebAPIClientUtils.buildUrl(serverSettings.getServerUrl(),
                                                                         settings.getSyncSettings()
                                                                                 .getSettingsUrl(),
                                                                         SETTINGS_FILE)))
            {
                updateJsonSettingsFromServer(jsonSettingsFile,
                                             WebAPIClientUtils.buildUrl(serverSettings.getServerUrl(),
                                                                        serverSettings.getSettingsUrl(),
                                                                        SETTINGS_FILE));
            }
        }

        // reload 'settings.json' from user directory or load the default 'settings.json' in case of errors
        loadJsonSettings(jsonSettingsFile);

        LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages")
                                                    .getString("MainWindow.shell.settings.server.text"),
                                      LoadSettingsCallable.getInstance()
                                                          .getServerSettings()
                                                          .getServerUrl()));

        return settings;
    }

    /**
     * Tries to load {@link #SERVER_SETTINGS_FILE} from a given {@code File}.
     * <p>
     * If something was wrong, try to load the default one.
     * </p>
     *
     * @param jsonServerSettingsFile the {@code File} to load as JSON
     */
    private void loadJsonServerSettings(final File jsonServerSettingsFile)
    {
        if (jsonServerSettingsFile.exists())
        {
            try
            {
                serverSettings = new ServerSettings(new JSONObject(FileUtils.readFileToString(jsonServerSettingsFile,
                                                                                              Charset.defaultCharset())));
            }
            catch (IOException ioe)
            {
                LOG.warn(MessageFormat.format(ResourceBundle.getBundle("messages")
                                                            .getString("MainWindow.shell.settings.load.failed.text"),
                                              SERVER_SETTINGS_FILE) + ": " + ioe.getMessage());
            }
        }

        // something was wrong while loading server settings, try to load the default one
        if (serverSettings == null)
        {
            InputStream is = null;

            try
            {
                is = Thread.currentThread()
                           .getContextClassLoader()
                           .getResourceAsStream(SERVER_SETTINGS_FILE);

                if (is == null)
                {
                    LOG.error(MessageFormat.format(ResourceBundle.getBundle("messages")
                                                                 .getString("MainWindow.shell.settings.load.default.failed.text"),
                                                   SERVER_SETTINGS_FILE));
                }
                else
                {
                    try
                    {
                        serverSettings = new ServerSettings(new JSONObject(IOUtils.toString(is,
                                                                                            Charset.defaultCharset())));
                    }
                    catch (IOException ioe)
                    {
                        LOG.error(MessageFormat.format(ResourceBundle.getBundle("messages")
                                                                     .getString("MainWindow.shell.settings.load.default.failed.text"),
                                                       SERVER_SETTINGS_FILE) + ": " + ioe.getMessage());
                    }
                }
            }
            finally
            {
                if (is != null)
                {
                    try
                    {
                        is.close();
                    }
                    catch (IOException ioe)
                    {
                        LOG.error(MessageFormat.format(ResourceBundle.getBundle("messages")
                                                                     .getString("MainWindow.shell.settings.load.default.failed.text"),
                                                       SERVER_SETTINGS_FILE));
                    }
                }
            }
        }
    }

    private void loadJsonSettings(final File jsonSettingsFile) throws
                                                               IOException,
                                                               JSONException
    {
        if (jsonSettingsFile.exists())
        {
            try
            {
                settings = new Settings(new JSONObject(FileUtils.readFileToString(jsonSettingsFile,
                                                                                  Charset.defaultCharset())));

                LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages")
                                                            .getString("MainWindow.shell.settings.loaded.text"),
                                              SETTINGS_FILE));
            }
            catch (Exception e)
            {
                LOG.warn(MessageFormat.format(ResourceBundle.getBundle("messages")
                                                            .getString("MainWindow.shell.settings.load.failed.text"),
                                              SETTINGS_FILE) + ": " + e.getMessage());

                copyAndLoadDefaultJsonSettingsToFile(jsonSettingsFile);
            }
        }
        else
        {
            copyAndLoadDefaultJsonSettingsToFile(jsonSettingsFile);
        }
    }

    private boolean updateJsonSettingsFromServer(final File jsonSettingsFile,
                                                 final String settingsUrl)
    {
        final File tempDir = new File(FileUtils.getTempDirectory(),
                                      "sync_settings_" + Long.toString(System.currentTimeMillis()));

        // noinspection ResultOfMethodCallIgnored
        tempDir.mkdir();

        final HttpClient httpClient = WebAPIClientUtils.getHttpClient(LoadSettingsCallable.getInstance()
                                                                                          .getServerSettings()
                                                                                          .getServerTimeout());

        HttpResponse httpResponse = null;
        boolean success = false;

        try
        {
            final HttpPost httpPost = WebAPIClientUtils.httpPost(settingsUrl,
                                                                 LoadSettingsCallable.getInstance()
                                                                                     .getServerSettings()
                                                                                     .getServerToken());

            httpResponse = httpClient.execute(httpPost);

            // checks if serverSettings response is valid
            final StatusLine status = httpResponse.getStatusLine();

            if (status.getStatusCode() == HttpStatus.SC_OK)
            {
                // pulls content stream from response
                final HttpEntity entity = httpResponse.getEntity();

                FileUtils.copyInputStreamToFile(entity.getContent(),
                                                new File(tempDir,
                                                         SETTINGS_FILE));

                // do nothing if we have the same file
                if (FileUtils.contentEquals(new File(tempDir,
                                                     SETTINGS_FILE),
                                            jsonSettingsFile))
                {
                    LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages")
                                                                .getString("MainWindow.shell.settings.update.uptodate.text"),
                                                  SETTINGS_FILE));
                }
                else
                {
                    FileUtils.copyFile(new File(tempDir,
                                                SETTINGS_FILE),
                                       jsonSettingsFile);

                    LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages")
                                                                .getString("MainWindow.shell.settings.update.success.text"),
                                                  SETTINGS_FILE));
                }

                // ensure that the response body is fully consumed
                EntityUtils.consume(entity);
                success = true;
            }
            else
            {
                LOG.error(MessageFormat.format(ResourceBundle.getBundle("messages")
                                                             .getString("MainWindow.shell.settings.update.failed.text"),
                                               SETTINGS_FILE) + " (" + status.getStatusCode() + ")");
            }
        }
        catch (IOException ioe)
        {
            LOG.error(MessageFormat.format(ResourceBundle.getBundle("messages")
                                                         .getString("MainWindow.shell.settings.update.failed.text"),
                                           SETTINGS_FILE) + ": " + ioe.getMessage());
        }
        finally
        {
            HttpClientUtils.closeQuietly(httpResponse);
            HttpClientUtils.closeQuietly(httpClient);
        }

        FileDeleteStrategy.FORCE.deleteQuietly(tempDir);

        return success;
    }

    private void copyAndLoadDefaultJsonSettingsToFile(final File jsonSettingsFile) throws
                                                                                   IOException,
                                                                                   JSONException
    {
        InputStream is = null;

        try
        {
            is = Thread.currentThread()
                       .getContextClassLoader()
                       .getResourceAsStream(SETTINGS_FILE);

            if (is == null)
            {
                LOG.error(MessageFormat.format(ResourceBundle.getBundle("messages")
                                                             .getString("MainWindow.shell.settings.load.default.failed.text"),
                                               SETTINGS_FILE));
            }
            else
            {
                final JSONObject defaultSettingsAsJson = new JSONObject(IOUtils.toString(is, Charset.defaultCharset()));

                // tries to update default settings url from 'server.json' configuration file
                if (serverSettings != null && StringUtils.isNotBlank(serverSettings.getSettingsUrl()))
                {
                    defaultSettingsAsJson.getJSONObject(Settings.KEY_SYNC)
                                         .put(SyncSettings.KEY_SETTINGS_URL,
                                              serverSettings.getSettingsUrl());
                }

                FileUtils.writeStringToFile(jsonSettingsFile,
                                            defaultSettingsAsJson.toString(4),
                                            Charset.defaultCharset());

                settings = new Settings(defaultSettingsAsJson);

                LOG.info(MessageFormat.format(ResourceBundle.getBundle("messages")
                                                            .getString("MainWindow.shell.settings.loaded.default.text"),
                                              SETTINGS_FILE));
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
