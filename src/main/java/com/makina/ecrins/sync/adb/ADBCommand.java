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
import org.apache.commons.io.output.ByteArrayOutputStream;
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

	public static ADBCommand getInstance() throws ADBCommandException
	{
		if (ADBCommandHolder.instance.isDisposed())
		{
			throw new ADBCommandException("ADBCommand is disposed");
		}
		
		try
		{
			ADBCommandHolder.instance.startServer();
		}
		catch (ADBCommandException ace)
		{
			LOG.warn(ace.getMessage());
		}
		
		return ADBCommandHolder.instance;
	}
	
	/**
	 * Lists all devices currently connected.
	 * 
	 * @return a <code>List</code> of connected devices
	 * @throws ADBCommandException 
	 */
	public List<String> getDevices() throws ADBCommandException
	{
		try
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
		catch (IOException ioe)
		{
			throw new ADBCommandException(ioe.getMessage(), ioe);
		}
	}
	
	/**
	 * Copy file or a directory to the connected device.
	 * 
	 * @param localPath local path to use for the copy
	 * @param remotePath remote path from the connected device
	 * @throws ADBCommandException 
	 */
	public void push(String localPath, String remotePath) throws ADBCommandException
	{
		try
		{
			CommandLine cmdLine = new CommandLine(adbCommandFile.getAbsolutePath());
			cmdLine.addArgument("push");
			cmdLine.addArgument(localPath);
			cmdLine.addArgument(remotePath);
			
			LOG.debug("push : " + cmdLine.toString());
			
			DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
			
			ExecuteWatchdog watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
			Executor executor = new DefaultExecutor();
			executor.setExitValue(1);
			executor.setWatchdog(watchdog);
			
			executor.execute(cmdLine, resultHandler);
			resultHandler.waitFor();
		}
		catch (ExecuteException ee)
		{
			throw new ADBCommandException(ee.getMessage(), ee);
		}
		catch (IOException ioe)
		{
			throw new ADBCommandException(ioe.getMessage(), ioe);
		}
		catch (InterruptedException ie)
		{
			throw new ADBCommandException(ie.getMessage(), ie);
		}
	}
	
	/**
	 * Copy file or a directory from device to a given local folder.
	 * 
	 * @param remotePath remote path from the connected device to copy
	 * @return <code>true</code> if the given file or directory was successfully copied
	 * @param localPath local path to use for the copy
	 * @throws ADBCommandException 
	 */
	public boolean pull(String remotePath, String localPath) throws ADBCommandException
	{
		try
		{
			CommandLine cmdLine = new CommandLine(adbCommandFile.getAbsolutePath());
			cmdLine.addArgument("pull");
			cmdLine.addArgument(remotePath);
			cmdLine.addArgument(localPath);
			
			LOG.debug("pull : " + cmdLine.toString());
			
			DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PumpStreamHandler streamHandler = new PumpStreamHandler(baos);
			
			ExecuteWatchdog watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
			Executor executor = new DefaultExecutor();
			executor.setExitValue(1);
			executor.setWatchdog(watchdog);
			executor.setStreamHandler(streamHandler);
			
			executor.execute(cmdLine, resultHandler);
			resultHandler.waitFor();
			
			if (baos.toString().trim().contains("does not exist"))
			{
				return false;
			}
			
			return true;
		}
		catch (ExecuteException ee)
		{
			throw new ADBCommandException(ee.getMessage(), ee);
		}
		catch (IOException ioe)
		{
			throw new ADBCommandException(ioe.getMessage(), ioe);
		}
		catch (InterruptedException ie)
		{
			throw new ADBCommandException(ie.getMessage(), ie);
		}
	}
	
	/**
	 * Executes a remote shell command
	 * 
	 * @param command the command to execute
	 * @return the output as <code>List</code>
	 * @throws ADBCommandException 
	 */
	public List<String> executeCommand(String command) throws ADBCommandException
	{
		try
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
		catch (IOException ioe)
		{
			throw new ADBCommandException(ioe.getMessage(), ioe);
		}
	}
	
	/**
	 * Tries to get the size of a given filename.
	 * @param filename the name of the file on which we want to obtain its current size
	 * @return the file size in bytes
	 * @throws ADBCommandException 
	 */
	public long getFileSize(String filename) throws ADBCommandException
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
	 * @throws ADBCommandException 
	 */
	public String getVersion() throws ADBCommandException
	{
		try
		{
			ProcessBuilder pb = new ProcessBuilder(adbCommandFile.getAbsolutePath(), "version");
			Process p = pb.start();
			
			return IOUtils.toString(p.getInputStream()).trim();
		}
		catch (IOException ioe)
		{
			throw new ADBCommandException(ioe.getMessage(), ioe);
		}
	}
	
	/**
	 * Gets the current Android build version of the connected device.
	 * 
	 * @return the build version from <code>/system/build.prop</code>
	 * @throws ADBCommandException 
	 */
	public int getBuildVersion() throws ADBCommandException
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
	 * @throws ADBCommandException 
	 */
	public boolean install(String apkPath, boolean keepData) throws ADBCommandException
	{
		try
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
		catch (IOException ioe)
		{
			throw new ADBCommandException(ioe.getMessage(), ioe);
		}
	}
	
	/**
	 * Blocks until device is connected.
	 * 
	 * @throws ADBCommandException 
	 */
	public void waitForDevice() throws ADBCommandException
	{
		try
		{
			ProcessBuilder pb = new ProcessBuilder(adbCommandFile.getAbsolutePath(), "wait-for-device");
			pb.start().waitFor();
		}
		catch (InterruptedException ie)
		{
			throw new ADBCommandException(ie.getMessage(), ie);
		}
		catch (IOException ioe)
		{
			throw new ADBCommandException(ioe.getMessage(), ioe);
		}
	}
	
	/**
	 * Ensures that there is a server running.
	 * 
	 * @throws ADBCommandException
	 */
	public void startServer() throws ADBCommandException
	{
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
	 * @throws ADBCommandException 
	 */
	public String getState() throws ADBCommandException
	{
		try
		{
			ProcessBuilder pb = new ProcessBuilder(adbCommandFile.getAbsolutePath(), "get-state");
			Process p = pb.start();
			
			return IOUtils.toString(p.getInputStream()).trim();
		}
		catch (IOException ioe)
		{
			throw new ADBCommandException(ioe.getMessage(), ioe);
		}
	}
	
	/**
	 * Clean all resources used by this {@link ADBCommand} instance.
	 */
	public void dispose()
	{
		isDisposed = true;
		
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
		}
		
		if (adbCommandFile.exists())
		{
			LOG.info("cleaning up resources ...");
			
			FileUtils.deleteQuietly(adbCommandFile.getParentFile());
		}
	}
	
	/**
	 * Returns <code>true</code> if this {@link ADBCommand} instance is disposed or not.
	 * @return <code>true</code> if this {@link ADBCommand} instance is disposed, <code>false</code> otherwise
	 */
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
				return Thread.currentThread().getContextClassLoader().getResourceAsStream("adb-win32-x86_1.0.31.exe");
			}
			else
			{
				return Thread.currentThread().getContextClassLoader().getResourceAsStream("adb-win32-x86_64_1.0.31.exe");
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