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

import java.io.IOException;

import org.yocto.crops.poky.core.internal.Activator;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;

public class PokyToolSystem {

	private String host;
	private String archiveFileName;
	private String url;
	private String checksum;
	private String size;

	private transient PokyTool tool;

	public void setOwner(PokyTool tool) {
		this.tool = tool;
	}

	public String getHost() {
		return host;
	}

	public String getArchiveFileName() {
		return archiveFileName;
	}

	public String getUrl() {
		return url;
	}

	public String getChecksum() {
		return checksum;
	}

	public String getSize() {
		return size;
	}

	public boolean isApplicable() {
		switch (Platform.getOS()) {
		case Platform.OS_WIN32:
			return "i686-mingw32".equals(host); //$NON-NLS-1$
		case Platform.OS_MACOSX:
			switch (host) {
			case "i386-apple-darwin11": //$NON-NLS-1$
			case "x86_64-apple-darwin": //$NON-NLS-1$
				return true;
			default:
				return false;
			}
		case Platform.OS_LINUX:
			switch (Platform.getOSArch()) {
			case Platform.ARCH_X86_64:
				switch (host) {
				case "x86_64-pc-linux-gnu": //$NON-NLS-1$
				case "x86_64-linux-gnu": //$NON-NLS-1$
					return true;
				default:
					return false;
				}
			case Platform.ARCH_X86:
				switch (host) {
				case "i686-pc-linux-gnu": //$NON-NLS-1$
				case "i686-linux-gnu": //$NON-NLS-1$
					return true;
				default:
					return false;
				}
			default:
				return false;
			}
		default:
			return false;
		}
	}

	public void install(IProgressMonitor monitor) throws CoreException {
		try {
			PokyManager.downloadAndInstall(url, archiveFileName, tool.getInstallPath(), monitor);
		} catch (IOException e) {
			throw Activator.coreException(e);
		}
	}

}