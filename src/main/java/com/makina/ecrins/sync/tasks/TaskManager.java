package com.makina.ecrins.sync.tasks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileDeleteStrategy;
import org.apache.commons.io.FileUtils;
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
	private File tempDir = null;
	private File userDir = null;
	
	private TaskManager()
	{
		LOG.debug("new instance of TaskManager");
		
		this.status = Status.NONE;
		this.tempDir = new File(FileUtils.getTempDirectory(), "sync_data_" + Long.toString(System.currentTimeMillis()));
		this.tempDir.mkdir();
		
		this.userDir = new File(FileUtils.getUserDirectory(), ".sync");
		this.userDir.mkdir();
		
		try
		{
			FileUtils.forceDeleteOnExit(this.tempDir);
		}
		catch (IOException ioe)
		{
			LOG.warn("unable to delete '" + this.tempDir.getAbsolutePath() + "'", ioe);
		}
		
		LOG.debug("using temporary directory '" + this.tempDir.getAbsolutePath() + "'");
		LOG.debug("using user directory '" + this.userDir.getAbsolutePath() + "'");
		
		executor = Executors.newSingleThreadExecutor();
		this.tasks = Collections.synchronizedList(new ArrayList<AbstractTaskRunnable>());
	}
	
	public static TaskManager getInstance()
	{
		return TaskManagerHolder.instance;
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
			if (this.status.equals(Status.NONE))
			{
				this.status = Status.PENDING;
				
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
		
		if (this.tempDir != null)
		{
			FileDeleteStrategy.FORCE.deleteQuietly(this.tempDir);
		}
	}
	
	@Override
	public void update(Observable o, Object arg)
	{
		if (o instanceof AbstractTaskRunnable)
		{
			switch (((AbstractTaskRunnable) o).getTaskStatus().getStatus())
			{
				case FINISH:
					synchronized (tasks)
					{
						Status currentStatus = Status.FINISH;
						Iterator<AbstractTaskRunnable> iterator = tasks.iterator();
						
						while (iterator.hasNext() && currentStatus.equals(Status.FINISH))
						{
							currentStatus = iterator.next().getTaskStatus().getStatus();
						}
						
						if (currentStatus.equals(Status.FINISH))
						{
							this.status = Status.FINISH;
						}
					}
					
					break;
				default:
					this.status = ((AbstractTaskRunnable) o).getTaskStatus().getStatus();
			}
		}
	}
	
	protected File getTemporaryDirectory()
	{
		return this.tempDir;
	}
	
	protected File getUserDir()
	{
		return this.userDir;
	}

	private static class TaskManagerHolder
	{
		private final static TaskManager instance = new TaskManager();
	}
}
