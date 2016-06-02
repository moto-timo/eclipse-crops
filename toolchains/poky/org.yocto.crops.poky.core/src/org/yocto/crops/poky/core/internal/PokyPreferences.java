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

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

public class PokyPreferences {

	private static final String POKY_HOME = "pokyHome"; //$NON-NLS-1$
	private static final String BOARD_URLS = "boardUrls"; //$NON-NLS-1$

	/* TODO: difference between .crops and .poky ? */
	private static final String defaultHome = Paths.get(System.getProperty("user.home"), ".poky").toString(); //$NON-NLS-1$ //$NON-NLS-2$
	/* TODO: concept of package and package_index? */
	private static final String defaultBoardUrls = "http://crops.github.io/packages/package_index.json"; //$NON-NLS-1$
	//		+ "\nhttp://arduino.esp8266.com/stable/package_esp8266com_index.json" //$NON-NLS-1$
	//		+ "\nhttps://adafruit.github.io/arduino-board-index/package_adafruit_index.json"; //$NON-NLS-1$

	private static IEclipsePreferences getPrefs() {
		return InstanceScope.INSTANCE.getNode(Activator.getId());
	}

	public static Path getPokyHome() {
		return Paths.get(getPrefs().get(POKY_HOME, defaultHome));
	}

	public static String getBoardUrls() {
		return getPrefs().get(BOARD_URLS, defaultBoardUrls);
	}

	public static Collection<URL> getBoardUrlList() throws CoreException {
		List<URL> urlList = new ArrayList<>();
		for (String url : getBoardUrls().split("\n")) { //$NON-NLS-1$
			try {
				urlList.add(new URL(url.trim()));
			} catch (MalformedURLException e) {
				throw Activator.coreException(e);
			}
		}
		return urlList;
	}
	
	public static void setBoardUrls(String boardUrls) {
		IEclipsePreferences prefs = getPrefs();
		prefs.put(BOARD_URLS, boardUrls);
		try {
			prefs.flush();
		} catch (BackingStoreException e) {
			Activator.log(e);
		}
	}

	public static String getDefaultBoardUrls() {
		return defaultBoardUrls;
	}
}