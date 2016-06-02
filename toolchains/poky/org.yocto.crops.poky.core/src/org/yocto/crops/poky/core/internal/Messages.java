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
package org.yocto.crops.poky.core.internal;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {
	private static final String BUNDLE_NAME = "org.yocto.crops.poky.core.internal.messages"; //$NON-NLS-1$
	public static String PokyBoardManager_0;
	public static String PokyBoardManager_1;
	public static String PokyLaunchConfigurationDelegate_0;
	public static String PokyLaunchConfigurationDelegate_1;
	public static String PokyLaunchConfigurationDelegate_2;
	public static String PokyManager_0;
	public static String PokyManager_1;
	public static String PokyManager_2;
	public static String PokyPlatform_0;
	public static String PokyPlatform_1;
	public static String PokyProjectGenerator_0;

	static {
		// initialize resource bundle
		NLS.initializeMessages(BUNDLE_NAME, Messages.class);
	}

	private Messages() {
	}
}