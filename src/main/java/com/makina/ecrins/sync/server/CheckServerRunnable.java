package com.makina.ecrins.sync.server;

import com.makina.ecrins.sync.server.WebAPIClientUtils.HTTPCallback;
import com.makina.ecrins.sync.service.Status;
import com.makina.ecrins.sync.settings.LoadSettingsCallable;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.util.Observable;
import java.util.Observer;

/**
 * <code>Runnable</code> implementation for checking server status periodically.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class CheckServerRunnable
        extends Observable
        implements Runnable
{
    private static final Logger LOG = Logger.getLogger(CheckServerRunnable.class);

    private Status status;

    public CheckServerRunnable()
    {
        this.status = Status.NONE;
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
        if (getStatus().equals(Status.NONE))
        {
            setStatus(Status.PENDING);
        }

        final HttpClient httpClient = WebAPIClientUtils.getHttpClient(
                LoadSettingsCallable.getInstance()
                        .getServerSettings()
                        .getServerTimeout()
        );
        WebAPIClientUtils.httpPost(
                httpClient,
                LoadSettingsCallable.getInstance()
                        .getServerSettings()
                        .getServerUrl() + LoadSettingsCallable.getInstance()
                        .getSettings()
                        .getSyncSettings()
                        .getStatusUrl(),
                LoadSettingsCallable.getInstance()
                        .getServerSettings()
                        .getServerToken(),
                true,
                new HTTPCallback()
                {
                    @Override
                    public void onResponse(HttpRequestBase httpRequestBase,
                                           HttpResponse httpResponse)
                    {
                        // checks if server response is valid
                        StatusLine status = httpResponse.getStatusLine();

                        if (status.getStatusCode() == HttpStatus.SC_OK)
                        {
                            try
                            {
                                // pulls content stream from response
                                final HttpEntity entity = httpResponse.getEntity();

                                InputStream is = entity.getContent();
                                JSONObject jsonResponse = new JSONObject(IOUtils.toString(is));

                                if (jsonResponse.getInt("status_code") == 0)
                                {
                                    if (!getStatus().equals(Status.CONNECTED))
                                    {
                                        setStatus(Status.CONNECTED);
                                    }
                                }
                                else
                                {
                                    setStatus(Status.FAILED);
                                }

                                // ensure that the response body is fully consumed
                                EntityUtils.consume(entity);
                            }
                            catch (IllegalStateException ise)
                            {
                                LOG.error(ise.getMessage());
                                setStatus(Status.FAILED);
                            }
                            catch (IOException ioe)
                            {
                                LOG.error(ioe.getMessage());
                                setStatus(Status.FAILED);
                            }
                            catch (JSONException je)
                            {
                                LOG.error(je.getMessage());
                                setStatus(Status.FAILED);
                            }
                        }
                        else
                        {
                            LOG.warn("unable to check server status from URL : " + httpRequestBase.getURI() + ", HTTP status : " + status.getStatusCode());
                            setStatus(Status.FAILED);
                        }
                    }

                    @Override
                    public void onError(Exception e)
                    {
                        LOG.error(e.getCause().getLocalizedMessage());
                        setStatus(Status.FAILED);
                    }
                }
        );
    }
}
