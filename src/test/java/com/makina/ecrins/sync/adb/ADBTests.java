package com.makina.ecrins.sync.adb;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.makina.ecrins.sync.tasks.ApkUtilsTest;

/**
 * Tests suite for {@link ADBCommandTest} and {@link ApkUtilsTest}.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(
{
	ADBCommandTest.class,
	ApkUtilsTest.class
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
	
	@AfterClass
	public static void release()
	{
		LOG.debug("release");
		
		ADBCommand.getInstance().dispose();
	}
}
