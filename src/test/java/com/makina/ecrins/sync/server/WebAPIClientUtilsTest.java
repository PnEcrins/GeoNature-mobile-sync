package com.makina.ecrins.sync.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

import com.makina.ecrins.sync.server.WebAPIClientUtils.HTTPCallback;
import com.makina.ecrins.sync.settings.ExportSettings;
import com.makina.ecrins.sync.settings.LoadSettingsCallable;


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
		HttpClient httpClient = WebAPIClientUtils.getHttpClient(LoadSettingsCallable.getInstance().getSyncSettings().getServerTimeout());
		WebAPIClientUtils.httpPost(httpClient,
				LoadSettingsCallable.getInstance().getSyncSettings().getServerUrl() +
				LoadSettingsCallable.getInstance().getSyncSettings().getStatusUrl(),
				LoadSettingsCallable.getInstance().getSyncSettings().getServerToken(),
				new HTTPCallback()
				{
					@Override
					public void onResponse(HttpRequestBase httpRequestBase, HttpResponse httpResponse)
					{
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
								Assert.assertTrue("status_code", jsonResponse.getInt("status_code") == 0);
								
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
							httpRequestBase.abort();
							Assert.fail("unable to check server status, HTTP status : " + status.getStatusCode());
						}
					}
					
					@Override
					public void onError(Exception e)
					{
						Assert.fail(e.getMessage());
					}
				});
		
		WebAPIClientUtils.shutdownHttpClient(httpClient);
	}
	
	@Test()
	public void softVersionTest()
	{
		HttpClient httpClient = WebAPIClientUtils.getHttpClient(LoadSettingsCallable.getInstance().getSyncSettings().getServerTimeout());
		WebAPIClientUtils.httpPost(httpClient,
				LoadSettingsCallable.getInstance().getSyncSettings().getServerUrl() +
				LoadSettingsCallable.getInstance().getSyncSettings().getAppUpdateVersionUrl(),
				LoadSettingsCallable.getInstance().getSyncSettings().getServerToken(),
				new HTTPCallback()
				{
					@Override
					public void onResponse(HttpRequestBase httpRequestBase, HttpResponse httpResponse)
					{
						// checks if server response is valid
						StatusLine status = httpResponse.getStatusLine();
						
						if (status.getStatusCode() == HttpStatus.SC_OK)
						{
							httpRequestBase.abort();
							Assert.assertTrue(true);
						}
						else
						{
							httpRequestBase.abort();
							Assert.fail("unable to get mobile apps versions, HTTP status : " + status.getStatusCode());
						}
					}
					
					@Override
					public void onError(Exception e)
					{
						Assert.fail(e.getMessage());
					}
				});
		
		WebAPIClientUtils.shutdownHttpClient(httpClient);
	}
	
	@Test
	public void exportsTest()
	{
		HttpClient httpClient = WebAPIClientUtils.getHttpClient(LoadSettingsCallable.getInstance().getSyncSettings().getServerTimeout());
		
		List<ExportSettings> exportsSettings = LoadSettingsCallable.getInstance().getSyncSettings().getExportsSettings();
		
		for (ExportSettings exportSettings : exportsSettings)
		{
			WebAPIClientUtils.httpPost(httpClient,
					LoadSettingsCallable.getInstance().getSyncSettings().getServerUrl() +
					exportSettings.getExportUrl(),
					LoadSettingsCallable.getInstance().getSyncSettings().getServerToken(),
					new HTTPCallback()
					{
						@Override
						public void onResponse(HttpRequestBase httpRequestBase, HttpResponse httpResponse)
						{
							// checks if server response is valid
							StatusLine status = httpResponse.getStatusLine();
							
							if (status.getStatusCode() == HttpStatus.SC_OK)
							{
								httpRequestBase.abort();
								Assert.assertTrue("success", true);
							}
							else
							{
								httpRequestBase.abort();
								Assert.fail("unable to download file from URL '" + httpRequestBase.getURI() + "', HTTP status : " + status.getStatusCode());
							}
						}
						
						@Override
						public void onError(Exception e)
						{
							Assert.fail(e.getMessage());
						}
					});
		}
		
		WebAPIClientUtils.shutdownHttpClient(httpClient);
	}
}
