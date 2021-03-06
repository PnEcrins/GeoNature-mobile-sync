package com.makina.ecrins.sync.adb;

import com.makina.ecrins.sync.tasks.DeviceUtils;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * Test class for {@link ADBCommand}
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ADBCommandTest
{
    private static final Logger LOG = Logger.getLogger(ADBCommandTest.class);

    @Test
    public void getDevicesTest()
    {
        try
        {
            final List<String> devices = ADBCommand.getInstance()
                    .getDevices();
            Assert.assertTrue(devices.size() >= 0);
        }
        catch (ADBCommandException ace)
        {
            LOG.error(
                    ace.getMessage(),
                    ace
            );
            Assert.fail(ace.getMessage());
        }
    }

    @Test
    public void pushAndPullTest()
    {
        // creates the temporary directory to use for copying files to the connected device
        final File tempDir = new File(
                FileUtils.getTempDirectory(),
                "sync_" + Long.toString(System.currentTimeMillis())
        );

        // noinspection ResultOfMethodCallIgnored
        tempDir.mkdir();

        try
        {
            // gets the input sample file to copy
            final File inputResourceJson = FileUtils.toFile(getClass().getResource("/input_1234.json"));
            Assert.assertTrue(inputResourceJson.exists());

            // copy the sample file to the temporary directory
            final File inputJson = new File(
                    tempDir,
                    "input_1234.json"
            );
            FileUtils.copyFile(
                    inputResourceJson,
                    inputJson
            );
            LOG.debug("input JSON : " + inputJson.getAbsolutePath());
            Assert.assertTrue(inputJson.exists());

            final File inputJsonFromDevice = new File(
                    tempDir,
                    "input_1234_copy.json"
            );
            ADBCommand.getInstance()
                    .push(
                            inputJson.getAbsolutePath(),
                            DeviceUtils.getDefaultExternalStorageDirectory() + "/Android/data/sync/input_1234.json"
                    );
            ADBCommand.getInstance()
                    .pull(
                            DeviceUtils.getDefaultExternalStorageDirectory() + "/Android/data/sync/input_1234.json",
                            inputJsonFromDevice.getAbsolutePath()
                    );

            LOG.debug("input JSON from device : " + inputJsonFromDevice.getAbsolutePath());
            LOG.debug("input_1234.json checksum : " + FileUtils.checksumCRC32(inputJson));
            LOG.debug("input_1234_copy.json checksum : " + FileUtils.checksumCRC32(inputJsonFromDevice));

            Assert.assertEquals(
                    FileUtils.checksumCRC32(inputJson),
                    FileUtils.checksumCRC32(inputJsonFromDevice)
            );
        }
        catch (ADBCommandException ace)
        {
            LOG.error(
                    ace.getMessage(),
                    ace
            );
            Assert.fail(ace.getMessage());
        }
        catch (FileNotFoundException fnfe)
        {
            LOG.error(
                    fnfe.getMessage(),
                    fnfe
            );
            Assert.fail(fnfe.getMessage());
        }
        catch (IOException ioe)
        {
            LOG.error(
                    ioe.getMessage(),
                    ioe
            );
            Assert.fail(ioe.getMessage());
        }
        finally
        {
            FileUtils.deleteQuietly(tempDir);
        }
    }

    @Test
    public void executeTest()
    {
        try
        {
            final List<String> results = ADBCommand.getInstance()
                    .executeCommand("[ -d /mnt/sdcard/Android/data/com.makina.ecrins ] && echo '1' || echo '0'");

            for (String line : results)
            {
                LOG.debug(line);
            }
        }
        catch (ADBCommandException ace)
        {
            LOG.error(
                    ace.getMessage(),
                    ace
            );
            Assert.fail(ace.getMessage());
        }
    }

    @Test
    public void getFileSizeTest()
    {
        // creates the temporary directory to use for copying files to the connected device
        final File tempDir = new File(
                FileUtils.getTempDirectory(),
                "sync_" + Long.toString(System.currentTimeMillis())
        );

        // noinspection ResultOfMethodCallIgnored
        tempDir.mkdir();

        try
        {
            // gets the input sample file to copy
            final File inputResourceJson = FileUtils.toFile(getClass().getResource("/input_1234.json"));

            // copy the sample file to the temporary directory
            final File inputJson = new File(
                    tempDir,
                    "input_1234.json"
            );
            FileUtils.copyFile(
                    inputResourceJson,
                    inputJson
            );
            Assert.assertTrue(inputJson.exists());

            ADBCommand.getInstance()
                    .push(
                            inputJson.getAbsolutePath(),
                            DeviceUtils.getDefaultExternalStorageDirectory() + "/Android/data/sync/input_1234.json"
                    );

            long fileSize = ADBCommand.getInstance()
                    .getFileSize(DeviceUtils.getDefaultExternalStorageDirectory() + "/Android/data/sync/input_1234.json");

            LOG.debug("file size : " + fileSize);

            Assert.assertTrue(fileSize > 0);
        }
        catch (ADBCommandException ace)
        {
            LOG.error(
                    ace.getMessage(),
                    ace
            );
            Assert.fail(ace.getMessage());
        }
        catch (IOException ioe)
        {
            LOG.error(
                    ioe.getMessage(),
                    ioe
            );
            Assert.fail(ioe.getMessage());
        }
        finally
        {
            FileUtils.deleteQuietly(tempDir);
        }
    }

    @Test
    public void getFileLastModifiedTest()
    {
        // creates the temporary directory to use for copying files to the connected device
        final File tempDir = new File(
                FileUtils.getTempDirectory(),
                "sync_" + Long.toString(System.currentTimeMillis())
        );

        // noinspection ResultOfMethodCallIgnored
        tempDir.mkdir();

        try
        {
            // gets the input sample file to copy
            final File inputResourceJson = FileUtils.toFile(getClass().getResource("/input_1234.json"));

            // copy the sample file to the temporary directory
            final File inputJson = new File(
                    tempDir,
                    "input_1234.json"
            );
            FileUtils.copyFile(
                    inputResourceJson,
                    inputJson
            );
            Assert.assertTrue(inputJson.exists());

            ADBCommand.getInstance()
                    .push(
                            inputJson.getAbsolutePath(),
                            DeviceUtils.getDefaultExternalStorageDirectory() + "/Android/data/sync/input_1234.json"
                    );

            final Date lastModified = ADBCommand.getInstance()
                    .getFileLastModified(DeviceUtils.getDefaultExternalStorageDirectory() + "/Android/data/sync/input_1234.json");

            LOG.debug("last modified date : " + lastModified);

            Assert.assertTrue(lastModified.after(new Date(0)));
        }
        catch (ADBCommandException ace)
        {
            LOG.error(
                    ace.getMessage(),
                    ace
            );
            Assert.fail(ace.getMessage());
        }
        catch (IOException ioe)
        {
            LOG.error(
                    ioe.getMessage(),
                    ioe
            );
            Assert.fail(ioe.getMessage());
        }
        finally
        {
            FileUtils.deleteQuietly(tempDir);
        }
    }

    @Test
    public void getStateTest()
    {
        try
        {
            final String state = ADBCommand.getInstance()
                    .getState();

            LOG.debug("device state : " + state);

            Assert.assertEquals(
                    ADBCommand.STATE_DEVICE,
                    state
            );
        }
        catch (ADBCommandException ace)
        {
            LOG.error(
                    ace.getMessage(),
                    ace
            );
            Assert.fail(ace.getMessage());
        }
    }

    @Test
    public void getBuildVersionTest()
    {
        try
        {
            int buildVersion = ADBCommand.getInstance()
                    .getBuildVersion();

            LOG.debug("build version : " + buildVersion);

            Assert.assertTrue(buildVersion > 0);
        }
        catch (ADBCommandException ace)
        {
            LOG.error(
                    ace.getMessage(),
                    ace
            );
            Assert.fail(ace.getMessage());
        }
    }

    @Test
    public void testGetApkInfo() throws Exception
    {
        final ApkInfo apkInfo = ADBCommand.getInstance().getApkInfo("com.makina.ecrins.fauna");

        Assert.assertNotNull(apkInfo);
        Assert.assertEquals("com.makina.ecrins.fauna", apkInfo.getPackageName());
        Assert.assertEquals("com.makina.ecrins", apkInfo.getSharedUserId());
        Assert.assertTrue(apkInfo.getVersionCode() > 0);
        Assert.assertNotNull(apkInfo.getVersionName());
    }
}
