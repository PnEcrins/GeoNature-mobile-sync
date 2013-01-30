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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;

import com.makina.ecrins.sync.tasks.TaskStatus;

/**
 * Widget for displaying the current data synchronization between a connected device and the server.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class DataUpdateComposite extends Composite implements Observer
{
	protected final Layout layout;
	
	protected ProgressBar progressBarDataUpdate;
	protected Canvas canvasLedDataUpdate;
	protected Label labelDataUpdate;
	protected Label labelDataUpdateStatus;
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public DataUpdateComposite(Composite parent, int style, Layout layout)
	{
		super(parent, SWT.NONE);
		
		this.layout = layout;
		
		initialize();
	}

	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}
	
	private void initialize()
	{
		setLayout(new FormLayout());
		
		FormData fdComposite = new FormData();
		fdComposite.left = new FormAttachment(0);
		fdComposite.right = new FormAttachment(100);
		fdComposite.top = new FormAttachment(0);
		fdComposite.height = 100;
		setLayoutData(fdComposite);
		
		Canvas canvasFromDeviceDataUpdate = new Canvas(this, SWT.NONE);
		FormData fdCanvasFromDeviceDataUpdate = new FormData();
		fdCanvasFromDeviceDataUpdate.top = new FormAttachment(0, 10);
		fdCanvasFromDeviceDataUpdate.left = new FormAttachment(0, 10);
		fdCanvasFromDeviceDataUpdate.height = 64;
		fdCanvasFromDeviceDataUpdate.width = 64;
		canvasFromDeviceDataUpdate.setLayoutData(fdCanvasFromDeviceDataUpdate);
		
		canvasFromDeviceDataUpdate.addPaintListener(new PaintListener()
		{
			public void paintControl(PaintEvent pe)
			{
				switch (layout)
				{
					case DEVICE_SERVER:
						pe.gc.drawImage(UIResourceManager.getImage("smartphone.png"), 0, 0);
						break;
					case SERVER_DEVICE:
						pe.gc.drawImage(UIResourceManager.getImage("server.png"), 0, 0);
						break;
				}
			}
		});
		
		Canvas canvasToDeviceDataUpdate = new Canvas(this, SWT.NONE);
		FormData fdCanvasToDeviceDataUpdate = new FormData();
		fdCanvasToDeviceDataUpdate.top = new FormAttachment(0, 10);
		fdCanvasToDeviceDataUpdate.height = 64;
		fdCanvasToDeviceDataUpdate.width = 64;
		fdCanvasToDeviceDataUpdate.right = new FormAttachment(100, -10);
		canvasToDeviceDataUpdate.setLayoutData(fdCanvasToDeviceDataUpdate);
		canvasToDeviceDataUpdate.addPaintListener(new PaintListener()
		{
			public void paintControl(PaintEvent pe)
			{
				switch (layout)
				{
					case DEVICE_SERVER:
						pe.gc.drawImage(UIResourceManager.getImage("server.png"), 0, 0);
						break;
					case SERVER_DEVICE:
						pe.gc.drawImage(UIResourceManager.getImage("smartphone.png"), 0, 0);
						break;
				}
			}
		});
		
		progressBarDataUpdate = new ProgressBar(this, SWT.NONE);
		FormData fdProgressBarDataUpate = new FormData();
		fdProgressBarDataUpate.left = new FormAttachment(canvasFromDeviceDataUpdate, 5);
		fdProgressBarDataUpate.right = new FormAttachment(canvasToDeviceDataUpdate, -5);
		fdProgressBarDataUpate.top = new FormAttachment(canvasFromDeviceDataUpdate, -38);
		progressBarDataUpdate.setLayoutData(fdProgressBarDataUpate);
		progressBarDataUpdate.setMinimum(0);
		progressBarDataUpdate.setMaximum(100);
		
		canvasLedDataUpdate = new Canvas(this, SWT.NONE);
		FormData fdCanvasLedDataUpdate = new FormData();
		fdCanvasLedDataUpdate.left = new FormAttachment(canvasFromDeviceDataUpdate, -40);
		fdCanvasLedDataUpdate.top = new FormAttachment(canvasFromDeviceDataUpdate, 5);
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
		
		labelDataUpdate = new Label(this, SWT.NONE);
		labelDataUpdate.setForeground(UIResourceManager.getColor(0, 0, 0));
		FormData fdLabelDataUpdate = new FormData();
		fdLabelDataUpdate.top = new FormAttachment(canvasFromDeviceDataUpdate, 5);
		fdLabelDataUpdate.left = new FormAttachment(canvasLedDataUpdate, 5);
		labelDataUpdate.setLayoutData(fdLabelDataUpdate);
		labelDataUpdate.setText(ResourceBundle.getBundle("messages").getString("MainWindow.labelDataUpdate.default.text"));
		
		Label labelDataSeparator = new Label(this, SWT.NONE);
		labelDataSeparator.setForeground(UIResourceManager.getColor(0, 0, 0));
		FormData fdlabelDataSeparator = new FormData();
		fdlabelDataSeparator.top = new FormAttachment(canvasLedDataUpdate, 0, SWT.TOP);
		fdlabelDataSeparator.left = new FormAttachment(80);
		labelDataSeparator.setLayoutData(fdlabelDataSeparator);
		labelDataSeparator.setText(":");
		
		labelDataUpdateStatus = new Label(this, SWT.NONE);
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
			
			getDisplay().syncExec(new Runnable()
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
					
					labelDataUpdate.setText(taskStatus.getMessage());
					
					progressBarDataUpdate.setSelection(taskStatus.getProgress());
					
					labelDataUpdateStatus.setText(ResourceBundle.getBundle("messages").getString("MainWindow.status." + taskStatus.getStatus().getLabel()));
					labelDataUpdateStatus.getParent().layout();
				}
			});
		}
	}
	
	public static enum Layout
	{
		DEVICE_SERVER,
		SERVER_DEVICE
	}
}
