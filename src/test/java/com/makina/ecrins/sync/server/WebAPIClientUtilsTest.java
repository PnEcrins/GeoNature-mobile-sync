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
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import static org.junit.Assert.*;

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
                        .getServerSettings()
                        .getServerTimeout()
        );

        HttpResponse httpResponse = null;

        try
        {
            final HttpPost httpPost = WebAPIClientUtils.httpPost(
                    LoadSettingsCallable.getInstance()
                            .getServerSettings()
                            .getServerUrl() + LoadSettingsCallable.getInstance()
                            .getSettings()
                            .getSyncSettings()
                            .getStatusUrl(),
                    LoadSettingsCallable.getInstance()
                            .getServerSettings()
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
                    JSONObject jsonResponse = new JSONObject(IOUtils.toString(is, Charset.defaultCharset()));

                    assertTrue(jsonResponse.has("status_code"));
                    assertTrue(
                            "status_code",
                            jsonResponse.getInt("status_code") == 0
                    );

                    LOG.debug("server status : " + jsonResponse.getInt("status_code"));
                }
                catch (IllegalStateException ise)
                {
                    fail(ise.getMessage());
                }
                catch (IOException ioe)
                {
                    fail(ioe.getMessage());
                }
                catch (JSONException je)
                {
                    fail(je.getMessage());
                }
            }
            else
            {
                httpPost.abort();
                fail("unable to check server status, HTTP status : " + status.getStatusCode());
            }
        }
        catch (IOException ioe)
        {
            fail(ioe.getMessage());
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
                        .getServerSettings()
                        .getServerTimeout()
        );

        HttpResponse httpResponse = null;

        try
        {
            final HttpPost httpPost = WebAPIClientUtils.httpPost(
                    LoadSettingsCallable.getInstance()
                            .getServerSettings()
                            .getServerUrl() + LoadSettingsCallable.getInstance()
                            .getSettings()
                            .getSyncSettings()
                            .getAppUpdateSettings()
                            .getVersionUrl(),
                    LoadSettingsCallable.getInstance()
                            .getServerSettings()
                            .getServerToken()
            );

            httpResponse = httpClient.execute(httpPost);

            // checks if server response is valid
            StatusLine status = httpResponse.getStatusLine();

            if (status.getStatusCode() == HttpStatus.SC_OK)
            {
                httpPost.abort();
                assertTrue(true);
            }
            else
            {
                httpPost.abort();
                fail("unable to get mobile apps versions, HTTP status : " + status.getStatusCode());
            }
        }
        catch (IOException ioe)
        {
            fail(ioe.getMessage());
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
                        .getServerSettings()
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
                        LoadSettingsCallable.getInstance()
                                .getServerSettings()
                                .getServerUrl() + exportSettings.getExportUrl(),
                        LoadSettingsCallable.getInstance()
                                .getServerSettings()
                                .getServerToken()
                );

                httpResponse = httpClient.execute(httpPost);

                // checks if server response is valid
                StatusLine status = httpResponse.getStatusLine();

                if (status.getStatusCode() == HttpStatus.SC_OK)
                {
                    httpPost.abort();
                    assertTrue(
                            "success",
                            true
                    );
                }
                else
                {
                    httpPost.abort();
                    fail("unable to download file from URL '" + httpPost.getURI() + "', HTTP status : " + status.getStatusCode());
                }
            }
            catch (IOException ioe)
            {
                fail(ioe.getMessage());
            }
            finally
            {
                HttpClientUtils.closeQuietly(httpResponse);
            }
        }

        HttpClientUtils.closeQuietly(httpClient);
    }

    @Test
    public void testBuildUrl() throws
                               Exception {
        final String baseUrl = "http://webapi";
        assertEquals(baseUrl + '/', WebAPIClientUtils.buildUrl(baseUrl));

        final String baseUrlWithEndingSlash = "http://webapi/";
        assertEquals(baseUrlWithEndingSlash, WebAPIClientUtils.buildUrl(baseUrlWithEndingSlash));

        final String segment1 = "segment1";
        assertEquals(baseUrl + '/' + segment1 + '/', WebAPIClientUtils.buildUrl(baseUrl, segment1));
        assertEquals(baseUrl + '/' + segment1 + '/', WebAPIClientUtils.buildUrl(baseUrlWithEndingSlash, segment1));

        final String segment1WithStartingSlash = "/segment1";
        assertEquals(baseUrl + segment1WithStartingSlash + '/', WebAPIClientUtils.buildUrl(baseUrl, segment1WithStartingSlash));
        assertEquals(baseUrl + segment1WithStartingSlash + '/', WebAPIClientUtils.buildUrl(baseUrlWithEndingSlash, segment1WithStartingSlash));

        final String segment1WithEndingSlash = "segment1/";
        assertEquals(baseUrl + '/' + segment1WithEndingSlash, WebAPIClientUtils.buildUrl(baseUrl, segment1WithEndingSlash));
        assertEquals(baseUrl + '/' + segment1WithEndingSlash, WebAPIClientUtils.buildUrl(baseUrlWithEndingSlash, segment1WithEndingSlash));

        final String segment1WithSlashs = "/segment1/";
        assertEquals(baseUrl + segment1WithSlashs, WebAPIClientUtils.buildUrl(baseUrl, segment1WithSlashs));
        assertEquals(baseUrl + segment1WithSlashs, WebAPIClientUtils.buildUrl(baseUrlWithEndingSlash, segment1WithSlashs));

        final String segment2 = "segment2";
        assertEquals(baseUrl + '/' + segment1 + '/' + segment2 + '/', WebAPIClientUtils.buildUrl(baseUrl, segment1, segment2));
        assertEquals(baseUrl + '/' + segment1 + '/' + segment2 + '/', WebAPIClientUtils.buildUrl(baseUrlWithEndingSlash, segment1, segment2));

        final String multiplePartSegment = "seg1/seg2";
        assertEquals(baseUrl + '/' + multiplePartSegment + '/', WebAPIClientUtils.buildUrl(baseUrl, multiplePartSegment));
        assertEquals(baseUrl + '/' + segment1 + '/' + multiplePartSegment + '/', WebAPIClientUtils.buildUrl(baseUrl, segment1, multiplePartSegment));
        assertEquals(baseUrl + '/' + segment1 + '/' + multiplePartSegment + '/', WebAPIClientUtils.buildUrl(baseUrlWithEndingSlash, segment1, multiplePartSegment));
    }
}
