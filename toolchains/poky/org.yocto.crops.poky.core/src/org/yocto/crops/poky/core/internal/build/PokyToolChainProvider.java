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

import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.core.build.IToolChainProvider;
import org.eclipse.core.runtime.CoreException;

public class PokyToolChainProvider implements IToolChainProvider {

	public static final String ID = "org.yocto.crops.poky.core.toolChainProvider"; //$NON-NLS-1$

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public IToolChain getToolChain(String id, String version) throws CoreException {
		return new PokyToolChain(this, id, version);
	}

}