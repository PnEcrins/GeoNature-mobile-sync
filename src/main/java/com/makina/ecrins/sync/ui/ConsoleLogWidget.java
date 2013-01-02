package com.makina.ecrins.sync.ui;

import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.ExpandListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;

/**
 * Console logger widget.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ConsoleLogWidget implements Observer
{
	private Display display;
	private Composite parent;
	private Control control;
	
	public ConsoleLogWidget(Display display, Composite parent, Control control)
	{
		this.display = display;
		this.parent = parent;
		this.control = control;
		
		createContents();
	}
	
	private void createContents()
	{
		final ExpandBar expandBarLogs = new ExpandBar(parent, SWT.NONE);
		expandBarLogs.setVisible(true);
		FormData fdExpandBarLogs = new FormData();
		fdExpandBarLogs.bottom = new FormAttachment(90);
		fdExpandBarLogs.left = new FormAttachment(0, 5);
		fdExpandBarLogs.right = new FormAttachment(100, -5);
		fdExpandBarLogs.top = new FormAttachment(control, 5);
		expandBarLogs.setLayoutData(fdExpandBarLogs);
		
		expandBarLogs.addExpandListener(new ExpandListener()
		{
			@Override
			public void itemExpanded(ExpandEvent ee)
			{
				if (ee.item instanceof ExpandItem)
				{
					ExpandItem item = (ExpandItem) ee.item;
					display.getActiveShell().setSize(display.getActiveShell().getSize().x, display.getActiveShell().getSize().y + item.getHeight());
				}
			}
			
			@Override
			public void itemCollapsed(ExpandEvent ee)
			{
				if (ee.item instanceof ExpandItem)
				{
					ExpandItem item = (ExpandItem) ee.item;
					display.getActiveShell().setSize(display.getActiveShell().getSize().x, display.getActiveShell().getSize().y - item.getHeight());
				}
			}
		});
		
		final ExpandItem expandItemLogs = new ExpandItem(expandBarLogs, SWT.NONE);
		expandItemLogs.setExpanded(false);
		expandItemLogs.setText(ResourceBundle.getBundle("messages").getString("MainWindow.expandItemLogs.text"));
		
		org.eclipse.swt.widgets.List list = new org.eclipse.swt.widgets.List(expandBarLogs, SWT.BORDER);
		expandItemLogs.setControl(list);
		expandItemLogs.setHeight(expandItemLogs.getControl().computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
	}
	
	@Override
	public void update(Observable o, Object arg)
	{
		// TODO Auto-generated method stub
	}
}
