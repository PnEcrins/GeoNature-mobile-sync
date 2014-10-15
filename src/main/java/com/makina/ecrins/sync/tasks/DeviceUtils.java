package com.makina.ecrins.sync.tasks;

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import com.makina.ecrins.sync.adb.ADBCommand;
import com.makina.ecrins.sync.adb.ADBCommandException;
import com.makina.ecrins.sync.settings.DeviceSettings;
import com.makina.ecrins.sync.settings.LoadSettingsCallable;

/**
 * Helpers about Android devices.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class DeviceUtils
{
    public static final String DEFAULT_MOUNT_PATH = "/mnt/sdcard";

    private static final Logger LOG = Logger.getLogger(DeviceUtils.class);

    /**
     * {@link DeviceUtils} instances should NOT be constructed in standard programming.
     */
    private DeviceUtils()
    {
        // nothing to do ...
    }

    /**
     * Tries to find the default mount path used by external storage using {@link DeviceSettings}
     * mount points settings or from adb command line if no {@link DeviceSettings}.
     * <p>
     * If not, returns the default mount path (usually '/mnt/sdcard').
     * </p>
     *
     * @param deviceSettings the {@link DeviceSettings} to use
     *
     * @return the mount path used by external storage.
     */
    public static String getDefaultExternalStorageDirectory(final DeviceSettings deviceSettings)
    {
        if ((deviceSettings == null) || (deviceSettings.getMountsSettings() == null))
        {
            return getDefaultExternalStorageDirectory();
        }
        else
        {
            return deviceSettings.getMountsSettings()
                    .getDefaultMountPoint();
        }
    }

    /**
     * Tries to find the default mount path used by external storage using adb command line.
     * <p>
     * If not, returns the default mount path (usually '/mnt/sdcard').
     * </p>
     *
     * @return the mount path used by external storage.
     */
    public static String getDefaultExternalStorageDirectory()
    {
        String defaultExternalStorage = null;

        try
        {
            final Iterator<String> iterator = ADBCommand.getInstance()
                    .executeCommand("echo \\$EXTERNAL_STORAGE")
                    .iterator();

            while (iterator.hasNext() && (defaultExternalStorage == null))
            {
                String line = iterator.next();

                if (line.startsWith("/"))
                {
                    defaultExternalStorage = line;
                }
            }
        }
        catch (ADBCommandException ace)
        {
            LOG.warn(
                    ace.getMessage(),
                    ace
            );
        }

        if (defaultExternalStorage == null)
        {
            defaultExternalStorage = DEFAULT_MOUNT_PATH;
        }

        return defaultExternalStorage;
    }

    /**
     * Tries to find the mount path used by external storage using {@link DeviceSettings}
     * mount points settings or from adb command line if no {@link DeviceSettings}
     * is found for the connected device.
     * <p>
     * If not, returns the default mount path (usually '/mnt/sdcard').
     * </p>
     *
     * @param deviceSettings the {@link DeviceSettings} to use
     *
     * @return the mount path used by external storage.
     */
    public static String getExternalStorageDirectory(final DeviceSettings deviceSettings)
    {
        if ((deviceSettings == null) || (deviceSettings.getMountsSettings() == null))
        {
            return getExternalStorageDirectory();
        }
        else
        {
            // returns the default mount point if external mount point is not defined
            if (StringUtils.isBlank(
                    deviceSettings.getMountsSettings()
                            .getExternalMountPoint()
            ))
            {
                return deviceSettings.getMountsSettings()
                        .getDefaultMountPoint();
            }

            return deviceSettings.getMountsSettings()
                    .getExternalMountPoint();
        }
    }

    /**
     * Tries to find the mount path used by external storage using adb command line.
     * <p>
     * If not, returns the default mount path (usually '/mnt/sdcard').
     * </p>
     *
     * @return the mount path used by external storage.
     */
    public static String getExternalStorageDirectory()
    {
        String defaultExternalStorage = getDefaultExternalStorageDirectory();
        String externalStorage = null;

        try
        {
            final Iterator<String> iterator = ADBCommand.getInstance()
                    .executeCommand("cat /proc/mounts")
                    .iterator();

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
                    if (!element.equals(defaultExternalStorage) && !element.equals("/mnt/secure/asec"))
                    {
                        externalStorage = element;
                    }
                }
            }
        }
        catch (ADBCommandException ace)
        {
            LOG.warn(
                    ace.getMessage(),
                    ace
            );
        }

        if (externalStorage == null)
        {
            externalStorage = defaultExternalStorage;
        }

        return externalStorage;
    }

    public static DeviceSettings findLoadedDeviceSettings(final DeviceSettings deviceSettings)
    {
        if (deviceSettings == null)
        {
            return null;
        }

        if (LoadSettingsCallable.getInstance()
                .getSettings()
                .getDevicesSettings()
                .contains(deviceSettings))
        {
            return LoadSettingsCallable.getInstance()
                    .getSettings()
                    .getDevicesSettings()
                    .get(
                            (LoadSettingsCallable.getInstance()
                                    .getSettings()
                                    .getDevicesSettings()
                                    .indexOf(deviceSettings))
                    );
        }

        return null;
    }
}
