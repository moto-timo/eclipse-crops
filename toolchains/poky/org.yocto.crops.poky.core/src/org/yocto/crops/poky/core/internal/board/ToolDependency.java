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
package org.yocto.crops.poky.core.internal.board;

import org.yocto.crops.poky.core.internal.Activator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class ToolDependency {

	private String packager;
	private String name;
	private String version;

	public String getPackager() {
		return packager;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public PokyTool getTool() throws CoreException {
		return Activator.getService(PokyManager.class).getTool(packager, name, version);
	}

	public void install(IProgressMonitor monitor) throws CoreException {
		PokyTool tool = getTool();
		if (tool == null) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.getId(),
					String.format("Tool not found %s %s", name, version)));
		}
		getTool().install(monitor);
	}

}