/**
 * 
 */
package org.yocto.crops.poky.core.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;

/**
 *
 *
 */
public class PokyProjectNature implements IProjectNature {

	private IProject project;
	public static final String ID = Activator.getId() + ".pokyNature"; //$NON-NLS-1$

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IProjectNature#configure()
	 */
	@Override
	public void configure() throws CoreException {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IProjectNature#deconfigure()
	 */
	@Override
	public void deconfigure() throws CoreException {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IProjectNature#getProject()
	 */
	@Override
	public IProject getProject() {
		return project;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
	 */
	@Override
	public void setProject(IProject project) {
		this.project = project;
	}

	public static boolean hasNature(IProject project) throws CoreException {
		IProjectDescription projDesc = project.getDescription();
		for (String id : projDesc.getNatureIds()) {
			if (id.equals(ID))
				return true;
		}
		return false;
	}
}
