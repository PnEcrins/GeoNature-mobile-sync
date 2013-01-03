package com.makina.ecrins.sync.adb;

import java.io.IOException;
import java.util.Observer;

import org.apache.log4j.Logger;

import com.makina.ecrins.sync.service.Status;
import com.makina.ecrins.sync.service.StatusObservable;

/**
 * <code>Runnable</code> implementation for checking device status through adb command line periodically.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class CheckDeviceRunnable implements Runnable
{
	private static final Logger LOG = Logger.getLogger(CheckDeviceRunnable.class);
	
	private StatusObservable statusObservable;
	private Status status;
	
	public CheckDeviceRunnable()
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
		try
		{
			if (getStatus().equals(Status.STATUS_NONE))
			{
				this.status = Status.STATUS_PENDING;
				this.statusObservable.update(getStatus());
			}
			
			if (ADBCommand.getInstance().getDevices().isEmpty())
			{
				if (!getStatus().equals(Status.STATUS_PENDING))
				{
					this.status = Status.STATUS_PENDING;
					this.statusObservable.update(getStatus());
				}
			}
			else
			{
				if (!getStatus().equals(Status.STATUS_CONNECTED))
				{
					this.status = Status.STATUS_CONNECTED;
					this.statusObservable.update(getStatus());
				}
			}
		}
		catch (InterruptedException ie)
		{
			LOG.error(ie.getMessage(), ie);
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
