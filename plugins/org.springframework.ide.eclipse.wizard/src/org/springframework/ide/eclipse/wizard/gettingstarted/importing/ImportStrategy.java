/*******************************************************************************
 *  Copyright (c) 2013 GoPivotal, Inc.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  Contributors:
 *      GoPivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.wizard.gettingstarted.importing;

import org.eclipse.jface.operation.IRunnableWithProgress;

/**
 * Strategy for importing a certain type of getting started content
 *
 * @author Kris De Volder
 */
public abstract class ImportStrategy {

	public abstract IRunnableWithProgress createOperation(ImportConfiguration conf);

	public boolean isSupported() {
		return true;
	}

	/**
	 * Subclasses should override to provide more precise message
	 */
	public String getNotInstalledMessage() {
		return "Can't use this, something it requires is not installed";
	}

}
