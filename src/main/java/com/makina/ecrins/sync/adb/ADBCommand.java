package com.makina.ecrins.sync.adb;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;

import com.makina.ecrins.sync.util.FileUtils;

/**
 * Wrapper class used for invoking adb command.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ADBCommand
{
	private static final Logger LOG = Logger.getLogger(ADBCommand.class);
	
	private String adbCommandPath = null;
	
	private ADBCommand()
	{
		if (adbCommandPath == null)
		{
			try
			{
				adbCommandPath = extractAdbCommand().getAbsolutePath();
				
				LOG.debug("using adb command '" + adbCommandPath + "'");
				LOG.info(getVersion());
			}
			catch (IOException ioe)
			{
				LOG.error(ioe.getMessage(), ioe);
			}
			catch (InterruptedException ie)
			{
				LOG.error(ie.getMessage(), ie);
			}
			catch (UnsupportedOSVersionException uosve)
			{
				LOG.error(uosve.getMessage(), uosve);
			}
		}
	}

	public static ADBCommand getInstance()
	{
		return ADBCommandHolder.instance;
	}
	
	/**
	 * Lists all devices currently connected.
	 * 
	 * @return a <code>List</code> of connected devices
	 * @throws IOException 
	 */
	public List<String> getDevices() throws IOException
	{
		ProcessBuilder pb = new ProcessBuilder(adbCommandPath, "devices");
		
		List<String> devices = new ArrayList<String>();
		
		Process p = pb.start();
		
		BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String line = "";
		boolean firstLine = true;
		
		while ((line = br.readLine()) != null)
		{
			LOG.debug(line);
			
			if (firstLine)
			{
				firstLine = false;
			}
			else
			{
				if (!line.isEmpty())
				{
					devices.add(line);
				}
			}
		}

		return devices;
	}
	
	/**
	 * Blocks until device is connected.
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void waitForDevice() throws InterruptedException, IOException
	{
		ProcessBuilder pb = new ProcessBuilder(adbCommandPath, "wait-for-device");
		pb.start().waitFor();
	}
	
	/**
	 * Gets the version number
	 * 
	 * @return the version number
	 * @throws IOException 
	 */
	public String getVersion() throws IOException
	{
		ProcessBuilder pb = new ProcessBuilder(adbCommandPath, "version");
		Process p = pb.start();
		
		return FileUtils.readInputStreamAsString(p.getInputStream());
	}
	
	private synchronized static File extractAdbCommand() throws IOException, InterruptedException, UnsupportedOSVersionException
	{
		File tempFile = File.createTempFile("adb_", Long.toString(System.currentTimeMillis()));
		tempFile.deleteOnExit();
		
		final byte[] buf = new byte[1024];
		InputStream is = null;
		OutputStream os = null;
		
		try
		{
			is = getAdbCommandFromSystemResource();
			os = new FileOutputStream(tempFile);
			int i = 0;
			
			while ((i = is.read(buf)) != -1)
			{
				os.write(buf, 0, i);
			}
		}
		finally
		{
			if (is != null)
			{
				is.close();
			}
			
			if (os != null)
			{
				os.close();
			}
		}
		
		if (!SystemUtils.IS_OS_WINDOWS)
		{
			ProcessBuilder pb = new ProcessBuilder("chmod", "u+x", tempFile.getAbsolutePath());
			pb.start().waitFor();
		}
		
		return tempFile;
	}
	
	private static InputStream getAdbCommandFromSystemResource() throws UnsupportedOSVersionException
	{
		LOG.info(SystemUtils.OS_NAME + " (" + SystemUtils.OS_ARCH + ", version : " + SystemUtils.OS_VERSION + ")");
		
		if (SystemUtils.IS_OS_WINDOWS)
		{
			if (SystemUtils.OS_ARCH.equalsIgnoreCase("x86"))
			{
				return ClassLoader.getSystemResourceAsStream("adb-win32-" + SystemUtils.OS_ARCH + "_1.0.31.exe");
			}
			else
			{
				throw new UnsupportedOSVersionException();
			}
		}
		else if (SystemUtils.IS_OS_LINUX)
		{
			if (SystemUtils.OS_ARCH.equalsIgnoreCase("i386"))
			{
				return ClassLoader.getSystemResourceAsStream("adb-linux-x86_1.0.31");
			}
			else
			{
				return ClassLoader.getSystemResourceAsStream("adb-linux-x86_64_1.0.31");
			}
		}
		else if (SystemUtils.IS_OS_MAC_OSX)
		{
			return ClassLoader.getSystemResourceAsStream("adb-macosx-cocoa_1.0.31");
		}
		else
		{
			throw new UnsupportedOSVersionException();
		}
	}
	
	private static class ADBCommandHolder
	{
		private final static ADBCommand instance = new ADBCommand();
	}
}