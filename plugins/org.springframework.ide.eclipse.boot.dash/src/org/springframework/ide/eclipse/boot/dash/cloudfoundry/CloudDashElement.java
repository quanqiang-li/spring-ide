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
package org.springframework.ide.eclipse.boot.dash.cloudfoundry;

import java.util.ArrayList;
import java.util.List;

import org.cloudfoundry.client.lib.domain.CloudApplication;
import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.springframework.ide.eclipse.boot.dash.cloudfoundry.ops.ApplicationDeleteOperation;
import org.springframework.ide.eclipse.boot.dash.cloudfoundry.ops.ApplicationOperationWithModelUpdate;
import org.springframework.ide.eclipse.boot.dash.cloudfoundry.ops.ApplicationStartOperation;
import org.springframework.ide.eclipse.boot.dash.cloudfoundry.ops.ApplicationStopOperation;
import org.springframework.ide.eclipse.boot.dash.cloudfoundry.ops.CloudApplicationOperation;
import org.springframework.ide.eclipse.boot.dash.cloudfoundry.ops.ProjectsDeployer;
import org.springframework.ide.eclipse.boot.dash.cloudfoundry.ops.StartOnlyUpdateListener;
import org.springframework.ide.eclipse.boot.dash.metadata.IPropertyStore;
import org.springframework.ide.eclipse.boot.dash.metadata.PropertyStoreApi;
import org.springframework.ide.eclipse.boot.dash.metadata.PropertyStoreFactory;
import org.springframework.ide.eclipse.boot.dash.model.BootDashElement;
import org.springframework.ide.eclipse.boot.dash.model.Operation;
import org.springframework.ide.eclipse.boot.dash.model.RunState;
import org.springframework.ide.eclipse.boot.dash.model.RunTarget;
import org.springframework.ide.eclipse.boot.dash.model.UserInteractions;
import org.springframework.ide.eclipse.boot.dash.model.WrappingBootDashElement;
import org.springframework.ide.eclipse.boot.dash.model.requestmappings.RequestMapping;

/**
 * A handle to a Cloud application. NOTE: This element should NOT hold Cloud
 * application state as it may be discarded and created multiple times for the
 * same app for any reason.
 * <p/>
 * Cloud application state should always be resolved from external sources
 *
 */
public class CloudDashElement extends WrappingBootDashElement<String> {

	private final CloudFoundryRunTarget cloudTarget;

	private final CloudFoundryBootDashModel cloudModel;

	private PropertyStoreApi persistentProperties;

	public CloudDashElement(CloudFoundryBootDashModel model, String appName, IPropertyStore modelStore) {
		super(model, appName);
		this.cloudTarget = model.getCloudTarget();
		this.cloudModel = model;
		IPropertyStore backingStore = PropertyStoreFactory.createSubStore(delegate, modelStore);
		this.persistentProperties = PropertyStoreFactory.createApi(backingStore);
	}

	protected CloudFoundryBootDashModel getCloudModel() {
		return (CloudFoundryBootDashModel) getParent();
	}

	@Override
	public void stopAsync(UserInteractions ui) throws Exception {

		CloudApplicationOperation op = new ApplicationOperationWithModelUpdate(
				new ApplicationStopOperation(this, (CloudFoundryBootDashModel) getParent()));
		cloudModel.getOperationsExecution(ui).runOpAsynch(op);
	}

	@Override
	public void restart(RunState runingOrDebugging, UserInteractions ui) throws Exception {

		Operation<?> op = null;

		if (getProject() != null) {
			boolean shouldAutoReplaceApp = true;
			List<BootDashElement> elements = new ArrayList<BootDashElement>();
			elements.add(this);
			op = new ProjectsDeployer((CloudFoundryBootDashModel) getParent(), ui, elements, shouldAutoReplaceApp);
		} else {
			CloudApplicationOperation restartOp = new ApplicationStartOperation(getName(),
					(CloudFoundryBootDashModel) getParent());

			restartOp.addApplicationUpdateListener(new StartOnlyUpdateListener(getName(), getCloudModel()));

			op = new ApplicationOperationWithModelUpdate(restartOp);
		}

		cloudModel.getOperationsExecution(ui).runOpAsynch(op);
	}

	public void delete(UserInteractions ui) throws Exception {
		CloudApplicationOperation op = new ApplicationDeleteOperation(getName(),
				(CloudFoundryBootDashModel) getParent());
		cloudModel.getOperationsExecution(ui).runOpAsynch(op);
	}

	@Override
	public void openConfig(UserInteractions ui) {

	}

	@Override
	public String getName() {
		return delegate;
	}

	@Override
	public IProject getProject() {
		return getCloudModel().getAppCache().getProject(getName());
	}

	@Override
	public RunState getRunState() {
		return getCloudModel().getAppCache().getRunState(getName());
	}

	public CloudApplicationDeploymentProperties getDeploymentProperties() {
		CloudApplication app = getCloudModel().getAppCache().getApp(getName());
		if (app != null) {
			return CloudApplicationDeploymentProperties.getFor(app);
		}

		return null;
	}

	@Override
	public RunTarget getTarget() {
		return cloudTarget;
	}

	@Override
	public int getLivePort() {
		return 80;
	}

	@Override
	public String getLiveHost() {
		CloudApplication app = getCloudModel().getAppCache().getApp(getName());
		if (app != null) {
			List<String> uris = app.getUris();
			if (uris != null) {
				for (String uri : uris) {
					return uri;
				}
			}
		}
		return null;
	}

	@Override
	public List<RequestMapping> getLiveRequestMappings() {
		return new ArrayList<RequestMapping>(0);
	}

	@Override
	public ILaunchConfiguration getActiveConfig() {
		return null;
	}

	@Override
	public ILaunchConfiguration getPreferredConfig() {
		return null;
	}

	@Override
	public void setPreferredConfig(ILaunchConfiguration config) {

	}

	@Override
	public int getActualInstances() {
		return getCloudModel().getAppCache().getApp(getName()) != null
				? getCloudModel().getAppCache().getApp(getName()).getRunningInstances() : 0;
	}

	@Override
	public int getDesiredInstances() {
		return getCloudModel().getAppCache().getApp(getName()) != null
				? getCloudModel().getAppCache().getApp(getName()).getInstances() : 0;
	}

	@Override
	public PropertyStoreApi getPersistentProperties() {
		return persistentProperties;
	}
}