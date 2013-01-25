package com.makina.ecrins.sync.adb;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.FileUtils;
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
	public void pushAndPullTest()
	{
		try
		{
			// creates the temporary directory to use for copying files to the connected device
			File tempDir = new File(FileUtils.getTempDirectory(), "sync_" + Long.toString(System.currentTimeMillis()));
			tempDir.mkdir();
			FileUtils.forceDeleteOnExit(tempDir);
			
			// gets the input sample file to copy
			File inputResourceJson = FileUtils.toFile(getClass().getResource("/input_1234.json"));
			Assert.assertTrue(inputResourceJson.exists());
			
			// copy the sample file to the temporary directory
			File inputJson = new File(tempDir, "input_1234.json");
			FileUtils.copyFile(inputResourceJson, inputJson);
			LOG.debug("input JSON : " + inputJson.getAbsolutePath());
			Assert.assertTrue(inputJson.exists());
			
			File inputJsonFromDevice = new File(tempDir, "input_1234_copy.json");
			ADBCommand.getInstance().push(inputJson.getAbsolutePath(), com.makina.ecrins.sync.adb.FileUtils.getExternalStorageDirectory() + "Android/data/sync/input_1234.json");
			ADBCommand.getInstance().pull(com.makina.ecrins.sync.adb.FileUtils.getExternalStorageDirectory() + "Android/data/sync/input_1234.json", inputJsonFromDevice.getAbsolutePath());
			
			LOG.debug("input JSON from device : " + inputJsonFromDevice.getAbsolutePath());
			LOG.debug("input_1234.json checksum : " + FileUtils.checksumCRC32(inputJson));
			LOG.debug("input_1234_copy.json checksum : " + FileUtils.checksumCRC32(inputJsonFromDevice));
			
			Assert.assertEquals(FileUtils.checksumCRC32(inputJson), FileUtils.checksumCRC32(inputJsonFromDevice));
		}
		catch (IOException ioe)
		{
			LOG.error(ioe.getMessage(), ioe);
			Assert.fail(ioe.getMessage());
		}
		catch (InterruptedException ie)
		{
			LOG.error(ie.getMessage(), ie);
			Assert.fail(ie.getMessage());
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
