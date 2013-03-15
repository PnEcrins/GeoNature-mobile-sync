package com.makina.ecrins.sync.tasks;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for {@link ApkUtils}
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ApkUtilsTest
{
	private static final Logger LOG = Logger.getLogger(ApkUtilsTest.class);
	
	@Test
	public void getExternalStorageDirectory()
	{
		String externalStorage = ApkUtils.getExternalStorageDirectory();
		
		LOG.debug("external storage : " + externalStorage);
		
		Assert.assertNotNull(externalStorage);
		Assert.assertTrue(externalStorage.startsWith("/mnt/"));
	}
}
