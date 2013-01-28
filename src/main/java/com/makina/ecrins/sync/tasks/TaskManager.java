package com.makina.ecrins.sync.tasks;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import com.makina.ecrins.sync.service.Status;

/**
 * {@link TaskManager} acts as a thread pool, it does the followings :
 * <ul>
 * <li>maintain a list of {@link AbstractTaskRunnable}</li>
 * <li>manage a queue of {@link AbstractTaskRunnable}</li>
 * </ul>
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class TaskManager implements Observer
{
	private static final Logger LOG = Logger.getLogger(TaskManager.class);
	
	/**
	 * Pool executor, it is responsible to running all the tasks
	 */
	private final ExecutorService executor;

	/**
	 * A list of all registered {@link AbstractTaskRunnable}.
	 */
	private final List<AbstractTaskRunnable> tasks;
	
	private Status status;
	
	public TaskManager()
	{
		this.status = Status.STATUS_NONE;
		
		executor = Executors.newSingleThreadExecutor();
		this.tasks = Collections.synchronizedList(new ArrayList<AbstractTaskRunnable>());
	}
	
	public Status getStatus()
	{
		return this.status;
	}
	
	public void addTask(AbstractTaskRunnable runnableTask)
	{
		synchronized (tasks)
		{
			runnableTask.addObserver(this);
			this.tasks.add(runnableTask);
		}
	}
	
	public void start()
	{
		LOG.debug("start " + this.status.name());
		
		synchronized (tasks)
		{
			if (this.status.equals(Status.STATUS_NONE))
			{
				this.status = Status.STATUS_PENDING;
				
				for (AbstractTaskRunnable taskRunnable : this.tasks)
				{
					executor.execute(taskRunnable);
				}
			}
		}
	}
	
	/**
	 * @see ExecutorService#shutdownNow()
	 */
	public void shutdownNow()
	{
		this.executor.shutdownNow();
	}
	
	@Override
	public void update(Observable o, Object arg)
	{
		if (o instanceof AbstractTaskRunnable)
		{
			switch (((AbstractTaskRunnable) o).getTaskStatus().getStatus())
			{
				case STATUS_FINISH:
					synchronized (tasks)
					{
						Status currentStatus = Status.STATUS_FINISH;
						Iterator<AbstractTaskRunnable> iterator = tasks.iterator();
						
						while (iterator.hasNext() && currentStatus.equals(Status.STATUS_FINISH))
						{
							currentStatus = iterator.next().getTaskStatus().getStatus();
						}
						
						if (currentStatus.equals(Status.STATUS_FINISH))
						{
							this.status = Status.STATUS_FINISH;
						}
					}
					
					break;
				default:
					this.status = ((AbstractTaskRunnable) o).getTaskStatus().getStatus();
			}
		}
	}
}
