/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
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
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * -------------------------------------------------------------------
 *
 * History
 *   09.06.2005 (Florian Georg): created
 */
package org.knime.workbench.editor2.editparts;

import java.util.ArrayList;

import org.eclipse.draw2d.AbsoluteBendpoint;
import org.eclipse.draw2d.IFigure;
import org.eclipse.draw2d.PolylineConnection;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.editparts.AbstractConnectionEditPart;
import org.eclipse.gef.editparts.ZoomListener;
import org.eclipse.gef.editparts.ZoomManager;
import org.eclipse.gef.editpolicies.ConnectionEndpointEditPolicy;
import org.eclipse.gef.requests.ChangeBoundsRequest;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.flowvariable.FlowVariablePortObject;
import org.knime.core.node.workflow.ConnectionContainer;
import org.knime.core.node.workflow.ConnectionProgressEvent;
import org.knime.core.node.workflow.ConnectionProgressListener;
import org.knime.core.node.workflow.ConnectionUIInformation;
import org.knime.core.node.workflow.ConnectionUIInformationEvent;
import org.knime.core.node.workflow.ConnectionUIInformationListener;
import org.knime.core.node.workflow.NodeContainer;
import org.knime.core.node.workflow.NodeID;
import org.knime.core.node.workflow.NodeOutPort;
import org.knime.core.node.workflow.WorkflowManager;
import org.knime.workbench.editor2.commands.ChangeBendPointLocationCommand;
import org.knime.workbench.editor2.editparts.policy.ConnectionBendpointEditPolicy;
import org.knime.workbench.editor2.editparts.snap.SnapOffBendPointConnectionRouter;
import org.knime.workbench.editor2.figures.AbstractPortFigure;
import org.knime.workbench.editor2.figures.CurvedPolylineConnection;
import org.knime.workbench.editor2.figures.ProgressPolylineConnection;

/**
 * EditPart controlling a <code>ConnectionContainer</code> object in the
 * workflow. Model: {@link ConnectionContainer} View: {@link PolylineConnection}
 * created in {@link #createFigure()} Controller:
 * {@link ConnectionContainerEditPart}
 *
 *
 * @author Florian Georg, University of Konstanz
 */
public class ConnectionContainerEditPart extends AbstractConnectionEditPart
        implements ZoomListener, ConnectionUIInformationListener,
        ConnectionProgressListener {

    private static final NodeLogger LOGGER =
            NodeLogger.getLogger(ConnectionContainerEditPart.class);

    /** {@inheritDoc} */
    @Override
    public ConnectionContainer getModel() {
        return (ConnectionContainer)super.getModel();
    }

    /**
     * Returns the parent WFM. This method may also return null if the target
     * edit part has no parent assigned.
     *
     * @return The hosting WFM
     */
    public WorkflowManager getWorkflowManager() {
        EditPart targetEditPart = getTarget();
        if (targetEditPart instanceof NodeInPortEditPart) {
            return ((NodeInPortEditPart)targetEditPart).getManager();
        }
        if (targetEditPart instanceof WorkflowOutPortEditPart) {
            return ((WorkflowOutPortEditPart)targetEditPart).getManager();
        }
        return null;
    }

    /**
     * Returns the node with the corresponding id. If the passed wfm is null, it
     * traverses the hierarchy to find it. If the wfm is passed, it must contain
     * the node - or be the node itself (that happens in metanodes and outgoing
     * connectinos).
     *
     *
     * @param wfm if not null, the node is taken from there
     * @param node id of the node to return
     * @return the node with the specified id.
     */
    private NodeContainer getNode(final WorkflowManager wfm, final NodeID node) {
        if (wfm != null) {
            if (wfm.getID().equals(node)) {
                return wfm;
            } else {
                return wfm.getNodeContainer(node);
            }
        }

        // follow the hierarchy
        NodeID prefix = node.getPrefix();
        if (prefix.equals(WorkflowManager.ROOT.getID())) {
            return WorkflowManager.ROOT.getNodeContainer(node);
        } else {
            NodeContainer nc = getNode(null, prefix);
            if (!(nc instanceof WorkflowManager)) {
                return null;
            }
            WorkflowManager parent = (WorkflowManager)nc;
            return parent.getNodeContainer(node);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void activate() {
        super.activate();
        getModel().addUIInformationListener(this);
        getModel().addProgressListener(this);
    }

    /** {@inheritDoc} */
    @Override
    public void deactivate() {
        getModel().removeUIInformationListener(this);
        getModel().removeProgressListener(this);
        super.deactivate();
    }

    /**
     * Creates a GEF command to shift the connections bendpoints.
     *
     * @param request the underlying request holding information about the shift
     * @return the command to change the bendpoint locations
     */
    public Command getBendpointAdaptionCommand(final Request request) {
        assert (request instanceof ChangeBoundsRequest) : "Unexpected request type: " + request.getClass();

        ZoomManager zoomManager =
                (ZoomManager)(getRoot().getViewer()
                        .getProperty(ZoomManager.class.toString()));

        Point moveDelta = ((ChangeBoundsRequest) request).getMoveDelta();
        return new ChangeBendPointLocationCommand(this, moveDelta, zoomManager);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void createEditPolicies() {

        installEditPolicy(EditPolicy.CONNECTION_ENDPOINTS_ROLE,
                new ConnectionEndpointEditPolicy());

        // enable bendpoints (must be stored in extra info)
        installEditPolicy(EditPolicy.CONNECTION_BENDPOINTS_ROLE,
                new ConnectionBendpointEditPolicy());

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected IFigure createFigure() {

        ProgressPolylineConnection conn = new CurvedPolylineConnection(false);
        // Bendpoints
        SnapOffBendPointConnectionRouter router =
                new SnapOffBendPointConnectionRouter();
        conn.setConnectionRouter(router);
        conn.setRoutingConstraint(new ArrayList());
        // register as zoom listener to adapt the line width
        ZoomManager zoomManager =
                (ZoomManager)getRoot().getViewer().getProperty(
                        ZoomManager.class.toString());
        zoomManager.addZoomListener(this);
        conn
                .setLineWidth(calculateLineWidthFromZoomLevel(zoomManager
                        .getZoom()));

        // make flow variable port connections look red.
        if (isFlowVariablePortConnection()) {
            conn.setForegroundColor(AbstractPortFigure.getFlowVarPortColor());
        }
        return conn;
    }

    private boolean isFlowVariablePortConnection() {
        ConnectionContainer connCon = getModel();
        NodeContainer node = getNode(getWorkflowManager(), connCon.getSource());
        if (node != null) {
            NodeOutPort srcPort;
            if (isIncomingConnection(connCon)) {
                // then this is a workflow port
                srcPort =
                        ((WorkflowManager)node).getWorkflowIncomingPort(connCon
                                .getSourcePort());
            } else {
                srcPort = node.getOutPort(connCon.getSourcePort());
            }
            if (srcPort != null
                    && srcPort.getPortType()
                            .equals(FlowVariablePortObject.TYPE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true, if the connection is a connection from a metanode incoming
     * port to a node inside the metanode.
     *
     * @param conn
     * @return
     */
    private boolean isIncomingConnection(final ConnectionContainer conn) {
        switch (conn.getType()) {
            case WFMIN:
            case WFMTHROUGH:
                return true;
            default:
                return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void connectionUIInformationChanged(
            final ConnectionUIInformationEvent evt) {
        refreshVisuals();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void refreshVisuals() {
        super.refreshVisuals();
        LOGGER.debug("refreshing visuals for: " + getModel());
        ConnectionUIInformation ei = null;
        ei = getModel().getUIInfo();
        LOGGER.debug("modelling info: " + ei);

        // make flow variable port connections look red.
        CurvedPolylineConnection fig = (CurvedPolylineConnection)getFigure();
        if (isFlowVariablePortConnection()) {
            fig.setForegroundColor(AbstractPortFigure.getFlowVarPortColor());
        }

        //update 'curved' settings
        if (getWorkflowManager() != null && getWorkflowManager().getEditorUIInformation() != null) {
            fig.setCurved(getWorkflowManager().getEditorUIInformation().getHasCurvedConnections());
        }


        // recreate list of bendpoints
        ArrayList<AbsoluteBendpoint> constraint =
                new ArrayList<AbsoluteBendpoint>();
        if (ei != null) {
            int[][] p = ei.getAllBendpoints();
            for (int i = 0; i < p.length; i++) {
                AbsoluteBendpoint bp = new AbsoluteBendpoint(p[i][0], p[i][1]);
                constraint.add(bp);
            }
        }

        fig.setRoutingConstraint(constraint);
    }

    private int calculateLineWidthFromZoomLevel(final double zoom) {
        double newZoomValue = zoom;
        // if the zoom level is larger than 100% the width
        // is adapted accordingly
        if (zoom < 1.0) {
            newZoomValue = 1.0;
        }
        return (int) Math.round(newZoomValue);
    }

    /**
     * Adapts the line width according to the zoom level.
     *
     * @param zoom the zoom level from the zoom manager
     */
    @Override
    public void zoomChanged(final double zoom) {
        ((PolylineConnection)getFigure())
                .setLineWidth(calculateLineWidthFromZoomLevel(zoom));
    }

    /** {@inheritDoc} */
    @Override
    public void progressChanged(final ConnectionProgressEvent pe) {
        ProgressPolylineConnection conn =
            (ProgressPolylineConnection)getFigure();
        conn.progressChanged(pe.getConnectionProgress());
    }
}
