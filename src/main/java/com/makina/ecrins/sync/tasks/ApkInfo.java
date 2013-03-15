package com.makina.ecrins.sync.tasks;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Describes an Android application package.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ApkInfo
{
	public static final String KEY_PACKAGE = "package";
	public static final String KEY_SHARED_USED_ID = "sharedUserId";
	public static final String KEY_APK_NAME = "apkName";
	public static final String KEY_VERSION_CODE = "versionCode";
	public static final String KEY_VERSION_NAME = "versionName";
	
	private String packageName;
	private String sharedUserId;
	private String apkName;
	private int versionCode;
	private String versionName;
	
	public ApkInfo(JSONObject jsonObject) throws JSONException
	{
		this.packageName = jsonObject.getString(KEY_PACKAGE);
		this.sharedUserId = jsonObject.getString(KEY_SHARED_USED_ID);
		
		if (jsonObject.has(KEY_APK_NAME))
		{
			this.apkName = jsonObject.getString(KEY_APK_NAME);
		}
		
		this.versionCode = jsonObject.getInt(KEY_VERSION_CODE);
		this.versionName = jsonObject.getString(KEY_VERSION_NAME);
	}

	public String getPackageName()
	{
		return packageName;
	}

	public String getSharedUserId()
	{
		return sharedUserId;
	}

	public String getApkName()
	{
		return apkName;
	}

	public int getVersionCode()
	{
		return versionCode;
	}

	public String getVersionName()
	{
		return versionName;
	}
}
