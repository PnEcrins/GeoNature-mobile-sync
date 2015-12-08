package com.makina.ecrins.sync.adb;

/**
 * Describes an Android application package.
 *
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ApkInfo
{
    private final String packageName;
    private final String sharedUserId;
    private final int versionCode;
    private final String versionName;

    public ApkInfo(String packageName, String sharedUserId, int versionCode, String versionName)
    {
        this.packageName = packageName;
        this.sharedUserId = sharedUserId;
        this.versionCode = versionCode;
        this.versionName = versionName;
    }

    public String getPackageName()
    {
        return packageName;
    }

    public String getSharedUserId()
    {
        return sharedUserId;
    }

    public int getVersionCode()
    {
        return versionCode;
    }

    public String getVersionName()
    {
        return versionName;
    }

    @Override
    public String toString()
    {
        return "ApkInfo{" +
                "packageName='" + packageName + '\'' +
                ", sharedUserId='" + sharedUserId + '\'' +
                ", versionCode=" + versionCode +
                ", versionName='" + versionName + '\'' +
                '}';
    }
}
