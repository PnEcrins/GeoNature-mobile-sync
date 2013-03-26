package com.makina.ecrins.sync.ui;

import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;

import com.makina.ecrins.sync.service.Status;

/**
 * Widget for displaying server status.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ServerStatusWidget implements Observer
{
	private Display display;
	private Composite parent;
	
	private Canvas canvasServerStatus;
	private Label labelServer;
	private Label labelServerSeparator;
	private Label labelServerStatus;
	
	private Status status;
	
	public ServerStatusWidget(Display display, Composite parent)
	{
		this.display = display;
		this.parent = parent;
		
		this.status = Status.STATUS_NONE;
		
		createContents();
	}

	private void createContents()
	{
		canvasServerStatus = new Canvas(parent, SWT.NONE);
		canvasServerStatus.setLayout(new FormLayout());
		
		FormData fd_canvasServerStatus = new FormData();
		fd_canvasServerStatus.left = new FormAttachment(parent, 10);
		fd_canvasServerStatus.top = new FormAttachment(parent, 37);
		fd_canvasServerStatus.height = 22;
		fd_canvasServerStatus.width = 22;
		
		canvasServerStatus.setLayoutData(fd_canvasServerStatus);
		canvasServerStatus.addPaintListener(new PaintListener()
		{
			public void paintControl(PaintEvent pe)
			{
				pe.gc.drawImage(UIResourceManager.getImage("server_status_" + status.getLabel() + ".png"), 0, 0);
			}
		});
		
		labelServer = new Label(parent, SWT.NONE);
		FormData fdLabelServer = new FormData();
		fdLabelServer.top = new FormAttachment(canvasServerStatus, 5, SWT.TOP);
		fdLabelServer.left = new FormAttachment(canvasServerStatus, 5);
		labelServer.setLayoutData(fdLabelServer);
		labelServer.setText(ResourceBundle.getBundle("messages").getString("MainWindow.labelServer.text"));
		
		labelServerSeparator = new Label(parent, SWT.NONE);
		FormData fdlabelServerSeparator = new FormData();
		fdlabelServerSeparator.top = new FormAttachment(canvasServerStatus, 5, SWT.TOP);
		fdlabelServerSeparator.left = new FormAttachment(40);
		labelServerSeparator.setLayoutData(fdlabelServerSeparator);
		labelServerSeparator.setText(":");
		
		labelServerStatus = new Label(parent, SWT.NONE);
		FormData fd_labelServerStatus = new FormData();
		fd_labelServerStatus.top = new FormAttachment(canvasServerStatus, 5, SWT.TOP);
		fd_labelServerStatus.left = new FormAttachment(labelServerSeparator, 5);
		labelServerStatus.setLayoutData(fd_labelServerStatus);
		labelServerStatus.setText(ResourceBundle.getBundle("messages").getString("MainWindow.status.none"));
	}

	@Override
	public void update(Observable o, Object arg)
	{
		if (arg instanceof Status)
		{
			this.status = (Status) arg;
			
			if (!display.isDisposed())
			{
				display.syncExec(new Runnable()
				{
					@Override
					public void run()
					{
						canvasServerStatus.redraw();
						
						labelServerStatus.setText(ResourceBundle.getBundle("messages").getString("MainWindow.status." + status.getLabel()));
						labelServerStatus.getParent().layout();
						
						switch (status)
						{
							case STATUS_PENDING:
								labelServer.setForeground(UIResourceManager.getColor(218, 165, 32));
								labelServerSeparator.setForeground(UIResourceManager.getColor(218, 165, 32));
								labelServerStatus.setForeground(UIResourceManager.getColor(218, 165, 32));
								break;
							case STATUS_FAILED:
								labelServer.setForeground(UIResourceManager.getColor(255, 0, 0));
								labelServerSeparator.setForeground(UIResourceManager.getColor(255, 0, 0));
								labelServerStatus.setForeground(UIResourceManager.getColor(255, 0, 0));
								break;
							case STATUS_CONNECTED:
							case STATUS_FINISH:
								labelServer.setForeground(UIResourceManager.getColor(0, 128, 0));
								labelServerSeparator.setForeground(UIResourceManager.getColor(0, 128, 0));
								labelServerStatus.setForeground(UIResourceManager.getColor(0, 128, 0));
								break;
							default:
								labelServer.setForeground(UIResourceManager.getColor(0, 0, 0));
								labelServerSeparator.setForeground(UIResourceManager.getColor(0, 0, 0));
								labelServerStatus.setForeground(UIResourceManager.getColor(0, 0, 0));
								break;
						}
					}
				});
			}
		}
	}
}
