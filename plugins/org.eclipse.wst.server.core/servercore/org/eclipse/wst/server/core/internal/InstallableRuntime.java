/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.server.core.internal;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.update.standalone.InstallCommand;
import org.osgi.framework.Bundle;
/**
 * 
 */
public class InstallableRuntime implements IInstallableRuntime {
	private IConfigurationElement element;

	public InstallableRuntime(IConfigurationElement element) {
		super();
		this.element = element;
	}

	/**
	 * 
	 * @return the id
	 */
	public String getId() {
		try {
			return element.getAttribute("id");
		} catch (Exception e) {
			return null;
		}
	}

	public String getFeatureVersion() {
		try {
			return element.getAttribute("featureVersion");
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	public String getFeatureId() {
		try {
			return element.getAttribute("featureId");
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	public String getBundleId() {
		try {
			return element.getAttribute("bundleId");
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	public String getPath() {
		try {
			return element.getAttribute("path");
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	public String getFromSite() {
		try {
			return element.getAttribute("featureSite");
		} catch (Exception e) {
			// ignore
		}
		return null;
	}

	/*
	 * @see IInstallableServer#install(IProgressMonitor)
	 */
	public void install(IPath path, IProgressMonitor monitor) throws CoreException {
		String featureId = getFeatureId();
		String featureVersion = getFeatureVersion();
		String fromSite = getFromSite();
		
		if (featureId == null || featureVersion == null || fromSite == null)
			return;
		
		// download and install plugins
		Bundle bundle = Platform.getBundle(getBundleId());
		if (bundle == null) {
			try {
				monitor.setTaskName("Installing feature");
				InstallCommand command = new InstallCommand(featureId, featureVersion, fromSite, null, "false");
				command.run(monitor);
				command.applyChangesNow();
			} catch (Exception e) {
				Trace.trace(Trace.SEVERE, "Error installing feature", e);
				return;
			}
		}
		
		// unzip from bundle into path
		try {
			byte[] buf = new byte[8192];
			bundle = Platform.getBundle(getBundleId());
			URL url = bundle.getEntry(getPath());
			url = Platform.resolve(url);
			InputStream in = url.openStream();
			BufferedInputStream bin = new BufferedInputStream(in);
			ZipInputStream zin = new ZipInputStream(bin);
			ZipEntry entry = zin.getNextEntry();
			while (entry != null) {
				String name = entry.getName();
				monitor.setTaskName("Unzipping: " + name);
				
				if (entry.isDirectory()) {
					path.append(name).toFile().mkdirs();
				} else {
					FileOutputStream fout = new FileOutputStream(path.append(name).toFile());
					int r = zin.read(buf);
					while (r >= 0) {
						fout.write(buf, 0, r);
						r = zin.read(buf);
					}
				}
				zin.closeEntry();
				entry = zin.getNextEntry();
			}
			zin.close();
		} catch (Exception e) {
			Trace.trace(Trace.SEVERE, "Error installing feature", e);
		} 
	}

	public String toString() {
		return "InstallableRuntime[" + getId() + "]";
	}
}