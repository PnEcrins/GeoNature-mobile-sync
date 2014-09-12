package com.makina.ecrins.sync.settings;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Android version settings used by {@link DeviceSettings}.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class AndroidSettings
{
	private static final String KEY_RELEASE = "release";
	private static final String KEY_SDK = "sdk";
	
	private String release;
	private int sdk;
	
	public AndroidSettings(String release, int sdk)
	{
		super();
		
		this.release = release;
		this.sdk = sdk;
	}

	public AndroidSettings(JSONObject json) throws JSONException
	{
		release = json.getString(KEY_RELEASE);
		sdk = json.getInt(KEY_SDK);
	}

	public String getRelease()
	{
		return release;
	}

	public int getSdk()
	{
		return sdk;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((release == null) ? 0 : release.hashCode());
		result = prime * result + sdk;
		
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
		
		AndroidSettings other = (AndroidSettings) obj;
		
		if (release == null)
		{
			if (other.release != null)
			{
				return false;
			}
		}
		else if (!release.equals(other.release))
		{
			return false;
		}
		
		if (sdk != other.sdk)
		{
			return false;
		}
		
		return true;
	}
}
