package com.makina.ecrins.sync.tasks;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test class for {@link DeviceUtils}
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class DeviceUtilsTest
{
	private static final Logger LOG = Logger.getLogger(DeviceUtilsTest.class);
	
	@Test
	public void getExternalStorageDirectory()
	{
		String externalStorage = DeviceUtils.getExternalStorageDirectory();
		
		LOG.debug("external storage : " + externalStorage);
		
		Assert.assertNotNull(externalStorage);
		Assert.assertTrue(externalStorage.startsWith("/mnt/"));
	}
}
