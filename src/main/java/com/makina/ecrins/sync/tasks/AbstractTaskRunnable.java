package com.makina.ecrins.sync.tasks;

import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

import com.makina.ecrins.sync.service.Status;

/**
 * Represents a background running task.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public abstract class AbstractTaskRunnable extends Observable implements Runnable
{
	//private static final Logger LOG = Logger.getLogger(AbstractTaskRunnable.class);
	
	private TaskStatus taskStatus;
	
	public AbstractTaskRunnable()
	{
		this.taskStatus = new TaskStatus(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"), Status.NONE);
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
			/*
			switch (this.taskStatus.getStatus())
			{
				case PENDING:
					LOG.info(this.taskStatus.getMessage());
					break;
				case FAILED:
					LOG.error(this.taskStatus.getMessage());
					break;
				default:
			}
			*/
			notifyObservers(getTaskStatus());
		}
	}
	
	@Override
	public abstract void run();
}
