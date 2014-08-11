/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Red Hat Inc. - Initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.server.ui.internal.cnf;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.ui.views.properties.IPropertySource;

/**
 * Filter the properties tab to only allow selections
 * that implement IPropertySource
 */
public class ServerViewPropertiesSectionFilter implements IFilter {

    public boolean select(Object toTest) {
    	if( toTest instanceof IAdaptable ) {
    		return ((IAdaptable)toTest).getAdapter(IPropertySource.class) != null;
    	}
        IPropertySource  properties = (IPropertySource) Platform.getAdapterManager().getAdapter(toTest, IPropertySource.class);
        return properties != null;
    }

}