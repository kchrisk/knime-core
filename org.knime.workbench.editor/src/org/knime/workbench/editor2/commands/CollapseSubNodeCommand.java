/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME. The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ------------------------------------------------------------------------
 *
 * History
 *   07.05.2011 (mb): created
 */
package org.knime.workbench.editor2.commands;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.SubNodeContainer;
import org.knime.core.node.workflow.WorkflowAnnotation;
import org.knime.core.node.workflow.WorkflowManager;

/**
 * Command that collapses the given nodes to a sub node.
 *
 * @author Patrick Winter, KNIME.com AG, Zurich, Switzerland
 */
public class CollapseSubNodeCommand extends AbstractKNIMECommand {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(
            CollapseSubNodeCommand.class);

    private NodeID[] m_nodes;
    private WorkflowAnnotation[] m_annos;
    private String m_name;
    private NodeID m_wrapper;

    /**
     * @param wfm the workflow manager holding the new metanode
     * @param nodes the ids of the nodes to collapse
     * @param annos the workflow annotations to collapse
     * @param name of new metanode
     */
    public CollapseSubNodeCommand(final WorkflowManager wfm,
            final NodeID[] nodes, final WorkflowAnnotation[] annos,
            final String name) {
        super(wfm);
        m_nodes = nodes.clone();
        m_annos = annos.clone();
        m_name = name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canExecute() {
        if (!super.canExecute()) {
            return false;
        }
        return null == getHostWFM().canCollapseNodesIntoMetaNode(m_nodes);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() {
        try {
            NodeID id =
                getHostWFM().collapseIntoMetaNode(m_nodes, m_annos,
                        m_name).getID();
            m_wrapper = getHostWFM().convertMetaNodeToSubNode(id);
        } catch (Exception e) {
            String error = "Collapsing Sub Node failed: " + e.getMessage();
            LOGGER.error(error, e);
            MessageDialog.openError(Display.getCurrent().getActiveShell(),
                    "Collapse failed", error);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canUndo() {
        if (m_wrapper != null) {
            return null == getHostWFM().canExpandSubNode(m_wrapper);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void undo() {
        WorkflowManager wfm = ((SubNodeContainer)getHostWFM().getNodeContainer(m_wrapper)).getWorkflowManager();
        getHostWFM().expandSubWorkflow(m_wrapper, wfm);
        m_wrapper = null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void redo() {
        if (m_annos.length >= 0) {
            String error = "Redo of Collapse-Command not possible.";
            LOGGER.error(error);
            MessageDialog.openError(Display.getCurrent().getActiveShell(),
                    "Redo failed", error);
            return;
        }
        execute();
    }

}