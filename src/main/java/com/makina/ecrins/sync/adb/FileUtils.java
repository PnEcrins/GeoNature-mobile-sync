package com.makina.ecrins.sync.adb;

import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * Helpers for File utilities using adb command line.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class FileUtils
{
	private static final Logger LOG = Logger.getLogger(FileUtils.class);
	
	/**
	 * {@link FileUtils} instances should NOT be constructed in standard programming.
	 */
	private FileUtils()
	{
		
	}
	
	/**
	 * Tries to find the mount path used by external storage using adb command line.
	 * If not, returns the default mount path '/mnt/sdcard'.
	 * @return the mount path used by external storage.
	 */
	public static String getExternalStorageDirectory()
	{
		String externalStorage = null;
		
		try
		{
			Iterator<String> iterator = ADBCommand.getInstance().executeCommand("cat /proc/mounts").iterator();
			
			while (iterator.hasNext() && (externalStorage == null))
			{
				String line = iterator.next();
				
				if (line.startsWith("/dev/block/vold/"))
				{
					// device mount_path fs_type options
					String[] lineElements = line.split(" ");
					// gets the mount path
					String element = lineElements[1];
					
					// ignore default mount path and others
					if (!element.equals("/mnt/sdcard") && !element.equals("/mnt/secure/asec"))
					{
						externalStorage = element;
					}
				}
			}
		}
		catch (IOException ioe)
		{
			LOG.warn(ioe.getMessage(),  ioe);
		}
		catch (InterruptedException ie)
		{
			LOG.warn(ie.getMessage(),  ie);
		}
		
		if (externalStorage == null)
		{
			externalStorage = "/mnt/sdcard/";
		}
		
		return externalStorage;
	}
}
