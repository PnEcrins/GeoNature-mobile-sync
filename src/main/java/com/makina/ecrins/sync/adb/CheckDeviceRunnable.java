package com.makina.ecrins.sync.adb;

import java.text.MessageFormat;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

import com.makina.ecrins.sync.adb.ADBCommand.Prop;
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
			notifyObservers(this.status);
			
			switch (this.status)
			{
				case CONNECTED :
					try
					{
						LOG.info(
								MessageFormat.format(
										ResourceBundle.getBundle("messages").getString("MainWindow.shell.device.found.text"),
										ADBCommand.getInstance().getProp(Prop.RO_PRODUCT_MANUFACTURER),
										ADBCommand.getInstance().getProp(Prop.RO_PRODUCT_MODEL),
										ADBCommand.getInstance().getProp(Prop.RO_PRODUCT_NAME),
										ADBCommand.getInstance().getProp(Prop.RO_BUILD_VERSION_RELEASE),
										ADBCommand.getInstance().getBuildVersion()
								)
						);
					}
					catch (ADBCommandException ace)
					{
						LOG.warn(ace.getMessage(), ace);
					}
					
					break;
				default :
					break;
			}
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
