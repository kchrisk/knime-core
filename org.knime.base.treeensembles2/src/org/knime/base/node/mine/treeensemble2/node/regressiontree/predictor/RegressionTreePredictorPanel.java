/*
 * ------------------------------------------------------------------------
 *  Copyright by KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.com; Email: contact@knime.com
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
package org.knime.base.node.mine.treeensemble2.node.regressiontree.predictor;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.knime.base.node.mine.treeensemble2.model.RegressionTreeModelPortObjectSpec;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class RegressionTreePredictorPanel extends JPanel {

    /** Panel name. */
    public static final String PANEL_NAME = "Prediction Settings";

    private final JTextField m_predictionColNameField;
    private final JCheckBox m_changePredictionColNameCheckBox;

    /**
     * */
    public RegressionTreePredictorPanel() {
        super(new GridBagLayout());
        m_predictionColNameField = new JTextField(20);
        m_changePredictionColNameCheckBox = new JCheckBox("Change prediction column name", false);
        final String defColName = RegressionTreePredictorConfiguration.getPredictColumnName("");
        m_predictionColNameField.setText(defColName);
        m_predictionColNameField.addFocusListener(new FocusAdapter() {
            /** {@inheritDoc} */
            @Override
            public void focusGained(final FocusEvent e) {
                if (m_predictionColNameField.getText().equals(defColName)) {
                    m_predictionColNameField.selectAll();
                }
            }
        });
        m_changePredictionColNameCheckBox.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(final ActionEvent e) {
                m_predictionColNameField.setEnabled(m_changePredictionColNameCheckBox.isSelected());
            }

        });
        m_predictionColNameField.setEnabled(m_changePredictionColNameCheckBox.isSelected());
        initLayout();
    }

    /**
     *  */
    private void initLayout() {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0;
        gbc.gridy = 0;
        add(m_changePredictionColNameCheckBox);

        gbc.gridy += 1;
        add(new JLabel("Prediction column name"), gbc);
        gbc.gridx += 1;
        add(m_predictionColNameField, gbc);

    }

    /**
     * Loads the settings.
     *
     * @param settings
     * @param specs
     * @throws NotConfigurableException
     */
    public void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
            throws NotConfigurableException {
        final RegressionTreeModelPortObjectSpec modelSpec = (RegressionTreeModelPortObjectSpec)specs[0];
        final DataColumnSpec targetSpec = modelSpec.getTargetColumn();
        RegressionTreePredictorConfiguration config = new RegressionTreePredictorConfiguration(targetSpec.getName());
        config.loadInDialog(settings);

        String colName = config.getPredictionColumnName();
        if (colName == null || colName.isEmpty()) {
            colName = RegressionTreePredictorConfiguration.getPredictColumnName("");
        }
        m_predictionColNameField.setText(colName);
        m_changePredictionColNameCheckBox.setSelected(config.isChangePredictionColumnName());
    }

    /**
     * Saves the settings.
     *
     * @param settings
     * @throws InvalidSettingsException
     */
    public void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        RegressionTreePredictorConfiguration config = new RegressionTreePredictorConfiguration("");
        config.setPredictionColumnName(m_predictionColNameField.getText());
        config.setChangePredictionColumnName(m_changePredictionColNameCheckBox.isSelected());
        config.save(settings);
    }

}
