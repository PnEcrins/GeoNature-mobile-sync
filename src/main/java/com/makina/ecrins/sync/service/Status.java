package com.makina.ecrins.sync.service;

/**
 * Service status.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public enum Status
{
	STATUS_NONE(0, "none"),
	STATUS_PENDING(1, "pending"),
	STATUS_RUNNING(2, "running"),
	STATUS_CONNECTED(3, "connected"),
	STATUS_FINISH(4, "finish"),
	STATUS_FAILED(5, "failed");

	private final int status;
	private final String label;
	
	Status(int status, String label)
	{
		this.status = status;
		this.label = label;
	}

	public int getStatus()
	{
		return this.status;
	}
	
	public String getLabel()
	{
		return this.label;
	}
}
