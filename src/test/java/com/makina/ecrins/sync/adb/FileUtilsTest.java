package com.makina.ecrins.sync.adb;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for {@link FileUtils}
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class FileUtilsTest
{
	private static final Logger LOG = Logger.getLogger(FileUtilsTest.class);
	
	@Test
	public void getExternalStorageDirectory()
	{
		String externalStorage = FileUtils.getExternalStorageDirectory();
		
		LOG.debug("external storage : " + externalStorage);
		
		Assert.assertNotNull(externalStorage);
		Assert.assertTrue(externalStorage.startsWith("/mnt/"));
	}
}
