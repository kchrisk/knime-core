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
 */
package org.knime.core.node.workflow;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

import org.knime.core.node.KNIMEConstants;
import org.knime.core.node.NodeView;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.DatabasePortObject.DatabaseOutPortPanel;
import org.knime.core.node.util.ViewUtils;

/**
 *
 *
 * @author Fabian Dill, University of Konstanz
 */
public class OutPortView extends JFrame {

    /** Keeps track if view has been opened before. */
    private boolean m_wasOpened = false;

    /** Initial frame width. */
    static final int INIT_WIDTH = 500;

    /** Initial frame height. */
    static final int INIT_HEIGHT = 400;

    private final JTabbedPane m_tabbedPane;

    private final LoadingPanel m_loadingPanel = new LoadingPanel();

    private static final ExecutorService UPDATE_EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger m_counter = new AtomicInteger();

        @Override
        public Thread newThread(final Runnable r) {
            Thread t = new Thread(r, "OutPortView-Updater-" + m_counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        }
    });

    /**
     * A view showing the data stored in the specified output port.
     *
     * @param nodeNameWithID The name of the node the inspected port belongs to
     * @param portName name of the port which is also displayed in the title
     */
    OutPortView(final String nodeNameWithID, final String portName) {
        super(portName + " - " + nodeNameWithID);
        // init frame
        super.setName(getTitle());
        if (KNIMEConstants.KNIME16X16 != null) {
            super.setIconImage(KNIMEConstants.KNIME16X16.getImage());
        }
        super.setBackground(NodeView.COLOR_BACKGROUND);
        super.setSize(INIT_WIDTH, INIT_HEIGHT);
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("File");
        menu.setMnemonic('F');
        JMenuItem item = new JMenuItem("Close");
        item.setMnemonic('C');
        item.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(final ActionEvent event) {
                setVisible(false);
            }
        });
        menu.add(item);
        menuBar.add(menu);
        setJMenuBar(menuBar);
        m_tabbedPane = new JTabbedPane();
        getContentPane().add(m_loadingPanel);
    }

    /**
     * shows this view and brings it to front.
     */
    void openView(final Rectangle knimeWindowBounds) {
        if (!m_wasOpened) {
            m_wasOpened = true;
            updatePortView();
            ViewUtils.centerLocation(this, knimeWindowBounds);
        }
        // if the view was already visible
        /* bug1922: if the portview is minimized and then opened again (from the
         * context menu) it stays behind the KNIME main window.
         * fix: this strange sequence of calls. It seems to work on Win, Linux,
         * and MacOS.
         */
        setVisible(false);
        setExtendedState(NORMAL);
        setVisible(true);
        toFront();
    }

    /**
     * Validates and repaints the super component.
     */
    final void updatePortView() {
        invalidate();
        validate();
        repaint();
    }

    /**
     * A utility class that aggregates all objects that are updated in the update method.
     */
    private static final class UpdateObject {
        private final PortObject m_portObject;

        private final PortObjectSpec m_portObjectSpec;

        private final FlowObjectStack m_flowObjectStack;

        private final CredentialsProvider m_credentialsProvider;

        private final NodeContext m_nodeContext;

        private UpdateObject(final PortObject po, final PortObjectSpec spec, final FlowObjectStack stack,
            final CredentialsProvider prov) {
            m_portObject = po;
            m_portObjectSpec = spec;
            m_flowObjectStack = stack;
            m_credentialsProvider = prov;
            m_nodeContext = NodeContext.getContext();
        }
    }

    private final AtomicReference<UpdateObject> m_updateObjectReference = new AtomicReference<UpdateObject>();

    /**
     * Sets the content of the view.
     *
     * @param portObject a data table, model content or other
     * @param portObjectSpec data table spec or model content spec or other spec
     * @param stack The {@link FlowObjectStack} of the node.
     * @param credentials the CredenialsProvider used in out-port view
     */
    void update(final PortObject portObject, final PortObjectSpec portObjectSpec, final FlowObjectStack stack,
        final CredentialsProvider credentials) {
        UpdateObject updateObject = new UpdateObject(portObject, portObjectSpec, stack, credentials);

        // set update object, run update thread only if there was no previous
        // update object (otherwise an update is currently ongoing)
        if (m_updateObjectReference.getAndSet(updateObject) == null) {
            UPDATE_EXECUTOR.execute(new Runnable() {
                @Override
                public void run() {
                    UpdateObject upO;
                    while ((upO = m_updateObjectReference.get()) != null) {
                        updateInternal(upO);
                        // invalidate update reference only if there is no
                        // new update object in the reference, otherwise
                        // do a new iteration.
                        if (m_updateObjectReference.compareAndSet(upO, null)) {
                            // break out here, do not rely on while() statement
                            // as new UO may be set (another thread is queued)
                            break;
                        }
                    }
                }
            });
        }

    }

    /** Internal update method that creates new tabs and displays them. */
    private void updateInternal(final UpdateObject updateObject) {
        ViewUtils.invokeAndWaitInEDT(new Runnable() {
            @Override
            public void run() {
                NodeContext.pushContext(updateObject.m_nodeContext);
                try {
                    runWithContext();
                } finally {
                    NodeContext.removeLastContext();
                }
            }

            private void runWithContext() {
                // add all port object tabs
                final Map<String, JComponent> views = new LinkedHashMap<String, JComponent>();
                PortObject portObject = updateObject.m_portObject;
                PortObjectSpec portObjectSpec = updateObject.m_portObjectSpec;
                FlowObjectStack stack = updateObject.m_flowObjectStack;
                CredentialsProvider credentials = updateObject.m_credentialsProvider;
                if (portObject != null) {
                    JComponent[] poViews = portObject.getViews();
                    if (poViews != null) {
                        for (JComponent comp : poViews) {
                            // fix 2379: CredentialsProvider needed in
                            // DatabasePortObject to create db connection
                            // while accessing data for preview
                            if (comp instanceof DatabaseOutPortPanel) {
                                DatabaseOutPortPanel dbcomp = (DatabaseOutPortPanel)comp;
                                dbcomp.setCredentialsProvider(credentials);
                            }
                            views.put(comp.getName(), comp);
                        }
                    }
                } else {
                    // what to display, if no port object is available?
                    JPanel noDataPanel = new JPanel();
                    noDataPanel.setLayout(new BorderLayout());
                    Box boexle = Box.createHorizontalBox();
                    boexle.add(Box.createHorizontalGlue());
                    boexle.add(new JLabel("No data available!"));
                    boexle.add(Box.createHorizontalGlue());
                    noDataPanel.add(boexle, BorderLayout.CENTER);
                    noDataPanel.setName("No Table");
                    views.put("No Table", noDataPanel);
                }
                JComponent[] posViews = portObjectSpec == null ? new JComponent[0] : portObjectSpec.getViews();
                if (posViews != null) {
                    for (JComponent comp : posViews) {
                        views.put(comp.getName(), comp);
                    }
                }

                FlowObjectStackView stackView = new FlowObjectStackView();
                stackView.update(stack);
                views.put("Flow Variables", stackView);

                m_tabbedPane.removeAll();
                for (Map.Entry<String, JComponent> entry : views.entrySet()) {
                    m_tabbedPane.addTab(entry.getKey(), entry.getValue());
                }
                remove(m_loadingPanel);
                add(m_tabbedPane);
                invalidate();
                validate();
                repaint();
            }
        });

    }

    /** {@inheritDoc} */
    @Override
    public void dispose() {
        // release all - identified memory leak via
        // sun.awt.AppContext -> ... Maps -> swing.RepaintManager -> ...> JTabbedPane -> ... -> WFM
        m_tabbedPane.removeAll();
        remove(m_tabbedPane);
        super.dispose();
    }

    /** Displays "loading port content". */
    @SuppressWarnings("serial")
    private static final class LoadingPanel extends JPanel {

        LoadingPanel() {
            setLayout(new BorderLayout());
            setBackground(Color.WHITE);
            Box centerBox = Box.createHorizontalBox();
            centerBox.add(Box.createHorizontalGlue());
            centerBox.add(new JLabel("Loading port content..."));
            centerBox.add(Box.createHorizontalGlue());
            add(centerBox);
        }

    }
}
