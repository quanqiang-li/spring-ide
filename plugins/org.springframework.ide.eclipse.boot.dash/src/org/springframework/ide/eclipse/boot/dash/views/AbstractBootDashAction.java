/*******************************************************************************
 * Copyright (c) 2015 Pivotal Software, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal Software, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.boot.dash.views;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.springframework.ide.eclipse.boot.dash.model.UserInteractions;
import org.springsource.ide.eclipse.commons.livexp.ui.Disposable;

/**
 * @author Kris De Volder
 */
public class AbstractBootDashAction extends Action implements Disposable {

	protected final UserInteractions ui;
	private boolean isVisible = true;

	protected AbstractBootDashAction(UserInteractions ui, int style) {
		super("", style);
		this.ui = ui;
	}

	protected AbstractBootDashAction(UserInteractions ui) {
		this(ui, IAction.AS_UNSPECIFIED);
	}

	public void dispose() {
	}

	public boolean isVisible() {
		return isVisible;
	}

	public void setVisible(boolean show) {
		this.isVisible = show;
	}

}
