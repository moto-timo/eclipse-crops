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

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.yocto.crops.poky.core.internal.Activator;
import org.yocto.crops.poky.core.internal.PokyPreferences;
import org.eclipse.core.runtime.CoreException;

public class PokyPackage {

	// JSON fields
	private String name;
	private String maintainer;
	private String websiteURL;
	private String email;
	private PokyHelp help;
	private List<PokyPlatform> platforms;
	private List<PokyTool> tools;
	// end JSON fields

	private Map<String, PokyPlatform> installedPlatforms;

	public String getName() {
		return name;
	}

	public String getMaintainer() {
		return maintainer;
	}

	public String getWebsiteURL() {
		return websiteURL;
	}

	public String getEmail() {
		return email;
	}

	public PokyHelp getHelp() {
		return help;
	}

	public Collection<PokyPlatform> getPlatforms() {
		return Collections.unmodifiableCollection(platforms);
	}

	void init() {
		for (PokyPlatform platform : platforms) {
			platform.init(this);
		}
		for (PokyTool tool : tools) {
			tool.init(this);
		}
	}

	public PokyPlatform getPlatform(String architecture, String version) {
		if (platforms != null) {
			for (PokyPlatform plat : platforms) {
				if (plat.getArchitecture().equals(architecture) && plat.getVersion().equals(version)) {
					return plat;
				}
			}
		}
		return null;
	}

	public Path getInstallPath() {
		return PokyPreferences.getPokyHome().resolve("packages").resolve(getName()); //$NON-NLS-1$
	}

	private void initInstalledPlatforms() throws CoreException {
		if (installedPlatforms == null) {
			installedPlatforms = new HashMap<>();

			if (Files.isDirectory(getInstallPath())) {
				Path platformTxt = Paths.get("platform.txt"); //$NON-NLS-1$
				try {
					Files.find(getInstallPath().resolve("hardware"), 2, //$NON-NLS-1$
							(path, attrs) -> path.getFileName().equals(platformTxt))
							.forEach(path -> {
								try (FileReader reader = new FileReader(path.toFile())) {
									Properties platformProperties = new Properties();
									platformProperties.load(reader);
									String arch = path.getName(path.getNameCount() - 2).toString();
									String version = platformProperties.getProperty("version"); //$NON-NLS-1$

									PokyPlatform platform = getPlatform(arch, version);
									if (platform != null) {
										platform.setPlatformProperties(platformProperties);
										installedPlatforms.put(arch, platform);
									} // TODO manually add it if was removed from index
								} catch (IOException e) {
									throw new RuntimeException(e);
								}
							});
				} catch (IOException e) {
					throw Activator.coreException(e);
				}
			}
		}
	}

	public Collection<PokyPlatform> getInstalledPlatforms() throws CoreException {
		initInstalledPlatforms();
		return installedPlatforms.values();
	}

	public PokyPlatform getInstalledPlatform(String architecture) throws CoreException {
		if (architecture == null) {
			return null;
		} else {
			initInstalledPlatforms();
			return installedPlatforms.get(architecture);
		}
	}

	void platformInstalled(PokyPlatform platform) {
		installedPlatforms.put(platform.getArchitecture(), platform);
	}

	void platformUninstalled(PokyPlatform platform) {
		installedPlatforms.remove(platform.getArchitecture());
	}

	public Collection<PokyPlatform> getAvailablePlatforms() throws CoreException {
		initInstalledPlatforms();
		Map<String, PokyPlatform> platformMap = new HashMap<>();
		for (PokyPlatform platform : platforms) {
			if (!installedPlatforms.containsKey(platform.getArchitecture())) {
				PokyPlatform p = platformMap.get(platform.getName());
				if (p == null || PokyManager.compareVersions(platform.getVersion(), p.getVersion()) > 0) {
					platformMap.put(platform.getName(), platform);
				}
			}
		}
		return platformMap.values();
	}

	public List<PokyTool> getTools() {
		return tools;
	}

	public PokyTool getTool(String toolName, String version) {
		for (PokyTool tool : tools) {
			if (tool.getName().equals(toolName) && tool.getVersion().equals(version)) {
				return tool;
			}
		}
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof PokyPackage) {
			return ((PokyPackage) obj).getName().equals(name);
		}
		return super.equals(obj);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}

}