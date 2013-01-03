package com.makina.ecrins.sync.adb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Logger;

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
				
				startServer();
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
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public List<String> getDevices() throws InterruptedException, IOException
	{
		List<String> devices = new ArrayList<String>();
		
		startServer();
		
		ProcessBuilder pb = new ProcessBuilder(adbCommandPath, "devices");
		Process p = pb.start();
		boolean firstLine = true;
		
		for (String line : IOUtils.readLines(p.getInputStream()))
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
	 * Gets the version number
	 * 
	 * @return the version number
	 * @throws IOException 
	 */
	public String getVersion() throws IOException
	{
		ProcessBuilder pb = new ProcessBuilder(adbCommandPath, "version");
		Process p = pb.start();
		
		return IOUtils.toString(p.getInputStream()).trim();
	}
	
	/**
	 * Blocks until device is connected
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void waitForDevice() throws InterruptedException, IOException
	{
		startServer();
		
		ProcessBuilder pb = new ProcessBuilder(adbCommandPath, "wait-for-device");
		pb.start().waitFor();
	}
	
	/**
	 * Ensures that there is a server running
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public void startServer() throws IOException, InterruptedException
	{
		ProcessBuilder pb = new ProcessBuilder(adbCommandPath, "start-server");
		pb.start().waitFor();
	}
	
	/**
	 * Kills the server if it is running
	 * 
	 * @throws IOException
	 * @throws InterruptedException 
	 */
	public void killServer() throws IOException, InterruptedException
	{
		ProcessBuilder pb = new ProcessBuilder(adbCommandPath, "kill-server");
		pb.start().waitFor();
	}
	
	private synchronized static File extractAdbCommand() throws IOException, InterruptedException, UnsupportedOSVersionException
	{
		File tempDir = new File(FileUtils.getTempDirectory(), "sync_" + Long.toString(System.currentTimeMillis()));
		tempDir.mkdir();
		FileUtils.forceDeleteOnExit(tempDir);
		
		File adbCommandFile = new File(tempDir, "adb");
		
		if (SystemUtils.IS_OS_WINDOWS)
		{
			adbCommandFile = new File(tempDir, "adb.exe");
			FileUtils.copyInputStreamToFile(ClassLoader.getSystemResourceAsStream("AdbWinApi.dll"), new File(tempDir, "AdbWinApi.dll"));
			FileUtils.copyInputStreamToFile(ClassLoader.getSystemResourceAsStream("AdbWinUsbApi.dll"), new File(tempDir, "AdbWinUsbApi.dll"));
		}
		
		FileUtils.copyInputStreamToFile(getAdbCommandFromSystemResource(), adbCommandFile);
		
		if (!SystemUtils.IS_OS_WINDOWS)
		{
			ProcessBuilder pb = new ProcessBuilder("chmod", "u+x", adbCommandFile.getAbsolutePath());
			pb.start().waitFor();
		}
		
		return adbCommandFile;
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