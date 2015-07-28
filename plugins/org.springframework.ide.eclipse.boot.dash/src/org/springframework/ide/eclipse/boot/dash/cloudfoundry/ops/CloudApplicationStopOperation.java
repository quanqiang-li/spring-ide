/*******************************************************************************
 * Copyright (c) 2015 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.boot.dash.cloudfoundry.ops;

import org.cloudfoundry.client.lib.CloudFoundryOperations;
import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.core.runtime.IProgressMonitor;
import org.springframework.ide.eclipse.boot.dash.cloudfoundry.CloudDashElement;
import org.springframework.ide.eclipse.boot.dash.cloudfoundry.CloudFoundryBootDashModel;
import org.springframework.ide.eclipse.boot.dash.model.UserInteractions;

public class CloudApplicationStopOperation extends CloudApplicationOperation {

	private CloudDashElement element;

	public CloudApplicationStopOperation(CloudFoundryOperations client, CloudDashElement element,
			CloudFoundryBootDashModel model, UserInteractions ui) {
		super("Stopping application", client, element.getName(), model, ui);
		this.element = element;
	}

	@Override
	protected CloudApplication doCloudOp(CloudFoundryOperations client, IProgressMonitor monitor) throws Exception {
		client.stopApplication(element.getName());
		// fetch an updated Cloud Application that reflects changes that
		// were
		// performed on it
		CloudApplication app = client.getApplication(appName);

		model.notifyApplicationChanged(app);

		return app;
	}

}
