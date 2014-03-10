/* ------------------------------------------------------------------
 * This source code, its documentation and all appendant files
 * are protected by copyright law. All rights reserved.
 *
 * Copyright by 
 * University of Konstanz, Germany
 * Chair for Bioinformatics and Information Mining (Prof. M. Berthold)
 * and KNIME GmbH, Konstanz, Germany
 *
 * You may not modify, publish, transmit, transfer or sell, reproduce,
 * create derivative works from, distribute, perform, display, or in
 * any way exploit any of the content, in whole or in part, except as
 * otherwise expressly permitted in writing by the copyright owner or
 * as specified in the license file distributed with this product.
 *
 * If you have any questions please contact the copyright holder:
 * website: www.knime.org
 * email: contact@knime.org
 * ---------------------------------------------------------------------
 *
 * History
 *   01.11.2008 (wiswedel): created
 */
package org.knime.core.node.workflow;

import org.knime.core.node.ExecutionMonitor;


/**
 *
 * @author wiswedel, University of Konstanz
 */
public class TestRecursiveMetaNodeUpdate extends WorkflowTestCase {

    private NodeID m_tableDiff_BeforeUpdate_11;
    private NodeID m_tableDiff_BeforeUpdate_13;
    private NodeID m_tableDiff_BeforeUpdate_15;
    private NodeID m_tableDiff_BeforeUpdate_17;
    private NodeID m_tableDiff_BeforeUpdate_21;
    private NodeID m_tableDiff_AfterUpdate_22;
    private NodeID m_tableDiff_AfterUpdate_23;
    private NodeID m_tableDiff_AfterUpdate_27;
    private NodeID m_tableDiff_AfterUpdate_28;
    private NodeID m_tableDiff_AfterUpdate_30;
    private NodeID m_metaNoUpdateAvail_4;
    private NodeID m_metaUpdateOnlyInChild_5;
    private NodeID m_metaUpdateTwoChildren_6;
    private NodeID m_metaDifferentDefault_7;
    private NodeID m_metaHiddenLink_19;

    /** {@inheritDoc} */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        NodeID baseID = loadAndSetWorkflow(
            getWorkflowDirectory("testRecursiveMetaNodeUpdate_Group/TestRecursiveMetaNodeUpdate"));
        m_tableDiff_BeforeUpdate_11 = new NodeID(baseID, 11);
        m_tableDiff_BeforeUpdate_13 = new NodeID(baseID, 13);
        m_tableDiff_BeforeUpdate_15 = new NodeID(baseID, 15);
        m_tableDiff_BeforeUpdate_17 = new NodeID(baseID, 17);
        m_tableDiff_BeforeUpdate_21 = new NodeID(baseID, 21);
        m_tableDiff_AfterUpdate_22 = new NodeID(baseID, 22);
        m_tableDiff_AfterUpdate_23 = new NodeID(baseID, 23);
        m_tableDiff_AfterUpdate_27 = new NodeID(baseID, 27);
        m_tableDiff_AfterUpdate_28 = new NodeID(baseID, 28);
        m_tableDiff_AfterUpdate_30 = new NodeID(baseID, 30);
        m_metaNoUpdateAvail_4 = new NodeID(baseID, 4);
        m_metaUpdateOnlyInChild_5 = new NodeID(baseID, 5);
        m_metaUpdateTwoChildren_6 = new NodeID(baseID, 6);
        m_metaDifferentDefault_7 = new NodeID(baseID, 7);
        m_metaHiddenLink_19 = new NodeID(baseID, 19);
    }
    
    private WorkflowLoadHelper createTemplateLoadHelper() {
        return new WorkflowLoadHelper(true, getManager().getContext());
    }

    public void testNoUpdateAfterLoad() throws Exception {
        assertTrue("expected update to be available", 
            getManager().checkUpdateMetaNodeLink(m_metaUpdateTwoChildren_6, createTemplateLoadHelper()));
        executeAllAndWait();
        checkStateOfMany(InternalNodeContainerState.EXECUTED, 
            m_tableDiff_BeforeUpdate_11, m_tableDiff_BeforeUpdate_13, m_tableDiff_BeforeUpdate_15, 
            m_tableDiff_BeforeUpdate_17, m_tableDiff_BeforeUpdate_21);
        checkStateOfMany(InternalNodeContainerState.EXECUTED, 
            m_metaNoUpdateAvail_4, m_metaUpdateOnlyInChild_5, m_metaUpdateTwoChildren_6, 
            m_metaDifferentDefault_7, m_metaHiddenLink_19); 
        
        checkStateOfMany(InternalNodeContainerState.CONFIGURED,
            m_tableDiff_AfterUpdate_22, m_tableDiff_AfterUpdate_23, m_tableDiff_AfterUpdate_27, 
            m_tableDiff_AfterUpdate_28, m_tableDiff_AfterUpdate_30);
    }
    
    public void testAllUpdateAfterLoad() throws Exception {
        getManager().updateMetaNodeLinks(createTemplateLoadHelper(), true, new ExecutionMonitor());
        executeAllAndWait();

        checkStateOfMany(InternalNodeContainerState.CONFIGURED, 
            m_tableDiff_BeforeUpdate_11, m_tableDiff_BeforeUpdate_13, m_tableDiff_BeforeUpdate_15, 
            m_tableDiff_BeforeUpdate_17, m_tableDiff_BeforeUpdate_21);
        
        checkStateOfMany(InternalNodeContainerState.EXECUTED, 
            m_metaNoUpdateAvail_4, m_metaUpdateOnlyInChild_5, m_metaUpdateTwoChildren_6, 
            m_metaDifferentDefault_7, m_metaHiddenLink_19); 
        
        checkStateOfMany(InternalNodeContainerState.EXECUTED,
            m_tableDiff_AfterUpdate_22, m_tableDiff_AfterUpdate_23, m_tableDiff_AfterUpdate_27, 
            m_tableDiff_AfterUpdate_28, m_tableDiff_AfterUpdate_30);
    }
    
    public void testUpdateMetaDifferentDefault() throws Exception {
        executeAndWait(m_tableDiff_BeforeUpdate_11);
        checkState(m_tableDiff_BeforeUpdate_11, InternalNodeContainerState.EXECUTED);
        executeAndWait(m_tableDiff_AfterUpdate_28);
        checkState(m_tableDiff_AfterUpdate_28, InternalNodeContainerState.CONFIGURED); // failed
        assertTrue("Expected meta node update available", 
            getManager().checkUpdateMetaNodeLink(m_metaDifferentDefault_7, createTemplateLoadHelper()));
        getManager().updateMetaNodeLink(m_metaDifferentDefault_7, new ExecutionMonitor(), createTemplateLoadHelper());
        checkState(m_tableDiff_BeforeUpdate_11, InternalNodeContainerState.CONFIGURED);
        executeAndWait(m_tableDiff_BeforeUpdate_11);
        checkState(m_tableDiff_BeforeUpdate_11, InternalNodeContainerState.CONFIGURED); // failed
        executeAndWait(m_tableDiff_AfterUpdate_28);
        checkState(m_tableDiff_AfterUpdate_28, InternalNodeContainerState.EXECUTED); // failed
    }
    
    public void testUpdateOnlyInChild() throws Exception {
        executeAndWait(m_tableDiff_BeforeUpdate_15);
        checkState(m_tableDiff_BeforeUpdate_15, InternalNodeContainerState.EXECUTED);
        executeAndWait(m_tableDiff_AfterUpdate_22);
        checkState(m_tableDiff_AfterUpdate_22, InternalNodeContainerState.CONFIGURED); // failed
        assertTrue("Expected meta node update available", 
            getManager().checkUpdateMetaNodeLink(m_metaUpdateOnlyInChild_5, createTemplateLoadHelper()));
        getManager().updateMetaNodeLink(m_metaUpdateOnlyInChild_5, new ExecutionMonitor(), createTemplateLoadHelper());
        checkState(m_tableDiff_BeforeUpdate_15, InternalNodeContainerState.CONFIGURED);
        executeAndWait(m_tableDiff_BeforeUpdate_15);
        checkState(m_tableDiff_BeforeUpdate_15, InternalNodeContainerState.CONFIGURED); // failed
        executeAndWait(m_tableDiff_AfterUpdate_22);
        checkState(m_tableDiff_AfterUpdate_22, InternalNodeContainerState.EXECUTED); // failed
    }

}