package com.makina.ecrins.sync.tasks;

import com.makina.ecrins.sync.adb.ADBCommand;
import com.makina.ecrins.sync.adb.ADBCommand.Prop;
import com.makina.ecrins.sync.adb.ADBCommandException;
import com.makina.ecrins.sync.server.WebAPIClientUtils;
import com.makina.ecrins.sync.service.Status;
import com.makina.ecrins.sync.settings.AndroidSettings;
import com.makina.ecrins.sync.settings.DeviceSettings;
import com.makina.ecrins.sync.settings.ExportSettings;
import com.makina.ecrins.sync.settings.LoadSettingsCallable;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.DateUtils;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * {@link AbstractTaskRunnable} implementation for fetching all data to be updated from the server to the connected device.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class UpdateApplicationDataFromServerTaskRunnable
        extends AbstractTaskRunnable
{
    private static final Logger LOG = Logger.getLogger(UpdateApplicationDataFromServerTaskRunnable.class);

    private ApkInfo apkInfo;
    private DeviceSettings deviceSettings;

    @Override
    public void run()
    {
        setTaskStatus(
                new TaskStatus(
                        ResourceBundle.getBundle("messages")
                                .getString("MainWindow.labelDataUpdate.default.text"),
                        Status.PENDING
                )
        );

        if (getApkInfo())
        {
            loadDeviceSettings();

            if (downloadDataFromServer())
            {
                setTaskStatus(
                        new TaskStatus(
                                100,
                                ResourceBundle.getBundle("messages")
                                        .getString("MainWindow.labelDataUpdate.default.text"),
                                Status.FINISH
                        )
                );
            }
            else
            {
                setTaskStatus(
                        new TaskStatus(
                                100,
                                ResourceBundle.getBundle("messages")
                                        .getString("MainWindow.labelDataUpdate.default.text"),
                                Status.FAILED
                        )
                );
            }
        }
        else
        {
            setTaskStatus(
                    new TaskStatus(
                            100,
                            ResourceBundle.getBundle("messages")
                                    .getString("MainWindow.labelDataUpdate.default.text"),
                            Status.FAILED
                    )
            );
        }
    }

    private boolean getApkInfo()
    {
        final List<ApkInfo> apks = ApkUtils.getApkInfosFromJson(
                new File(
                        TaskManager.getInstance()
                                .getTemporaryDirectory(),
                        "versions.json"
                )
        );

        if (apks.isEmpty())
        {
            return false;
        }
        else
        {
            // uses the first mobile application declaration
            apkInfo = apks.get(0);

            return true;
        }
    }

    private void loadDeviceSettings()
    {
        try
        {
            deviceSettings = DeviceUtils.findLoadedDeviceSettings(
                    new DeviceSettings(
                            ADBCommand.getInstance()
                                    .getProp(Prop.RO_PRODUCT_MANUFACTURER),
                            ADBCommand.getInstance()
                                    .getProp(Prop.RO_PRODUCT_MODEL),
                            ADBCommand.getInstance()
                                    .getProp(Prop.RO_PRODUCT_NAME),
                            new AndroidSettings(
                                    ADBCommand.getInstance()
                                            .getProp(Prop.RO_BUILD_VERSION_RELEASE),
                                    ADBCommand.getInstance()
                                            .getBuildVersion()
                            )
                    )
            );

            LOG.debug("loadDeviceSettings: " + deviceSettings);
        }
        catch (ADBCommandException ace)
        {
            LOG.warn(ace.getMessage());

            deviceSettings = null;
        }
    }

    private boolean checkFileLastModified(String headerLastModified,
                                          String remoteName)
    {
        boolean check = false;

        if (StringUtils.isNotBlank(headerLastModified))
        {
            try
            {
                final Date remoteLastModified = DateUtils.parseDate(headerLastModified);
                final Date localFileLastModified = ADBCommand.getInstance().getFileLastModified(getDeviceFilePath(remoteName));

                LOG.debug("remoteName: " + remoteName + ", localFileLastModified: " + localFileLastModified + ", remoteLastModified: " + remoteLastModified);

                check = (remoteLastModified == null) || (!remoteLastModified.after(localFileLastModified));
            }
            catch (ADBCommandException ace)
            {
                LOG.debug(ace.getMessage());
            }
        }

        return check;
    }

    private boolean downloadDataFromServer()
    {
        final AtomicBoolean result = new AtomicBoolean(true);

        final HttpClient httpClient = WebAPIClientUtils.getHttpClient(
                LoadSettingsCallable.getInstance()
                        .getServerSettings()
                        .getServerTimeout()
        );

        HttpResponse httpResponse = null;

        final List<ExportSettings> exportsSettings = LoadSettingsCallable.getInstance()
                .getSettings()
                .getSyncSettings()
                .getExportsSettings();
        final AtomicInteger increment = new AtomicInteger();

        for (final ExportSettings exportSettings : exportsSettings)
        {
            setTaskStatus(
                    new TaskStatus(
                            (int) (((double) increment.get() / (double) exportsSettings.size()) * 100),
                            MessageFormat.format(
                                    ResourceBundle.getBundle("messages")
                                            .getString("MainWindow.labelDataUpdate.download.text"),
                                    exportSettings.getExportFile()
                            ),
                            Status.PENDING
                    )
            );
            LOG.info(
                    MessageFormat.format(
                            ResourceBundle.getBundle("messages")
                                    .getString("MainWindow.labelDataUpdate.download.text"),
                            exportSettings.getExportFile()
                    )
            );

            try
            {
                final HttpPost httpPost = WebAPIClientUtils.httpPost(
                        httpClient,
                        LoadSettingsCallable.getInstance()
                                .getServerSettings()
                                .getServerUrl() + exportSettings.getExportUrl(),
                        LoadSettingsCallable.getInstance()
                                .getServerSettings()
                                .getServerToken()
                );

                httpResponse = httpClient.execute(httpPost);

                // checks if server response is valid
                StatusLine status = httpResponse.getStatusLine();

                if (status.getStatusCode() == HttpStatus.SC_OK)
                {
                    // check the last modified date between the remote file and the local file
                    if (httpResponse.containsHeader("Last-Modified") && checkFileLastModified(
                            httpResponse.getFirstHeader("Last-Modified")
                                    .getValue(),
                            exportSettings.getExportFile()
                    ))
                    {
                        LOG.info(
                                MessageFormat.format(
                                        ResourceBundle.getBundle("messages")
                                                .getString("MainWindow.labelDataUpdate.download.uptodate.text"),
                                        exportSettings.getExportFile()
                                )
                        );

                        httpPost.abort();
                    }
                    else
                    {
                        // pulls content stream from response
                        final HttpEntity entity = httpResponse.getEntity();
                        InputStream inputStream = entity.getContent();

                        final File localFile = new File(
                                TaskManager.getInstance()
                                        .getTemporaryDirectory(),
                                exportSettings.getExportFile()
                        );
                        FileUtils.touch(localFile);

                        FileOutputStream fos = new FileOutputStream(localFile);

                        IOUtils.copy(
                                inputStream,
                                new CountingOutputStream(fos)
                                {
                                    @Override
                                    protected void afterWrite(int n) throws
                                                                     IOException
                                    {
                                        super.afterWrite(n);

                                        int currentProgress = (int) (((double) getCount() / (double) entity.getContentLength()) * 100);

                                        if (currentProgress < 0)
                                        {
                                            currentProgress = 0;
                                        }

                                        int globalProgress = (int) (((double) increment.get() / (double) exportsSettings.size()) * 100) + (currentProgress / exportsSettings.size());

                                        setTaskStatus(
                                                new TaskStatus(
                                                        globalProgress,
                                                        MessageFormat.format(
                                                                ResourceBundle.getBundle("messages")
                                                                        .getString("MainWindow.labelDataUpdate.download.text"),
                                                                exportSettings.getExportFile()
                                                        ),
                                                        Status.PENDING
                                                )
                                        );
                                    }
                                }
                        );

                        // ensure that the response body is fully consumed
                        EntityUtils.consume(entity);

                        setTaskStatus(
                                new TaskStatus(
                                        MessageFormat.format(
                                                ResourceBundle.getBundle("messages")
                                                        .getString("MainWindow.labelDataUpdate.download.copytodevice.text"),
                                                exportSettings.getExportFile()
                                        ),
                                        Status.PENDING
                                )
                        );
                        copyFileToDevice(
                                localFile,
                                exportSettings.getExportFile()
                        );
                        setTaskStatus(
                                new TaskStatus(
                                        MessageFormat.format(
                                                ResourceBundle.getBundle("messages")
                                                        .getString("MainWindow.labelDataUpdate.download.finish.text"),
                                                exportSettings.getExportFile()
                                        ),
                                        Status.PENDING
                                )
                        );
                        LOG.info(
                                MessageFormat.format(
                                        ResourceBundle.getBundle("messages")
                                                .getString("MainWindow.labelDataUpdate.download.finish.text"),
                                        exportSettings.getExportFile()
                                )
                        );
                    }
                }
                else
                {
                    LOG.error(
                            MessageFormat.format(
                                    ResourceBundle.getBundle("messages")
                                            .getString("MainWindow.labelDataUpdate.download.text"),
                                    exportSettings.getExportFile()
                            ) + " (URL '" + httpPost.getURI()
                                    .toString() + "', HTTP status : " + status.getStatusCode() + ")"
                    );
                    setTaskStatus(
                            new TaskStatus(
                                    MessageFormat.format(
                                            ResourceBundle.getBundle("messages")
                                                    .getString("MainWindow.labelDataUpdate.download.text"),
                                            exportSettings.getExportFile()
                                    ),
                                    Status.FAILED
                            )
                    );

                    result.set(false);
                }
            }
            catch (IOException ioe)
            {
                LOG.error(ioe.getLocalizedMessage());

                result.set(false);
                setTaskStatus(
                        new TaskStatus(
                                MessageFormat.format(
                                        ResourceBundle.getBundle("messages")
                                                .getString("MainWindow.labelDataUpdate.download.text"),
                                        exportSettings.getExportFile()
                                ),
                                Status.FAILED
                        )
                );
            }
            catch (ADBCommandException ace)
            {
                LOG.error(ace.getLocalizedMessage());

                result.set(false);
                setTaskStatus(
                        new TaskStatus(
                                MessageFormat.format(
                                        ResourceBundle.getBundle("messages")
                                                .getString("MainWindow.labelDataUpdate.download.text"),
                                        exportSettings.getExportFile()
                                ),
                                Status.FAILED
                        )
                );
            }
            finally
            {
                HttpClientUtils.closeQuietly(httpResponse);
            }

            increment.addAndGet(1);
        }

        HttpClientUtils.closeQuietly(httpClient);

        return result.get();
    }

    private String getDeviceFilePath(String remoteName)
    {
        return DeviceUtils.getDefaultExternalStorageDirectory() + "/" + ApkUtils.getRelativeSharedPath(apkInfo) + remoteName;
    }

    private void copyFileToDevice(File localFile,
                                  String remoteName) throws
                                                     ADBCommandException
    {
        LOG.info(
                MessageFormat.format(
                        ResourceBundle.getBundle("messages")
                                .getString("MainWindow.labelDataUpdate.download.copytodevice.text"),
                        localFile.getName()
                )
        );

        ADBCommand.getInstance()
                .push(
                        localFile.getAbsolutePath(),
                        getDeviceFilePath(remoteName)
                );
    }
}
