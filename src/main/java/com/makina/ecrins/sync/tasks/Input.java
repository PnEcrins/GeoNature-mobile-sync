package com.makina.ecrins.sync.tasks;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Describes a current input.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class Input
{
	public static final String KEY_ID = "id";
	public static final String KEY_DATE_OBS = "dateobs";
	
	private static DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
	
	private long inputId;
	private Date date;
	
	public Input(JSONObject jsonObject) throws JSONException, ParseException
	{
		this.inputId = jsonObject.getLong(KEY_ID);
		this.date = dateFormat.parse(jsonObject.getString(KEY_DATE_OBS));
	}

	public long getInputId()
	{
		return inputId;
	}

	public Date getDate()
	{
		return date;
	}
	
}
