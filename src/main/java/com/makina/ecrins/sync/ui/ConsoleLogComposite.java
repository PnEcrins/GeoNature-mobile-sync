package com.makina.ecrins.sync.ui;

import java.util.Observable;
import java.util.Observer;
import java.util.ResourceBundle;

import org.apache.log4j.Level;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ExpandEvent;
import org.eclipse.swt.events.ExpandListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ExpandBar;
import org.eclipse.swt.widgets.ExpandItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;

import com.makina.ecrins.sync.logger.LogMessage;

/**
 * Console logger widget.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class ConsoleLogComposite extends Composite implements Observer
{
	private ExpandItem expandItemLogs;
	private Table table;
	
	/**
	 * Create the composite.
	 * @param parent
	 * @param style
	 */
	public ConsoleLogComposite(Composite parent, int style)
	{
		super(parent, SWT.NONE);
		
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
		
		final ExpandBar expandBarLogs = new ExpandBar(this, SWT.NONE);
		expandBarLogs.setVisible(true);
		FormData fdExpandBarLogs = new FormData();
		fdExpandBarLogs.bottom = new FormAttachment(100);
		fdExpandBarLogs.left = new FormAttachment(0);
		fdExpandBarLogs.right = new FormAttachment(100);
		fdExpandBarLogs.top = new FormAttachment(0);
		expandBarLogs.setLayoutData(fdExpandBarLogs);
		
		expandBarLogs.setBackground(getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		expandBarLogs.addExpandListener(new ExpandListener()
		{
			@Override
			public void itemExpanded(ExpandEvent ee)
			{
				if (ee.item instanceof ExpandItem)
				{
					ExpandItem item = (ExpandItem) ee.item;
					getDisplay().getActiveShell().setSize(getDisplay().getActiveShell().getSize().x, getDisplay().getActiveShell().getSize().y + item.getHeight() + 10);
				}
			}
			
			@Override
			public void itemCollapsed(ExpandEvent ee)
			{
				if (ee.item instanceof ExpandItem)
				{
					ExpandItem item = (ExpandItem) ee.item;
					getDisplay().getActiveShell().setSize(getDisplay().getActiveShell().getSize().x, getDisplay().getActiveShell().getSize().y - item.getHeight() - 10);
				}
			}
		});
		
		expandItemLogs = new ExpandItem(expandBarLogs, SWT.NONE | SWT.H_SCROLL | SWT.V_SCROLL);
		expandItemLogs.setExpanded(false);
		expandItemLogs.setText(ResourceBundle.getBundle("messages").getString("MainWindow.expandItemLogs.text"));
		
		table = new Table(expandBarLogs,SWT.NONE);
		expandItemLogs.setControl(table);
		expandItemLogs.setHeight(expandItemLogs.getControl().computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
	}
	
	@Override
	public void update(Observable o, Object arg)
	{
		if (arg instanceof LogMessage)
		{
			final LogMessage message = (LogMessage) arg;
			
			if (!isDisposed())
			{
				getDisplay().asyncExec(new Runnable()
				{
					@Override
					public void run()
					{
						if (!isDisposed())
						{
							switch (message.getAction())
							{
								case RESET:
									table.clearAll();
									break;
								default:
									TableItem tableItem = new TableItem(table, SWT.None);
									tableItem.setText(message.getMessage());
									
									switch (message.getLevel().toInt())
									{
										case Level.ERROR_INT:
											tableItem.setForeground(getDisplay().getSystemColor(SWT.COLOR_RED));
											
											if (!expandItemLogs.getExpanded())
											{
												expandItemLogs.setExpanded(true);
												
												getDisplay().asyncExec(new Runnable()
												{
													@Override
													public void run()
													{
														getDisplay().getActiveShell().setSize(getDisplay().getActiveShell().getSize().x, getDisplay().getActiveShell().getSize().y + expandItemLogs.getHeight() + 10);
													}
												});
											}
											
											break;
										case Level.WARN_INT:
											tableItem.setForeground(getDisplay().getSystemColor(SWT.COLOR_DARK_YELLOW));
										default:
											tableItem.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLUE));
											break;
									}
							}
						}
					}
				});
			}
		}
	}
}
