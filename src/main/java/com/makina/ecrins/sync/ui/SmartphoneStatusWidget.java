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
 * Widget for displaying device status.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class SmartphoneStatusWidget implements Observer
{
	private Display display;
	private Composite composite;
	private Canvas canvasSmartphoneStatus;
	private Label labelSmartphone;
	private Label labelSmartphoneSeparator;
	private Label labelSmartphoneStatus;
	
	public SmartphoneStatusWidget(Display display, Composite composite)
	{
		this.display = display;
		this.composite = composite;
		
		createContents();
	}

	private void createContents()
	{
		canvasSmartphoneStatus = new Canvas(composite, SWT.NONE);
		canvasSmartphoneStatus.setLayout(new FormLayout());
		FormData fd_canvasSmartphoneStatus = new FormData();
		fd_canvasSmartphoneStatus.left = new FormAttachment(composite, 10);
		fd_canvasSmartphoneStatus.top = new FormAttachment(composite, 10);
		fd_canvasSmartphoneStatus.height = 22;
		fd_canvasSmartphoneStatus.width = 22;
		canvasSmartphoneStatus.setLayoutData(fd_canvasSmartphoneStatus);
		
		canvasSmartphoneStatus.addPaintListener(new PaintListener()
		{
			public void paintControl(PaintEvent pe)
			{
				pe.gc.drawImage(UIResourceManager.getImage("smartphone_status_none.png"), 0, 0);
			}
		});

		labelSmartphone = new Label(composite, SWT.NONE);
		FormData fdLabelSmartphone = new FormData();
		fdLabelSmartphone.top = new FormAttachment(canvasSmartphoneStatus, 5, SWT.TOP);
		fdLabelSmartphone.left = new FormAttachment(canvasSmartphoneStatus, 5);
		labelSmartphone.setLayoutData(fdLabelSmartphone);
		labelSmartphone.setText(ResourceBundle.getBundle("messages").getString("MainWindow.labelSmartphone.text"));
		
		labelSmartphoneSeparator = new Label(composite, SWT.NONE);
		FormData fdlabelSmartphoneSeparator = new FormData();
		fdlabelSmartphoneSeparator.top = new FormAttachment(canvasSmartphoneStatus, 5, SWT.TOP);
		fdlabelSmartphoneSeparator.left = new FormAttachment(40);
		labelSmartphoneSeparator.setLayoutData(fdlabelSmartphoneSeparator);
		labelSmartphoneSeparator.setText(":");
				
		labelSmartphoneStatus = new Label(composite, SWT.NONE);
		FormData fd_labelSmartphoneStatus = new FormData();
		fd_labelSmartphoneStatus.top = new FormAttachment(canvasSmartphoneStatus, 5, SWT.TOP);
		fd_labelSmartphoneStatus.left = new FormAttachment(labelSmartphoneSeparator, 5);
		labelSmartphoneStatus.setLayoutData(fd_labelSmartphoneStatus);
		labelSmartphoneStatus.setText(ResourceBundle.getBundle("messages").getString("MainWindow.status.none"));
	}
	
	@Override
	public void update(Observable o, Object arg)
	{
		if (arg instanceof Status)
		{
			final Status status = (Status) arg;
			
			display.syncExec(new Runnable()
			{
				@Override
				public void run()
				{
					canvasSmartphoneStatus.addPaintListener(new PaintListener()
					{
						public void paintControl(PaintEvent pe)
						{
							pe.gc.drawImage(UIResourceManager.getImage("smartphone_status_" + status.getLabel() + ".png"), 0, 0);
						}
					});
					
					canvasSmartphoneStatus.redraw();
					
					labelSmartphoneStatus.setText(ResourceBundle.getBundle("messages").getString("MainWindow.status." + status.getLabel()));
					labelSmartphoneStatus.getParent().layout();
					
					switch (status)
					{
						case STATUS_PENDING:
							labelSmartphone.setForeground(UIResourceManager.getColor(218, 165, 32));
							labelSmartphoneSeparator.setForeground(UIResourceManager.getColor(218, 165, 32));
							labelSmartphoneStatus.setForeground(UIResourceManager.getColor(218, 165, 32));
							break;
						case STATUS_FAILED:
							labelSmartphone.setForeground(UIResourceManager.getColor(255, 0, 0));
							labelSmartphoneSeparator.setForeground(UIResourceManager.getColor(255, 0, 0));
							labelSmartphoneStatus.setForeground(UIResourceManager.getColor(255, 0, 0));
							break;
						case STATUS_CONNECTED:
						case STATUS_FINISH:
							labelSmartphone.setForeground(UIResourceManager.getColor(0, 128, 0));
							labelSmartphoneSeparator.setForeground(UIResourceManager.getColor(0, 128, 0));
							labelSmartphoneStatus.setForeground(UIResourceManager.getColor(0, 128, 0));
							break;
						default:
							labelSmartphone.setForeground(UIResourceManager.getColor(0, 0, 0));
							labelSmartphoneSeparator.setForeground(UIResourceManager.getColor(0, 0, 0));
							labelSmartphoneStatus.setForeground(UIResourceManager.getColor(0, 0, 0));
							break;
					}
				}
			});
		}
	}
}
