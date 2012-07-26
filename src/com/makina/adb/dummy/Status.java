package com.makina.adb.dummy;

public enum Status
{
	STATUS_NONE(0, "none"),
	STATUS_PENDING(1, "pending"),
	STATUS_CONNECTED(2, "connected"),
	STATUS_FINISH(3, "finish"),
	STATUS_FAILED(4, "failed");

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
