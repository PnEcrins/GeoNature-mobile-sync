package com.makina.ecrins.sync.adb;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for {@link ADBCommand}
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ADBCommandTest
{
	private static final Logger LOG = Logger.getLogger(ADBCommandTest.class);
	
	@Test
	public void getDevicesTest()
	{
		try
		{
			List<String> devices = ADBCommand.getInstance().getDevices();
			Assert.assertTrue(devices.size() >= 0);
		}
		catch (InterruptedException ie)
		{
			LOG.error(ie.getMessage(), ie);
			Assert.fail(ie.getMessage());
		}
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
			Assert.fail(ioe.getMessage());
		}
	}
	
	@Test
	public void getStateTest()
	{
		try
		{
			String state = ADBCommand.getInstance().getState();
			
			LOG.debug("device state : " + state);
			
			Assert.assertEquals(ADBCommand.STATE_DEVICE, state);
		}
		catch (InterruptedException ie)
		{
			LOG.error(ie.getMessage(), ie);
			Assert.fail(ie.getMessage());
		}
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
			Assert.fail(ioe.getMessage());
		}
	}
	
	@Test
	public void getBuildVersionTest()
	{
		try
		{
			int buildVersion = ADBCommand.getInstance().getBuildVersion();
			
			LOG.debug("build version : " + buildVersion);
			
			Assert.assertTrue(buildVersion > 0);
		}
		catch (InterruptedException ie)
		{
			LOG.error(ie.getMessage(), ie);
			Assert.fail(ie.getMessage());
		}
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
			Assert.fail(ioe.getMessage());
		}
	}
}
