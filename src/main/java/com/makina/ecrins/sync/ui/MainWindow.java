package com.makina.ecrins.sync.ui;

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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONObject;

import com.makina.ecrins.sync.adb.CheckDeviceRunnable;
import com.makina.ecrins.sync.server.CheckServerRunnable;
import com.makina.ecrins.sync.settings.LoadSettingsCallable;

/**
 * Main application window.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class MainWindow
{
	private static final Logger LOG = Logger.getLogger(MainWindow.class);
	
	protected Shell shell;
	protected SmartphoneStatusWidget smartphoneStatusWidget;
	protected ServerStatusWidget serverStatusWidget;
	
	/**
	 * open the main window and launch tasks
	 */
	public void open()
	{
		final Display display = Display.getDefault();
		createContents(display);
		
		final ExecutorService threadExecutor = Executors.newSingleThreadExecutor();
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
		
		try
		{
			shell.open();
			shell.layout();
			
			(new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					CompletionService<JSONObject> completionService = new ExecutorCompletionService<JSONObject>(threadExecutor);
					Future<JSONObject> future = completionService.submit(LoadSettingsCallable.getInstance());
					
					try
					{
						JSONObject jsonSettings = completionService.take().get();
						
						if (jsonSettings.length() > 0)
						{
							CheckDeviceRunnable checkDeviceRunnable = new CheckDeviceRunnable();
							checkDeviceRunnable.addObserver(smartphoneStatusWidget);
							scheduler.scheduleAtFixedRate(checkDeviceRunnable, 2, 2, TimeUnit.SECONDS);
							
							CheckServerRunnable checkServerRunnable = new CheckServerRunnable();
							checkServerRunnable.addObserver(serverStatusWidget);
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
			})).start();
			
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
	}
	
	public static void main(String[] args)
	{
		MainWindow window = new MainWindow();
		window.open();
	}
}