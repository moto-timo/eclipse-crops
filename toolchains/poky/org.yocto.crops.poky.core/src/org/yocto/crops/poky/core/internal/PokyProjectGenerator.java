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

import java.util.Map;

import org.eclipse.cdt.core.CCProjectNature;
import org.eclipse.cdt.core.CProjectNature;
import org.eclipse.cdt.core.build.CBuilder;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.tools.templates.freemarker.FMProjectGenerator;
import org.osgi.framework.Bundle;

/**
 *
 *
 */
public class PokyProjectGenerator extends FMProjectGenerator {

	public PokyProjectGenerator(String manifestFile) {
		super(manifestFile);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.tools.templates.freemarker.FMProjectGenerator#initProjectDescription(org.eclipse.core.resources.IProjectDescription)
	 */
	@Override
	protected void initProjectDescription(IProjectDescription description) {
		description
			.setNatureIds(new String[] { CProjectNature.C_NATURE_ID, CCProjectNature.CC_NATURE_ID, PokyProjectNature.ID});
		ICommand command = description.newCommand();
		CBuilder.setupBuilder(command);
		description.setBuildSpec(new ICommand[] { command });

	}

	/* (non-Javadoc)
	 * @see org.eclipse.tools.templates.freemarker.FMGenerator#getSourceBundle()
	 */
	@Override
	protected Bundle getSourceBundle() {
		return Activator.getPlugin().getBundle();
	}

	@Override
	public void generate(Map<String, Object> model, IProgressMonitor monitor) throws CoreException {
		super.generate(model, monitor);
		IProject project = getProject();
		CoreModel.getDefault().create(project).setRawPathEntries(new IPathEntry[] {
				CoreModel.newSourceEntry(project.getFullPath())
		}, monitor);
	}

}
