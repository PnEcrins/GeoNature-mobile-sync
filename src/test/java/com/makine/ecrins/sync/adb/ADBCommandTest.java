package com.makine.ecrins.sync.adb;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.makina.ecrins.sync.adb.ADBCommand;

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
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
			Assert.fail(ioe.getMessage());
		}
	}
}
