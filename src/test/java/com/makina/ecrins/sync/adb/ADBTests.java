package com.makina.ecrins.sync.adb;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses(
{
	ADBCommandTest.class,
	FileUtilsTest.class
})
public class ADBTests
{
	private static final Logger LOG = Logger.getLogger(ADBTests.class);
	
	@BeforeClass
	public static void init()
	{
		LOG.debug("init");
		
		ADBCommand.getInstance();
	}
	
	@AfterClass
	public static void release()
	{
		LOG.debug("release");
		
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
