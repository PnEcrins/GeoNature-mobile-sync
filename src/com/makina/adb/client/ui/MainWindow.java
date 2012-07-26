package com.makina.adb.client.ui;

import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.ExpandListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wb.swt.SWTResourceManager;

public class MainWindow
{
	protected Shell shell;
	protected Canvas canvasSmartphoneStatus;
	protected Label labelSmartphoneStatus;
	protected Canvas canvasServerStatus;
	protected Label labelServerStatus;

	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args)
	{
		try
		{
			MainWindow window = new MainWindow();
			window.open();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * Open the window.
	 */
	public void open()
	{
		final Display display = Display.getDefault();
		createContents(display);
		
		try
		{
			shell.open();
			shell.layout();
			
			//(new Thread(new CheckSmartphone(display, canvasSmartphoneStatus, labelSmartphoneStatus, 5))).start();
			//(new Thread(new CheckServer(display, canvasServerStatus, labelServerStatus, 4, 2))).start();
			
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
			if (!shell.isDisposed())
			{
				shell.dispose();
			}
			
			System.exit(0);
		}
	}

	/**
	 * Create contents of the window.
	 */
	protected void createContents(Display display)
	{
		shell = new Shell(display, SWT.CLOSE | SWT.TITLE | SWT.MIN);
		shell.setSize(450, 360);
		shell.setText(ResourceBundle.getBundle("com.makina.adb.client.messages").getString("MainWindow.shell.text")); //$NON-NLS-1$ //$NON-NLS-2$
		FormLayout fl_shell = new FormLayout();
		fl_shell.marginLeft = 1;
		fl_shell.marginRight = 1;
		fl_shell.marginTop = 1;
		fl_shell.marginBottom = 1;
		shell.setLayout(fl_shell);
		
		Composite composite = new Composite(shell, SWT.BORDER);
		composite.setLayout(new FormLayout());
		FormData fdComposite = new FormData();
		fdComposite.left = new FormAttachment(0);
		fdComposite.right = new FormAttachment(100);
		fdComposite.top = new FormAttachment(0);
		fdComposite.bottom = new FormAttachment(100);
		composite.setLayoutData(fdComposite);
		
		Group groupStatuses = new Group(composite, SWT.NONE);
		groupStatuses.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		groupStatuses.setText(ResourceBundle.getBundle("com.makina.adb.client.messages").getString("MainWindow.groupStatuses.text")); //$NON-NLS-1$ //$NON-NLS-2$
		groupStatuses.setLayout(new FormLayout());
		FormData fdGroupStatuses = new FormData();
		fdGroupStatuses.top = new FormAttachment(0);
		fdGroupStatuses.left = new FormAttachment(0, 5);
		fdGroupStatuses.right = new FormAttachment(100, -5);
		fdGroupStatuses.height = 70;
		groupStatuses.setLayoutData(fdGroupStatuses);
		
		canvasSmartphoneStatus = new Canvas(groupStatuses, SWT.NONE);
		canvasSmartphoneStatus.setLayout(new FormLayout());
		FormData fd_canvasSmartphoneStatus = new FormData();
		fd_canvasSmartphoneStatus.left = new FormAttachment(groupStatuses, 10);
		fd_canvasSmartphoneStatus.top = new FormAttachment(groupStatuses, 10);
		fd_canvasSmartphoneStatus.height = 22;
		fd_canvasSmartphoneStatus.width = 22;
		canvasSmartphoneStatus.setLayoutData(fd_canvasSmartphoneStatus);
		
		canvasSmartphoneStatus.addPaintListener(new PaintListener()
		{
			public void paintControl(PaintEvent pe)
			{
				pe.gc.drawImage(SWTResourceManager.getImage(getClass(), "/resources/smartphone_status_pending.png"), 0, 0);
			}
		});

		Label labelSmartphone = new Label(groupStatuses, SWT.NONE);
		labelSmartphone.setForeground(SWTResourceManager.getColor(218, 165, 32));
		FormData fdLabelSmartphone = new FormData();
		fdLabelSmartphone.top = new FormAttachment(canvasSmartphoneStatus, 5, SWT.TOP);
		fdLabelSmartphone.left = new FormAttachment(canvasSmartphoneStatus, 5);
		labelSmartphone.setLayoutData(fdLabelSmartphone);
		labelSmartphone.setText(ResourceBundle.getBundle("com.makina.adb.client.messages").getString("MainWindow.labelSmartphone.text")); //$NON-NLS-1$ //$NON-NLS-2$
		
		Label labelSmartphoneSeparator = new Label(groupStatuses, SWT.NONE);
		labelSmartphoneSeparator.setForeground(SWTResourceManager.getColor(0, 0, 0));
		FormData fdlabelSmartphoneSeparator = new FormData();
		fdlabelSmartphoneSeparator.top = new FormAttachment(canvasSmartphoneStatus, 5, SWT.TOP);
		fdlabelSmartphoneSeparator.left = new FormAttachment(40);
		labelSmartphoneSeparator.setLayoutData(fdlabelSmartphoneSeparator);
		labelSmartphoneSeparator.setText(":");
				
		labelSmartphoneStatus = new Label(groupStatuses, SWT.NONE);
		labelSmartphoneStatus.setForeground(SWTResourceManager.getColor(218, 165, 32));
		FormData fd_labelSmartphoneStatus = new FormData();
		fd_labelSmartphoneStatus.top = new FormAttachment(canvasSmartphoneStatus, 5, SWT.TOP);
		fd_labelSmartphoneStatus.left = new FormAttachment(labelSmartphoneSeparator, 5);
		labelSmartphoneStatus.setLayoutData(fd_labelSmartphoneStatus);
		labelSmartphoneStatus.setText(ResourceBundle.getBundle("com.makina.adb.client.messages").getString("MainWindow.status.pending")); //$NON-NLS-1$ //$NON-NLS-2$
		
		canvasServerStatus = new Canvas(groupStatuses, SWT.NONE);
		canvasServerStatus.setLayout(new FormLayout());
		FormData fd_canvasServerStatus = new FormData();
		fd_canvasServerStatus.top = new FormAttachment(canvasSmartphoneStatus, 5);
		fd_canvasServerStatus.right = new FormAttachment(canvasSmartphoneStatus, 0, SWT.RIGHT);
		fd_canvasServerStatus.height = 22;
		fd_canvasServerStatus.width = 22;
		canvasServerStatus.setLayoutData(fd_canvasServerStatus);
		canvasServerStatus.addPaintListener(new PaintListener()
		{
			public void paintControl(PaintEvent pe)
			{
				pe.gc.drawImage(SWTResourceManager.getImage(getClass(), "/resources/server_status_none.png"), 0, 0);
			}
		});
		
		Label labelServer = new Label(groupStatuses, SWT.NONE);
		labelServer.setForeground(SWTResourceManager.getColor(0, 0, 0));
		FormData fdLabelServer = new FormData();
		fdLabelServer.top = new FormAttachment(canvasServerStatus, 5, SWT.TOP);
		fdLabelServer.left = new FormAttachment(canvasServerStatus, 5);
		labelServer.setLayoutData(fdLabelServer);
		labelServer.setText(ResourceBundle.getBundle("com.makina.adb.client.messages").getString("MainWindow.labelServer.text")); //$NON-NLS-1$ //$NON-NLS-2$
		
		Label labelServerSeparator = new Label(groupStatuses, SWT.NONE);
		labelServerSeparator.setForeground(SWTResourceManager.getColor(0, 0, 0));
		FormData fdlabelServerSeparator = new FormData();
		fdlabelServerSeparator.top = new FormAttachment(canvasServerStatus, 5, SWT.TOP);
		fdlabelServerSeparator.left = new FormAttachment(40);
		labelServerSeparator.setLayoutData(fdlabelServerSeparator);
		labelServerSeparator.setText(":");
		
		labelServerStatus = new Label(groupStatuses, SWT.NONE);
		labelServerStatus.setForeground(SWTResourceManager.getColor(0, 0, 0));
		FormData fd_labelServerStatus = new FormData();
		fd_labelServerStatus.top = new FormAttachment(canvasServerStatus, 5, SWT.TOP);
		fd_labelServerStatus.left = new FormAttachment(labelServerSeparator, 5);
		labelServerStatus.setLayoutData(fd_labelServerStatus);
		labelServerStatus.setText(ResourceBundle.getBundle("com.makina.adb.client.messages").getString("MainWindow.status.none")); //$NON-NLS-1$ //$NON-NLS-2$
		
		Button buttonValidate = new Button(composite, SWT.NONE);
		//buttonValidate.setEnabled(false);
		FormData fdButtonValidate = new FormData();
		fdButtonValidate.right = new FormAttachment(100, -5);
		fdButtonValidate.bottom = new FormAttachment(100);
		buttonValidate.setLayoutData(fdButtonValidate);
		buttonValidate.setText(ResourceBundle.getBundle("com.makina.adb.client.messages").getString("MainWindow.buttonValidate.text")); //$NON-NLS-1$ //$NON-NLS-2$
		
		buttonValidate.addListener(SWT.Selection, new Listener()
		{
			@Override
			public void handleEvent(Event e)
			{
				shell.dispose();
			}
		});
		
		final Button buttonRetry = new Button(composite, SWT.NONE);
		buttonRetry.setVisible(false);
		FormData fdButtonRetry = new FormData();
		fdButtonRetry.top = new FormAttachment(buttonValidate, 0, SWT.TOP);
		fdButtonRetry.right = new FormAttachment(buttonValidate);
		buttonRetry.setLayoutData(fdButtonRetry);
		buttonRetry.setText(ResourceBundle.getBundle("com.makina.adb.client.messages").getString("MainWindow.buttonRetry.text")); //$NON-NLS-1$ //$NON-NLS-2$
		
		Group groupUpdate = new Group(composite, SWT.NONE);
		groupUpdate.setBackground(SWTResourceManager.getColor(SWT.COLOR_WIDGET_BACKGROUND));
		groupUpdate.setText(ResourceBundle.getBundle("com.makina.adb.client.messages").getString("MainWindow.groupUpdate.text")); //$NON-NLS-1$ //$NON-NLS-2$
		groupUpdate.setLayout(new FormLayout());
		FormData fdGroupUpdate = new FormData();
		//fdGroupUpdate.bottom = new FormAttachment(buttonValidate, -140);
		fdGroupUpdate.top = new FormAttachment(groupStatuses);
		fdGroupUpdate.left = new FormAttachment(0, 5);
		fdGroupUpdate.right = new FormAttachment(100, -5);
		fdGroupUpdate.height = 195;
		groupUpdate.setLayoutData(fdGroupUpdate);
		
		Canvas canvasSmartphoneDataUpdate = new Canvas(groupUpdate, SWT.NONE);
		FormData fdCanvasSmartphoneDataUpdate = new FormData();
		fdCanvasSmartphoneDataUpdate.left = new FormAttachment(0, 10);
		fdCanvasSmartphoneDataUpdate.height = 64;
		fdCanvasSmartphoneDataUpdate.width = 64;
		fdCanvasSmartphoneDataUpdate.top = new FormAttachment(0, 10);
		canvasSmartphoneDataUpdate.setLayoutData(fdCanvasSmartphoneDataUpdate);
		canvasSmartphoneDataUpdate.addPaintListener(new PaintListener()
		{
			public void paintControl(PaintEvent pe)
			{
				pe.gc.drawImage(SWTResourceManager.getImage(getClass(), "/resources/smartphone.png"), 0, 0);
			}
		});
		
		Canvas canvasServerDataUpdate = new Canvas(groupUpdate, SWT.NONE);
		FormData fdCanvasServerDataUpdate = new FormData();
		fdCanvasServerDataUpdate.top = new FormAttachment(0, 10);
		fdCanvasServerDataUpdate.height = 64;
		fdCanvasServerDataUpdate.width = 64;
		fdCanvasServerDataUpdate.right = new FormAttachment(100, -10);
		canvasServerDataUpdate.setLayoutData(fdCanvasServerDataUpdate);
		canvasServerDataUpdate.addPaintListener(new PaintListener()
		{
			public void paintControl(PaintEvent pe)
			{
				pe.gc.drawImage(SWTResourceManager.getImage(getClass(), "/resources/server.png"), 0, 0);
			}
		});
		
		ProgressBar progressBarDataUpdate = new ProgressBar(groupUpdate, SWT.NONE);
		FormData fdProgressBarDataUpate = new FormData();
		fdProgressBarDataUpate.left = new FormAttachment(canvasSmartphoneDataUpdate, 5);
		fdProgressBarDataUpate.right = new FormAttachment(canvasServerDataUpdate, -5);
		fdProgressBarDataUpate.top = new FormAttachment(canvasSmartphoneDataUpdate, -38);
		progressBarDataUpdate.setLayoutData(fdProgressBarDataUpate);
		
		Canvas canvasLedDataUpdate = new Canvas(groupUpdate, SWT.NONE);
		FormData fdCanvasLedDataUpdate = new FormData();
		fdCanvasLedDataUpdate.left = new FormAttachment(canvasSmartphoneDataUpdate, -40);
		fdCanvasLedDataUpdate.top = new FormAttachment(canvasSmartphoneDataUpdate, 5);
		fdCanvasLedDataUpdate.height = 16;
		fdCanvasLedDataUpdate.width = 16;
		canvasLedDataUpdate.setLayoutData(fdCanvasLedDataUpdate);
		canvasLedDataUpdate.addPaintListener(new PaintListener()
		{
			public void paintControl(PaintEvent pe)
			{
				pe.gc.drawImage(SWTResourceManager.getImage(getClass(), "/resources/led_none.png"), 0, 0);
			}
		});
		
		Label labelDataUpdate = new Label(groupUpdate, SWT.NONE);
		labelDataUpdate.setForeground(SWTResourceManager.getColor(0, 0, 0));
		FormData fdLabelDataUpdate = new FormData();
		fdLabelDataUpdate.top = new FormAttachment(canvasSmartphoneDataUpdate, 5);
		fdLabelDataUpdate.left = new FormAttachment(canvasLedDataUpdate, 5);
		labelDataUpdate.setLayoutData(fdLabelDataUpdate);
		labelDataUpdate.setText(ResourceBundle.getBundle("com.makina.adb.client.messages").getString("MainWindow.labelDataUpdate.text")); //$NON-NLS-1$ //$NON-NLS-2$
		
		Label labelDataSeparator = new Label(groupUpdate, SWT.NONE);
		labelDataSeparator.setForeground(SWTResourceManager.getColor(0, 0, 0));
		FormData fdlabelDataSeparator = new FormData();
		fdlabelDataSeparator.top = new FormAttachment(canvasLedDataUpdate, 0, SWT.TOP);
		fdlabelDataSeparator.left = new FormAttachment(80);
		labelDataSeparator.setLayoutData(fdlabelDataSeparator);
		labelDataSeparator.setText(":");
		
		Label labelDataUpdateStatus = new Label(groupUpdate, SWT.NONE);
		labelDataUpdateStatus.setForeground(SWTResourceManager.getColor(0, 0, 0));
		FormData fdLabelDataUpdateStatus = new FormData();
		fdLabelDataUpdateStatus.top = new FormAttachment(canvasLedDataUpdate, 0, SWT.TOP);
		fdLabelDataUpdateStatus.left = new FormAttachment(labelDataSeparator, 5);
		labelDataUpdateStatus.setLayoutData(fdLabelDataUpdateStatus);
		labelDataUpdateStatus.setText(ResourceBundle.getBundle("com.makina.adb.client.messages").getString("MainWindow.status.none")); //$NON-NLS-1$ //$NON-NLS-2$
		
		Canvas canvasServerAppUpdate = new Canvas(groupUpdate, SWT.NONE);
		FormData fdCanvasServerAppUpdate = new FormData();
		fdCanvasServerAppUpdate.top = new FormAttachment(canvasLedDataUpdate, 5);
		fdCanvasServerAppUpdate.left = new FormAttachment(canvasSmartphoneDataUpdate, 0, SWT.LEFT);
		fdCanvasServerAppUpdate.height = 64;
		fdCanvasServerAppUpdate.width = 64;
		canvasServerAppUpdate.setLayoutData(fdCanvasServerAppUpdate);
		canvasServerAppUpdate.addPaintListener(new PaintListener()
		{
			public void paintControl(PaintEvent pe)
			{
				pe.gc.drawImage(SWTResourceManager.getImage(getClass(), "/resources/server.png"), 0, 0);
			}
		});
		
		Canvas canvasSmartphoneAppUpdate = new Canvas(groupUpdate, SWT.NONE);
		FormData fdCanvasSmartphoneAppUpdate = new FormData();
		fdCanvasSmartphoneAppUpdate.top = new FormAttachment(canvasServerAppUpdate, 0, SWT.TOP);
		fdCanvasSmartphoneAppUpdate.right = new FormAttachment(canvasServerDataUpdate, 0, SWT.RIGHT);
		fdCanvasSmartphoneAppUpdate.height = 64;
		fdCanvasSmartphoneAppUpdate.width = 64;
		canvasSmartphoneAppUpdate.setLayoutData(fdCanvasSmartphoneAppUpdate);
		canvasSmartphoneAppUpdate.addPaintListener(new PaintListener()
		{
			public void paintControl(PaintEvent pe)
			{
				pe.gc.drawImage(SWTResourceManager.getImage(getClass(), "/resources/smartphone.png"), 0, 0);
			}
		});
		
		ProgressBar progressBarAppUpdate = new ProgressBar(groupUpdate, SWT.NONE);
		FormData fdProgressBarAppUpdate = new FormData();
		fdProgressBarAppUpdate.left = new FormAttachment(canvasServerAppUpdate, 5);
		fdProgressBarAppUpdate.right = new FormAttachment(canvasSmartphoneAppUpdate, -5);
		fdProgressBarAppUpdate.top = new FormAttachment(canvasServerAppUpdate, -38);
		progressBarAppUpdate.setLayoutData(fdProgressBarAppUpdate);
		
		Canvas canvasLedAppUpdate = new Canvas(groupUpdate, SWT.NONE);
		FormData fdCanvasLedAppUpdate = new FormData();
		fdCanvasLedAppUpdate.left = new FormAttachment(canvasServerAppUpdate, -40);
		fdCanvasLedAppUpdate.top = new FormAttachment(canvasServerAppUpdate, 5);
		fdCanvasLedAppUpdate.height = 16;
		fdCanvasLedAppUpdate.width = 16;
		canvasLedAppUpdate.setLayoutData(fdCanvasLedAppUpdate);
		canvasLedAppUpdate.addPaintListener(new PaintListener()
		{
			public void paintControl(PaintEvent pe)
			{
				pe.gc.drawImage(SWTResourceManager.getImage(getClass(), "/resources/led_none.png"), 0, 0);
			}
		});
		
		Label labelAppUpdate = new Label(groupUpdate, SWT.NONE);
		labelAppUpdate.setForeground(SWTResourceManager.getColor(0, 0, 0));
		FormData fdLabelAppUpdate = new FormData();
		fdLabelAppUpdate.top = new FormAttachment(canvasServerAppUpdate, 5);
		fdLabelAppUpdate.left = new FormAttachment(canvasLedAppUpdate, 5);
		labelAppUpdate.setLayoutData(fdLabelAppUpdate);
		labelAppUpdate.setText(ResourceBundle.getBundle("com.makina.adb.client.messages").getString("MainWindow.labelAppUpdate.text")); //$NON-NLS-1$ //$NON-NLS-2$
		
		Label labelAppUpdateSeparator = new Label(groupUpdate, SWT.NONE);
		labelAppUpdateSeparator.setForeground(SWTResourceManager.getColor(0, 0, 0));
		FormData fdlabelAppUpdateSeparator = new FormData();
		fdlabelAppUpdateSeparator.top = new FormAttachment(canvasLedAppUpdate, 0, SWT.TOP);
		fdlabelAppUpdateSeparator.left = new FormAttachment(80);
		labelAppUpdateSeparator.setLayoutData(fdlabelAppUpdateSeparator);
		labelAppUpdateSeparator.setText(":");
		
		Label labelAppUpdateStatus = new Label(groupUpdate, SWT.NONE);
		labelAppUpdateStatus.setForeground(SWTResourceManager.getColor(0, 0, 0));
		FormData fdLabelAppUpdateStatus = new FormData();
		fdLabelAppUpdateStatus.top = new FormAttachment(canvasLedAppUpdate, 0, SWT.TOP);
		fdLabelAppUpdateStatus.left = new FormAttachment(labelAppUpdateSeparator, 5);
		labelAppUpdateStatus.setLayoutData(fdLabelAppUpdateStatus);
		labelAppUpdateStatus.setText(ResourceBundle.getBundle("com.makina.adb.client.messages").getString("MainWindow.status.none")); //$NON-NLS-1$ //$NON-NLS-2$
		
		final ExpandBar expandBarLogs = new ExpandBar(composite, SWT.NONE);
		expandBarLogs.setVisible(false);
		FormData fdExpandBarLogs = new FormData();
		fdExpandBarLogs.bottom = new FormAttachment(buttonValidate, -5);
		fdExpandBarLogs.left = new FormAttachment(0, 5);
		fdExpandBarLogs.right = new FormAttachment(100, -5);
		fdExpandBarLogs.top = new FormAttachment(groupUpdate, 5);
		expandBarLogs.setLayoutData(fdExpandBarLogs);
		
		expandBarLogs.addExpandListener(new ExpandListener()
		{
			@Override
			public void itemExpanded(ExpandEvent ee)
			{
				if (ee.item instanceof ExpandItem)
				{
					ExpandItem item = (ExpandItem) ee.item;
					shell.setSize(shell.getSize().x, shell.getSize().y + item.getHeight());
				}
			}
			
			@Override
			public void itemCollapsed(ExpandEvent ee)
			{
				if (ee.item instanceof ExpandItem)
				{
					ExpandItem item = (ExpandItem) ee.item;
					shell.setSize(shell.getSize().x, shell.getSize().y - item.getHeight());
				}
			}
		});
		
		//shell.setSize(shell.getSize().x, shell.getSize().y + 100);
		
		final ExpandItem expandItemLogs = new ExpandItem(expandBarLogs, SWT.NONE);
		expandItemLogs.setExpanded(true);
		expandItemLogs.setText(ResourceBundle.getBundle("com.makina.adb.client.messages").getString("MainWindow.expandItemLogs.text")); //$NON-NLS-1$ //$NON-NLS-2$
		
		List list = new List(expandBarLogs, SWT.BORDER);
		expandItemLogs.setControl(list);
		expandItemLogs.setHeight(expandItemLogs.getControl().computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
		
		//list.add("Impossible de joindre le serveur '<nom du service>'");
		//list.add("Echec lors du transfert <cause>");
		
		buttonRetry.addListener(SWT.Selection, new Listener()
		{
			@Override
			public void handleEvent(Event e)
			{
				if (expandBarLogs.isVisible())
				{
					expandBarLogs.setVisible(false);
					shell.setSize(shell.getSize().x, shell.getSize().y - 100);
					buttonRetry.setVisible(false);
				}
				else
				{
					expandBarLogs.setVisible(true);
					shell.setSize(shell.getSize().x, shell.getSize().y + 100);
				}
			}
		});
	}
}
