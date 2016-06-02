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

import java.util.Collection;

import org.yocto.crops.poky.core.internal.Activator;
import org.yocto.crops.poky.core.internal.board.PokyBoard;
import org.yocto.crops.poky.core.internal.board.PokyManager;
import org.yocto.crops.poky.core.internal.remote.PokyRemoteConnection;
import org.eclipse.cdt.core.build.ICBuildConfiguration;
import org.eclipse.cdt.core.build.ICBuildConfigurationManager;
import org.eclipse.cdt.core.build.ICBuildConfigurationProvider;
import org.eclipse.cdt.core.build.IToolChain;
import org.eclipse.cdt.core.build.IToolChainManager;
import org.eclipse.cdt.core.build.IToolChainProvider;
import org.eclipse.core.resources.IBuildConfiguration;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

public class PokyBuildConfigurationProvider implements ICBuildConfigurationProvider {

	public static final String ID = "org.yocto.crops.poky.core.provider"; //$NON-NLS-1$

	private static final ICBuildConfigurationManager configManager = Activator
			.getService(ICBuildConfigurationManager.class);
	private static final PokyManager pokyManager = Activator.getService(PokyManager.class);

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public ICBuildConfiguration getCBuildConfiguration(IBuildConfiguration config, String name) throws CoreException {
		return new PokyBuildConfiguration(config, name);
	}

	@Override
	public ICBuildConfiguration getDefaultCBuildConfiguration(IProject project) throws CoreException {
		/* TODO: figure out how we handle board descriptions */
		PokyBoard board = pokyManager.getBoard("poky", "poky", "Poky"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (board == null) {
			Collection<PokyBoard> boards = pokyManager.getInstalledBoards();
			if (!boards.isEmpty()) {
				board = boards.iterator().next();
			}
		}
		if (board != null) {
			String launchMode = "run"; //$NON-NLS-1$
			for (IBuildConfiguration config : project.getBuildConfigs()) {
				ICBuildConfiguration cconfig = config.getAdapter(ICBuildConfiguration.class);
				if (cconfig != null) {
					PokyBuildConfiguration pokyConfig = cconfig.getAdapter(PokyBuildConfiguration.class);
					if (pokyConfig != null && pokyConfig.getLaunchMode().equals(launchMode)
							&& pokyConfig.getBoard().equals(board)) {
						return pokyConfig;
					}
				}
			}

			// not found, create one
			String configName = PokyBuildConfiguration.generateName(board, launchMode);
			IBuildConfiguration config = configManager.createBuildConfiguration(this, project, configName,
					null);

			// Create the toolChain
			IToolChainManager toolChainManager = Activator.getService(IToolChainManager.class);
			IToolChainProvider provider = toolChainManager.getProvider(PokyToolChainProvider.ID);
			IToolChain toolChain = new PokyToolChain(provider, config);
			toolChainManager.addToolChain(toolChain);

			PokyBuildConfiguration pokyConfig = new PokyBuildConfiguration(config, configName, board,
					launchMode, toolChain);
			pokyConfig.setActive(null);
			configManager.addBuildConfiguration(config, pokyConfig);
			return pokyConfig;
		}
		return null;
	}

	public PokyBuildConfiguration getConfiguration(IProject project, PokyRemoteConnection target,
			String launchMode,
			IProgressMonitor monitor) throws CoreException {
		PokyBoard board = target.getBoard();
		for (IBuildConfiguration config : project.getBuildConfigs()) {
			ICBuildConfiguration cconfig = config.getAdapter(ICBuildConfiguration.class);
			if (cconfig != null) {
				PokyBuildConfiguration pokyConfig = cconfig.getAdapter(PokyBuildConfiguration.class);
				if (pokyConfig != null && pokyConfig.getLaunchMode().equals(launchMode)
						&& pokyConfig.getBoard().equals(board) && pokyConfig.matches(target)) {
					return pokyConfig;
				}
			}
		}
		return null;
	}

	public PokyBuildConfiguration createConfiguration(IProject project, PokyRemoteConnection target,
			String launchMode,
			IProgressMonitor monitor) throws CoreException {
		PokyBoard board = target.getBoard();
		String configName = PokyBuildConfiguration.generateName(board, launchMode);
		IBuildConfiguration config = configManager.createBuildConfiguration(this, project, configName,
				monitor);
		IToolChainManager toolChainManager = Activator.getService(IToolChainManager.class);
		IToolChainProvider provider = toolChainManager.getProvider(PokyToolChainProvider.ID);
		IToolChain toolChain = new PokyToolChain(provider, config);
		toolChainManager.addToolChain(toolChain);
		PokyBuildConfiguration pokyConfig = new PokyBuildConfiguration(config, configName, target, launchMode,
				toolChain);
		configManager.addBuildConfiguration(config, pokyConfig);
		return pokyConfig;
	}

}