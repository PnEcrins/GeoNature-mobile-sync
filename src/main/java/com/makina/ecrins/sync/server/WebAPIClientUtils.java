package com.makina.ecrins.sync.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.log4j.Logger;

/**
 * Helpers for calling {@link HttpClient} used to invoke WebAPI urls.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public final class WebAPIClientUtils
{
	public static final String PARAM_TOKEN = "token";
	public static final String PARAM_DATA = "data";
	
	private static final Logger LOG = Logger.getLogger(WebAPIClientUtils.class);
	
	/**
	 * {@link WebAPIClientUtils} instances should NOT be constructed in standard programming.
	 */
	private WebAPIClientUtils()
	{
		
	}
	
	public static HttpClient getHttpClient(int timeout)
	{
		final HttpClient httpClient = new DefaultHttpClient();
		HttpConnectionParams.setConnectionTimeout(httpClient.getParams(), timeout);
		
		return httpClient;
	}
	
	public static void shutdownHttpClient(HttpClient httpClient)
	{
		httpClient.getConnectionManager().shutdown();
	}
	
	public static void httpPost(HttpClient httpClient, String url, String token, HTTPCallback callback)
	{
		httpPost(httpClient, url, token, null, callback);
	}
	
	public static void httpPost(HttpClient httpClient, String url, String token, String data, HTTPCallback callback)
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
			httpPost.setHeader("Cache-Control", "no-cache");
			httpPost.setHeader("ContentType", "application/x-force-download");
			httpPost.setEntity(new UrlEncodedFormEntity(Arrays.asList(new BasicNameValuePair(PARAM_TOKEN, token), new BasicNameValuePair(PARAM_DATA, sanitizeData))));
			httpResponse = httpClient.execute(httpPost);
			
			callback.onResponse(httpPost, httpResponse);
		}
		catch (UnsupportedEncodingException uee)
		{
			httpPost.abort();
			HttpClientUtils.closeQuietly(httpResponse);
			callback.onError(uee);
		}
		catch (ClientProtocolException cpe)
		{
			httpPost.abort();
			HttpClientUtils.closeQuietly(httpResponse);
			callback.onError(cpe);
		}
		catch (IOException ioe)
		{
			httpPost.abort();
			HttpClientUtils.closeQuietly(httpResponse);
			callback.onError(ioe);
		}
		finally
		{
			HttpClientUtils.closeQuietly(httpResponse);
		}
	}
	
	public interface HTTPCallback
	{
		public void onResponse(HttpRequestBase httpRequestBase, HttpResponse httpResponse);
		public void onError(Exception e);
	}
}
