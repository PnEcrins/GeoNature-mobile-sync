package com.makina.ecrins.sync.ui;

import java.text.MessageFormat;
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
import org.eclipse.swt.graphics.Image;
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

import com.makina.ecrins.sync.adb.ADBCommand;
import com.makina.ecrins.sync.adb.ADBCommandException;
import com.makina.ecrins.sync.adb.CheckDeviceRunnable;
import com.makina.ecrins.sync.logger.ConsoleLogAppender;
import com.makina.ecrins.sync.server.CheckServerRunnable;
import com.makina.ecrins.sync.service.Status;
import com.makina.ecrins.sync.settings.LoadSettingsCallable;
import com.makina.ecrins.sync.settings.Settings;
import com.makina.ecrins.sync.tasks.ImportInputsFromDeviceTaskRunnable;
import com.makina.ecrins.sync.tasks.TaskManager;
import com.makina.ecrins.sync.tasks.UpdateApplicationDataFromServerTaskRunnable;
import com.makina.ecrins.sync.tasks.UpdateApplicationsFromServerTaskRunnable;

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
	
	protected Shell shell;
	protected SmartphoneStatusWidget smartphoneStatusWidget;
	protected ServerStatusWidget serverStatusWidget;
	protected DataUpdateComposite appUpdateFromServerComposite;
	protected DataUpdateComposite dataUpdateFromDeviceComposite;
	protected DataUpdateComposite dataUpdateFromServerComposite;
	
	protected ConsoleLogComposite consoleLogComposite;

	/**
	 * open the main window and launch tasks
	 */
	public void open()
	{
		serverStatus = Status.NONE;
		deviceStatus = Status.NONE;
		
		final Display display = Display.getDefault();
		createContents(display);
		
		configureLogger();
		
		LOG.info(
				MessageFormat.format(
						ResourceBundle.getBundle("messages").getString("MainWindow.shell.startup.text"),
						ResourceBundle.getBundle("messages").getString("MainWindow.shell.text"),
						ResourceBundle.getBundle("messages").getString("version")));
		
		final ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
		
		TaskManager.getInstance();
		
		try
		{
			shell.open();
			shell.layout();
			
			threadExecutor.execute(new Runnable()
			{
				@Override
				public void run()
				{
					try
					{
						ADBCommand.getInstance();
					}
					catch (ADBCommandException ace)
					{
						LOG.warn(ace.getMessage());
					}
					
					UpdateApplicationsFromServerTaskRunnable updateApplicationsFromServerTaskRunnable = new UpdateApplicationsFromServerTaskRunnable();
					updateApplicationsFromServerTaskRunnable.addObserver(appUpdateFromServerComposite);
					TaskManager.getInstance().addTask(updateApplicationsFromServerTaskRunnable);
					
					ImportInputsFromDeviceTaskRunnable importInputsFromDeviceTaskRunnable = new ImportInputsFromDeviceTaskRunnable();
					importInputsFromDeviceTaskRunnable.addObserver(dataUpdateFromDeviceComposite);
					TaskManager.getInstance().addTask(importInputsFromDeviceTaskRunnable);
					
					UpdateApplicationDataFromServerTaskRunnable updateApplicationDataFromServerTaskRunnable = new UpdateApplicationDataFromServerTaskRunnable();
					updateApplicationDataFromServerTaskRunnable.addObserver(dataUpdateFromServerComposite);
					TaskManager.getInstance().addTask(updateApplicationDataFromServerTaskRunnable);
				}
			});
			
			threadExecutor.execute(new Runnable()
			{
				@Override
				public void run()
				{
					CompletionService<Settings> completionService = new ExecutorCompletionService<Settings>(Executors.newSingleThreadExecutor());
					Future<Settings> future = completionService.submit(LoadSettingsCallable.getInstance());
					
					try
					{
						Settings settings = completionService.take().get();
						
						if (settings != null)
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
						LOG.error(ee.getLocalizedMessage(), ee);
						LOG.error(
								MessageFormat.format(
										ResourceBundle.getBundle("messages").getString("MainWindow.shell.settings.load.failed.text"),
										LoadSettingsCallable.SETTINGS_FILE));
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
			TaskManager.getInstance().shutdownNow();
			
			try
			{
				ADBCommand.getInstance().dispose();
			}
			catch (ADBCommandException ace)
			{
				LOG.warn(ace.getMessage());
			}
			
			UIResourceManager.dispose();
			
			if (!shell.isDisposed())
			{
				shell.dispose();
			}
			
			System.exit(0);
		}
	}
	
	/**
	 * create contents of the window
	 */
	protected void createContents(Display display)
	{
		shell = new Shell(display, SWT.CLOSE | SWT.TITLE | SWT.MIN);
		shell.setSize(480, 505);
		shell.setText(
				MessageFormat.format(
						ResourceBundle.getBundle("messages").getString("MainWindow.shell.text.full"),
						ResourceBundle.getBundle("messages").getString("MainWindow.shell.text"),
						ResourceBundle.getBundle("messages").getString("version")));
		
		final FormLayout flShell = new FormLayout();
		flShell.marginLeft = 1;
		flShell.marginRight = 1;
		flShell.marginTop = 1;
		flShell.marginBottom = 1;
		
		shell.setLayout(flShell);
		shell.setImages(
				new Image[]
				{
						UIResourceManager.getImage("icon_32.png"),
						UIResourceManager.getImage("icon_48.png")
				}
		);
		
		final Composite composite = new Composite(shell, SWT.BORDER);
		composite.setLayout(new FormLayout());
		
		final FormData fdComposite = new FormData();
		fdComposite.left = new FormAttachment(0);
		fdComposite.right = new FormAttachment(100);
		fdComposite.top = new FormAttachment(0);
		fdComposite.bottom = new FormAttachment(100);
		
		composite.setLayoutData(fdComposite);
		
		final Group groupStatuses = new Group(composite, SWT.NONE);
		groupStatuses.setFont(UIResourceManager.getFont("Lucida Grande", 11, SWT.BOLD));
		groupStatuses.setBackground(UIResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		groupStatuses.setText(ResourceBundle.getBundle("messages").getString("MainWindow.groupStatuses.text"));
		groupStatuses.setLayout(new FormLayout());
		
		final FormData fdGroupStatuses = new FormData();
		fdGroupStatuses.top = new FormAttachment(0);
		fdGroupStatuses.left = new FormAttachment(0, 5);
		fdGroupStatuses.right = new FormAttachment(100, -5);
		fdGroupStatuses.height = 70;
		
		groupStatuses.setLayoutData(fdGroupStatuses);
		
		smartphoneStatusWidget = new SmartphoneStatusWidget(display, groupStatuses);
		serverStatusWidget = new ServerStatusWidget(display, groupStatuses);
		
		final Button buttonQuit = new Button(composite, SWT.NONE);
		
		final FormData fdButtonValidate = new FormData();
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
		
		final Group groupUpdate = new Group(composite, SWT.NONE);
		groupUpdate.setFont(UIResourceManager.getFont("Lucida Grande", 11, SWT.BOLD));
		groupUpdate.setBackground(UIResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		groupUpdate.setText(ResourceBundle.getBundle("messages").getString("MainWindow.groupUpdate.text"));
		groupUpdate.setLayout(new FormLayout());
		
		final FormData fdGroupUpdate = new FormData();
		fdGroupUpdate.top = new FormAttachment(groupStatuses);
		fdGroupUpdate.left = new FormAttachment(0, 5);
		fdGroupUpdate.right = new FormAttachment(100, -5);
		fdGroupUpdate.height = 300;
		
		groupUpdate.setLayoutData(fdGroupUpdate);
		
		appUpdateFromServerComposite = new DataUpdateComposite(groupUpdate, SWT.NONE, DataUpdateComposite.Layout.SERVER_DEVICE);
		dataUpdateFromDeviceComposite = new DataUpdateComposite(groupUpdate, SWT.NONE, DataUpdateComposite.Layout.DEVICE_SERVER);
		((FormData) dataUpdateFromDeviceComposite.getLayoutData()).top = new FormAttachment(appUpdateFromServerComposite);
		dataUpdateFromServerComposite = new DataUpdateComposite(groupUpdate, SWT.NONE, DataUpdateComposite.Layout.SERVER_DEVICE);
		((FormData) dataUpdateFromServerComposite.getLayoutData()).top = new FormAttachment(dataUpdateFromDeviceComposite);
		
		consoleLogComposite = new ConsoleLogComposite(composite, SWT.NONE);
		
		final FormData fdConsoleLogComposite = new FormData();
		fdConsoleLogComposite.top = new FormAttachment(groupUpdate);
		fdConsoleLogComposite.left = new FormAttachment(0, 5);
		fdConsoleLogComposite.right = new FormAttachment(100, -5);
		fdConsoleLogComposite.bottom = new FormAttachment(buttonQuit);
		
		consoleLogComposite.setLayoutData(fdConsoleLogComposite);
	}
	
	private void startTaskManager()
	{
		if (this.deviceStatus.equals(Status.CONNECTED) && this.serverStatus.equals((Status.CONNECTED)))
		{
			TaskManager.getInstance().start();
		}
	}
	
	private void configureLogger()
	{
		((ConsoleLogAppender) Logger.getRootLogger().getAppender("UI")).addObserver(consoleLogComposite);
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
