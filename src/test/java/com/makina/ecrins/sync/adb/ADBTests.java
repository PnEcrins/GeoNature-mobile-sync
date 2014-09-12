package com.makina.ecrins.sync.adb;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.makina.ecrins.sync.tasks.DeviceUtilsTest;

/**
 * Tests suite for {@link ADBCommandTest} and {@link DeviceUtilsTest}.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(
{
	ADBCommandTest.class,
	DeviceUtilsTest.class
})
public class ADBTests
{
	private static final Logger LOG = Logger.getLogger(ADBTests.class);
	
	@BeforeClass
	public static void init()
	{
		LOG.debug("init");
		
		try
		{
			ADBCommand.getInstance().waitForDevice();
		}
		catch (ADBCommandException ace)
		{
			LOG.error(ace.getMessage(), ace);
			Assert.fail(ace.getMessage());
		}
	}
	
	@AfterClass
	public static void release()
	{
		LOG.debug("release");
		
		try
		{
			ADBCommand.getInstance().dispose();
		}
		catch (ADBCommandException ace)
		{
			LOG.warn(ace.getMessage(), ace);
		}
	}
}
