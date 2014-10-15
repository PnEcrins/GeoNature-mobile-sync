package com.makina.ecrins.sync.settings;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Device information (device version, storage mount points) loaded from a JSON file.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class DeviceSettings
{
    private static final String KEY_MANUFACTURER = "manufacturer";
    private static final String KEY_MODEL = "model";
    private static final String KEY_NAME = "name";
    private static final String KEY_ANDROID = "android";
    private static final String KEY_MOUNTS = "mounts";

    private String manufacturer;
    private String model;
    private String name;
    private AndroidSettings androidSettings;
    private MountsSettings mountsSettings;

    public DeviceSettings(String manufacturer,
                          String model,
                          String name,
                          AndroidSettings androidSettings)
    {
        super();

        this.manufacturer = manufacturer;
        this.model = model;
        this.name = name;
        this.androidSettings = androidSettings;
    }

    public DeviceSettings(JSONObject json) throws
                                           JSONException
    {
        manufacturer = json.getString(KEY_MANUFACTURER);
        model = json.getString(KEY_MODEL);
        name = json.getString(KEY_NAME);

        androidSettings = new AndroidSettings(json.getJSONObject(KEY_ANDROID));
        mountsSettings = new MountsSettings(json.getJSONObject(KEY_MOUNTS));
    }

    public String getManufacturer()
    {
        return manufacturer;
    }

    public String getModel()
    {
        return model;
    }

    public String getName()
    {
        return name;
    }

    public AndroidSettings getAndroidSettings()
    {
        return androidSettings;
    }

    public MountsSettings getMountsSettings()
    {
        return mountsSettings;
    }

    @Override
    public String toString()
    {
        return "DeviceSettings{" +
                "manufacturer='" + manufacturer + '\'' +
                ", model='" + model + '\'' +
                ", name='" + name + '\'' +
                ", androidSettings=" + androidSettings +
                ", mountsSettings=" + mountsSettings +
                '}';
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((androidSettings == null) ? 0 : androidSettings.hashCode());
        result = prime * result + ((manufacturer == null) ? 0 : manufacturer.hashCode());
        result = prime * result + ((model == null) ? 0 : model.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());

        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
        {
            return true;
        }

        if (obj == null)
        {
            return false;
        }

        if (getClass() != obj.getClass())
        {
            return false;
        }

        DeviceSettings other = (DeviceSettings) obj;

        if (androidSettings == null)
        {
            if (other.androidSettings != null)
            {
                return false;
            }
        }
        else if (!androidSettings.equals(other.androidSettings))
        {
            return false;
        }

        if (manufacturer == null)
        {
            if (other.manufacturer != null)
            {
                return false;
            }
        }
        else if (!manufacturer.equals(other.manufacturer))
        {
            return false;
        }

        if (model == null)
        {
            if (other.model != null)
            {
                return false;
            }
        }
        else if (!model.equals(other.model))
        {
            return false;
        }

        if (name == null)
        {
            if (other.name != null)
            {
                return false;
            }
        }
        else if (!name.equals(other.name))
        {
            return false;
        }

        return true;
    }
}
