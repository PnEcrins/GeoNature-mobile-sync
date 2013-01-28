package com.makina.ecrins.sync.tasks;

import java.util.Observable;
import java.util.Observer;

import com.makina.ecrins.sync.service.Status;

/**
 * Represents a background running task.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public abstract class AbstractTaskRunnable extends Observable implements Runnable
{
	private TaskStatus taskStatus;
	
	public AbstractTaskRunnable()
	{
		this.taskStatus = new TaskStatus("MainWindow.status.none", Status.STATUS_NONE);
	}
	
	@Override
	public synchronized void addObserver(Observer o)
	{
		super.addObserver(o);
		
		setChanged();
		notifyObservers(getTaskStatus());
	}

	public TaskStatus getTaskStatus()
	{
		return taskStatus;
	}

	protected void setTaskStatus(TaskStatus taskStatus)
	{
		if (!this.taskStatus.equals(taskStatus))
		{
			this.taskStatus = taskStatus;
			setChanged();
			notifyObservers(getTaskStatus());
		}
	}
	
	@Override
	public abstract void run();
}
