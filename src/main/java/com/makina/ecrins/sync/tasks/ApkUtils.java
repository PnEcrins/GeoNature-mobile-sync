package com.makina.ecrins.sync.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.makina.ecrins.sync.adb.ADBCommand;

/**
 * Helpers for Android application packages using adb command line.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ApkUtils
{
	private static final Logger LOG = Logger.getLogger(ApkUtils.class);
	
	/**
	 * {@link ApkUtils} instances should NOT be constructed in standard programming.
	 */
	private ApkUtils()
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
						externalStorage = element + "/";
					}
				}
			}
		}
		catch (IOException ioe)
		{
			LOG.warn(ioe.getMessage(), ioe);
		}
		catch (InterruptedException ie)
		{
			LOG.warn(ie.getMessage(), ie);
		}
		
		if (externalStorage == null)
		{
			externalStorage = "/mnt/sdcard/";
		}
		
		return externalStorage;
	}
	
	/**
	 * Tries to find the mount path used by external storage using adb command line.
	 * If not, returns the default mount path '/mnt/sdcard/'.
	 * @return the mount path used by external storage.
	 */
	public static String getExternalStorageDirectory(ApkInfo apkInfo)
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
					
					if (!element.equals("/mnt/secure/asec"))
					{
						String testPath = element + File.separator + "Android" + File.separator + "data" + File.separator + apkInfo.getSharedUserId();
						
						List<String> results = ADBCommand.getInstance().executeCommand("[ -d " + testPath + " ] && echo '1' || echo '0'");
						
						if (!results.isEmpty() && Integer.valueOf(results.get(0)).intValue() == 1)
						{
							externalStorage = element + "/";
						}
						else
						{
							// ignore default mount path and others
							if (!element.equals("/mnt/sdcard"))
							{
								externalStorage = element + "/";
							}
						}
					}
				}
			}
		}
		catch (IOException ioe)
		{
			LOG.warn(ioe.getMessage(), ioe);
		}
		catch (InterruptedException ie)
		{
			LOG.warn(ie.getMessage(), ie);
		}
		
		if (externalStorage == null)
		{
			externalStorage = "/mnt/sdcard/";
		}
		
		return externalStorage;
	}
	
	/**
	 * Reads and parse <code>version.json</code> from a given JSON file and builds a {@link List} of {@link ApkInfo}.
	 * @param jsonFile JSON file to parse
	 * @return a {@link List} of {@link ApkInfo} parsed from <code>version.json</code> or empty list if <code>version.json</code> cannot be found
	 */
	public static List<ApkInfo> getApkInfosFromJson(File jsonFile)
	{
		final List<ApkInfo> apkInfos = new ArrayList<ApkInfo>();
		
		try
		{
			JSONObject appsVersionsJson = new JSONObject(FileUtils.readFileToString(jsonFile));
			JSONArray apksInfos = appsVersionsJson.getJSONArray("apps");
			
			for (int i = 0; i < apksInfos.length(); i++)
			{
				apkInfos.add(new ApkInfo(apksInfos.getJSONObject(i)));
			}
		}
		catch (JSONException je)
		{
			LOG.warn(je);
		}
		catch (IOException ioe)
		{
			LOG.warn(ioe);
		}
		
		return apkInfos;
	}
}

