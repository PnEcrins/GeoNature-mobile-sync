package com.makina.ecrins.sync.ui;

import java.io.IOException;
import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONObject;

import com.makina.ecrins.sync.adb.ADBCommand;
import com.makina.ecrins.sync.adb.CheckDeviceRunnable;
import com.makina.ecrins.sync.logger.ConsoleLogAppender;
import com.makina.ecrins.sync.server.CheckServerRunnable;
import com.makina.ecrins.sync.service.Status;
import com.makina.ecrins.sync.settings.LoadSettingsCallable;
import com.makina.ecrins.sync.tasks.ImportInputsFromDeviceTaskRunnable;
import com.makina.ecrins.sync.tasks.TaskManager;
import com.makina.ecrins.sync.tasks.UpdateApplicationDataFromServerTaskRunnable;

/**
 * Main application window.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class MainWindow implements Observer
{
	private static final Logger LOG = Logger.getLogger(MainWindow.class);
	
	private Status serverStatus;
	private Status deviceStatus;
	
	protected TaskManager taskManager;
	
	protected Shell shell;
	protected SmartphoneStatusWidget smartphoneStatusWidget;
	protected ServerStatusWidget serverStatusWidget;
	protected DataUpdateComposite dataUpdateFromDeviceComposite;
	protected DataUpdateComposite dataUpdateFromServerComposite;
	
	protected ConsoleLogWidget consoleLogWidget;

	/**
	 * open the main window and launch tasks
	 */
	public void open()
	{
		serverStatus = Status.STATUS_NONE;
		deviceStatus = Status.STATUS_NONE;
		
		final Display display = Display.getDefault();
		createContents(display);
		
		((ConsoleLogAppender) Logger.getRootLogger().getAppender("UI")).addObserver(consoleLogWidget);
		
		LOG.info("starting " + ResourceBundle.getBundle("messages").getString("MainWindow.shell.text") + " (version : " + ResourceBundle.getBundle("messages").getString("version") + ")");
		
		final ExecutorService threadExecutor = Executors.newFixedThreadPool(2);
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
		
		try
		{
			shell.open();
			shell.layout();
			
			threadExecutor.execute(new Runnable()
			{
				@Override
				public void run()
				{
					ADBCommand.getInstance();
					
					taskManager = new TaskManager();
					
					ImportInputsFromDeviceTaskRunnable importInputsFromDeviceTaskRunnable = new ImportInputsFromDeviceTaskRunnable();
					importInputsFromDeviceTaskRunnable.addObserver(dataUpdateFromDeviceComposite);
					taskManager.addTask(importInputsFromDeviceTaskRunnable);
					
					UpdateApplicationDataFromServerTaskRunnable updateApplicationDataFromServerTaskRunnable = new UpdateApplicationDataFromServerTaskRunnable();
					updateApplicationDataFromServerTaskRunnable.addObserver(dataUpdateFromServerComposite);
					taskManager.addTask(updateApplicationDataFromServerTaskRunnable);
				}
			});
			
			threadExecutor.execute(new Runnable()
			{
				@Override
				public void run()
				{
					CompletionService<JSONObject> completionService = new ExecutorCompletionService<JSONObject>(Executors.newSingleThreadExecutor());
					Future<JSONObject> future = completionService.submit(LoadSettingsCallable.getInstance());
					
					try
					{
						JSONObject jsonSettings = completionService.take().get();
						
						if (jsonSettings.length() > 0)
						{
							CheckDeviceRunnable checkDeviceRunnable = new CheckDeviceRunnable();
							checkDeviceRunnable.addObserver(smartphoneStatusWidget);
							checkDeviceRunnable.addObserver(MainWindow.this);
							scheduler.scheduleAtFixedRate(checkDeviceRunnable, 2, 2, TimeUnit.SECONDS);
							
							CheckServerRunnable checkServerRunnable = new CheckServerRunnable();
							checkServerRunnable.addObserver(serverStatusWidget);
							checkServerRunnable.addObserver(MainWindow.this);
							scheduler.scheduleAtFixedRate(checkServerRunnable, 5, 5, TimeUnit.SECONDS);
						}
					}
					catch (InterruptedException ie)
					{
						LOG.error(ie.getMessage(), ie);
					}
					catch (ExecutionException ee)
					{
						LOG.error(ee.getMessage(), ee);
					}
					finally
					{
						future.cancel(true);
					}
				}
			});
			
			while (!shell.isDisposed())
			{
				if (!display.readAndDispatch())
				{
					display.sleep();
				}
			}
		}
		finally
		{
			threadExecutor.shutdownNow();
			scheduler.shutdownNow();
			taskManager.shutdownNow();
			
			try
			{
				ADBCommand.getInstance().killServer();
			}
			catch (IOException ioe)
			{
				LOG.error(ioe.getMessage(), ioe);
			}
			catch (InterruptedException ie)
			{
				LOG.error(ie.getMessage(), ie);
			}
			finally
			{
				ADBCommand.getInstance().dispose();
				UIResourceManager.dispose();
				
				if (!shell.isDisposed())
				{
					shell.dispose();
				}
				
				System.exit(0);
			}
		}
	}
	
	/**
	 * create contents of the window
	 */
	protected void createContents(Display display)
	{
		shell = new Shell(display, SWT.CLOSE | SWT.TITLE | SWT.MIN);
		shell.setSize(450, 415);
		shell.setText(ResourceBundle.getBundle("messages").getString("MainWindow.shell.text"));
		FormLayout flShell = new FormLayout();
		flShell.marginLeft = 1;
		flShell.marginRight = 1;
		flShell.marginTop = 1;
		flShell.marginBottom = 1;
		shell.setLayout(flShell);
		
		Composite composite = new Composite(shell, SWT.BORDER);
		composite.setLayout(new FormLayout());
		FormData fdComposite = new FormData();
		fdComposite.left = new FormAttachment(0);
		fdComposite.right = new FormAttachment(100);
		fdComposite.top = new FormAttachment(0);
		fdComposite.bottom = new FormAttachment(100);
		composite.setLayoutData(fdComposite);
		
		Group groupStatuses = new Group(composite, SWT.NONE);
		groupStatuses.setFont(UIResourceManager.getFont("Lucida Grande", 11, SWT.BOLD));
		groupStatuses.setBackground(UIResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		groupStatuses.setText(ResourceBundle.getBundle("messages").getString("MainWindow.groupStatuses.text"));
		groupStatuses.setLayout(new FormLayout());
		FormData fdGroupStatuses = new FormData();
		fdGroupStatuses.top = new FormAttachment(0);
		fdGroupStatuses.left = new FormAttachment(0, 5);
		fdGroupStatuses.right = new FormAttachment(100, -5);
		fdGroupStatuses.height = 70;
		groupStatuses.setLayoutData(fdGroupStatuses);
		
		smartphoneStatusWidget = new SmartphoneStatusWidget(display, groupStatuses);
		serverStatusWidget = new ServerStatusWidget(display, groupStatuses);
		
		Button buttonQuit = new Button(composite, SWT.NONE);
		FormData fdButtonValidate = new FormData();
		fdButtonValidate.right = new FormAttachment(100, -5);
		fdButtonValidate.bottom = new FormAttachment(100);
		buttonQuit.setLayoutData(fdButtonValidate);
		buttonQuit.setText(ResourceBundle.getBundle("messages").getString("MainWindow.buttonQuit.text"));
		
		buttonQuit.addListener(SWT.Selection, new Listener()
		{
			@Override
			public void handleEvent(Event e)
			{
				shell.dispose();
			}
		});
		
		Group groupUpdate = new Group(composite, SWT.NONE);
		groupUpdate.setFont(UIResourceManager.getFont("Lucida Grande", 11, SWT.BOLD));
		groupUpdate.setBackground(UIResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		groupUpdate.setText(ResourceBundle.getBundle("messages").getString("MainWindow.groupUpdate.text"));
		groupUpdate.setLayout(new FormLayout());
		FormData fdGroupUpdate = new FormData();
		fdGroupUpdate.top = new FormAttachment(groupStatuses);
		fdGroupUpdate.left = new FormAttachment(0, 5);
		fdGroupUpdate.right = new FormAttachment(100, -5);
		fdGroupUpdate.height = 200;
		groupUpdate.setLayoutData(fdGroupUpdate);
		
		dataUpdateFromDeviceComposite = new DataUpdateComposite(groupUpdate, SWT.NONE, DataUpdateComposite.Layout.DEVICE_SERVER);
		dataUpdateFromServerComposite = new DataUpdateComposite(groupUpdate, SWT.NONE, DataUpdateComposite.Layout.SERVER_DEVICE);
		((FormData) dataUpdateFromServerComposite.getLayoutData()).top = new FormAttachment(dataUpdateFromDeviceComposite);
		
		consoleLogWidget = new ConsoleLogWidget(display, composite, groupUpdate);
	}
	
	private void startTaskManager()
	{
		if (this.deviceStatus.equals(Status.STATUS_CONNECTED) && this.serverStatus.equals((Status.STATUS_CONNECTED)))
		{
			taskManager.start();
		}
	}
	
	@Override
	public void update(Observable o, Object arg)
	{
		if (o instanceof CheckDeviceRunnable)
		{
			this.deviceStatus = ((CheckDeviceRunnable) o).getStatus();
			startTaskManager();
		}
		
		if (o instanceof CheckServerRunnable)
		{
			this.serverStatus = ((CheckServerRunnable) o).getStatus();
			startTaskManager();
		}
	}
	
	public static void main(String[] args)
	{
		MainWindow window = new MainWindow();
		window.open();
	}
}
