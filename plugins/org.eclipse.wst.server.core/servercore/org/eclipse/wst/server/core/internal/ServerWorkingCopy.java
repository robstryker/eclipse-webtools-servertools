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

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.wst.server.core.*;
import org.eclipse.wst.server.core.model.ServerDelegate;
/**
 * 
 */
public class ServerWorkingCopy extends Server implements IServerWorkingCopy {
	protected Server server;
	protected WorkingCopyHelper wch;
	
	protected ServerDelegate workingCopyDelegate;
	
	// working copy
	public ServerWorkingCopy(Server server) {
		super(server.getFile());
		this.server = server;
		
		map = new HashMap(server.map);
		wch = new WorkingCopyHelper(this);
		
		resolve();
	}
	
	// creation
	public ServerWorkingCopy(String id, IFile file, IRuntime runtime, IServerType serverType) {
		super(id, file, runtime, serverType);
		//server = this;
		wch = new WorkingCopyHelper(this);
		wch.setDirty(true);
		serverState = ((ServerType)serverType).getInitialState();
	}

	public boolean isWorkingCopy() {
		return true;
	}
	
	public IServer getOriginal() {
		return server;
	}
	
	public IServerWorkingCopy createWorkingCopy() {
		return this;
	}

	public int getServerState() {
		if (server != null)
			return server.getServerState();
		return serverState;
	}

	public void setServerState(int state) {
		if (server != null)
			server.setServerState(state);
		else
			super.setServerState(state);
	}
	
	public int getServerPublishState() {
		if (server != null)
			return server.getServerPublishState();
		return serverState;
	}

	public void setServerPublishState(int state) {
		if (server != null)
			server.setServerPublishState(state);
		else
			super.setServerPublishState(state);
	}
	
	public String getMode() {
		if (server != null)
			return server.getMode();
		return mode;
	}

	public void setMode(String mode) {
		if (server != null)
			server.setMode(mode);
		else
			super.setMode(mode);
	}

	public void setAttribute(String attributeName, int value) {
		wch.setAttribute(attributeName, value);
	}

	public void setAttribute(String attributeName, boolean value) {
		wch.setAttribute(attributeName, value);
	}

	public void setAttribute(String attributeName, String value) {
		wch.setAttribute(attributeName, value);
	}

	public void setAttribute(String attributeName, List value) {
		wch.setAttribute(attributeName, value);
	}

	public void setAttribute(String attributeName, Map value) {
		wch.setAttribute(attributeName, value);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.server.core.IServerWorkingCopy#setName(java.lang.String)
	 */
	public void setName(String name) {
		setAttribute(PROP_NAME, name);
	}

	public void setReadOnly(boolean b) {
		setAttribute(PROP_LOCKED, b);
	}

	/**
	 * Sets whether this element is private.
	 * Generally speaking, elements marked private are internal ones
	 * that should not be shown to users (because they won't know
	 * anything about them).
	 * 
	 * @param b <code>true</code> if this element is private,
	 * and <code>false</code> otherwise
	 * @see IServerAttributes#isPrivate()
	 */
	public void setPrivate(boolean b) {
		setAttribute(PROP_PRIVATE, b);
	}

	public void setHost(String host) {
		setAttribute(PROP_HOSTNAME, host);
	}

	public void setAutoPublishTime(int p) {
		setAttribute(PROP_AUTO_PUBLISH_TIME, p);
	}
	
	public void setAutoPublishDefault(boolean p) {
		setAttribute(PROP_AUTO_PUBLISH_DEFAULT, p);
	}

	public void setServerConfiguration(IFolder config) {
		this.configuration = config;
		if (configuration == null)
			setAttribute(CONFIGURATION_ID, (String)null);
		else
			setAttribute(CONFIGURATION_ID, configuration.getFullPath().toString());
	}

	/**
	 * Sets the file where this server instance is serialized.
	 * 
	 * @param file the file in the workspace where the server instance
	 *    is serialized, or <code>null</code> if the information is
	 *    instead to be persisted with the workspace but not with any
	 *    particular workspace resource
	 */
	public void setFile(IFile file) {
		this.file = file;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.server.core.IServerWorkingCopy#isDirty()
	 */
	public boolean isDirty() {
		return wch.isDirty();
	}
	
	public ServerDelegate getWorkingCopyDelegate(IProgressMonitor monitor) {
		// make sure that the regular delegate is loaded 
		//getDelegate();
		
		if (workingCopyDelegate != null)
			return workingCopyDelegate;
		
		if (serverType != null) {
			synchronized (this) {
				if (workingCopyDelegate == null) {
					try {
						long time = System.currentTimeMillis();
						IConfigurationElement element = ((ServerType) serverType).getElement();
						workingCopyDelegate = (ServerDelegate) element.createExecutableExtension("class");
						workingCopyDelegate.initialize(this);
						Trace.trace(Trace.PERFORMANCE, "ServerWorkingCopy.getWorkingCopyDelegate(): <" + (System.currentTimeMillis() - time) + "> " + getServerType().getId());
					} catch (Exception e) {
						Trace.trace(Trace.SEVERE, "Could not create delegate " + toString(), e);
					}
				}
			}
		}
		return workingCopyDelegate;
	}
	
	public void dispose() {
		super.dispose();
		if (workingCopyDelegate != null)
			workingCopyDelegate.dispose();
	}
	
	public IServer save(boolean force, IProgressMonitor monitor) throws CoreException {
		monitor = ProgressUtil.getMonitorFor(monitor);
		monitor.subTask(ServerPlugin.getResource("%savingTask", getName()));

		if (!force && getOriginal() != null)
			wch.validateTimestamp(((Server)getOriginal()).getTimestamp());

		if (server == null) {
			server = new Server(file);
			server.setServerState(serverState);
			server.publishListeners = publishListeners;
			server.notificationManager = notificationManager;
		}
		
		if (getServerType().hasServerConfiguration()) {
			IFolder folder = getServerConfiguration();
			if (folder == null) {
				folder = ServerType.getServerProject().getFolder(getName() + "-config");
				if (!folder.exists())
					folder.create(true, true, null);
				setServerConfiguration(folder);
			}
		}
		
		server.setInternal(this);
		server.doSave(monitor);
		if (getServerType().hasServerConfiguration()) {
			IFolder folder = getServerConfiguration();
			if (folder != null) {
				IProject project = folder.getProject();
				if (project != null && !project.exists()) {
					project.create(null);
					project.open(null);
					((ProjectProperties)ServerCore.getProjectProperties(project)).setServerProject(true, monitor);
				}
				if (!folder.exists())
					folder.create(IResource.FORCE, true, null);
			}
		}
		getDelegate().saveConfiguration(monitor);
		wch.setDirty(false);
		
		return server;
	}

	public IServer saveAll(boolean force, IProgressMonitor monitor) throws CoreException {
		if (runtime != null && runtime.isWorkingCopy()) {
			IRuntimeWorkingCopy wc = (IRuntimeWorkingCopy) runtime;
			wc.save(force, monitor);
		}
		
		return save(force, monitor);
	}

	/**
	 * Add a property change listener to this server.
	 *
	 * @param listener java.beans.PropertyChangeListener
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		wch.addPropertyChangeListener(listener);
	}
	
	/**
	 * Remove a property change listener from this server.
	 *
	 * @param listener java.beans.PropertyChangeListener
	 */
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		wch.removePropertyChangeListener(listener);
	}
	
	/**
	 * Fire a property change event.
	 */
	public void firePropertyChangeEvent(String propertyName, Object oldValue, Object newValue) {
		wch.firePropertyChangeEvent(propertyName, oldValue, newValue);
	}
	
	public void addServerListener(IServerListener listener) {
		if (server != null)
			server.addServerListener(listener);
		else
			super.addServerListener(listener);
	}
	
	public void removeServerListener(IServerListener listener) {
		if (server != null)
			server.removeServerListener(listener);
		else
			super.removeServerListener(listener);
	}
	
	public void addPublishListener(IPublishListener listener) {
		if (server != null)
			server.addPublishListener(listener);
		else
			super.addPublishListener(listener);
	}
	
	public void removePublishListener(IPublishListener listener) {
		if (server != null)
			server.removePublishListener(listener);
		else
			super.removePublishListener(listener);
	}

	public void setRuntime(IRuntime runtime) {
		this.runtime = runtime;
		if (runtime != null)
			setAttribute(RUNTIME_ID, runtime.getId());
		else
			setAttribute(RUNTIME_ID, (String)null);
	}
	
	public void setRuntimeId(String runtimeId) {
		setAttribute(RUNTIME_ID, runtimeId);
		resolve();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.wst.server.core.IServer#modifyModule(org.eclipse.wst.server.core.model.IModule)
	 */
	public void modifyModules(IModule[] add, IModule[] remove, IProgressMonitor monitor) throws CoreException {
		try {
			monitor = ProgressUtil.getMonitorFor(monitor);
			monitor.subTask(ServerPlugin.getResource("%taskModifyModules"));
			getWorkingCopyDelegate(monitor).modifyModules(add, remove, monitor);
			wch.setDirty(true);
			
			// trigger load of modules list
			getModules();
			
			if (add != null) {
				int size = add.length;
				for (int i = 0; i < size; i++) {
					if (!modules.contains(add[i]))
						modules.add(add[i]);
				}
			}
			
			if (remove != null) {
				int size = remove.length;
				for (int i = 0; i < size; i++) {
					if (modules.contains(remove[i]))
						modules.remove(remove[i]);
				}
			}
			
			// convert to attribute
			List list = new ArrayList();
			Iterator iterator = modules.iterator();
			while (iterator.hasNext()) {
				IModule module = (IModule) iterator.next();
				list.add(module.getId());
			}
			setAttribute(MODULE_LIST, list);
		} catch (CoreException ce) {
			throw ce;
		} catch (Exception e) {
			Trace.trace(Trace.SEVERE, "Error calling delegate modifyModule() " + toString(), e);
			throw new CoreException(new Status(IStatus.ERROR, ServerPlugin.PLUGIN_ID, 0, e.getLocalizedMessage(), e));
		}
	}

	public void setDefaults(IProgressMonitor monitor) {
		try {
			getWorkingCopyDelegate(monitor).setDefaults();
		} catch (Exception e) {
			Trace.trace(Trace.SEVERE, "Error calling delegate setDefaults() " + toString(), e);
		}
	}
	
	public String toString() {
		return "ServerWorkingCopy " + getId();
	}
}