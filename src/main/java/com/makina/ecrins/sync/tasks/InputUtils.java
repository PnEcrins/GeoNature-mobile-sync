package com.makina.ecrins.sync.tasks;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Helpers for {@link Input} instances.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class InputUtils
{
	/**
	 * {@link InputUtils} instances should NOT be constructed in standard programming.
	 */
	private InputUtils()
	{
		
	}
	
	/**
	 * Creates a new instance of {@link Input} from a given JSON file
	 * @param jsonFile JSON file to parse
	 * @return instance of {@link Input}
	 * @throws JSONException
	 * @throws ParseException
	 * @throws IOException
	 */
	public static Input getInputFromJson(File jsonFile) throws JSONException, ParseException, IOException
	{
		return new Input(new JSONObject(FileUtils.readFileToString(jsonFile)));
	}
}
