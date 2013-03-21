package com.makina.ecrins.sync.server;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
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
import org.json.JSONObject;

import com.makina.ecrins.sync.service.Status;
import com.makina.ecrins.sync.settings.LoadSettingsCallable;

/**
 * <code>Runnable</code> implementation for checking server status periodically.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class CheckServerRunnable extends Observable implements Runnable
{
	private static final Logger LOG = Logger.getLogger(CheckServerRunnable.class);
	
	private Status status;
	
	public CheckServerRunnable()
	{
		this.status = Status.STATUS_NONE;
	}
	
	@Override
	public synchronized void addObserver(Observer o)
	{
		super.addObserver(o);
		
		setChanged();
		notifyObservers(getStatus());
	}

	public Status getStatus()
	{
		return status;
	}

	protected void setStatus(Status status)
	{
		if (!this.status.equals(status))
		{
			this.status = status;
			setChanged();
			notifyObservers(getStatus());
		}
	}
	
	@Override
	public void run()
	{
		if (getStatus().equals(Status.STATUS_NONE))
		{
			setStatus(Status.STATUS_PENDING);
		}
		
		try
		{
			final DefaultHttpClient httpClient = new DefaultHttpClient();
			final HttpParams httpParameters = httpClient.getParams();
			HttpConnectionParams.setConnectionTimeout(httpParameters, LoadSettingsCallable.getInstance().getSyncSettings().getServerTimeout());
			//HttpConnectionParams.setSoTimeout(httpParameters, 5000);
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(1);
			nameValuePairs.add(new BasicNameValuePair("token", LoadSettingsCallable.getInstance().getSyncSettings().getServerToken()));
			
			String urlStatus =	LoadSettingsCallable.getInstance().getSyncSettings().getServerUrl() +
								LoadSettingsCallable.getInstance().getSyncSettings().getStatusUrl();
			
			HttpPost httpPost = new HttpPost(urlStatus);
			httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
			
			HttpResponse httpResponse = httpClient.execute(httpPost);
			
			// checks if server response is valid
			StatusLine status = httpResponse.getStatusLine();
			
			if (status.getStatusCode() == HttpStatus.SC_OK)
			{
				HttpEntity entity = httpResponse.getEntity();
				InputStream is = entity.getContent();
				JSONObject jsonResponse = new JSONObject(IOUtils.toString(is));
				
				LOG.debug("status_code : " + jsonResponse.getInt("status_code"));
				
				if (jsonResponse.getInt("status_code") == 0)
				{
					if (!getStatus().equals(Status.STATUS_CONNECTED))
					{
						setStatus(Status.STATUS_CONNECTED);
					}
				}
				else
				{
					setStatus(Status.STATUS_FAILED);
				}
			}
			else
			{
				LOG.warn("unable to check server status from URL : " + urlStatus + ", HTTP status : " + status.getStatusCode());
				setStatus(Status.STATUS_FAILED);
			}
		}
		catch (JSONException je)
		{
			LOG.error(je.getMessage(), je);
			setStatus(Status.STATUS_FAILED);
		}
		catch (UnsupportedEncodingException uee)
		{
			LOG.error(uee.getMessage(), uee);
			setStatus(Status.STATUS_FAILED);
		}
		catch (ClientProtocolException cpe)
		{
			LOG.error(cpe.getMessage(), cpe);
			setStatus(Status.STATUS_FAILED);
		}
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
			setStatus(Status.STATUS_FAILED);
		}
	}
}
