package com.makina.ecrins.sync.adb;

import com.makina.ecrins.sync.logger.LoggingOutputStream;
import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Wrapper class used for invoking {@code adb} command.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public final class ADBCommand
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
                LOG.error(
                        ioe.getMessage(),
                        ioe
                );
            }
            catch (InterruptedException ie)
            {
                LOG.error(
                        ie.getMessage(),
                        ie
                );
            }
            catch (UnsupportedOSVersionException uosve)
            {
                LOG.error(
                        uosve.getMessage(),
                        uosve
                );
            }
            catch (ADBCommandException ace)
            {
                LOG.error(
                        ace.getMessage(),
                        ace
                );
            }
        }
    }

    public static ADBCommand getInstance() throws
                                           ADBCommandException
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
     *
     * @throws ADBCommandException
     */
    public List<String> getDevices() throws
                                     ADBCommandException
    {
        try
        {
            final List<String> devices = new ArrayList<String>();

            final ProcessBuilder pb = new ProcessBuilder(
                    adbCommandFile.getAbsolutePath(),
                    "devices"
            );
            Process p = pb.start();

            boolean firstLine = true;

            for (String line : IOUtils.readLines(p.getInputStream(), Charset.defaultCharset()))
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
            throw new ADBCommandException(
                    ioe.getMessage(),
                    ioe
            );
        }
    }

    /**
     * Copy file or a directory to the connected device.
     *
     * @param localPath  local path to use for the copy
     * @param remotePath remote path from the connected device
     *
     * @throws ADBCommandException
     */
    public void push(String localPath,
                     String remotePath) throws
                                        ADBCommandException
    {
        try
        {
            final CommandLine cmdLine = new CommandLine(adbCommandFile.getAbsolutePath());
            cmdLine.addArgument("push");
            cmdLine.addArgument(localPath);
            cmdLine.addArgument(remotePath);

            LOG.debug("push : " + cmdLine.toString());

            final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

            final ExecuteWatchdog watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
            final Executor executor = new DefaultExecutor();
            executor.setExitValue(0);
            executor.setWatchdog(watchdog);

            executor.execute(
                    cmdLine,
                    resultHandler
            );
            resultHandler.waitFor();
        }
        catch (ExecuteException ee)
        {
            throw new ADBCommandException(
                    ee.getMessage(),
                    ee
            );
        }
        catch (IOException ioe)
        {
            throw new ADBCommandException(
                    ioe.getMessage(),
                    ioe
            );
        }
        catch (InterruptedException ie)
        {
            throw new ADBCommandException(
                    ie.getMessage(),
                    ie
            );
        }
    }

    /**
     * Copy file or a directory from device to a given local folder.
     *
     * @param remotePath remote path from the connected device to copy
     * @param localPath  local path to use for the copy
     *
     * @return <code>true</code> if the given file or directory was successfully copied
     *
     * @throws ADBCommandException
     */
    public boolean pull(String remotePath,
                        String localPath) throws
                                          ADBCommandException
    {
        try
        {
            final CommandLine cmdLine = new CommandLine(adbCommandFile.getAbsolutePath());
            cmdLine.addArgument("pull");
            cmdLine.addArgument(remotePath);
            cmdLine.addArgument(localPath);

            LOG.debug("pull : " + cmdLine.toString());

            final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PumpStreamHandler streamHandler = new PumpStreamHandler(baos);

            final ExecuteWatchdog watchdog = new ExecuteWatchdog(ExecuteWatchdog.INFINITE_TIMEOUT);
            final Executor executor = new DefaultExecutor();
            executor.setExitValue(0);
            executor.setWatchdog(watchdog);
            executor.setStreamHandler(streamHandler);

            executor.execute(
                    cmdLine,
                    resultHandler
            );
            resultHandler.waitFor();

            return !baos.toString(Charset.defaultCharset())
                    .trim()
                    .contains("does not exist");
        }
        catch (ExecuteException ee)
        {
            throw new ADBCommandException(
                    ee.getMessage(),
                    ee
            );
        }
        catch (IOException ioe)
        {
            throw new ADBCommandException(
                    ioe.getMessage(),
                    ioe
            );
        }
        catch (InterruptedException ie)
        {
            throw new ADBCommandException(
                    ie.getMessage(),
                    ie
            );
        }
    }

    /**
     * Executes a remote shell command
     *
     * @param command the command to execute
     *
     * @return the output as <code>List</code>
     *
     * @throws ADBCommandException
     */
    public List<String> executeCommand(String command) throws
                                                       ADBCommandException
    {
        try
        {
            final List<String> output = new ArrayList<String>();

            final ProcessBuilder pb = new ProcessBuilder(
                    adbCommandFile.getAbsolutePath(),
                    "shell",
                    command
            );
            LOG.debug(
                    "executeCommand : " + pb.command()
                            .toString()
            );

            Process p = pb.start();

            for (String line : IOUtils.readLines(p.getInputStream(), Charset.defaultCharset()))
            {
                output.add(line);
            }

            return output;
        }
        catch (IOException ioe)
        {
            throw new ADBCommandException(
                    ioe.getMessage(),
                    ioe
            );
        }
    }

    /**
     * Tries to get the size of a given filename.
     *
     * @param filename the name of the file on which we want to obtain its current size
     *
     * @return the file size in bytes
     *
     * @throws ADBCommandException
     */
    public long getFileSize(String filename) throws
                                             ADBCommandException
    {
        final List<String> results = executeCommand("ls -la " + filename);

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
     * Tries to get the last modified date of a given filename.
     *
     * @param filename the name of the file on which we want to obtain the last modified date
     *
     * @return the last modified date as {@link Date}
     *
     * @throws ADBCommandException
     */
    public Date getFileLastModified(String filename) throws
                                                     ADBCommandException
    {
        final List<String> results = executeCommand("ls -la " + filename);

        if (results.size() > 0)
        {
            String[] tokens = StringUtils.split(results.get(0));

            if (tokens.length == 7)
            {
                String[] dateTokens = StringUtils.split(
                        tokens[4],
                        "-"
                );
                String[] timeTokens = StringUtils.split(
                        tokens[5],
                        ":"
                );

                if ((dateTokens.length == 3) &&
                        (timeTokens.length == 2) &&
                        (StringUtils.isNumeric(dateTokens[0])) &&
                        (StringUtils.isNumeric(dateTokens[1])) &&
                        (StringUtils.isNumeric(dateTokens[2])) &&
                        (StringUtils.isNumeric(timeTokens[0])) &&
                        (StringUtils.isNumeric(timeTokens[1])))
                {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

                    try
                    {
                        return sdf.parse(tokens[4] + " " + tokens[5]);
                    }
                    catch (ParseException pe)
                    {
                        LOG.debug(pe.getMessage());

                        return new Date(0);
                    }
                }
                else
                {
                    LOG.debug("getFileLastModified : wrong date format");

                    return new Date(0);
                }
            }
        }

        return new Date(0);
    }

    /**
     * Gets the version number
     *
     * @return the version number
     *
     * @throws ADBCommandException
     */
    public String getVersion() throws
                               ADBCommandException
    {
        try
        {
            final ProcessBuilder pb = new ProcessBuilder(
                    adbCommandFile.getAbsolutePath(),
                    "version"
            );
            Process p = pb.start();

            return IOUtils.toString(p.getInputStream(), Charset.defaultCharset())
                    .trim();
        }
        catch (IOException ioe)
        {
            throw new ADBCommandException(
                    ioe.getMessage(),
                    ioe
            );
        }
    }

    /**
     * Gets the value for a given {@link Prop} from the connected device.
     *
     * @param property the {@link Prop} as property on which to retrieve its value
     *
     * @return the value of the given {@link Prop}
     *
     * @throws ADBCommandException
     */
    public String getProp(Prop property) throws
                                         ADBCommandException
    {
        if (property == null)
        {
            return null;
        }

        final List<String> output = executeCommand("getprop " + property.getValue());

        if (!output.isEmpty())
        {
            return output.get(0)
                    .trim();
        }

        LOG.warn("getProp, Property not found: " + property);

        return null;
    }

    /**
     * Gets the current Android build version of the connected device.
     *
     * @return the build version from <code>/system/build.prop</code>
     *
     * @throws ADBCommandException
     */
    public int getBuildVersion() throws
                                 ADBCommandException
    {
        try
        {
            return Integer.valueOf(getProp(Prop.RO_BUILD_VERSION_SDK));
        }
        catch (NumberFormatException nfe)
        {
            LOG.debug(nfe.getMessage());
        }

        return -1;
    }

    /**
     * Pushes a given package file to the device and install it
     *
     * @param apkPath  path to the apk file to install
     * @param keepData flag to indicate if we wants to reinstall the app, keeping its data
     *
     * @return <code>true</code> if the given package file was successfully installed
     *
     * @throws ADBCommandException
     */
    public boolean install(String apkPath,
                           boolean keepData) throws
                                             ADBCommandException
    {
        final CommandLine cmdLine = new CommandLine(adbCommandFile.getAbsolutePath());
        cmdLine.addArgument("install");

        if (keepData)
        {
            cmdLine.addArgument("-r");
        }

        cmdLine.addArgument(apkPath);

        LOG.debug("install: " + cmdLine.toString());

        final Executor executor = new DefaultExecutor();
        executor.setExitValue(0);

        try
        {
            return executor.execute(cmdLine) == 0;
        }
        catch (ExecuteException ee)
        {
            throw new ADBCommandException(
                    ee.getMessage(),
                    ee
            );
        }
        catch (IOException ioe)
        {
            throw new ADBCommandException(
                    ioe.getMessage(),
                    ioe
            );
        }
    }

    /**
     * Removes the given application package from the device
     *
     * @param packageName the application package to remove
     *
     * @return <code>true</code> if the given application package was successfully removed
     *
     * @throws ADBCommandException
     */
    public boolean uninstall(String packageName) throws
                                                 ADBCommandException
    {
        final CommandLine cmdLine = new CommandLine(adbCommandFile.getAbsolutePath());
        cmdLine.addArgument("uninstall");
        cmdLine.addArgument(packageName);

        LOG.debug("uninstall: " + cmdLine.toString());

        final Executor executor = new DefaultExecutor();
        executor.setExitValue(0);

        try
        {
            return executor.execute(cmdLine) == 0;
        }
        catch (ExecuteException ee)
        {
            throw new ADBCommandException(
                    ee.getMessage(),
                    ee
            );
        }
        catch (IOException ioe)
        {
            throw new ADBCommandException(
                    ioe.getMessage(),
                    ioe
            );
        }
    }

    /**
     * Returns a list of installed packages, optionally only those whose package name contains the text in filterNames.
     *
     * @param filter filter to apply
     *
     * @return a list of installed packages
     *
     * @throws ADBCommandException
     */
    public List<String> listPackages(String filter) throws
                                                    ADBCommandException
    {
        if (StringUtils.isNotBlank(filter))
        {
            return ADBCommand.getInstance()
                    .executeCommand("pm list packages " + filter);
        }
        else
        {
            return ADBCommand.getInstance()
                    .executeCommand("pm list packages");
        }
    }

    /**
     * Returns the corresponding {@link ApkInfo} instance about the given application package.
     *
     * @param packageName the application package to fetch
     * @return the corresponding {@link ApkInfo} instance about the application package
     * @throws ADBCommandException
     */
    public ApkInfo getApkInfo(String packageName) throws ADBCommandException
    {
        if (StringUtils.isBlank(packageName))
        {
            return null;
        }

        final List<String> results = ADBCommand.getInstance().executeCommand("dumpsys package " + packageName);

        String sharedUserId = null;
        int versionCode = 0;
        String versionName = null;

        for (String line : results)
        {
            String lineToParse = line.trim();

            if (lineToParse.startsWith("SharedUser"))
            {
                sharedUserId = StringUtils.substringBefore(StringUtils.substringAfter(lineToParse, "["), "]");
            }

            if (lineToParse.startsWith("versionCode"))
            {
                try
                {
                    versionCode = Integer.parseInt(StringUtils.substringBefore(StringUtils.substringAfter(lineToParse, "="), " "));
                }
                catch (NumberFormatException nfe)
                {
                    LOG.debug("getApkInfo: unable to parse 'versionCode' from " + lineToParse);
                }
            }

            if (lineToParse.startsWith("versionName"))
            {
                versionName = StringUtils.substringAfter(lineToParse, "=");
            }
        }

        return new ApkInfo(packageName, sharedUserId, versionCode, versionName);
    }

    /**
     * Blocks until device is connected.
     *
     * @throws ADBCommandException
     */
    public void waitForDevice() throws
                                ADBCommandException
    {
        try
        {
            final ProcessBuilder pb = new ProcessBuilder(
                    adbCommandFile.getAbsolutePath(),
                    "wait-for-device"
            );
            pb.start()
                    .waitFor();
        }
        catch (InterruptedException ie)
        {
            throw new ADBCommandException(
                    ie.getMessage(),
                    ie
            );
        }
        catch (IOException ioe)
        {
            throw new ADBCommandException(
                    ioe.getMessage(),
                    ioe
            );
        }
    }

    /**
     * Ensures that there is a server running.
     *
     * @throws ADBCommandException
     */
    public void startServer() throws
                              ADBCommandException
    {
        final CommandLine cmdLine = new CommandLine(adbCommandFile.getAbsolutePath());
        cmdLine.addArgument("start-server");

        final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

        final PumpStreamHandler streamHandler = new PumpStreamHandler(
                new LoggingOutputStream(
                        LOG,
                        Level.INFO
                ),
                new LoggingOutputStream(
                        LOG,
                        Level.WARN
                )
        );

        final ExecuteWatchdog watchdog = new ExecuteWatchdog(10 * 1000);
        final Executor executor = new DefaultExecutor();
        executor.setExitValue(0);
        executor.setWatchdog(watchdog);
        executor.setStreamHandler(streamHandler);

        try
        {
            executor.execute(
                    cmdLine,
                    resultHandler
            );

            if (!SystemUtils.IS_OS_WINDOWS)
            {
                resultHandler.waitFor();
            }
        }
        catch (ExecuteException ee)
        {
            throw new ADBCommandException(
                    ee.getLocalizedMessage(),
                    ee
            );
        }
        catch (IOException ioe)
        {
            throw new ADBCommandException(
                    ioe.getLocalizedMessage(),
                    ioe
            );
        }
        catch (InterruptedException ie)
        {
            throw new ADBCommandException(
                    ie.getLocalizedMessage(),
                    ie
            );
        }
    }

    /**
     * Kills the server if it is running.
     *
     * @throws ADBCommandException
     */
    public void killServer() throws
                             ADBCommandException
    {
        LOG.info("kill adb server ...");

        final CommandLine cmdLine = new CommandLine(adbCommandFile.getAbsolutePath());
        cmdLine.addArgument("kill-server");

        final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

        final PumpStreamHandler streamHandler = new PumpStreamHandler(
                new LoggingOutputStream(
                        LOG,
                        Level.INFO
                ),
                new LoggingOutputStream(
                        LOG,
                        Level.WARN
                )
        );

        final ExecuteWatchdog watchdog = new ExecuteWatchdog(10 * 1000);
        final Executor executor = new DefaultExecutor();
        executor.setExitValue(0);
        executor.setWatchdog(watchdog);
        executor.setStreamHandler(streamHandler);

        try
        {
            executor.execute(
                    cmdLine,
                    resultHandler
            );
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
     *
     * @throws ADBCommandException
     */
    public String getState() throws
                             ADBCommandException
    {
        try
        {
            final ProcessBuilder pb = new ProcessBuilder(
                    adbCommandFile.getAbsolutePath(),
                    "get-state"
            );
            Process p = pb.start();

            return IOUtils.toString(p.getInputStream(), Charset.defaultCharset())
                    .trim();
        }
        catch (IOException ioe)
        {
            throw new ADBCommandException(
                    ioe.getMessage(),
                    ioe
            );
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
                LOG.error(
                        ace.getMessage(),
                        ace
                );
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
     *
     * @return <code>true</code> if this {@link ADBCommand} instance is disposed, <code>false</code> otherwise
     */
    public boolean isDisposed()
    {
        return isDisposed;
    }

    private void killAdbProcess() throws
                                  ADBCommandException
    {
        LOG.info("kill adb process ...");

        if (SystemUtils.IS_OS_WINDOWS)
        {
            final CommandLine cmdLine = new CommandLine("taskkill");
            cmdLine.addArgument("/F");
            cmdLine.addArgument("/IM");
            cmdLine.addArgument(adbCommandFile.getName());

            LOG.debug(cmdLine.toString());

            final DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

            final PumpStreamHandler streamHandler = new PumpStreamHandler(
                    new LoggingOutputStream(
                            LOG,
                            Level.INFO
                    ),
                    new LoggingOutputStream(
                            LOG,
                            Level.WARN
                    )
            );

            final ExecuteWatchdog watchdog = new ExecuteWatchdog(10 * 1000);
            final Executor executor = new DefaultExecutor();
            executor.setExitValue(0);
            executor.setWatchdog(watchdog);
            executor.setStreamHandler(streamHandler);

            try
            {
                executor.execute(
                        cmdLine,
                        resultHandler
                );
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

    private synchronized static File extractAdbCommand() throws
                                                         IOException,
                                                         InterruptedException,
                                                         UnsupportedOSVersionException
    {
        final File tempDir = new File(
                FileUtils.getTempDirectory(),
                "sync_" + Long.toString(System.currentTimeMillis())
        );
        tempDir.mkdir();
        FileUtils.forceDeleteOnExit(tempDir);

        File adbCommandFile = new File(
                tempDir,
                "adb"
        );

        if (SystemUtils.IS_OS_WINDOWS)
        {
            adbCommandFile = new File(
                    tempDir,
                    "adb.exe"
            );
            FileUtils.copyInputStreamToFile(
                    Thread.currentThread()
                            .getContextClassLoader()
                            .getResourceAsStream("AdbWinApi.dll"),
                    new File(
                            tempDir,
                            "AdbWinApi.dll"
                    )
            );
            FileUtils.copyInputStreamToFile(
                    Thread.currentThread()
                            .getContextClassLoader()
                            .getResourceAsStream("AdbWinUsbApi.dll"),
                    new File(
                            tempDir,
                            "AdbWinUsbApi.dll"
                    )
            );
        }

        FileUtils.copyInputStreamToFile(
                getAdbCommandFromSystemResource(),
                adbCommandFile
        );

        if (!SystemUtils.IS_OS_WINDOWS)
        {
            final ProcessBuilder pb = new ProcessBuilder(
                    "chmod",
                    "u+x",
                    adbCommandFile.getAbsolutePath()
            );
            pb.start()
                    .waitFor();
        }

        return adbCommandFile;
    }

    private static InputStream getAdbCommandFromSystemResource() throws
                                                                 UnsupportedOSVersionException
    {
        LOG.info(SystemUtils.OS_NAME + " (" + SystemUtils.OS_ARCH + ", version : " + SystemUtils.OS_VERSION + ")");

        if (SystemUtils.IS_OS_WINDOWS)
        {
            if (SystemUtils.OS_ARCH.equalsIgnoreCase("x86"))
            {
                return Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream("adb-win32-x86_1.0.31.exe");
            }
            else
            {
                return Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream("adb-win32-x86_64_1.0.31.exe");
            }
        }
        else if (SystemUtils.IS_OS_LINUX)
        {
            if (SystemUtils.OS_ARCH.equalsIgnoreCase("i386"))
            {
                return Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream("adb-linux-x86_1.0.31");
            }
            else
            {
                return Thread.currentThread()
                        .getContextClassLoader()
                        .getResourceAsStream("adb-linux-x86_64_1.0.31");
            }
        }
        else if (SystemUtils.IS_OS_MAC_OSX)
        {
            return Thread.currentThread()
                    .getContextClassLoader()
                    .getResourceAsStream("adb-macosx-cocoa_1.0.31");
        }
        else
        {
            throw new UnsupportedOSVersionException();
        }
    }

    /**
     * Device system properties used to retrieve values from 'getprop' system command.
     *
     * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
     */
    public static enum Prop
    {
        RO_BUILD_VERSION_RELEASE("ro.build.version.release"),
        RO_BUILD_VERSION_SDK("ro.build.version.sdk"),

        RO_PRODUCT_MANUFACTURER("ro.product.manufacturer"),
        RO_PRODUCT_MODEL("ro.product.model"),
        RO_PRODUCT_NAME("ro.product.name");

        private String value;

        private Prop(String value)
        {
            this.value = value;
        }

        public String getValue()
        {
            return this.value;
        }
    }

    private static class ADBCommandHolder
    {
        private final static ADBCommand instance = new ADBCommand();
    }
}