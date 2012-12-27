package com.makina.ecrins.sync.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Helpers for File utilities.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class FileUtils
{
	/**
	 * {@link FileUtils} instances should NOT be constructed in standard programming.
	 */
	private FileUtils()
	{
		
	}
	
	public static String readFileAsString(File textFile) throws IOException
	{
		StringBuilder contents = new StringBuilder();

		BufferedReader input = new BufferedReader(new FileReader(textFile));
		
		try
		{
			String line = null;
			
			while ((line = input.readLine()) != null)
			{
				contents.append(line);
			}
		}
		finally
		{
			input.close();
		}
		
		return contents.toString();
	}
	
	public static String readInputStreamAsString(InputStream in) throws IOException
	{
		StringBuilder out = new StringBuilder();
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		
		try
		{
			for (String line = br.readLine(); line != null; line = br.readLine())
			{
				out.append(line);
			}
		}
		finally
		{
			br.close();
		}
		
		return out.toString();
	}
}
