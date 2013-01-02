package com.makina.ecrins.sync.logger;

import org.apache.log4j.Level;

/**
 * Defines a message logs.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class LogMessage
{
	private Action action;
	private String message;
	private Level level;
	
	public LogMessage(Action action, String message, Level level)
	{
		super();
		this.action = action;
		this.message = message;
		this.level = level;
	}

	public Action getAction()
	{
		return action;
	}

	public String getMessage()
	{
		return message;
	}

	public Level getLevel()
	{
		return level;
	}

	public enum Action
	{
		APPEND,
		RESET
	};
}
