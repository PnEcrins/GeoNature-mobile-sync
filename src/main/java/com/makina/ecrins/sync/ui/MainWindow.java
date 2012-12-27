package com.makina.ecrins.sync.ui;

import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;

import com.makina.ecrins.sync.adb.CheckDeviceRunnable;

/**
 * Main application window.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class MainWindow
{
	protected Shell shell;
	protected SmartphoneStatusWidget smartphoneStatusWidget;
	
	public void open()
	{
		final Display display = Display.getDefault();
		createContents(display);
		
		final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
		
		try
		{
			shell.open();
			shell.layout();
			
			(new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					CheckDeviceRunnable checkDeviceRunnable = new CheckDeviceRunnable();
					checkDeviceRunnable.addObserver(smartphoneStatusWidget);
					
					scheduler.scheduleAtFixedRate(checkDeviceRunnable, 2, 2, TimeUnit.SECONDS);
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
	}
	
	public static void main(String[] args)
	{
		MainWindow window = new MainWindow();
		window.open();
	}
}
