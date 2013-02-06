package com.makina.ecrins.sync.adb;

/**
 * Exception thrown by {@link ADBCommand}.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ADBCommandException extends Exception
{
	private static final long serialVersionUID = -7903956244315893551L;
	
	public ADBCommandException(String message)
	{
		super(message);
	}
	
	public ADBCommandException(String message, Throwable cause)
	{
		super(message, cause);
	}
}