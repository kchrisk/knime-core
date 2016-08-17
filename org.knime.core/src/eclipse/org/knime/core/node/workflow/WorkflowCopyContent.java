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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.core.node.workflow;

import java.util.Collections;
import java.util.Map;



/**
 * Class representing node IDs and workflow annotations that need to be
 * copied from a workflow. Both IDs and annotation must be contained in the
 * workflow that is copied from.
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class WorkflowCopyContent {

    private NodeID[] m_nodeIDs;
    private WorkflowAnnotation[] m_annotations;
    /** see {@link #setIncludeInOutConnections(boolean)}. */
    private boolean m_isIncludeInOutConnections;
    /** A map which maps old NodeID to preferred ID suffix in the target wfm. Used for template loading. */
    private Map<NodeID, Integer> m_suggestedNodeIDSuffixMap;
    /** A map which maps old NodeID to UI infos in the target wfm. Used for template loading. */
    private Map<NodeID, NodeUIInformation> m_uiInfoMap;

    /** @return the ids */
    public NodeID[] getNodeIDs() {
        return m_nodeIDs;
    }

    /** @param ids the ids to set */
    public void setNodeIDs(final NodeID... ids) {
        m_nodeIDs = ids;
    }

    /** Used when copying from metanode template space.
     * @param id The ID of the metanode in the template root workflow
     * @param suggestedNodeIDSuffix The suffix to be used in the target workflow (overwrite it)
     * @param uiInfo The UIInfo the in the target workflow (also overwritten)
     * @return this
     */
    WorkflowCopyContent setNodeID(final NodeID id, final int suggestedNodeIDSuffix, final NodeUIInformation uiInfo) {
        m_nodeIDs = new NodeID[] {id};
        m_suggestedNodeIDSuffixMap = Collections.singletonMap(id, suggestedNodeIDSuffix);
        m_uiInfoMap = Collections.singletonMap(id, uiInfo);
        return this;
    }

    /** The overwritten NodeID suffix to the given node or null if not overwritten.
     * @param id The ID in question.
     * @return Null or the suffix. */
    Integer getSuggestedNodIDSuffix(final NodeID id) {
        return m_suggestedNodeIDSuffixMap == null ? null : m_suggestedNodeIDSuffixMap.get(id);
    }

    /** Get overwritten UIInfo to node with given ID or null.
     * @param id ...
     * @return ...
     */
    NodeUIInformation getOverwrittenUIInfo(final NodeID id) {
        return m_uiInfoMap == null ? null : m_uiInfoMap.get(id);
    }

    /** see {@link #setIncludeInOutConnections(boolean)}.
     * @return the isIncludeInOutConnections */
    public boolean isIncludeInOutConnections() {
        return m_isIncludeInOutConnections;
    }

    /** Set whether connections that link to or from any of the contained nodes
     * should be included in the copy content. Connections whose source and
     * destination are part of the {@link #getNodeIDs() NodeIDs set} are
     * automatically included, this property determines whether connections
     * connecting to this island are included as well.
     * @param isIncludeInOutConnections the isIncludeInOutConnections to set */
    public void setIncludeInOutConnections(
            final boolean isIncludeInOutConnections) {
        m_isIncludeInOutConnections = isIncludeInOutConnections;
    }

    /** @return the annotations, never null */
    public WorkflowAnnotation[] getAnnotations() {
        if (m_annotations == null) {
            return new WorkflowAnnotation[0];
        } else {
            return m_annotations;
        }
    }

    /** Sets annotation references.
     * @param annotations The annotations references.
     */
    public void setAnnotation(final WorkflowAnnotation... annotations) {
        m_annotations = annotations;
    }

}
