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
package org.eclipse.jst.server.tomcat.core.internal;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.wst.server.core.IModule;
import org.eclipse.wst.server.core.IServer;
import org.eclipse.wst.server.core.model.PublishOperation;
import org.eclipse.wst.server.core.model.PublishTaskDelegate;

public class PublishTask extends PublishTaskDelegate {
	public PublishOperation[] getTasks(IServer server, List modules) {
		if (modules == null)
			return null;
	
		TomcatServerBehaviour tomcatServer = (TomcatServerBehaviour) server.loadAdapter(TomcatServerBehaviour.class, null);
		
		List tasks = new ArrayList();
		int size = modules.size();
		for (int i = 0; i < size; i++) {
			IModule[] module = (IModule[]) modules.get(i);
			IModule m = module[module.length - 1];
			tasks.add(new PublishOperation2(tomcatServer, m));
		}
		
		return (PublishOperation[]) tasks.toArray(new PublishOperation[tasks.size()]);
	}
}