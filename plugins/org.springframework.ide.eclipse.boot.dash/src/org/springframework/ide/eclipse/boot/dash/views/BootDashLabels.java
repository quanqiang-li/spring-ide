/*******************************************************************************
 * Copyright (c) 2015, 2017 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.boot.dash.views;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.internal.ui.viewsupport.AppearanceAwareLabelProvider;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.StyledString.Styler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.springframework.ide.eclipse.boot.core.BootPropertyTester;
import org.springframework.ide.eclipse.boot.dash.BootDashActivator;
import org.springframework.ide.eclipse.boot.dash.cloudfoundry.CloudAppDashElement;
import org.springframework.ide.eclipse.boot.dash.cloudfoundry.CloudFoundryBootDashModel;
import org.springframework.ide.eclipse.boot.dash.cloudfoundry.CloudServiceInstanceDashElement;
import org.springframework.ide.eclipse.boot.dash.cloudfoundry.DevtoolsUtil;
import org.springframework.ide.eclipse.boot.dash.cloudfoundry.JmxSshTunnelStatus;
import org.springframework.ide.eclipse.boot.dash.model.AbstractLaunchConfigurationsDashElement;
import org.springframework.ide.eclipse.boot.dash.model.BootDashElement;
import org.springframework.ide.eclipse.boot.dash.model.BootDashModel;
import org.springframework.ide.eclipse.boot.dash.model.ButtonModel;
import org.springframework.ide.eclipse.boot.dash.model.LocalCloudServiceDashElement;
import org.springframework.ide.eclipse.boot.dash.model.RunState;
import org.springframework.ide.eclipse.boot.dash.model.TagUtils;
import org.springframework.ide.eclipse.boot.dash.ngrok.NGROKClient;
import org.springframework.ide.eclipse.boot.dash.ngrok.NGROKLaunchTracker;
import org.springframework.ide.eclipse.boot.dash.views.sections.BootDashColumn;
import org.springframework.ide.eclipse.boot.util.Log;
import org.springsource.ide.eclipse.commons.livexp.ui.Disposable;
import org.springsource.ide.eclipse.commons.livexp.ui.Stylers;

import com.google.common.collect.ImmutableSet;

/**
 * Provides various methods for implementing various Label providers for the Boot Dash
 * and its related views, dialogs etc.
 * <p>
 * This is meant to be used as a 'delegate' object that different label provider
 * implementations can wrap and use rather than a direct implementation of
 * a particular label provider interface.
 * <p>
 * Instances of this class may allocate resources (e.g. images)
 * and must be disposed when they are not needed anymore.
 *
 * @author Alex Boyko
 * @author Kris De Volder
 */
@SuppressWarnings("restriction")
public class BootDashLabels implements Disposable {

	private static final String UNKNOWN_LABEL = "???";

	private static final Image[] NO_IMAGES = null;

	private AppearanceAwareLabelProvider javaLabels = null;
	private RunStateImages runStateImages = null;


	/**
	 * TODO replace 'runStateImages' and this registry with a single registry
	 * for working with both animatons & simple images.
	 */
	private ImageDecorator images = new ImageDecorator();

	private Stylers stylers;

	private boolean declutter = true;

	/**
	 * This constructor is deprecated. It produces something incapable of
	 * properly styling some kinds of labels (e.g. those requiring the use
	 * of a 'bold' font. Use the alternate constructor which
	 * takes a {@link Stylers} argument.
	 */
	@Deprecated
	public BootDashLabels() {
		//Create slighly less-capable 'Stylers':
		this(new Stylers(null));
	}

	public BootDashLabels(Stylers stylers) {
		this.stylers = stylers;
	}

	///////////////////////////////////////////////////////////////
	//// Main apis that clients should use:

	@Override
	public void dispose() {
		if (javaLabels != null) {
			javaLabels.dispose();
		}
		if (runStateImages!=null) {
			runStateImages.dispose();
			runStateImages = null;
		}
		if (images!=null) {
			images.dispose();
			images = null;
		}
	}

	public Image[] getImageAnimation(Object e, BootDashColumn forColum) {
		if (e instanceof BootDashElement) {
			return getImageAnimation((BootDashElement)e, forColum);
		} else if (e instanceof BootDashModel) {
			return getImageAnimation((BootDashModel)e, forColum);
		}
		return NO_IMAGES;
	}

	/**
	 * For those who don't care about animations, fetches the first image
	 * of the animation sequence only; or if the icon is non-animated just
	 * the image.
	 */
	public final Image getImage(Object element, BootDashColumn column) {
		Image[] imgs = getImageAnimation(element, column);
		if (imgs!=null && imgs.length>0) {
			return imgs[0];
		}
		return null;
	}

	public StyledString getStyledText(Object element, BootDashColumn column) {
		if (element instanceof BootDashElement) {
			return getStyledText((BootDashElement)element, column);
		} else if (element instanceof BootDashModel) {
			return getStyledText((BootDashModel)element, column);
		} else if (element instanceof ButtonModel) {
			return getStyledText((ButtonModel)element);
		}
		return new StyledString(""+element);
	}

	///////////////////////////////////////////////////
	// Type-specific apis below
	//
	// Some label providers may be only for specific types of elements and can use these
	// methods instead.

	public Image[] getImageAnimation(BootDashModel element, BootDashColumn column) {
		ImageDescriptor icon = getIcon(element);
		ImageDescriptor decoration = getDecoration(element);
		return toAnimation(icon, decoration);
	}

	private ImageDescriptor getIcon(BootDashModel element) {
		if (element instanceof CloudFoundryBootDashModel) {
			CloudFoundryBootDashModel cfModel = (CloudFoundryBootDashModel) element;
			if (cfModel.getRunTarget().isConnected()) {
				return BootDashActivator.getImageDescriptor("icons/cloud-ready.png");
			} else {
				return BootDashActivator.getImageDescriptor("icons/cloud-inactive.png");
			}
		}
		return element.getRunTarget().getType().getIcon();
	}

	private ImageDescriptor getDecoration(BootDashModel element) {
		if (element.getRefreshState().isError()) {
			return BootDashActivator.getImageDescriptor("icons/error_ovr.gif");
		} else if (element.getRefreshState().isWarning()) {
			return BootDashActivator.getImageDescriptor("icons/warning_ovr.png");
		} else if (element.getRefreshState().isLoading()) {
			return BootDashActivator.getImageDescriptor("icons/waiting_ovr.gif");
		}
		return null;
	}

	private Image[] toAnimation(ImageDescriptor icon, ImageDescriptor decoration) {
		Image img = images.get(icon, decoration);
		return toAnimation(img);
	}

	private Image[] toAnimation(Image img) {
		if (img!=null) {
			return new Image[]{img};
		}
		return NO_IMAGES;
	}

	public Image[] getImageAnimation(BootDashElement element, BootDashColumn column) {
		if (element instanceof CloudServiceInstanceDashElement) {
			ImageDescriptor img = BootDashActivator.getDefault().getImageRegistry().getDescriptor(BootDashActivator.SERVICE_ICON);
			return toAnimation(img, null);
		} else if (element instanceof LocalCloudServiceDashElement) {
			if (column == BootDashColumn.RUN_STATE_ICN || column == BootDashColumn.TREE_VIEWER_MAIN) {
				ImageDescriptor img;
				switch (element.getRunState()) {
				case RUNNING:
					img = BootDashActivator.getDefault().getImageRegistry().getDescriptor(BootDashActivator.SERVICE_ICON);
					return toAnimation(img, null);
				case STARTING:
					return getRunStateAnimation(element.getRunState());
				default:
					img = BootDashActivator.getDefault().getImageRegistry().getDescriptor(BootDashActivator.SERVICE_INACTIVE_ICON);
					return toAnimation(img, null);
				}
			} else {
				return NO_IMAGES;
			}
		}
		try {
			if (element != null) {
				switch (column) {
				case PROJECT:
					IJavaProject jp = element.getJavaProject();
					return jp == null ? new Image[0] : new Image[] { getJavaLabels().getImage(jp) };
				case TREE_VIEWER_MAIN:
				case RUN_STATE_ICN:
					return decorateRunStateImages(element);
				default:
					return NO_IMAGES;
				}
			}
		} catch (Exception e) {
			Log.log(e);
		}
		return NO_IMAGES;
	}

	public StyledString getStyledText(BootDashModel element, BootDashColumn column) {
		if (element != null) {
			if (element.getRunTarget() != null) {
				if (element.getRefreshState().isLoading()) {
					StyledString prefix = new StyledString();
					if (element.getRefreshState().getMessage() != null) {
						prefix = new StyledString(element.getRefreshState().getMessage() + " - ", stylers.italicColoured(SWT.COLOR_DARK_GRAY));
					}
					return prefix.append(new StyledString(element.getRunTarget().getDisplayName(), stylers.italic()));
				} else {
					return new StyledString(element.getRunTarget().getDisplayName(), stylers.bold());
				}
			} else {
				return new StyledString(UNKNOWN_LABEL);
			}
		}
		return stylers==null?new StyledString("null"):new StyledString("null", stylers.red());
	}

	public StyledString getStyledText(ButtonModel element) {
		return new StyledString(element.getLabel(), stylers.hyperlink());
	}

	/**
	 * For a given column type return the styler to use for any [...] that are
	 * added around it. Return null
	 */
	public Styler getPrefixSuffixStyler(BootDashColumn column) {
		switch (column) {
		case TAGS:
			return stylers.tagBrackets();
		case LIVE_PORT:
			return stylers.darkGreen();
		case INSTANCES:
			return stylers.darkBlue();
		case NAME:
		case PROJECT:
//			return null;
		case HOST:
		case RUN_STATE_ICN:
		case DEFAULT_PATH:
		default:
			return Stylers.NULL;
		}
	}

	public StyledString getStyledText(BootDashElement element, BootDashColumn column) {
		//The big case below should set either one of 'label' or'styledLabel', depending
		// on whether it is 'styling capable'.
		String label = null;
		StyledString styledLabel = null;

		if (element != null) {
			switch(column) {
			case TAGS:
				String text = TagUtils.toString(element.getTags());
				styledLabel = stylers == null ? new StyledString(text) : TagUtils.applyTagStyles(text, stylers.tag());
				break;
			case PROJECT:
				IJavaProject jp = element.getJavaProject();
				if (jp == null) {
					// Not all projects in elements are Java projects. CF elements accept any project that contains a valid manifest.yml since the manifest.yml may
					// point to an executable archive for the app (.jar/.war)
					IProject project = element.getProject();
					if (project != null) {
						label = project.getName();
					} else {
						// Project and app (element) name are shown in separate columns now. If
						// there is no project mapping
						// do not show the element name anymore. That way the user knows that there is
						// no mapping for that element.
						label = "";
					}
				} else {
					styledLabel = getJavaLabels().getStyledText(jp);
					//TODO: should use 'element.hasDevtools()' but its not implemented
					// yet on CF elements.
					boolean devtools = BootPropertyTester.hasDevtools(element.getProject());
					if (devtools) {
						StyledString devtoolsDecoration = new StyledString(" [devtools]", stylers.darkGreen());
						styledLabel.append(devtoolsDecoration);
					}
				}
				break;
			case HOST:
				String host = element.getLiveHost();
				label = host == null ? UNKNOWN_LABEL : host;
				break;
			case TREE_VIEWER_MAIN:
				BootDashColumn[] cols = element.getColumns();
				styledLabel = new StyledString();
				for (BootDashColumn col : cols) {
					//Ignore RUN_STATE_ICN because its already represented in the label's icon.
					if (col != BootDashColumn.RUN_STATE_ICN) {
						StyledString append = getStyledText(element, col);
						if (hasText(append)) {
							Styler styler = getPrefixSuffixStyler(col);
							if (!hasText(styledLabel)) {
								// Nothing in the label so far, don't added brackets to first piece
								styledLabel = styledLabel.append(append);
							} else {
								if (col == BootDashColumn.DEFAULT_PATH) {
									styledLabel = styledLabel.append(" ").append(append);
								}
								else {
									if (styler == null) {
										styledLabel = styledLabel.append(" [").append(append).append("]");
									} else {
										styledLabel = styledLabel.append(" [",styler).append(append).append("]",styler);
									}
								}
							}
						}
					}
				}
				break;
			case NAME:
				styledLabel = new StyledString();

				if (element.getName() != null) {
					styledLabel.append(element.getName());
				}
				else {
					styledLabel.append(UNKNOWN_LABEL);
				}

				break;
			case DEVTOOLS:
				if (element.hasDevtools()) {
					styledLabel = new StyledString("devtools", stylers.darkGreen());
				} else {
					styledLabel = new StyledString();
				}
				break;
			case JMX_SSH_TUNNEL:
				if (element instanceof CloudAppDashElement) {
					CloudAppDashElement cfApp = (CloudAppDashElement) element;
					JmxSshTunnelStatus tunnelState = cfApp.getJmxSshTunnelStatus().getValue();
					if (tunnelState!=JmxSshTunnelStatus.DISABLED) {
						styledLabel = new StyledString("jmx", tunnelState==JmxSshTunnelStatus.ACTIVE ? stylers.darkGreen() : stylers.darkGrey());
					}
				} else {
					styledLabel = new StyledString();
				}
				break;
			case RUN_STATE_ICN:
				label = element.getRunState().toString();
				break;
			case LIVE_PORT:
				RunState runState = element.getRunState();
				if (runState == RunState.RUNNING || runState == RunState.DEBUGGING) {
					String textLabel;
					ImmutableSet<Integer> ports = element.getLivePorts();
					if (ports.isEmpty()) {
						textLabel = "unknown port";
					} else {
						StringBuilder str = new StringBuilder();
						String separator = "";
						for (Integer port : ports) {
							str.append(separator);
							str.append(":");
							str.append(port);

							separator = " ";
						}
						textLabel = str.toString();
					}
					if (stylers == null) {
						label = textLabel;
					} else {
						styledLabel = new StyledString(textLabel, stylers.darkGreen());
					}
				}
				break;
			case DEFAULT_PATH:
				String path = element.getDefaultRequestMappingPath();
				if (stylers == null) {
					label = path == null ? "" : path;
				} else {
					styledLabel = new StyledString(path == null ? "" : path, stylers.darkGrey());
				}
				break;
			case INSTANCES:
				int actual = element.getActualInstances();
				int desired = element.getDesiredInstances();
				if (!declutter || desired!=1 || actual > 1) { //Don't show: less clutter, you can already see whether a single instance is running or not
					if (stylers == null) {
						label = actual + "/" + desired;
					} else {
						styledLabel = new StyledString(actual+"/"+desired,stylers.darkBlue());
					}
				}
				break;
			case EXPOSED_URL:
				runState = element.getRunState();
				if (runState == RunState.RUNNING || runState == RunState.DEBUGGING) {
					List<String> tunnelNames = new ArrayList<>();
					if (element instanceof AbstractLaunchConfigurationsDashElement<?>) {
						ImmutableSet<ILaunchConfiguration> launches = ((AbstractLaunchConfigurationsDashElement<?>) element).getLaunchConfigs();
						for (ILaunchConfiguration launchConfig : launches) {
							tunnelNames.add(launchConfig.getName());
						}
					}

					for (String tunnelName : tunnelNames) {
						NGROKClient ngrokClient = NGROKLaunchTracker.get(tunnelName);
						if (ngrokClient != null) {
							if (styledLabel == null) {
								styledLabel = new StyledString("\u27A4 " + ngrokClient.getTunnel().getPublic_url(),stylers.darkBlue());
							}
							else {
								styledLabel.append(new StyledString(" / \u27A4 " + ngrokClient.getTunnel().getPublic_url(),stylers.darkBlue()));
							}
						}
					}

				}
				break;
			default:
				label = UNKNOWN_LABEL;
			}
		}
		if (styledLabel!=null) {
			return styledLabel;
		} else if (label!=null) {
			return new StyledString(label);
		}
		return new StyledString("");
	}

	/**
	 * Deprecated: use getStyledText.
	 */
	@Deprecated
	public String getText(BootDashElement element, BootDashColumn column) {
		return getStyledText(element, column).getString();
	}

	/////////////////////////////////////////////////////////////////////////////////////////////
	// private / helper stuff

	private String commaSeparated(Collection<String> elements) {
		if (elements!=null) {
			StringBuilder buf = new StringBuilder();
			boolean needComma = false;
			for (String string : elements) {
				if (needComma) {
					buf.append(',');
				} else {
					needComma = true;
				}
				buf.append(string);
			}
			return buf.toString();
		}
		return "";
	}

	private boolean hasText(StyledString stext) {
		return !stext.getString().isEmpty();
	}

	private AppearanceAwareLabelProvider getJavaLabels() {
		if (javaLabels == null) {
			javaLabels = new AppearanceAwareLabelProvider();
		}
		return javaLabels;
	}

	private Image[] getRunStateAnimation(RunState runState) {
		try {
			if (runStateImages==null) {
				runStateImages = new RunStateImages();
			}
			return runStateImages.getAnimation(runState);
		} catch (Exception e) {
			Log.log(e);
		}
		return null;
	}

	private Image[] decorateRunStateImages(BootDashElement bde) throws Exception {
		Image[] decoratedImages = getRunStateAnimation(bde.getRunState());
		if (bde.getTarget() != null && bde instanceof CloudAppDashElement && bde.getRunState() == RunState.RUNNING) {
			if (DevtoolsUtil.isDevClientAttached((CloudAppDashElement)bde, ILaunchManager.RUN_MODE) && decoratedImages.length > 0) {
				ImageDescriptor decorDesc = BootDashActivator.getDefault().getImageRegistry().getDescriptor(BootDashActivator.DT_ICON_ID);
				decoratedImages = runStateImages.getDecoratedImages(bde.getRunState(), decorDesc, IDecoration.BOTTOM_RIGHT);
			}
		}
		return decoratedImages;
	}

	/**
	 * Enables or disables decluttering. Decluttering means some
	 * 'obvious' information is hidden from generated label text.
	 * <p>
	 * Decluttering is enabled by default.
	 */
	public BootDashLabels setDeclutter(boolean declutter) {
		this.declutter = declutter;
		return this;
	}

}
