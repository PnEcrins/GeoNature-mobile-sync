package com.makina.ecrins.sync.server;

import com.makina.ecrins.sync.settings.ExportSettings;
import com.makina.ecrins.sync.settings.LoadSettingsCallable;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Test class for {@link WebAPIClientUtils}
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class WebAPIClientUtilsTest
{
    private static final Logger LOG = Logger.getLogger(WebAPIClientUtilsTest.class);

    @Test()
    public void serverStatusTest()
    {
        final HttpClient httpClient = WebAPIClientUtils.getHttpClient(
                LoadSettingsCallable.getInstance()
                        .getSettings()
                        .getSyncSettings()
                        .getServerTimeout()
        );

        HttpResponse httpResponse = null;

        try
        {
            final HttpPost httpPost = WebAPIClientUtils.httpPost(
                    httpClient,
                    LoadSettingsCallable.getInstance()
                            .getSettings()
                            .getSyncSettings()
                            .getServerUrl() + LoadSettingsCallable.getInstance()
                            .getSettings()
                            .getSyncSettings()
                            .getStatusUrl(),
                    LoadSettingsCallable.getInstance()
                            .getSettings()
                            .getSyncSettings()
                            .getServerToken()
            );

            httpResponse = httpClient.execute(httpPost);

            // checks if server response is valid
            StatusLine status = httpResponse.getStatusLine();

            if (status.getStatusCode() == HttpStatus.SC_OK)
            {
                HttpEntity entity = httpResponse.getEntity();

                try
                {
                    InputStream is = entity.getContent();
                    JSONObject jsonResponse = new JSONObject(IOUtils.toString(is));

                    Assert.assertTrue(jsonResponse.has("status_code"));
                    Assert.assertTrue(
                            "status_code",
                            jsonResponse.getInt("status_code") == 0
                    );

                    LOG.debug("server status : " + jsonResponse.getInt("status_code"));
                }
                catch (IllegalStateException ise)
                {
                    Assert.fail(ise.getMessage());
                }
                catch (IOException ioe)
                {
                    Assert.fail(ioe.getMessage());
                }
                catch (JSONException je)
                {
                    Assert.fail(je.getMessage());
                }
            }
            else
            {
                httpPost.abort();
                Assert.fail("unable to check server status, HTTP status : " + status.getStatusCode());
            }
        }
        catch (IOException ioe)
        {
            Assert.fail(ioe.getMessage());
        }
        finally
        {
            HttpClientUtils.closeQuietly(httpResponse);
            HttpClientUtils.closeQuietly(httpClient);
        }
    }

    @Test()
    public void softVersionTest()
    {
        final HttpClient httpClient = WebAPIClientUtils.getHttpClient(
                LoadSettingsCallable.getInstance()
                        .getSettings()
                        .getSyncSettings()
                        .getServerTimeout()
        );

        HttpResponse httpResponse = null;

        try
        {
            final HttpPost httpPost = WebAPIClientUtils.httpPost(
                    httpClient,
                    LoadSettingsCallable.getInstance()
                            .getSettings()
                            .getSyncSettings()
                            .getServerUrl() + LoadSettingsCallable.getInstance()
                            .getSettings()
                            .getSyncSettings()
                            .getAppUpdateSettings()
                            .getVersionUrl(),
                    LoadSettingsCallable.getInstance()
                            .getSettings()
                            .getSyncSettings()
                            .getServerToken()
            );

            httpResponse = httpClient.execute(httpPost);

            // checks if server response is valid
            StatusLine status = httpResponse.getStatusLine();

            if (status.getStatusCode() == HttpStatus.SC_OK)
            {
                httpPost.abort();
                Assert.assertTrue(true);
            }
            else
            {
                httpPost.abort();
                Assert.fail("unable to get mobile apps versions, HTTP status : " + status.getStatusCode());
            }
        }
        catch (IOException ioe)
        {
            Assert.fail(ioe.getMessage());
        }
        finally
        {
            HttpClientUtils.closeQuietly(httpResponse);
            HttpClientUtils.closeQuietly(httpClient);
        }
    }

    @Test
    public void exportsTest()
    {
        final HttpClient httpClient = WebAPIClientUtils.getHttpClient(
                LoadSettingsCallable.getInstance()
                        .getSettings()
                        .getSyncSettings()
                        .getServerTimeout()
        );

        final List<ExportSettings> exportsSettings = LoadSettingsCallable.getInstance()
                .getSettings()
                .getSyncSettings()
                .getExportsSettings();

        for (ExportSettings exportSettings : exportsSettings)
        {
            HttpResponse httpResponse = null;

            try
            {
                final HttpPost httpPost = WebAPIClientUtils.httpPost(
                        httpClient,
                        LoadSettingsCallable.getInstance()
                                .getSettings()
                                .getSyncSettings()
                                .getServerUrl() + exportSettings.getExportUrl(),
                        LoadSettingsCallable.getInstance()
                                .getSettings()
                                .getSyncSettings()
                                .getServerToken()
                );

                httpResponse = httpClient.execute(httpPost);

                // checks if server response is valid
                StatusLine status = httpResponse.getStatusLine();

                if (status.getStatusCode() == HttpStatus.SC_OK)
                {
                    httpPost.abort();
                    Assert.assertTrue(
                            "success",
                            true
                    );
                }
                else
                {
                    httpPost.abort();
                    Assert.fail("unable to download file from URL '" + httpPost.getURI() + "', HTTP status : " + status.getStatusCode());
                }
            }
            catch (IOException ioe)
            {
                Assert.fail(ioe.getMessage());
            }
            finally
            {
                HttpClientUtils.closeQuietly(httpResponse);
            }
        }

        HttpClientUtils.closeQuietly(httpClient);
    }
}
