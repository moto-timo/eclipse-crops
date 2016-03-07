package org.yocto.crops.core.preferences;

import java.lang.reflect.Array;
import java.util.Set;

import org.eclipse.jface.preference.*;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;
import org.yocto.crops.core.CropsCorePlugin;
import org.yocto.crops.core.CropsUtils;
import org.yocto.crops.core.Messages;
//import org.yocto.crops.zephyr.model.Toolchain;
//import org.yocto.crops.zephyr.model.Toolchains;

/**
 * This class represents a preference page that
 * is contributed to the Preferences dialog. By 
 * subclassing <samp>FieldEditorPreferencePage</samp>, we
 * can use the field support built into JFace that allows
 * us to create a page that is small and knows how to 
 * save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They
 * are stored in the preference store that belongs to
 * the main plug-in class. That way, preferences can
 * be accessed directly via the preference store.
 */

public class CropsPreferencePage
	extends FieldEditorPreferencePage
	implements IWorkbenchPreferencePage {

	public CropsPreferencePage() {
		super(GRID);
		setPreferenceStore(CropsCorePlugin.getDefault().getPreferenceStore());
		setDescription(Messages.CropsPreferencePage_description);
	}
	
	/**
	 * Creates the field editors. Field editors are abstractions of
	 * the common GUI blocks needed to manipulate various types
	 * of preferences. Each field editor knows how to save and
	 * restore itself.
	 */
	public void createFieldEditors() {
		addField(new FileFieldEditor(PreferenceConstants.P_CEED_PATH, 
				Messages.CropsPreferencePage_ceed_label, getFieldEditorParent()));
		addField(new StringFieldEditor(PreferenceConstants.P_CROPS_ROOT,
				Messages.CropsPreferencePage_crops_root_label, getFieldEditorParent()));
//		addField(
//			new BooleanFieldEditor(PreferenceConstants.P_EXPERT_MODE,
//				                   "&Expert mode by default",
//				                   getFieldEditorParent()));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {
	}
	
}