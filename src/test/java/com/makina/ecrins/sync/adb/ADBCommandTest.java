package com.makina.ecrins.sync.adb;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test class for {@link ADBCommand}
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ADBCommandTest
{
	private static final Logger LOG = Logger.getLogger(ADBCommandTest.class);
	
	@BeforeClass
	public static void init()
	{
		ADBCommand.getInstance();
	}
	
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
	
	@AfterClass
	public static void release()
	{
		try
		{
			ADBCommand.getInstance().killServer();
		}
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
		}
		catch (InterruptedException ie)
		{
			LOG.error(ie.getMessage(), ie);
			Assert.fail(ie.getMessage());
		}
	}
}
