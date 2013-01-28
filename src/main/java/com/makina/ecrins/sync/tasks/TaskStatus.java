package com.makina.ecrins.sync.tasks;

import com.makina.ecrins.sync.service.Status;

/**
 * Describes a message status sent by {@link AbstractTaskRunnable}.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class TaskStatus
{
	private int progress;
	private String message;
	private Status status;
	
	public TaskStatus(String message, Status status)
	{
		this(0, message, status);
	}
	
	public TaskStatus(int progress, String message, Status status)
	{
		super();
		this.progress = progress;
		this.message = message;
		this.status = status;
	}

	public int getProgress()
	{
		return progress;
	}

	public String getMessage()
	{
		return message;
	}

	public Status getStatus()
	{
		return status;
	}
}
