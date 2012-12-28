package com.makina.ecrins.sync.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observer;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.log4j.Logger;
import org.json.JSONException;

import com.makina.ecrins.sync.service.Status;
import com.makina.ecrins.sync.service.StatusObservable;
import com.makina.ecrins.sync.settings.LoadSettingsCallable;

/**
 * <code>Runnable</code> implementation for checking server status periodically.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class CheckServerRunnable implements Runnable
{
	private static final Logger LOG = Logger.getLogger(CheckServerRunnable.class);
	
	private StatusObservable statusObservable;
	private Status status;
	
	public CheckServerRunnable()
	{
		this.status = Status.STATUS_NONE;
		this.statusObservable = new StatusObservable();
	}
	
	public void addObserver(final Observer observer)
	{
		this.statusObservable.addObserver(observer);
		this.statusObservable.update(getStatus());
	}
	
	public Status getStatus()
	{
		return status;
	}
	
	@Override
	public void run()
	{
		if (getStatus().equals(Status.STATUS_NONE))
		{
			this.status = Status.STATUS_PENDING;
			this.statusObservable.update(getStatus());
		}
		
		try
		{
			final DefaultHttpClient httpClient = new DefaultHttpClient();
			final HttpParams httpParameters = httpClient.getParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, 5000);
			HttpConnectionParams.setSoTimeout(httpParameters, 5000);
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			nameValuePairs.add(new BasicNameValuePair("token", LoadSettingsCallable.getInstance().getJsonSettings().getJSONObject(LoadSettingsCallable.KEY_SYNC).getString(LoadSettingsCallable.KEY_TOKEN)));
			
			String urlStatus = LoadSettingsCallable.getInstance().getJsonSettings().getJSONObject(LoadSettingsCallable.KEY_SYNC).getString(LoadSettingsCallable.KEY_SERVER_URL) + LoadSettingsCallable.getInstance().getJsonSettings().getJSONObject(LoadSettingsCallable.KEY_SYNC).getString(LoadSettingsCallable.KEY_STATUS_URL);
			
			HttpPost httpPost = new HttpPost(urlStatus);
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			
			HttpResponse httpResponse = httpClient.execute(httpPost);
			
			// checks if server response is valid
			StatusLine status = httpResponse.getStatusLine();
			
			if (status.getStatusCode() == HttpStatus.SC_OK)
			{
				// TODO: use JSON response to check server status
				// pulls content stream from response
				/*
				HttpEntity entity = httpResponse.getEntity();
				InputStream is = entity.getContent();
				String jsonStatus = FileUtils.readInputStreamAsString(is);
				*/
				if (!getStatus().equals(Status.STATUS_CONNECTED))
				{
					this.status = Status.STATUS_CONNECTED;
					this.statusObservable.update(getStatus());
				}
			}
			else
			{
				LOG.warn("unable to check server status from URL : " + urlStatus + ", HTTP status : " + status.getStatusCode());
				this.status = Status.STATUS_FAILED;
				this.statusObservable.update(getStatus());
			}
		}
		catch (JSONException je)
		{
			LOG.error(je.getMessage(), je);
			this.status = Status.STATUS_FAILED;
			this.statusObservable.update(getStatus());
		}
		catch (UnsupportedEncodingException uee)
		{
			LOG.error(uee.getMessage(), uee);
			this.status = Status.STATUS_FAILED;
			this.statusObservable.update(getStatus());
		}
		catch (ClientProtocolException cpe)
		{
			LOG.error(cpe.getMessage(), cpe);
			this.status = Status.STATUS_FAILED;
			this.statusObservable.update(getStatus());
		}
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
			this.status = Status.STATUS_FAILED;
			this.statusObservable.update(getStatus());
		}
	}
}
