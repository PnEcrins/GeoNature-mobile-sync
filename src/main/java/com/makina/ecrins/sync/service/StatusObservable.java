package com.makina.ecrins.sync.service;

import java.util.Observable;

/**
 * <code>Observable</code> implementation for service {@link Status}.
 * 
 * @author <a href="mailto:sebastien.grimault@makina-corpus.com">S. Grimault</a>
 */
public class StatusObservable extends Observable
{
	public void update(Status status)
	{
		setChanged();
		notifyObservers(status);
	}
}
