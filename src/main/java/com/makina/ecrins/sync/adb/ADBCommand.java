package com.makina.ecrins.sync.adb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecuteResultHandler;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.makina.ecrins.sync.logger.LoggingOutputStream;

/**
 * Wrapper class used for invoking adb command.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ADBCommand
{
	public static final String STATE_OFFLINE = "offline";
	public static final String STATE_BOOTLOADER = "bootloader";
	public static final String STATE_DEVICE = "device";
	
	private static final Logger LOG = Logger.getLogger(ADBCommand.class);
	
	private File adbCommandFile = null;
	private boolean isDisposed = false;
	
	private ADBCommand()
	{
		if (adbCommandFile == null)
		{
			try
			{
				adbCommandFile = extractAdbCommand();
				
				LOG.debug("using adb command '" + adbCommandFile.getAbsolutePath() + "'");
				LOG.info(getVersion());
				
				if (!SystemUtils.IS_OS_WINDOWS)
				{
					killServer();
				}
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
			catch (ADBCommandException ace)
			{
				LOG.error(ace.getMessage(), ace);
			}
		}
	}

	public static ADBCommand getInstance()
	{
		try
		{
			ADBCommandHolder.instance.startServer();
		}
		catch (ADBCommandException ace)
		{
			LOG.error(ace.getMessage(), ace);
		}
		
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
		
		ProcessBuilder pb = new ProcessBuilder(adbCommandFile.getAbsolutePath(), "devices");
		Process p = pb.start();
		boolean firstLine = true;
		
		for (String line : IOUtils.readLines(p.getInputStream()))
		{
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
	 * Copy file or a directory to the connected device.
	 * 
	 * @param localPath local path to use for the copy
	 * @param remotePath remote path from the connected device
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public void push(String localPath, String remotePath) throws InterruptedException, IOException
	{
		CommandLine cmdLine = new CommandLine(adbCommandFile.getAbsolutePath());
		cmdLine.addArgument("push");
		cmdLine.addArgument(localPath);
		cmdLine.addArgument(remotePath);
		
		LOG.debug("push : " + cmdLine.toString());
		
		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
		
		ExecuteWatchdog watchdog = new ExecuteWatchdog(10 * 1000);
		Executor executor = new DefaultExecutor();
		executor.setExitValue(1);
		executor.setWatchdog(watchdog);
		
		executor.execute(cmdLine, resultHandler);
		resultHandler.waitFor();
	}
	
	/**
	 * Copy file or a directory from device to a given local folder.
	 * 
	 * @param remotePath remote path from the connected device to copy
	 * @param localPath local path to use for the copy
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public void pull(String remotePath, String localPath) throws InterruptedException, IOException
	{
		CommandLine cmdLine = new CommandLine(adbCommandFile.getAbsolutePath());
		cmdLine.addArgument("pull");
		cmdLine.addArgument(remotePath);
		cmdLine.addArgument(localPath);
		
		LOG.debug("pull : " + cmdLine.toString());
		
		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
		
		ExecuteWatchdog watchdog = new ExecuteWatchdog(10 * 1000);
		Executor executor = new DefaultExecutor();
		executor.setExitValue(1);
		executor.setWatchdog(watchdog);
		
		executor.execute(cmdLine, resultHandler);
		resultHandler.waitFor();
	}
	
	/**
	 * Executes a remote shell command
	 * 
	 * @param command the command to execute
	 * @return the output as <code>List</code>
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public List<String> executeCommand(String command) throws IOException, InterruptedException
	{
		List<String> output = new ArrayList<String>();
		
		ProcessBuilder pb = new ProcessBuilder(adbCommandFile.getAbsolutePath(), "shell", command);
		LOG.debug("executeCommand : " + pb.command().toString());
		
		Process p = pb.start();
		
		for (String line : IOUtils.readLines(p.getInputStream()))
		{
			output.add(line);
		}
		
		return output;
	}
	
	/**
	 * Tries to get the size of a given filename.
	 * @param filename the name of the file on which we want to obtain its current size
	 * @return the file size in bytes
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public long getFileSize(String filename) throws IOException, InterruptedException
	{
		List<String> results = executeCommand("ls -la " + filename);
		
		if (results.size() > 0)
		{
			String[] tokens = StringUtils.split(results.get(0));
			
			if ((tokens.length > 4) && StringUtils.isNumeric(tokens[3]))
			{
				return Long.valueOf(tokens[3]);
			}
		}
		
		return -1;
	}
	
	/**
	 * Gets the version number
	 * 
	 * @return the version number
	 * @throws IOException 
	 */
	public String getVersion() throws IOException
	{
		ProcessBuilder pb = new ProcessBuilder(adbCommandFile.getAbsolutePath(), "version");
		Process p = pb.start();
		
		return IOUtils.toString(p.getInputStream()).trim();
	}
	
	/**
	 * Gets the current Android build version of the connected device.
	 * 
	 * @return the build version from <code>/system/build.prop</code>
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public int getBuildVersion() throws IOException, InterruptedException
	{
		List<String> output = executeCommand("grep ro.build.version.sdk= /system/build.prop");
		
		if (!output.isEmpty())
		{
			return Integer.valueOf(StringUtils.substringAfter(output.get(0), "="));
		}
		else
		{
			return -1;
		}
	}
	
	/**
	 * Pushes a given package file to the device and install it
	 * 
	 * @param apkPath path to the apk file to install
	 * @param keepData flag to indicate if we wants to reinstall the app, keeping its data
	 * @return <code>true</code> if the given package file was successfully installed
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public boolean install(String apkPath, boolean keepData) throws InterruptedException, IOException
	{
		boolean result = false;
		
		ProcessBuilder pb = new ProcessBuilder(adbCommandFile.getAbsolutePath(), "install", (keepData)?"-r":"", apkPath);
		LOG.debug("install : " + pb.command().toString());
		
		Process p = pb.start();
		
		for (String line : IOUtils.readLines(p.getInputStream()))
		{
			LOG.debug(line);
			
			if (!result)
			{
				result = line.startsWith("Success");
			}
		}
		
		return result;
	}
	
	/**
	 * Blocks until device is connected.
	 * 
	 * @throws InterruptedException
	 * @throws IOException
	 */
	public void waitForDevice() throws InterruptedException, IOException
	{
		ProcessBuilder pb = new ProcessBuilder(adbCommandFile.getAbsolutePath(), "wait-for-device");
		pb.start().waitFor();
	}
	
	/**
	 * Ensures that there is a server running.
	 * 
	 * @throws ADBCommandException
	 */
	public void startServer() throws ADBCommandException
	{
		if (isDisposed())
		{
			throw new ADBCommandException("ADBCommand is disposed");
		}
		
		CommandLine cmdLine = new CommandLine(adbCommandFile.getAbsolutePath());
		cmdLine.addArgument("start-server");
		
		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
		
		PumpStreamHandler streamHandler = new PumpStreamHandler(new LoggingOutputStream(LOG, Level.INFO), new LoggingOutputStream(LOG, Level.WARN));
		
		ExecuteWatchdog watchdog = new ExecuteWatchdog(10 * 1000);
		Executor executor = new DefaultExecutor();
		executor.setExitValue(1);
		executor.setWatchdog(watchdog);
		executor.setStreamHandler(streamHandler);
		
		try
		{
			executor.execute(cmdLine, resultHandler);
			
			if (!SystemUtils.IS_OS_WINDOWS)
			{
				resultHandler.waitFor();
			}
		}
		catch (ExecuteException ee)
		{
			throw new ADBCommandException(ee.getLocalizedMessage(), ee);
		}
		catch (IOException ioe)
		{
			throw new ADBCommandException(ioe.getLocalizedMessage(), ioe);
		}
		catch (InterruptedException ie)
		{
			throw new ADBCommandException(ie.getLocalizedMessage(), ie);
		}
	}
	
	/**
	 * Kills the server if it is running.
	 * 
	 * @throws ADBCommandException
	 */
	public void killServer() throws ADBCommandException
	{
		if (isDisposed())
		{
			throw new ADBCommandException("ADBCommand is disposed");
		}
		
		LOG.info("kill adb server ...");
		
		CommandLine cmdLine = new CommandLine(adbCommandFile.getAbsolutePath());
		cmdLine.addArgument("kill-server");
		
		DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
		
		PumpStreamHandler streamHandler = new PumpStreamHandler(new LoggingOutputStream(LOG, Level.INFO), new LoggingOutputStream(LOG, Level.WARN));
		
		ExecuteWatchdog watchdog = new ExecuteWatchdog(10 * 1000);
		Executor executor = new DefaultExecutor();
		executor.setExitValue(1);
		executor.setWatchdog(watchdog);
		executor.setStreamHandler(streamHandler);
		
		try
		{
			executor.execute(cmdLine, resultHandler);
			resultHandler.waitFor();
		}
		catch (ExecuteException ee)
		{
			throw new ADBCommandException(ee.getLocalizedMessage());
		}
		catch (IOException ioe)
		{
			throw new ADBCommandException(ioe.getLocalizedMessage());
		}
		catch (InterruptedException ie)
		{
			throw new ADBCommandException(ie.getLocalizedMessage());
		}
	}
	
	/**
	 * Returns the current status of the connected device :
	 * <ul>
	 * <li>{@link ADBCommand#STATE_OFFLINE}</li>
	 * <li>{@link ADBCommand#STATE_BOOTLOADER}</li>
	 * <li>{@link ADBCommand#STATE_DEVICE}</li>
	 * </ul>
	 * 
	 * @return the current status of the connected device
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	public String getState() throws IOException, InterruptedException
	{
		ProcessBuilder pb = new ProcessBuilder(adbCommandFile.getAbsolutePath(), "get-state");
		Process p = pb.start();
		
		return IOUtils.toString(p.getInputStream()).trim();
	}
	
	/**
	 * Clean all resources used by this {@link ADBCommand} instance.
	 */
	public void dispose()
	{
		try
		{
			killServer();
		}
		catch (ADBCommandException ace)
		{
			LOG.warn(ace.getMessage());
		}
		finally
		{
			try
			{
				killAdbProcess();
			}
			catch (ADBCommandException ace)
			{
				LOG.error(ace.getMessage(), ace);
			}
			
			isDisposed = true;
		}
		
		if (adbCommandFile.exists())
		{
			LOG.info("cleaning up resources ...");
			
			FileUtils.deleteQuietly(adbCommandFile.getParentFile());
		}
	}
	
	public boolean isDisposed()
	{
		return isDisposed;
	}
	
	private void killAdbProcess() throws ADBCommandException
	{
		LOG.info("kill adb process ...");
		
		if (SystemUtils.IS_OS_WINDOWS)
		{
			CommandLine cmdLine = new CommandLine("taskkill");
			cmdLine.addArgument("/F");
			cmdLine.addArgument("/IM");
			cmdLine.addArgument(adbCommandFile.getName());
			
			LOG.debug(cmdLine.toString());
			
			DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
			
			PumpStreamHandler streamHandler = new PumpStreamHandler(new LoggingOutputStream(LOG, Level.INFO), new LoggingOutputStream(LOG, Level.WARN));
			
			ExecuteWatchdog watchdog = new ExecuteWatchdog(10 * 1000);
			Executor executor = new DefaultExecutor();
			executor.setExitValue(1);
			executor.setWatchdog(watchdog);
			executor.setStreamHandler(streamHandler);
			
			try
			{
				executor.execute(cmdLine, resultHandler);
				resultHandler.waitFor();
			}
			catch (ExecuteException ee)
			{
				throw new ADBCommandException(ee.getLocalizedMessage());
			}
			catch (IOException ioe)
			{
				throw new ADBCommandException(ioe.getLocalizedMessage());
			}
			catch (InterruptedException ie)
			{
				throw new ADBCommandException(ie.getLocalizedMessage());
			}
		}
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
			FileUtils.copyInputStreamToFile(Thread.currentThread().getContextClassLoader().getResourceAsStream("AdbWinApi.dll"), new File(tempDir, "AdbWinApi.dll"));
			FileUtils.copyInputStreamToFile(Thread.currentThread().getContextClassLoader().getResourceAsStream("AdbWinUsbApi.dll"), new File(tempDir, "AdbWinUsbApi.dll"));
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
				return Thread.currentThread().getContextClassLoader().getResourceAsStream("adb-win32-" + SystemUtils.OS_ARCH + "_1.0.31.exe");
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
				return Thread.currentThread().getContextClassLoader().getResourceAsStream("adb-linux-x86_1.0.31");
			}
			else
			{
				return Thread.currentThread().getContextClassLoader().getResourceAsStream("adb-linux-x86_64_1.0.31");
			}
		}
		else if (SystemUtils.IS_OS_MAC_OSX)
		{
			return Thread.currentThread().getContextClassLoader().getResourceAsStream("adb-macosx-cocoa_1.0.31");
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