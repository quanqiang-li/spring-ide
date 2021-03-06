/*******************************************************************************
 * Copyright (c) 2013 Spring IDE Developers
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Spring IDE Developers - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.core.java.typehierarchy;

import org.eclipse.core.resources.IProject;

/**
 * @author Martin Lippert
 * @since 3.3.0
 */
public interface TypeHierarchyClassReaderFactory {
	
	public TypeHierarchyClassReader createClassReader(IProject project);

}
