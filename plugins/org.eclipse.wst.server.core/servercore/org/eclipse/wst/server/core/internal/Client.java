/**********************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - Initial API and implementation
 **********************************************************************/
package org.eclipse.wst.server.core.internal;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunch;

import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.ClientDelegate;
/**
 * 
 * @since 1.0
 */
public class Client implements IClient {
	private IConfigurationElement element;
	private ClientDelegate delegate;

	/**
	 * LaunchableClient constructor comment.
	 */
	public Client(IConfigurationElement element) {
		super();
		this.element = element;
	}

	/**
	 * Returns the id of this LaunchableClient.
	 *
	 * @return java.lang.String
	 */
	public String getId() {
		return element.getAttribute("id");
	}

	/*
	 * @see IPublishManager#getDescription()
	 */
	public String getDescription() {
		return element.getAttribute("description");
	}
	
	protected String getLaunchable() {
		return element.getAttribute("launchable");
	}

	/*
	 * @see IPublishManager#getLabel()
	 */
	public String getName() {
		String label = element.getAttribute("name");
		if (label == null)
			return "n/a";
		return label;
	}

	/*
	 * @see IPublishManager#getDelegate()
	 */
	public ClientDelegate getDelegate() {
		if (delegate == null) {
			try {
				delegate = (ClientDelegate) element.createExecutableExtension("class");
			} catch (Exception e) {
				Trace.trace(Trace.SEVERE, "Could not create delegate" + toString() + ": " + e.getMessage());
			}
		}
		return delegate;
	}

	/**
	 * 
	 */
	public boolean supports(IServer server, Object launchable, String launchMode) {
		if (launchable == null)
			return false;
		//if (!launchable.getClass().getName().equals(getLaunchable()))
		//	return false;
		try {
			return getDelegate().supports(server, launchable, launchMode);
		} catch (Exception e) {
			Trace.trace(Trace.SEVERE, "Error calling delegate " + toString() + ": " + e.getMessage());
			return false;
		}
	}

	/**
	 * Opens or executes on the launchable.
	 */
	public IStatus launch(IServer server, Object launchable, String launchMode, ILaunch launch) {
		try {
			return getDelegate().launch(server, launchable, launchMode, launch);
		} catch (Exception e) {
			Trace.trace(Trace.SEVERE, "Error calling delegate " + toString() + ": " + e.getMessage());
		}
		return null;
	}
	
	/**
	 * Return a string representation of this object.
	 * 
	 * @return java.lang.String
	 */
	public String toString() {
		return "Client[" + getId() + "]";
	}
}