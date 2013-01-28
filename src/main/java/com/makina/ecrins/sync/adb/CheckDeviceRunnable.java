package com.makina.ecrins.sync.adb;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import org.apache.log4j.Logger;

import com.makina.ecrins.sync.service.Status;

/**
 * <code>Runnable</code> implementation for checking device status through adb command line periodically.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class CheckDeviceRunnable extends Observable implements Runnable
{
	private static final Logger LOG = Logger.getLogger(CheckDeviceRunnable.class);
	
	private Status status;
	
	public CheckDeviceRunnable()
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
		try
		{
			if (getStatus().equals(Status.STATUS_NONE))
			{
				setStatus(Status.STATUS_PENDING);
			}
			
			if (ADBCommand.getInstance().getDevices().isEmpty())
			{
				if (!getStatus().equals(Status.STATUS_PENDING))
				{
					setStatus(Status.STATUS_PENDING);
				}
			}
			else
			{
				if (!getStatus().equals(Status.STATUS_CONNECTED))
				{
					setStatus(Status.STATUS_CONNECTED);
				}
			}
		}
		catch (InterruptedException ie)
		{
			LOG.error(ie.getMessage(), ie);
			setStatus(Status.STATUS_FAILED);
		}
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
			setStatus(Status.STATUS_FAILED);
		}
	}
}
