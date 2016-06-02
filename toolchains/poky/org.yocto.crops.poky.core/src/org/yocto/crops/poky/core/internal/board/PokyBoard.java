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

import java.util.Properties;

import org.yocto.crops.poky.core.internal.HierarchicalProperties;

public class PokyBoard {

	public static final String MENU_QUALIFIER = "menu_"; //$NON-NLS-1$

	private String name;

	private String id;

	private PokyPlatform platform;
	private HierarchicalProperties properties;

	public PokyBoard() {
	}

	public PokyBoard(String id, HierarchicalProperties properties) {
		this.properties = properties;
		this.id = id;
		this.name = this.properties.getChild("name").getValue(); //$NON-NLS-1$
	}

	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	public PokyPlatform getPlatform() {
		return platform;
	}

	PokyBoard setOwners(PokyPlatform platform) {
		this.platform = platform;
		return this;
	}

	public String getProperty(String key) {
		return properties.getProperty(key);
	}

	public HierarchicalProperties getMenus() {
		return properties.getChild("menu"); //$NON-NLS-1$
	}

	public Properties getBoardProperties() {
		return properties.flatten();
	}

	public Properties getMenuProperties(String id, String value) {
		return getMenus().getChild(id).getChild(value).flatten();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((platform == null) ? 0 : platform.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PokyBoard other = (PokyBoard) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (platform == null) {
			if (other.platform != null)
				return false;
		} else if (!platform.equals(other.platform))
			return false;
		return true;
	}

}