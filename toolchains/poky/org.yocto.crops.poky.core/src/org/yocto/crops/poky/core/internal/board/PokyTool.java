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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import org.yocto.crops.poky.core.internal.Activator;
import org.yocto.crops.poky.core.internal.PokyPreferences;
import org.yocto.crops.poky.core.internal.build.PokyBuildConfiguration;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class PokyTool {

	private String name;
	private String version;
	private List<PokyToolSystem> systems;

	private transient PokyPackage pkg;

	public PokyPackage getPackage() {
		return pkg;
	}

	public String getName() {
		return name;
	}

	public String getVersion() {
		return version;
	}

	public List<PokyToolSystem> getSystems() {
		return systems;
	}

	void init(PokyPackage pkg) {
		this.pkg = pkg;
		for (PokyToolSystem system : systems) {
			system.setOwner(this);
		}
	}

	public Path getInstallPath() {
		// TODO remove migration in Neon
		Path oldPath = PokyPreferences.getPokyHome().resolve("tools").resolve(pkg.getName()).resolve(name) //$NON-NLS-1$
				.resolve(version);
		Path newPath = getPackage().getInstallPath().resolve("tools").resolve(name).resolve(version); //$NON-NLS-1$
		if (Files.exists(oldPath)) {
			try {
				Files.createDirectories(newPath.getParent());
				Files.move(oldPath, newPath);
				for (Path parent = oldPath.getParent(); parent != null; parent = parent.getParent()) {
					if (Files.newDirectoryStream(parent).iterator().hasNext()) {
						break;
					} else {
						Files.delete(parent);
					}
				}
			} catch (IOException e) {
				Activator.log(e);
			}
		}
		return newPath;
	}

	public boolean isInstalled() {
		return getInstallPath().toFile().exists();
	}

	public void install(IProgressMonitor monitor) throws CoreException {
		if (isInstalled()) {
			return;
		}

		for (PokyToolSystem system : systems) {
			if (system.isApplicable()) {
				system.install(monitor);
			}
		}

		// No valid system
		throw new CoreException(
				new Status(IStatus.ERROR, Activator.getId(), String.format("No valid system found for %s", name))); //$NON-NLS-1$
	}

	public Properties getToolProperties() {
		Properties properties = new Properties();
		properties.put("runtime.tools." + name + ".path", PokyBuildConfiguration.pathString(getInstallPath())); //$NON-NLS-1$//$NON-NLS-2$
		properties.put("runtime.tools." + name + '-' + version + ".path", //$NON-NLS-1$//$NON-NLS-2$
				PokyBuildConfiguration.pathString(getInstallPath())); //$NON-NLS-1$
		return properties;
	}

}