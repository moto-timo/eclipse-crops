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

import java.io.IOException;

import org.yocto.crops.poky.core.internal.Activator;
import org.yocto.crops.poky.core.internal.Messages;
import org.yocto.crops.poky.core.internal.build.PokyBuildConfiguration;
import org.yocto.crops.poky.core.internal.build.PokyBuildConfigurationProvider;
import org.yocto.crops.poky.core.internal.remote.PokyRemoteConnection;
import org.eclipse.cdt.core.build.ICBuildConfigurationManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.launchbar.core.target.ILaunchTarget;
import org.eclipse.launchbar.core.target.launch.ITargetedLaunch;
import org.eclipse.launchbar.core.target.launch.LaunchConfigurationTargetedDelegate;
import org.eclipse.remote.core.IRemoteConnection;

public class PokyLaunchConfigurationDelegate extends LaunchConfigurationTargetedDelegate {

	public static final String TYPE_ID = "org.yocto.crops.poky.core.launchConfigurationType"; //$NON-NLS-1$
	public static final String CONNECTION_NAME = Activator.getId() + ".connectionName"; //$NON-NLS-1$

	private static final ICBuildConfigurationManager buildConfigManager = Activator
			.getService(ICBuildConfigurationManager.class);

	@Override
	public ITargetedLaunch getLaunch(ILaunchConfiguration configuration, String mode, ILaunchTarget target)
			throws CoreException {
		return new PokyLaunch(configuration, mode, null, target);
	}

	@Override
	public boolean buildForLaunch(ILaunchConfiguration configuration, String mode, ILaunchTarget target,
			IProgressMonitor monitor) throws CoreException {
		IRemoteConnection connection = target.getAdapter(IRemoteConnection.class);
		if (target != null) {
			PokyRemoteConnection pokyTarget = connection.getService(PokyRemoteConnection.class);

			// 1. make sure proper build config is set active
			IProject project = configuration.getMappedResources()[0].getProject();
			PokyBuildConfiguration pokyConfig = getPokyConfiguration(project, mode, pokyTarget,
					monitor);
			pokyConfig.setActive(monitor);
		}

		// 2. Run the build
		return super.buildForLaunch(configuration, mode, target, monitor);
	}

	@Override
	protected IProject[] getBuildOrder(ILaunchConfiguration configuration, String mode) throws CoreException {
		// 1. Extract project from configuration
		IProject project = configuration.getMappedResources()[0].getProject();
		return new IProject[] { project };
	}

	@Override
	public void launch(final ILaunchConfiguration configuration, String mode, final ILaunch launch,
			IProgressMonitor monitor) throws CoreException {
		try {
			ILaunchTarget target = ((ITargetedLaunch) launch).getLaunchTarget();
			IRemoteConnection connection = target.getAdapter(IRemoteConnection.class);
			if (connection == null) {
				throw new CoreException(
						new Status(IStatus.ERROR, Activator.getId(), Messages.PokyLaunchConfigurationDelegate_2));
			}
			PokyRemoteConnection pokyTarget = connection.getService(PokyRemoteConnection.class);

			// The project
			IProject project = (IProject) configuration.getMappedResources()[0];

			// The build config
			PokyBuildConfiguration pokyConfig = getPokyConfiguration(project, mode, pokyTarget,
					monitor);
			String[] uploadCmd = pokyConfig.getUploadCommand(pokyTarget.getPortName());

			StringBuilder cmdStr = new StringBuilder(uploadCmd[0]);
			for (int i = 1; i < uploadCmd.length; ++i) {
				cmdStr.append(' ');
				cmdStr.append(uploadCmd[i]);
			}
			// Start the launch
			((PokyLaunch) launch).start();

			// Run the process and capture the results in the console
			ProcessBuilder processBuilder = new ProcessBuilder(uploadCmd)
					.directory(pokyConfig.getBuildDirectory().toFile());
			pokyConfig.setBuildEnvironment(processBuilder.environment());
			Process process = processBuilder.start();
			DebugPlugin.newProcess(launch, process, cmdStr.toString());
		} catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, Activator.getId(), e.getLocalizedMessage(), e));
		}

	}

	public PokyBuildConfiguration getPokyConfiguration(IProject project, String launchMode,
			PokyRemoteConnection pokyTarget,
			IProgressMonitor monitor) throws CoreException {
		PokyBuildConfigurationProvider provider = (PokyBuildConfigurationProvider) buildConfigManager
				.getProvider(PokyBuildConfigurationProvider.ID);
		PokyBuildConfiguration config = provider.getConfiguration(project, pokyTarget, launchMode, monitor);
		if (config == null) {
			config = provider.createConfiguration(project, pokyTarget, launchMode, monitor);
		}
		return config;
	}
}