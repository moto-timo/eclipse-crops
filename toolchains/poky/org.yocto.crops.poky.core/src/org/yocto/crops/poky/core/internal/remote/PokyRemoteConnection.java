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
package org.yocto.crops.poky.core.internal.remote;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.yocto.crops.poky.core.internal.Activator;
import org.yocto.crops.poky.core.internal.board.PokyBoard;
import org.yocto.crops.poky.core.internal.board.PokyManager;
import org.eclipse.cdt.serial.SerialPort;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.remote.core.IRemoteCommandShellService;
import org.eclipse.remote.core.IRemoteConnection;
import org.eclipse.remote.core.IRemoteConnectionChangeListener;
import org.eclipse.remote.core.IRemoteConnectionPropertyService;
import org.eclipse.remote.core.IRemoteProcess;
import org.eclipse.remote.core.RemoteConnectionChangeEvent;
import org.eclipse.remote.serial.core.SerialPortCommandShell;

public class PokyRemoteConnection
		implements IRemoteConnectionChangeListener, IRemoteConnectionPropertyService, IRemoteCommandShellService {


	public static final String TYPE_ID = "org.yocto.crops.poky.core.connectionType"; //$NON-NLS-1$
	public static final String PORT_NAME = "arduinoPortName"; //$NON-NLS-1$
	public static final String PACKAGE_NAME = "arduinoPackageName"; //$NON-NLS-1$
	public static final String PLATFORM_NAME = "arduinoPlatformName"; //$NON-NLS-1$
	public static final String BOARD_NAME = "arduinoBoardName"; //$NON-NLS-1$

	private final IRemoteConnection remoteConnection;
	private SerialPort serialPort;
	private SerialPortCommandShell commandShell;

	private static final Map<IRemoteConnection, PokyRemoteConnection> connectionMap = new HashMap<>();

	public PokyRemoteConnection(IRemoteConnection remoteConnection) {
		this.remoteConnection = remoteConnection;
		remoteConnection.addConnectionChangeListener(this);
	}

	@Override
	public void connectionChanged(RemoteConnectionChangeEvent event) {
		if (event.getType() == RemoteConnectionChangeEvent.CONNECTION_REMOVED) {
			synchronized (connectionMap) {
				connectionMap.remove(event.getConnection());
			}
		}
	}

	public static class Factory implements IRemoteConnection.Service.Factory {
		@SuppressWarnings("unchecked")
		@Override
		public <T extends IRemoteConnection.Service> T getService(IRemoteConnection remoteConnection,
				Class<T> service) {
			if (PokyRemoteConnection.class.equals(service)) {
				synchronized (connectionMap) {
					PokyRemoteConnection connection = connectionMap.get(remoteConnection);
					if (connection == null) {
						connection = new PokyRemoteConnection(remoteConnection);
						connectionMap.put(remoteConnection, connection);
					}
					return (T) connection;
				}
			} else if (IRemoteConnectionPropertyService.class.equals(service)
					|| IRemoteCommandShellService.class.equals(service)) {
				return (T) remoteConnection.getService(PokyRemoteConnection.class);
			}
			return null;
		}
	}

	@Override
	public IRemoteConnection getRemoteConnection() {
		return remoteConnection;
	}

	@Override
	public String getProperty(String key) {
		if (IRemoteConnection.OS_NAME_PROPERTY.equals(key)) {
			return "arduino"; //$NON-NLS-1$
		} else if (IRemoteConnection.OS_ARCH_PROPERTY.equals(key)) {
			return "avr"; // TODO handle arm //$NON-NLS-1$
		} else {
			return null;
		}
	}

	public PokyBoard getBoard() throws CoreException {
		return Activator.getService(PokyManager.class).getBoard(remoteConnection.getAttribute(PACKAGE_NAME),
				remoteConnection.getAttribute(PLATFORM_NAME), remoteConnection.getAttribute(BOARD_NAME));
	}

	public String getPortName() {
		return remoteConnection.getAttribute(PORT_NAME);
	}

	@Override
	public IRemoteProcess getCommandShell(int flags) throws IOException {
		if (serialPort != null && serialPort.isOpen()) {
			// can only have one open at a time
			return null;
		}

		serialPort = new SerialPort(getPortName());
		commandShell = new SerialPortCommandShell(remoteConnection, serialPort);
		return commandShell;
	}

	public void pause() {
		if (serialPort != null) {
			try {
				if (serialPort.isOpen())
					serialPort.pause();
			} catch (IOException e) {
				Activator.log(e);
			}
		}
	}

	public void resume() {
		if (serialPort != null) {
			try {
				if (serialPort.isOpen())
					serialPort.resume();
			} catch (IOException e) {
				Activator.log(e);
			}
		}
	}

}
