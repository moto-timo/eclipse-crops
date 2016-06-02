/*******************************************************************************
 * Copyright (c) 2016 Intel Corporation and others.
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     Intel Corporation - CROPS Poky version
 *******************************************************************************/
package org.yocto.crops.poky.core.internal.build;

import org.eclipse.cdt.build.gcc.core.GCCToolChain;
import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.core.build.IToolChainProvider;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.runtime.CoreException;

public class PokyToolChain extends GCCToolChain {

	PokyToolChain(IToolChainProvider provider, IBuildConfiguration config) throws CoreException {
		super(provider, config.getProject().getName() + '#' + config.getName(), ""); //$NON-NLS-1$
	}
	
	public PokyToolChain(IToolChainProvider provider, String id, String version) {
		super(provider, id, version);
	}
	
	@Override
	public String getProperty(String key) {
		// TODO architecture if I need it
		if (key.equals(IToolChain.ATTR_OS)) {
			return "poky"; //$NON-NLS-1$
		} else {
			return null;
		}
	}

}