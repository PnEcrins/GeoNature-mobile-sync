package com.makina.ecrins.sync.ui;

import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

import com.makina.ecrins.sync.tasks.TaskStatus;

/**
 * Widget for displaying the current data synchronization between a connected device and the server.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class DataUpdateWidget implements Observer
{
	private Display display;
	private Composite parent;
	
	private ProgressBar progressBarDataUpdate;
	private Canvas canvasLedDataUpdate;
	private Label labelDataUpdateStatus;
	
	public DataUpdateWidget(Display display, Composite parent)
	{
		this.display = display;
		this.parent = parent;
		
		createContents();
	}
	
	private void createContents()
	{
		Canvas canvasSmartphoneDataUpdate = new Canvas(parent, SWT.NONE);
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
				pe.gc.drawImage(UIResourceManager.getImage("smartphone.png"), 0, 0);
			}
		});
		
		Canvas canvasServerDataUpdate = new Canvas(parent, SWT.NONE);
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
				pe.gc.drawImage(UIResourceManager.getImage("server.png"), 0, 0);
			}
		});
		
		progressBarDataUpdate = new ProgressBar(parent, SWT.NONE);
		FormData fdProgressBarDataUpate = new FormData();
		fdProgressBarDataUpate.left = new FormAttachment(canvasSmartphoneDataUpdate, 5);
		fdProgressBarDataUpate.right = new FormAttachment(canvasServerDataUpdate, -5);
		fdProgressBarDataUpate.top = new FormAttachment(canvasSmartphoneDataUpdate, -38);
		progressBarDataUpdate.setLayoutData(fdProgressBarDataUpate);
		progressBarDataUpdate.setMinimum(0);
		progressBarDataUpdate.setMaximum(100);
		
		canvasLedDataUpdate = new Canvas(parent, SWT.NONE);
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
				pe.gc.drawImage(UIResourceManager.getImage("led_none.png"), 0, 0);
			}
		});
		
		Label labelDataUpdate = new Label(parent, SWT.NONE);
		labelDataUpdate.setForeground(UIResourceManager.getColor(0, 0, 0));
		FormData fdLabelDataUpdate = new FormData();
		fdLabelDataUpdate.top = new FormAttachment(canvasSmartphoneDataUpdate, 5);
		fdLabelDataUpdate.left = new FormAttachment(canvasLedDataUpdate, 5);
		labelDataUpdate.setLayoutData(fdLabelDataUpdate);
		labelDataUpdate.setText(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.text"));
		
		Label labelDataSeparator = new Label(parent, SWT.NONE);
		labelDataSeparator.setForeground(UIResourceManager.getColor(0, 0, 0));
		FormData fdlabelDataSeparator = new FormData();
		fdlabelDataSeparator.top = new FormAttachment(canvasLedDataUpdate, 0, SWT.TOP);
		fdlabelDataSeparator.left = new FormAttachment(80);
		labelDataSeparator.setLayoutData(fdlabelDataSeparator);
		labelDataSeparator.setText(":");
		
		labelDataUpdateStatus = new Label(parent, SWT.NONE);
		labelDataUpdateStatus.setForeground(UIResourceManager.getColor(0, 0, 0));
		FormData fdLabelDataUpdateStatus = new FormData();
		fdLabelDataUpdateStatus.top = new FormAttachment(canvasLedDataUpdate, 0, SWT.TOP);
		fdLabelDataUpdateStatus.left = new FormAttachment(labelDataSeparator, 5);
		labelDataUpdateStatus.setLayoutData(fdLabelDataUpdateStatus);
		labelDataUpdateStatus.setText(ResourceBundle.getBundle("messages").getString("MainWindow.status.none"));
	}
	
	@Override
	public void update(Observable o, Object arg)
	{
		if (arg instanceof TaskStatus)
		{
			final TaskStatus taskStatus = (TaskStatus) arg;
			
			display.syncExec(new Runnable()
			{
				@Override
				public void run()
				{
					canvasLedDataUpdate.addPaintListener(new PaintListener()
					{
						public void paintControl(PaintEvent pe)
						{
							pe.gc.drawImage(UIResourceManager.getImage("led_" + taskStatus.getStatus().getLabel() + ".png"), 0, 0);
						}
					});
					
					canvasLedDataUpdate.redraw();
					
					progressBarDataUpdate.setSelection(taskStatus.getProgress());
					
					labelDataUpdateStatus.setText(ResourceBundle.getBundle("messages").getString("MainWindow.status." + taskStatus.getStatus().getLabel()));
					labelDataUpdateStatus.getParent().layout();
				}
			});
		}
	}
}
