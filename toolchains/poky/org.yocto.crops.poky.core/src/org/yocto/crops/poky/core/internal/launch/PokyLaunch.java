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
package org.yocto.crops.poky.core.internal.launch;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.launchbar.core.target.ILaunchTarget;
import org.eclipse.launchbar.core.target.launch.TargetedLaunch;
import org.eclipse.remote.core.IRemoteConnection;
import org.yocto.crops.poky.core.internal.remote.PokyRemoteConnection;

/**
 * 
 *
 */
public class PokyLaunch extends TargetedLaunch {

	private final PokyRemoteConnection remote;
	private boolean wasOpen;

	/**
	 * 
	 */
	public PokyLaunch(ILaunchConfiguration launchConfiguration, String mode, ISourceLocator locator,
			ILaunchTarget target) {
		super(launchConfiguration, mode, target, locator);
		IRemoteConnection connection = target.getAdapter(IRemoteConnection.class);
		this.remote = connection.getService(PokyRemoteConnection.class);

		DebugPlugin.getDefault().addDebugEventListener(this);
	}




	public void start() {
		this.wasOpen = remote.getRemoteConnection().isOpen();
		if (wasOpen) {
			remote.pause();
		}
	}

	@Override
	public void handleDebugEvents(DebugEvent[] events) {
		super.handleDebugEvents(events);
		if (isTerminated() && wasOpen) {
			remote.resume();
			wasOpen = false;
		}
	}
}
