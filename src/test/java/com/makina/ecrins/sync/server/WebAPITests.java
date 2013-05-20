package com.makina.ecrins.sync.server;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.makina.ecrins.sync.settings.LoadSettingsCallable;

/**
 * Tests suite for {@link WebAPIClientUtilsTest}.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
@RunWith(Suite.class)
@Suite.SuiteClasses(
{
	WebAPIClientUtilsTest.class
})
public class WebAPITests
{
	private static final Logger LOG = Logger.getLogger(WebAPITests.class);
	
	@BeforeClass
	public static void init()
	{
		try
		{
			LoadSettingsCallable.getInstance().call();
		}
		catch (Exception e)
		{
			LOG.error(e.getMessage(), e);
			Assert.fail(e.getMessage());
		}
	}
}
