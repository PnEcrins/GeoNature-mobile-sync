package com.makina.ecrins.sync.tasks;

import com.makina.ecrins.sync.adb.ADBCommand;
import com.makina.ecrins.sync.adb.ADBCommandException;
import com.makina.ecrins.sync.server.WebAPIClientUtils;
import com.makina.ecrins.sync.service.Status;
import com.makina.ecrins.sync.settings.LoadSettingsCallable;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * {@link AbstractTaskRunnable} implementation about mobile applications update :
 * <ul>
 * <li>Retrieve the list of mobile applications to install or to update from the server</li>
 * <li>for each application, check if the mobile application is installed to the connected device</li>
 * <li>for each application, check if mobile application updates are available from the server for the connected device</li>
 * </ul>
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class UpdateApplicationsFromServerTaskRunnable
        extends AbstractTaskRunnable
{
    private static final Logger LOG = Logger.getLogger(UpdateApplicationsFromServerTaskRunnable.class);

    private final List<ApkInfo> apks = new ArrayList<ApkInfo>();
    private int apkIndex;
    private int progress;

    private final AtomicBoolean result = new AtomicBoolean(true);

    @Override
    public void run()
    {
        apkIndex = 0;
        progress = 0;
        apks.clear();

        setTaskStatus(
                new TaskStatus(
                        -1,
                        ResourceBundle.getBundle("messages")
                                .getString("MainWindow.labelDataUpdate.check.update.text"),
                        Status.PENDING
                )
        );

        // gets all available applications information from the server
        if (fetchLastAppsVersionsFromServer(10))
        {
            int ratio = 100 - progress;

            for (ApkInfo apkinfo : apks)
            {
                try
                {
                    // checks if the mobile application is already installed or not
                    if (checkInstalledAppFromDevice(
                            apkinfo,
                            ratio,
                            10
                    ))
                    {
                        // gets application information from device
                        if (checkInstalledAppVersion(
                                apkinfo,
                                ratio,
                                10,
                                10
                        ))
                        {
                            // check if a newer version is available or not
                            if (checkInstalledAppVersion(
                                    apkinfo,
                                    ratio,
                                    10,
                                    20
                            ))
                            {
                                if (downloadLastAppFromServer(
                                        apkinfo,
                                        ratio,
                                        30,
                                        30
                                ))
                                {
                                    if (installAppToDevice(
                                            apkinfo,
                                            ratio,
                                            10,
                                            60,
                                            true
                                    ))
                                    {
                                        // everything is OK, now check if installation was successful
                                        if (checkInstalledAppVersion(
                                                apkinfo,
                                                ratio,
                                                10,
                                                70
                                        ))
                                        {

                                        }
                                    }
                                }
                            }
                        }
                        else
                        {
                            // unable to check the application version, so reinstall it
                            LOG.info(
                                    MessageFormat.format(
                                            ResourceBundle.getBundle("messages")
                                                    .getString("MainWindow.labelDataUpdate.update.notinstalled.text"),
                                            apkinfo.getPackageName()
                                    )
                            );

                            if (downloadLastAppFromServer(
                                    apkinfo,
                                    ratio,
                                    50,
                                    10
                            ))
                            {
                                if (installAppToDevice(
                                        apkinfo,
                                        ratio,
                                        10,
                                        60,
                                        false
                                ))
                                {
                                    // everything is OK, now check if installation was successful
                                    if (checkInstalledAppVersion(
                                            apkinfo,
                                            ratio,
                                            10,
                                            70
                                    ))
                                    {

                                    }
                                }
                            }
                        }
                    }
                    else
                    {
                        // the mobile application was not found on the connected device, so install it
                        LOG.info(
                                MessageFormat.format(
                                        ResourceBundle.getBundle("messages")
                                                .getString("MainWindow.labelDataUpdate.update.notinstalled.text"),
                                        apkinfo.getPackageName()
                                )
                        );

                        if (downloadLastAppFromServer(
                                apkinfo,
                                ratio,
                                50,
                                10
                        ))
                        {
                            if (installAppToDevice(
                                    apkinfo,
                                    ratio,
                                    10,
                                    60,
                                    false
                            ))
                            {
                                // everything is OK, now check if installation was successful
                                if (checkInstalledAppVersion(
                                        apkinfo,
                                        ratio,
                                        10,
                                        70
                                ))
                                {

                                }
                            }
                        }
                    }
                }
                catch (TaskException te)
                {
                    LOG.error(
                            te.getLocalizedMessage(),
                            te
                    );

                    progress = computeProgress(
                            apkIndex,
                            apks.size(),
                            1,
                            1,
                            ratio,
                            100,
                            0
                    );
                    this.result.set(false);
                    setTaskStatus(
                            new TaskStatus(
                                    progress,
                                    ResourceBundle.getBundle("messages")
                                            .getString("MainWindow.labelDataUpdate.check.update.text"),
                                    Status.FAILED
                            )
                    );
                }

                apkIndex++;
            }
        }
        else
        {
            LOG.warn("nothing to check");
        }

        progress = 100;

        if (this.result.get())
        {
            setTaskStatus(
                    new TaskStatus(
                            progress,
                            ResourceBundle.getBundle("messages")
                                    .getString("MainWindow.labelDataUpdate.check.update.text"),
                            Status.FINISH
                    )
            );
        }
        else
        {
            setTaskStatus(
                    new TaskStatus(
                            progress,
                            ResourceBundle.getBundle("messages")
                                    .getString("MainWindow.labelDataUpdate.check.update.text"),
                            Status.FAILED
                    )
            );
        }
    }

    private boolean fetchLastAppsVersionsFromServer(final int factor)
    {
        final HttpClient httpClient = WebAPIClientUtils.getHttpClient(
                LoadSettingsCallable.getInstance()
                        .getServerSettings()
                        .getServerTimeout()
        );
        HttpResponse httpResponse = null;

        try
        {
            final HttpPost httpPost = WebAPIClientUtils.httpPost(
                    httpClient,
                    LoadSettingsCallable.getInstance()
                            .getServerSettings()
                            .getServerUrl() + LoadSettingsCallable.getInstance()
                            .getSettings()
                            .getSyncSettings()
                            .getAppUpdateSettings()
                            .getVersionUrl(),
                    LoadSettingsCallable.getInstance()
                            .getServerSettings()
                            .getServerToken()
            );

            httpResponse = httpClient.execute(httpPost);

            // checks if server response is valid
            StatusLine status = httpResponse.getStatusLine();

            if (status.getStatusCode() == HttpStatus.SC_OK)
            {
                // pulls content stream from response
                final HttpEntity entity = httpResponse.getEntity();

                InputStream inputStream = entity.getContent();
                FileOutputStream fos = new FileOutputStream(
                        new File(
                                TaskManager.getInstance()
                                        .getTemporaryDirectory(),
                                "versions.json"
                        )
                );

                LOG.debug("fetchLastAppsVersionsFromServer, content length: " + entity.getContentLength());

                IOUtils.copy(
                        inputStream,
                        new CountingOutputStream(fos)
                        {
                            @Override
                            protected void afterWrite(int n) throws
                                                             IOException
                            {
                                super.afterWrite(n);

                                LOG.debug("fetchLastAppsVersionsFromServer, progress: " + ((Long.valueOf(getCount()).doubleValue() / Long.valueOf(entity.getContentLength())) * 100) + "%");

                                progress = computeProgress(
                                        0,
                                        1,
                                        getCount(),
                                        entity.getContentLength(),
                                        100,
                                        factor,
                                        0
                                );
                                setTaskStatus(
                                        new TaskStatus(
                                                progress,
                                                ResourceBundle.getBundle("messages")
                                                        .getString("MainWindow.labelDataUpdate.check.update.text"),
                                                Status.PENDING
                                        )
                                );
                            }
                        }
                );

                // ensure that the response body is fully consumed
                EntityUtils.consume(entity);

                fos.close();

                apks.addAll(
                        ApkUtils.getApkInfosFromJson(
                                new File(
                                        TaskManager.getInstance()
                                                .getTemporaryDirectory(),
                                        "versions.json"
                                )
                        )
                );

                progress = factor;
                setTaskStatus(
                        new TaskStatus(
                                progress,
                                ResourceBundle.getBundle("messages")
                                        .getString("MainWindow.labelDataUpdate.check.update.text"),
                                Status.PENDING
                        )
                );
            }
            else
            {
                LOG.error(
                        ResourceBundle.getBundle("messages")
                                .getString("MainWindow.labelDataUpdate.update.downloadversion.failed.text") + " (URL '" + httpPost.getURI()
                                .toString() + "', HTTP status : " + status.getStatusCode() + ")"
                );

                progress = 100;
                result.set(false);
                setTaskStatus(
                        new TaskStatus(
                                progress,
                                ResourceBundle.getBundle("messages")
                                        .getString("MainWindow.labelDataUpdate.check.update.text"),
                                Status.FAILED
                        )
                );
            }
        }
        catch (IOException ioe)
        {
            LOG.error(
                    ioe.getMessage(),
                    ioe
            );

            progress = 100;
            result.set(false);
            setTaskStatus(
                    new TaskStatus(
                            progress,
                            ResourceBundle.getBundle("messages")
                                    .getString("MainWindow.labelDataUpdate.check.update.text"),
                            Status.FAILED
                    )
            );
        }
        finally
        {
            HttpClientUtils.closeQuietly(httpResponse);
            HttpClientUtils.closeQuietly(httpClient);
        }

        return !apks.isEmpty();
    }

    /**
     * Returns {@code true} if the given {@link ApkInfo#getPackageName()} is currently installed or not.
     *
     * @param apkInfo {@link ApkInfo} instance to check
     * @param factor  factor as percentage to apply for the current progress
     *
     * @return {@code true} if the given {@link ApkInfo#getPackageName()} is currently installed, {@code false} otherwise
     *
     * @throws TaskException
     */
    private boolean checkInstalledAppFromDevice(ApkInfo apkInfo,
                                                int ratio,
                                                int factor) throws
                                                            TaskException
    {
        try
        {
            boolean result = !ADBCommand.getInstance().listPackages(apkInfo.getPackageName()).isEmpty();

            progress = computeProgress(
                    apkIndex,
                    apks.size(),
                    1,
                    1,
                    ratio,
                    factor,
                    0
            );
            setTaskStatus(
                    new TaskStatus(
                            progress,
                            ResourceBundle.getBundle("messages")
                                    .getString("MainWindow.labelDataUpdate.check.update.text"),
                            Status.PENDING
                    )
            );

            return result;
        }
        catch (ADBCommandException ace)
        {
            throw new TaskException(
                    ace.getLocalizedMessage(),
                    ace
            );
        }
    }

    private boolean checkInstalledAppVersion(ApkInfo apkInfo,
                                             int ratio,
                                             int factor,
                                             int offset) throws
                                                         TaskException
    {
        try
        {
            final com.makina.ecrins.sync.adb.ApkInfo apkInfoFromDevice = ADBCommand.getInstance().getApkInfo(apkInfo.getPackageName());

            if (apkInfoFromDevice == null) {
                return false;
            }

            LOG.info(
                    MessageFormat.format(
                            ResourceBundle.getBundle("messages")
                                    .getString("MainWindow.labelDataUpdate.update.app.installed.text"),
                            apkInfoFromDevice.getPackageName(),
                            apkInfoFromDevice.getVersionName(),
                            apkInfoFromDevice.getVersionCode()
                    )
            );

            if (apkInfoFromDevice.getVersionCode() < apkInfo.getVersionCode())
            {
                LOG.info(
                        MessageFormat.format(
                                ResourceBundle.getBundle("messages")
                                        .getString("MainWindow.labelDataUpdate.update.app.new.text"),
                                apkInfoFromDevice.getPackageName()
                        )
                );

                progress = computeProgress(
                        apkIndex,
                        apks.size(),
                        1,
                        1,
                        ratio,
                        factor,
                        offset
                );
                setTaskStatus(
                        new TaskStatus(
                                progress,
                                ResourceBundle.getBundle("messages")
                                        .getString("MainWindow.labelDataUpdate.check.update.text"),
                                Status.PENDING
                        )
                );

                return true;
            }
            else
            {
                LOG.info(
                        MessageFormat.format(
                                ResourceBundle.getBundle("messages")
                                        .getString("MainWindow.labelDataUpdate.update.app.uptodate.text"),
                                apkInfoFromDevice.getPackageName()
                        )
                );

                progress = computeProgress(
                        apkIndex,
                        apks.size(),
                        1,
                        1,
                        ratio,
                        factor,
                        offset
                );
                setTaskStatus(
                        new TaskStatus(
                                progress,
                                ResourceBundle.getBundle("messages")
                                        .getString("MainWindow.labelDataUpdate.check.update.text"),
                                Status.PENDING
                        )
                );

                return false;
            }
        }
        catch (ADBCommandException ace)
        {
            throw new TaskException(
                    ace.getLocalizedMessage(),
                    ace
            );
        }
        catch (JSONException je)
        {
            throw new TaskException(
                    je.getLocalizedMessage(),
                    je
            );
        }
    }

    private boolean downloadLastAppFromServer(final ApkInfo apkInfo,
                                              final int ratio,
                                              final int factor,
                                              final int offset)
    {
        boolean result = true;

        final HttpClient httpClient = WebAPIClientUtils.getHttpClient(
                LoadSettingsCallable.getInstance()
                        .getServerSettings()
                        .getServerTimeout()
        );

        HttpResponse httpResponse = null;

        try
        {
            final HttpPost httpPost = WebAPIClientUtils.httpPost(
                    httpClient,
                    LoadSettingsCallable.getInstance()
                            .getServerSettings()
                            .getServerUrl() +
                            LoadSettingsCallable.getInstance()
                                    .getSettings()
                                    .getSyncSettings()
                                    .getAppUpdateSettings()
                                    .getDownloadUrl() + "/" + apkInfo.getApkName() + "/",
                    LoadSettingsCallable.getInstance()
                            .getServerSettings()
                            .getServerToken()
            );

            httpResponse = httpClient.execute(httpPost);

            // checks if server response is valid
            StatusLine status = httpResponse.getStatusLine();

            if (status.getStatusCode() == HttpStatus.SC_OK)
            {
                // pulls content stream from response
                final HttpEntity entity = httpResponse.getEntity();

                InputStream inputStream = entity.getContent();
                FileOutputStream fos = new FileOutputStream(
                        new File(
                                TaskManager.getInstance()
                                        .getTemporaryDirectory(),
                                apkInfo.getApkName()
                        )
                );

                IOUtils.copy(
                        inputStream,
                        new CountingOutputStream(fos)
                        {
                            @Override
                            protected void afterWrite(int n) throws
                                                             IOException
                            {
                                super.afterWrite(n);

                                progress = computeProgress(
                                        apkIndex,
                                        apks.size(),
                                        getCount(),
                                        entity.getContentLength(),
                                        ratio,
                                        factor,
                                        offset
                                );
                                setTaskStatus(
                                        new TaskStatus(
                                                progress,
                                                MessageFormat.format(
                                                        ResourceBundle.getBundle("messages")
                                                                .getString("MainWindow.labelDataUpdate.update.download.text"),
                                                        apkInfo.getApkName()
                                                ),
                                                Status.PENDING
                                        )
                                );
                            }
                        }
                );

                // ensure that the response body is fully consumed
                EntityUtils.consume(entity);

                fos.close();
            }
            else
            {
                LOG.error(
                        MessageFormat.format(
                                ResourceBundle.getBundle("messages")
                                        .getString("MainWindow.labelDataUpdate.update.download.failed.text"),
                                apkInfo.getApkName()
                        ) + " (URL '" + httpPost.getURI()
                                .toString() + "', HTTP status : " + status.getStatusCode() + ")"
                );

                progress = computeProgress(
                        apkIndex,
                        apks.size(),
                        1,
                        1,
                        ratio,
                        factor,
                        offset
                );
                setTaskStatus(
                        new TaskStatus(
                                progress,
                                MessageFormat.format(
                                        ResourceBundle.getBundle("messages")
                                                .getString("MainWindow.labelDataUpdate.update.download.text"),
                                        apkInfo.getApkName()
                                ),
                                Status.FAILED
                        )
                );
                this.result.set(false);
                result = false;
            }
        }
        catch (IOException ioe)
        {
            LOG.error(
                    ioe.getMessage(),
                    ioe
            );

            progress = computeProgress(
                    apkIndex,
                    apks.size(),
                    1,
                    1,
                    ratio,
                    factor,
                    offset
            );
            setTaskStatus(
                    new TaskStatus(
                            progress,
                            MessageFormat.format(
                                    ResourceBundle.getBundle("messages")
                                            .getString("MainWindow.labelDataUpdate.update.download.text"),
                                    apkInfo.getApkName()
                            ),
                            Status.FAILED
                    )
            );
            this.result.set(false);
            result = false;
        }
        finally
        {
            HttpClientUtils.closeQuietly(httpResponse);
            HttpClientUtils.closeQuietly(httpClient);
        }

        return result;
    }

    private boolean installAppToDevice(ApkInfo apkInfo,
                                       int ratio,
                                       int factor,
                                       int offset,
                                       boolean keepData)
    {
        File apkFile = new File(
                TaskManager.getInstance()
                        .getTemporaryDirectory(),
                apkInfo.getApkName()
        );

        if (apkFile.exists())
        {
            progress = computeProgress(
                    apkIndex,
                    apks.size(),
                    0,
                    1,
                    ratio,
                    factor,
                    offset
            );
            setTaskStatus(
                    new TaskStatus(
                            progress,
                            ResourceBundle.getBundle("messages")
                                    .getString("MainWindow.labelDataUpdate.update.text"),
                            Status.PENDING
                    )
            );

            try
            {
                LOG.info(
                        MessageFormat.format(
                                ResourceBundle.getBundle("messages")
                                        .getString("MainWindow.labelDataUpdate.update.install.text"),
                                apkInfo.getApkName()
                        )
                );

                ADBCommand.getInstance()
                        .install(
                                apkFile.getAbsolutePath(),
                                keepData
                        );

                com.makina.ecrins.sync.adb.ApkInfo apkInfoFromDevice = ADBCommand.getInstance().getApkInfo(apkInfo.getPackageName());

                boolean result = (apkInfoFromDevice != null) && (apkInfoFromDevice.getVersionCode() > 0);

                if (result)
                {
                    LOG.info(
                            MessageFormat.format(
                                    ResourceBundle.getBundle("messages")
                                            .getString("MainWindow.labelDataUpdate.update.install.success.text"),
                                    apkInfo.getPackageName()
                            )
                    );

                    progress = computeProgress(
                            apkIndex,
                            apks.size(),
                            1,
                            1,
                            ratio,
                            factor,
                            offset
                    );

                    setTaskStatus(
                            new TaskStatus(
                                    progress,
                                    ResourceBundle.getBundle("messages")
                                            .getString("MainWindow.labelDataUpdate.update.text"),
                                    Status.PENDING
                            )
                    );
                }
                else
                {
                    // something is going wrong : trying to uninstall and reinstall the application package
                    LOG.warn(
                            MessageFormat.format(
                                    ResourceBundle.getBundle("messages")
                                            .getString("MainWindow.labelDataUpdate.update.install.failed.text"),
                                    apkInfo.getPackageName()
                            )
                    );

                    if (uninstallAllApplications())
                    {
                        progress = computeProgress(
                                apkIndex,
                                apks.size(),
                                1,
                                2,
                                ratio,
                                factor,
                                offset
                        );

                        setTaskStatus(
                                new TaskStatus(
                                        progress,
                                        ResourceBundle.getBundle("messages")
                                                .getString("MainWindow.labelDataUpdate.update.text"),
                                        Status.PENDING
                                )
                        );

                        LOG.info(
                                MessageFormat.format(
                                        ResourceBundle.getBundle("messages")
                                                .getString("MainWindow.labelDataUpdate.update.install.text"),
                                        apkInfo.getApkName()
                                )
                        );

                        ADBCommand.getInstance()
                                .install(
                                        apkFile.getAbsolutePath(),
                                        false
                                );

                        apkInfoFromDevice = ADBCommand.getInstance().getApkInfo(apkInfo.getPackageName());

                        result = (apkInfoFromDevice != null) && (apkInfoFromDevice.getVersionCode() > 0);

                        if (result)
                        {
                            LOG.info(
                                    MessageFormat.format(
                                            ResourceBundle.getBundle("messages")
                                                    .getString("MainWindow.labelDataUpdate.update.install.success.text"),
                                            apkInfo.getApkName()
                                    )
                            );

                            progress = computeProgress(
                                    apkIndex,
                                    apks.size(),
                                    1,
                                    1,
                                    ratio,
                                    factor,
                                    offset
                            );

                            setTaskStatus(
                                    new TaskStatus(
                                            progress,
                                            ResourceBundle.getBundle("messages")
                                                    .getString("MainWindow.labelDataUpdate.update.text"),
                                            Status.PENDING
                                    )
                            );
                        }
                        else
                        {
                            LOG.error(
                                    MessageFormat.format(
                                            ResourceBundle.getBundle("messages")
                                                    .getString("MainWindow.labelDataUpdate.update.install.failed.text"),
                                            apkInfo.getPackageName()
                                    )
                            );

                            progress = computeProgress(
                                    apkIndex,
                                    apks.size(),
                                    1,
                                    1,
                                    ratio,
                                    factor,
                                    offset
                            );

                            setTaskStatus(
                                    new TaskStatus(
                                            progress,
                                            ResourceBundle.getBundle("messages")
                                                    .getString("MainWindow.labelDataUpdate.update.text"),
                                            Status.FAILED
                                    )
                            );

                            this.result.set(false);
                        }
                    }
                    else
                    {
                        LOG.error(
                                MessageFormat.format(
                                        ResourceBundle.getBundle("messages")
                                                .getString("MainWindow.labelDataUpdate.update.uninstall.failed.text"),
                                        apkInfo.getPackageName()
                                )
                        );

                        progress = computeProgress(
                                apkIndex,
                                apks.size(),
                                1,
                                1,
                                ratio,
                                factor,
                                offset
                        );
                        setTaskStatus(
                                new TaskStatus(
                                        progress,
                                        ResourceBundle.getBundle("messages")
                                                .getString("MainWindow.labelDataUpdate.update.text"),
                                        Status.FAILED
                                )
                        );
                        this.result.set(false);
                    }
                }

                return result;
            }
            catch (ADBCommandException ace)
            {
                LOG.error(
                        ace.getMessage(),
                        ace
                );

                progress = computeProgress(
                        apkIndex,
                        apks.size(),
                        1,
                        1,
                        ratio,
                        factor,
                        offset
                );
                setTaskStatus(
                        new TaskStatus(
                                progress,
                                ResourceBundle.getBundle("messages")
                                        .getString("MainWindow.labelDataUpdate.update.text"),
                                Status.FAILED
                        )
                );
                this.result.set(false);

                return false;
            }
        }
        else
        {
            LOG.error(
                    MessageFormat.format(
                            ResourceBundle.getBundle("messages")
                                    .getString("MainWindow.labelDataUpdate.update.notfound.text"),
                            apkFile.getAbsolutePath()
                    )
            );

            progress = computeProgress(
                    apkIndex,
                    apks.size(),
                    1,
                    1,
                    ratio,
                    factor,
                    offset
            );
            setTaskStatus(
                    new TaskStatus(
                            progress,
                            ResourceBundle.getBundle("messages")
                                    .getString("MainWindow.labelDataUpdate.update.text"),
                            Status.FAILED
                    )
            );
            this.result.set(false);

            return false;
        }
    }

    /**
     * Tries to uninstall all registered mobile applications
     *
     * @return <code>true</code> if all registered mobile applications were successfully uninstalled
     */
    private boolean uninstallAllApplications()
    {
        boolean result = true;

        for (ApkInfo apkInfo : apks)
        {
            try
            {
                LOG.info(
                        MessageFormat.format(
                                ResourceBundle.getBundle("messages")
                                        .getString("MainWindow.labelDataUpdate.update.uninstall.text"),
                                apkInfo.getPackageName()
                        )
                );

                if (ADBCommand.getInstance()
                        .listPackages(apkInfo.getPackageName())
                        .isEmpty())
                {
                    LOG.info(
                            MessageFormat.format(
                                    ResourceBundle.getBundle("messages")
                                            .getString("MainWindow.labelDataUpdate.update.notinstalled.text"),
                                    apkInfo.getPackageName()
                            )
                    );
                }
                else
                {
                    if (ADBCommand.getInstance()
                            .uninstall(apkInfo.getPackageName()))
                    {
                        LOG.info(
                                MessageFormat.format(
                                        ResourceBundle.getBundle("messages")
                                                .getString("MainWindow.labelDataUpdate.update.uninstall.success.text"),
                                        apkInfo.getPackageName()
                                )
                        );
                    }
                    else
                    {
                        LOG.error(
                                MessageFormat.format(
                                        ResourceBundle.getBundle("messages")
                                                .getString("MainWindow.labelDataUpdate.update.uninstall.failed.text"),
                                        apkInfo.getPackageName()
                                )
                        );

                        result = false;
                    }
                }
            }
            catch (ADBCommandException ace)
            {
                LOG.error(
                        ace.getMessage(),
                        ace
                );

                result = false;
            }
        }

        return result;
    }

    /**
     * Computes the current progress (as percentage) according to given parameters.
     *
     * @param mainProgress
     * @param mainProgressSize
     * @param currentProgress
     * @param currentProgressSize
     * @param ratio               current ratio to apply for the current progress
     * @param factor              as percentage for the current progress
     * @param offset              as percentage for the current progress
     *
     * @return the progress as percentage
     */
    private int computeProgress(final int mainProgress,
                                final int mainProgressSize,
                                final long currentProgress,
                                final long currentProgressSize,
                                final int ratio,
                                final int factor,
                                final int offset)
    {
        return (int) ((((double) factor / 100) * ((double) ratio / (double) mainProgressSize)) * (Long.valueOf(currentProgress)
                .doubleValue() / Long.valueOf(currentProgressSize)
                .doubleValue()) +
                (100 - ratio) +
                (((double) mainProgress / (double) mainProgressSize) * ratio) +
                (((double) offset / 100) * ((double) ratio / (double) mainProgressSize)));
    }
}
