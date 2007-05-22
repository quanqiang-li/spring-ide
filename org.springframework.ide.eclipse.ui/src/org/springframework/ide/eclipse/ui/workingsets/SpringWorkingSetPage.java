/*******************************************************************************
 * Copyright (c) 2005, 2007 Spring IDE Developers
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Spring IDE Developers - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.ui.workingsets;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.internal.resources.WorkspaceRoot;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.fieldassist.FieldAssistColors;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.IWorkingSetPage;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.springframework.ide.eclipse.core.model.ISpringProject;
import org.springframework.ide.eclipse.ui.internal.navigator.SpringNavigatorContentProvider;
import org.springframework.ide.eclipse.ui.internal.navigator.SpringNavigatorLabelProvider;

/**
 * @author Christian Dupuis
 * @since 2.0
 */
@SuppressWarnings("restriction")
public class SpringWorkingSetPage extends WizardPage implements IWorkingSetPage {

	public class SpringExplorerAdaptingContentProvider implements
			ITreeContentProvider {

		private Set<ITreeContentProvider> contentProviders = WorkingSetsUtils.getContentProvider();

		private ITreeContentProvider rootContentProvider = new SpringNavigatorContentProvider();

		public void dispose() {
		}

		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}

		public Object[] getChildren(Object parentElement) {
			Set<Object> children = new HashSet<Object>();
			for (ITreeContentProvider contentProvider : contentProviders) {
				children.addAll(Arrays.asList(contentProvider
						.getChildren(parentElement)));
			}
			return children.toArray();
		}

		public Object getParent(Object element) {
			for (ITreeContentProvider contentProvider : contentProviders) {
				Object parent = contentProvider.getParent(element);
				if (parent != null) {
					return parent;
				}
			}

			return null;
		}

		public boolean hasChildren(Object element) {
			Set<Object> children = new HashSet<Object>();
			for (ITreeContentProvider contentProvider : contentProviders) {
				children.addAll(Arrays.asList(contentProvider
						.getChildren(element)));
			}
			return children.size() > 0;
		}

		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof WorkspaceRoot) {
				return rootContentProvider.getElements(inputElement);
			}
			else {
				Set<Object> children = new HashSet<Object>();
				for (ITreeContentProvider contentProvider : contentProviders) {
					children.addAll(Arrays.asList(contentProvider
							.getElements(inputElement)));
				}
				return children.toArray();
			}
		}
	}

	private class SpringExplorerAdaptingLabelProvider implements ILabelProvider {

		private Set<IElementSpecificLabelProvider> labelProviders = WorkingSetsUtils.getLabelProvider();

		private ILabelProvider rootLabelProviders = new SpringNavigatorLabelProvider();

		public Image getImage(Object element) {
			if (element instanceof ISpringProject) {
				return rootLabelProviders.getImage(element);
			}
			else {
				for (IElementSpecificLabelProvider labelProvider : labelProviders) {
					if (labelProvider.supportsElement(element)) {
						return labelProvider.getImage(element);
					}
				}
			}
			return rootLabelProviders.getImage(element);
		}

		public String getText(Object element) {
			if (element instanceof ISpringProject) {
				return rootLabelProviders.getText(element);
			}
			else {
				for (IElementSpecificLabelProvider labelProvider : labelProviders) {
					if (labelProvider.supportsElement(element)) {
						return labelProvider.getText(element);
					}
				}
			}
			return rootLabelProviders.getText(element);
		}

		public void addListener(ILabelProviderListener listener) {
		}

		public void dispose() {
		}

		public boolean isLabelProperty(Object element, String property) {
			return false;
		}

		public void removeListener(ILabelProviderListener listener) {
		}
	}

	private final static int SIZING_SELECTION_WIDGET_WIDTH = 50;

	private final static int SIZING_SELECTION_WIDGET_HEIGHT = 200;

	private Text text;

	private CheckboxTreeViewer tree;

	private IWorkingSet workingSet;

	private boolean firstCheck = false; // set to true if selection is set in

	private ITreeContentProvider contentProvider = new SpringExplorerAdaptingContentProvider();

	// setSelection

	/**
	 * Creates a new instance of the receiver.
	 */
	public SpringWorkingSetPage() {
		super("springWorkingSet", "Spring Working Set", null);
		setDescription("Enter a working set name and select the working set elements.");
	}

	/**
	 * Overrides method in WizardPage.
	 * @see org.eclipse.jface.wizard.WizardPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		Font font = parent.getFont();
		Composite composite = new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		setControl(composite);

		Label label = new Label(composite, SWT.WRAP);
		label.setText("Working set name:");
		GridData data = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_CENTER);
		label.setLayoutData(data);
		label.setFont(font);

		text = new Text(composite, SWT.SINGLE | SWT.BORDER);
		text.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL));
		text.setFont(font);
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateInput();
			}
		});
		text.setFocus();
		text.setBackground(FieldAssistColors
				.getRequiredFieldBackgroundColor(text));

		label = new Label(composite, SWT.WRAP);
		label.setText("Working set &contents:");
		data = new GridData(GridData.GRAB_HORIZONTAL
				| GridData.HORIZONTAL_ALIGN_FILL
				| GridData.VERTICAL_ALIGN_CENTER);
		label.setLayoutData(data);
		label.setFont(font);

		tree = new CheckboxTreeViewer(composite);
		tree.setUseHashlookup(true);
		final ITreeContentProvider treeContentProvider = new SpringExplorerAdaptingContentProvider();
		tree.setContentProvider(treeContentProvider);
		tree.setLabelProvider(new SpringExplorerAdaptingLabelProvider());
		tree.setSorter(new ViewerSorter());
		tree.setInput(IDEWorkbenchPlugin.getPluginWorkspace().getRoot());

		data = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
		data.heightHint = SIZING_SELECTION_WIDGET_HEIGHT;
		data.widthHint = SIZING_SELECTION_WIDGET_WIDTH;
		tree.getControl().setLayoutData(data);
		tree.getControl().setFont(font);

		tree.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				handleCheckStateChange(event);
			}
		});

		tree.addTreeListener(new ITreeViewerListener() {
			public void treeCollapsed(TreeExpansionEvent event) {
			}

			public void treeExpanded(TreeExpansionEvent event) {
				final Object element = event.getElement();
				if (tree.getGrayed(element) == false) {
					BusyIndicator.showWhile(getShell().getDisplay(),
							new Runnable() {
								public void run() {
									setSubtreeChecked(element, tree
											.getChecked(element), false);
								}
							});
				}
			}
		});

		Composite buttonComposite = new Composite(composite, SWT.NONE);
		buttonComposite.setLayout(new GridLayout(2, false));
		buttonComposite.setLayoutData(new GridData(
				GridData.HORIZONTAL_ALIGN_FILL));

		Button selectAllButton = new Button(buttonComposite, SWT.PUSH);
		selectAllButton.setText("Select &All");
		selectAllButton
				.setToolTipText("Select all of theses resource for this working set.");
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent selectionEvent) {
				tree.setCheckedElements(treeContentProvider.getElements(tree
						.getInput()));
				validateInput();
			}
		});
		selectAllButton.setFont(font);
		setButtonLayoutData(selectAllButton);

		Button deselectAllButton = new Button(buttonComposite, SWT.PUSH);
		deselectAllButton.setText("Dese&lect All");
		deselectAllButton
				.setToolTipText("Deselect all of these resources for this working set.");
		deselectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent selectionEvent) {
				tree.setCheckedElements(new Object[0]);
				validateInput();
			}
		});
		deselectAllButton.setFont(font);
		setButtonLayoutData(deselectAllButton);

		initializeCheckedState();
		if (workingSet != null) {
			text.setText(workingSet.getName());
		}
		setPageComplete(false);
	}

	/**
	 * Collects all checked resources in the specified container.
	 * @param checkedResources the output, list of checked resources
	 * @param container the container to collect checked resources in
	 */
	private void findCheckedResources(List<IAdaptable> checkedResources,
			Object[] checkedElements) {
		for (Object checkedElement : checkedElements) {
			if (checkedElement instanceof IFile) {
				Object[] children = contentProvider.getChildren(checkedElement);
				if (children != null && children.length == 1) {
					checkedElement = children[0];
				}
			}
			if (isValidPersistableElement(checkedElement)) {
				checkedResources.add((IAdaptable) checkedElement);
			}
		}
	}

	private boolean isValidPersistableElement(Object checkedElement) {
		return checkedElement instanceof IAdaptable
				&& ((IAdaptable) checkedElement)
						.getAdapter(IPersistableElement.class) != null;
	}

	/**
	 * Implements IWorkingSetPage.
	 * @see org.eclipse.ui.dialogs.IWorkingSetPage#finish()
	 */
	public void finish() {
		List<IAdaptable> resources = new ArrayList<IAdaptable>(10);
		findCheckedResources(resources, tree.getCheckedElements());
		if (workingSet == null) {
			IWorkingSetManager workingSetManager = PlatformUI.getWorkbench()
					.getWorkingSetManager();
			workingSet = workingSetManager.createWorkingSet(
					getWorkingSetName(), (IAdaptable[]) resources
							.toArray(new IAdaptable[resources.size()]));
		}
		else {
			workingSet.setName(getWorkingSetName());
			workingSet.setElements((IAdaptable[]) resources
					.toArray(new IAdaptable[resources.size()]));
		}
	}

	/**
	 * Implements IWorkingSetPage.
	 * @see org.eclipse.ui.dialogs.IWorkingSetPage#getSelection()
	 */
	public IWorkingSet getSelection() {
		return workingSet;
	}

	/**
	 * Returns the name entered in the working set name field.
	 * @return the name entered in the working set name field.
	 */
	private String getWorkingSetName() {
		return text.getText();
	}

	/**
	 * Called when the checked state of a tree item changes.
	 * @param event the checked state change event.
	 */
	private void handleCheckStateChange(final CheckStateChangedEvent event) {
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				boolean state = event.getChecked();
				setSubtreeChecked(event.getElement(), state, true);
				updateParentState(event.getElement());
				validateInput();
			}
		});
	}

	/**
	 * Sets the checked state of tree items based on the initial working set, if
	 * any.
	 */
	private void initializeCheckedState() {
		if (workingSet == null) {
			return;
		}

		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {
				IAdaptable[] items = workingSet.getElements();
				tree.setCheckedElements(items);
				for (int i = 0; i < items.length; i++) {
					IAdaptable item = items[i];
					IContainer container = null;
					IResource resource = null;

					if (item instanceof IContainer) {
						container = (IContainer) item;
					}
					else {
						container = (IContainer) item
								.getAdapter(IContainer.class);
					}
					if (container != null) {
						setSubtreeChecked(container, true, true);
					}
					if (item instanceof IResource) {
						resource = (IResource) item;
					}
					else {
						resource = (IResource) item.getAdapter(IResource.class);
					}
					if (resource != null && resource.isAccessible() == false) {
						IProject project = resource.getProject();
						if (tree.getChecked(project) == false) {
							tree.setGrayChecked(project, true);
						}
					}
					else {
						updateParentState(resource);
					}
				}
			}
		});
	}

	/**
	 * Implements IWorkingSetPage.
	 * @see org.eclipse.ui.dialogs.IWorkingSetPage#setSelection(IWorkingSet)
	 */
	public void setSelection(IWorkingSet workingSet) {
		if (workingSet == null) {
			throw new IllegalArgumentException("Working set must not be null"); //$NON-NLS-1$
		}
		this.workingSet = workingSet;
		if (getShell() != null && text != null) {
			firstCheck = true;
			initializeCheckedState();
			text.setText(workingSet.getName());
		}
	}

	/**
	 * Sets the checked state of the container's members.
	 * @param container the container whose children should be checked/unchecked
	 * @param state true=check all members in the container. false=uncheck all
	 * members in the container.
	 * @param checkExpandedState true=recurse into sub-containers and set the
	 * checked state. false=only set checked state of members of this container
	 */
	private void setSubtreeChecked(Object container, boolean state,
			boolean checkExpandedState) {
		// checked state is set lazily on expand, don't set it if container is
		// collapsed

		if (tree.getExpandedState(container) == false && state
				&& checkExpandedState) {
			return;
		}
		Object[] members = contentProvider.getChildren(container);
		for (int i = members.length - 1; i >= 0; i--) {
			Object element = members[i];
			boolean elementGrayChecked = tree.getGrayed(element)
					|| tree.getChecked(element);
			if (state) {
				tree.setChecked(element, true);
				tree.setGrayed(element, false);
			}
			else {
				tree.setGrayChecked(element, false);
			} // unchecked state only
			// needs
			if ((state || elementGrayChecked)) {
				setSubtreeChecked(element, state, true);
			}
		}
	}

	/**
	 * Check and gray the resource parent if all resources of the parent are
	 * checked.
	 * @param child the resource whose parent checked state should be set.
	 */
	private void updateParentState(Object child) {
		if (child == null || contentProvider.getParent(child) == null) {
			return;
		}
		Object parent = contentProvider.getParent(child);
		boolean childChecked = false;
		Object[] members = contentProvider.getChildren(parent);
		for (int i = members.length - 1; i >= 0; i--) {
			if (tree.getChecked(members[i]) || tree.getGrayed(members[i])) {
				childChecked = true;
				break;
			}
		}
		tree.setGrayChecked(parent, childChecked);
		updateParentState(parent);
	}

	/**
	 * Validates the working set name and the checked state of the resource
	 * tree.
	 */
	private void validateInput() {
		String errorMessage = null;
		String infoMessage = null;
		String newText = text.getText();

		if (newText.equals(newText.trim()) == false) {
			errorMessage = " The name must not have a leading or trailing whitespace.";
		}
		else if (firstCheck) {
			firstCheck = false;
			return;
		}
		if (newText.equals("")) { //$NON-NLS-1$
			errorMessage = " The name must not be empty.";
		}
		if (errorMessage == null
				&& (workingSet == null || newText.equals(workingSet.getName()) == false)) {
			IWorkingSet[] workingSets = PlatformUI.getWorkbench()
					.getWorkingSetManager().getWorkingSets();
			for (int i = 0; i < workingSets.length; i++) {
				if (newText.equals(workingSets[i].getName())) {
					errorMessage = " A working set with the same name already exists.";
				}
			}
		}
		if (infoMessage == null && tree.getCheckedElements().length == 0) {
			infoMessage = " No resources selected.";
		}
		setMessage(infoMessage, INFORMATION);
		setErrorMessage(errorMessage);
		setPageComplete(errorMessage == null);
	}
}
