package com.makina.ecrins.sync.adb;

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
		try
		{
			if (getStatus().equals(Status.NONE))
			{
				setStatus(Status.PENDING);
			}
			
			if (ADBCommand.getInstance().getDevices().isEmpty())
			{
				setStatus(Status.PENDING);
			}
			else
			{
				String deviceState = ADBCommand.getInstance().getState();
				
				if (deviceState.equals(ADBCommand.STATE_DEVICE))
				{
					setStatus(Status.CONNECTED);
				}
				else
				{
					setStatus(Status.PENDING);
				}
			}
		}
		catch (ADBCommandException ace)
		{
			LOG.error(ace.getMessage(), ace);
			setStatus(Status.FAILED);
		}
	}
}
