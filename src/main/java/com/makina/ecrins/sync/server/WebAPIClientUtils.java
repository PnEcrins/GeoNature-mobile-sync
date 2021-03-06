package com.makina.ecrins.sync.server;

import com.makina.ecrins.sync.settings.ServerSettings;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * Helpers for calling {@link HttpClient} used to invoke WebAPI urls.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public final class WebAPIClientUtils
{
    private static final String PARAM_TOKEN = "token";
    private static final String PARAM_DATA = "data";

    private static final Logger LOG = Logger.getLogger(WebAPIClientUtils.class);

    /**
     * {@link WebAPIClientUtils} instances should NOT be constructed in standard programming.
     */
    private WebAPIClientUtils()
    {
        // nothing to do ...
    }

    /**
     * Builds and instance of {@code HttpClient} using {@code HttpClientBuilder}.
     *
     * @param timeout the default timeout of this {@code HttpClient}
     *
     * @return instance of {@code HttpClient}
     */
    public static HttpClient getHttpClient(int timeout)
    {
        return HttpClientBuilder.create()
                .setConnectionManager(new BasicHttpClientConnectionManager())
                .setDefaultRequestConfig(
                        RequestConfig.custom()
                                .setConnectTimeout(timeout)
                                .setConnectionRequestTimeout(timeout)
                                .build()
                )
                .build();
    }

    /**
     * Build an instance of {@code HttpPost} to make synchronous calls.
     *
     * @param url        URL to use
     * @param token      the authentication token to use
     *
     * @return instance of {@code HttpPost}
     *
     * @throws UnsupportedEncodingException
     * @see #httpPost(String, String, String)
     */
    public static HttpPost httpPost(String url,
                                    String token) throws
                                                  UnsupportedEncodingException
    {
        return httpPost(
                url,
                token,
                null
        );
    }

    /**
     * Build an instance of {@code HttpPost} to make synchronous calls.
     *
     * @param url        URL to use
     * @param token      the authentication token to use
     * @param data       the data to send as JSON string
     *
     * @return instance of {@code HttpPost}
     *
     * @throws UnsupportedEncodingException
     */
    public static HttpPost httpPost(String url,
                                    String token,
                                    String data) throws
                                                 UnsupportedEncodingException
    {
        LOG.debug("httpPost '" + url + "'");

        String sanitizeData = data;

        if (StringUtils.isBlank(data))
        {
            sanitizeData = "{}";
        }

        HttpPost httpPost = new HttpPost(url);

        httpPost.setHeader(
                "Cache-Control",
                "no-cache"
        );
        httpPost.setHeader(
                "ContentType",
                "application/x-force-download"
        );

        try
        {
            httpPost.setEntity(
                    new UrlEncodedFormEntity(
                            Arrays.asList(
                                    new BasicNameValuePair(
                                            PARAM_TOKEN,
                                            token
                                    ),
                                    new BasicNameValuePair(
                                            PARAM_DATA,
                                            sanitizeData
                                    )
                            )
                    )
            );

            return httpPost;
        }
        catch (UnsupportedEncodingException uee)
        {
            httpPost.abort();

            throw new UnsupportedEncodingException(uee.getMessage());
        }
    }

    public static void httpPost(HttpClient httpClient,
                                String url,
                                String token,
                                boolean closeHttpClient,
                                HTTPCallback callback)
    {
        httpPost(
                httpClient,
                url,
                token,
                null,
                closeHttpClient,
                callback
        );
    }

    public static void httpPost(HttpClient httpClient,
                                String url,
                                String token,
                                String data,
                                boolean closeHttpClient,
                                HTTPCallback callback)
    {
        LOG.debug("httpPost '" + url + "'");

        String sanitizeData = data;

        if (StringUtils.isBlank(data))
        {
            sanitizeData = "{}";
        }

        HttpPost httpPost = new HttpPost(url);
        HttpResponse httpResponse = null;

        try
        {
            httpPost.setHeader(
                    "Cache-Control",
                    "no-cache"
            );
            httpPost.setHeader(
                    "ContentType",
                    "application/x-force-download"
            );
            httpPost.setEntity(
                    new UrlEncodedFormEntity(
                            Arrays.asList(
                                    new BasicNameValuePair(
                                            PARAM_TOKEN,
                                            token
                                    ),
                                    new BasicNameValuePair(
                                            PARAM_DATA,
                                            sanitizeData
                                    )
                            )
                    )
            );
            httpResponse = httpClient.execute(httpPost);

            callback.onResponse(
                    httpPost,
                    httpResponse
            );
        }
        catch (UnsupportedEncodingException uee)
        {
            httpPost.abort();
            callback.onError(uee);
        }
        catch (ClientProtocolException cpe)
        {
            httpPost.abort();
            callback.onError(cpe);
        }
        catch (IOException ioe)
        {
            httpPost.abort();
            callback.onError(ioe);
        }
        finally
        {
            HttpClientUtils.closeQuietly(httpResponse);

            if (closeHttpClient)
            {
                HttpClientUtils.closeQuietly(httpClient);
            }
        }
    }

    /**
     * Builds a complete URL to be used through WebAPI.
     *
     * @param baseUrl the base URL (e.g. {@link ServerSettings#getServerUrl()}
     * @param segment a set of segments URL to add
     *
     * @return the complete URL
     */
    public static String buildUrl(final String baseUrl, final String... segment)
    {
        final StringBuilder urlBuilder = new StringBuilder(StringUtils.endsWith(baseUrl, "/") ? StringUtils.substringBeforeLast(baseUrl, "/") : baseUrl);

        for (String part : segment)
        {
            if (StringUtils.isNotBlank(part)) {
                urlBuilder.append('/');
                urlBuilder.append(StringUtils.join(StringUtils.split(part, '/'), '/'));
            }
        }

        urlBuilder.append('/');

        return urlBuilder.toString();
    }

    public interface HTTPCallback
    {
        void onResponse(HttpRequestBase httpRequestBase,
                        HttpResponse httpResponse);

        void onError(Exception e);
    }
}
