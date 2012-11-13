/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2011
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
 *   Jan 20, 2012 (wiswedel): created
 */
package org.knime.base.node.mine.treeensemble.model;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

import org.knime.base.node.mine.decisiontree2.model.DecisionTree;
import org.knime.base.node.mine.treeensemble.data.PredictorRecord;
import org.knime.base.node.mine.treeensemble.data.TreeBitColumnMetaData;
import org.knime.base.node.mine.treeensemble.data.TreeMetaData;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.util.NonClosableInputStream;
import org.knime.core.data.util.NonClosableOutputStream;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.NodeLogger;

/**
 *
 * @author Bernd Wiswedel, KNIME.com, Zurich, Switzerland
 */
public final class TreeEnsembleModel {

    /** Tree type, whether learned on bit vector or normal (nominal & numeric)
     * data. Needed to decided whether the learn columns are real data columns
     * or fake columns that originate from different bit positions.
     */
    public enum TreeType {
        /** Bit vector / fingerprint type. */
        BitVector((byte)0),
        /** Normal type (i.e. normal data columns with numeric or
         * nominal content). */
        Ordinary((byte)1);

        private final byte m_persistByte;
        private TreeType(final byte persistByte) {
            m_persistByte = persistByte;
        }

        private void save(final DataOutputStream out) throws IOException {
            out.writeByte(m_persistByte);
        }
        private static TreeType load(final DataInputStream in)
            throws IOException {
            byte persistByte = in.readByte();
            for (TreeType t : values()) {
                if (t.m_persistByte == persistByte) {
                    return t;
                }
            }
            throw new IOException("Can't read data type, unknown "
                    + "byte identifier: " + persistByte);
        }
    }

    private final TreeMetaData m_metaData;
    private final TreeType m_type;
    private final AbstractTreeModel[] m_models;

    /**
     * @param models
     */
    public TreeEnsembleModel(final TreeMetaData metaData, final AbstractTreeModel[] models,
            final TreeType treeType) {
        m_metaData = metaData;
        m_models = models;
        m_type = treeType;
    }

    /** @return the models */
    public AbstractTreeModel<?> getTreeModel(final int index) {
        return m_models[index];
    }

    /** @return the models */
    public TreeModelClassification getTreeModelClassification(final int index) {
        return (TreeModelClassification)m_models[index];
    }

    /** @return the models */
    public TreeModelRegression getTreeModelRegression(final int index) {
        return (TreeModelRegression)m_models[index];
    }

    public int getNrModels() {
        return m_models.length;
    }

    /** @return the metaData */
    public TreeMetaData getMetaData() {
        return m_metaData;
    }

    /** @return the type */
    public TreeType getType() {
        return m_type;
    }

    public DecisionTree createDecisionTree(final int modelIndex,
            final DataTable sampleForHiliting) {
        final DecisionTree result;
        if (m_metaData.isRegression()) {
            TreeModelRegression treeModel = getTreeModelRegression(modelIndex);
            result = treeModel.createDecisionTree(m_metaData);
        } else {
            TreeModelClassification treeModel = getTreeModelClassification(modelIndex);
            result = treeModel.createDecisionTree(m_metaData);
        }
        if (sampleForHiliting != null) {
            final DataTableSpec dataSpec = sampleForHiliting.getDataTableSpec();
            final DataTableSpec spec = getLearnAttributeSpec(dataSpec);
            for (DataRow r : sampleForHiliting) {
                try {
                    DataRow fullAttributeRow = createLearnAttributeRow(r, spec);
                    result.addCoveredPattern(fullAttributeRow, spec);
                } catch (Exception e) {
                    // dunno what to do with that
                    NodeLogger.getLogger(getClass()).error(
                            "Error updating hilite info in tree view", e);
                    break;
                }
            }
        }
        return result;
    }

    /** Get a table spec representing the learn attributes (not the target!).
     * For ordinary data it is just a subset of the input columns, for bit
     * vector data it's an expanded table spec with each bit represented by a
     * StringCell column ("0" or "1"), whose name is "Bit x".
     * @param learnSpec The original learn spec (which is also the return value
     * for ordinary data)
     * @return Such a learn attribute spec.
     */
    public DataTableSpec getLearnAttributeSpec(final DataTableSpec learnSpec) {
        final TreeType type = getType();
        switch (type) {
        case Ordinary:
            return learnSpec;
        case BitVector:
            int nrAttributes = getMetaData().getNrAttributes();
            DataColumnSpec[] colSpecs = new DataColumnSpec[nrAttributes];
            for (int i = 0; i < nrAttributes; i++) {
                colSpecs[i] = new DataColumnSpecCreator(
                        TreeBitColumnMetaData.getAttributeName(i),
                        StringCell.TYPE).createSpec();
            }
            return new DataTableSpec(colSpecs);
        default:
            throw new IllegalStateException(
                    "Type unknown (not implemented): " + type);
        }
    }

    public DataRow createLearnAttributeRow(
            final DataRow learnRow, final DataTableSpec learnSpec) {
        final TreeType type = getType();
        switch (type) {
        case Ordinary:
            return learnRow;
        case BitVector:
            DataCell c = learnRow.getCell(0);
            if (c.isMissing()) {
                return null;
            }
            BitVectorValue bv = (BitVectorValue)c;
            final long length = bv.length();
            int nrAttributes = getMetaData().getNrAttributes();
            if (length != nrAttributes) {
                // TODO indicate error message
                return null;
            }
            DataCell trueCell = new StringCell("1");
            DataCell falseCell = new StringCell("0");
            DataCell[] cells = new DataCell[nrAttributes];
            for (int i = 0; i < nrAttributes; i++) {
                cells[i] = bv.get(i) ? trueCell : falseCell;
            }
            return new DefaultRow(learnRow.getKey(), cells);
        default:
            throw new IllegalStateException(
                    "Type unknown (not implemented): " + type);
        }
    }

    public PredictorRecord createPredictorRecord(final DataRow filterRow,
            final DataTableSpec learnSpec) {
        switch (m_type) {
        case Ordinary:
            return createNominalNumericPredictorRecord(filterRow, learnSpec);
        case BitVector:
            return createBitVectorPredictorRecord(filterRow);
        default:
            throw new IllegalStateException("Unknown tree type "
                    + "(not implemented): " + m_type);
        }
    }

    private PredictorRecord createBitVectorPredictorRecord(
            final DataRow filterRow) {
        assert filterRow.getNumCells() == 1
            : "Expected one cell as bit vector data";
        DataCell c = filterRow.getCell(0);
        if (c.isMissing()) {
            return null;
        }
        BitVectorValue bv = (BitVectorValue)c;
        final long length = bv.length();
        if (length != getMetaData().getNrAttributes()) {
            // TODO indicate error message
            return null;
        }
        Map<String, Object> valueMap =
            new LinkedHashMap<String, Object>((int)(length / 0.75 + 1.0));
        for (int i = 0; i < length; i++) {
            valueMap.put(TreeBitColumnMetaData.getAttributeName(i),
                    Boolean.valueOf(bv.get(i)));
        }
        return new PredictorRecord(valueMap);
    }

    private PredictorRecord createNominalNumericPredictorRecord(
            final DataRow filterRow, final DataTableSpec trainSpec) {
        final int nrCols = trainSpec.getNumColumns();
        Map<String, Object> valueMap =
            new LinkedHashMap<String, Object>((int)(nrCols / 0.75 + 1.0));
        for (int i = 0; i < nrCols; i++) {
            DataColumnSpec col = trainSpec.getColumnSpec(i);
            String colName = col.getName();
            DataType colType = col.getType();
            DataCell cell = filterRow.getCell(i);
            if (cell.isMissing()) {
                valueMap.put(colName, PredictorRecord.NULL);
            } else if (colType.isCompatible(NominalValue.class)) {
                valueMap.put(colName, cell.toString());
            } else if (colType.isCompatible(DoubleValue.class)) {
                valueMap.put(colName, ((DoubleValue)cell).getDoubleValue());
            } else {
                throw new IllegalStateException(
                        "Expected nominal or numeric column type for column \""
                                + colName + "\" but got \"" + colType + "\"");
            }
        }
        return new PredictorRecord(valueMap);
    }

    /** Saves ensemble to target in binary format, output is NOT
     * closed afterwards.
     * @param out ...
     * @param exec ...
     * @throws IOException ...
     * @throws CanceledExecutionException ... */
    public void save(final OutputStream out, final ExecutionMonitor exec)
    throws IOException, CanceledExecutionException {
        // wrapping the (zip) output stream with a buffered stream reduces
        // the write operation from, e.g. 63s to 8s
        DataOutputStream dataOutput = new DataOutputStream(
                new BufferedOutputStream(new NonClosableOutputStream(out)));
        dataOutput.writeInt(20121019); // version number
        m_type.save(dataOutput);
        m_metaData.save(dataOutput);
        dataOutput.writeInt(m_models.length);
        for (int i = 0; i < m_models.length; i++) {
            AbstractTreeModel singleModel = m_models[i];
            try {
                singleModel.save(dataOutput);
            } catch (IOException ioe) {
                throw new IOException("Can't save tree model " + (i + 1)
                        + "/" + m_models.length, ioe);
            }
            dataOutput.writeByte((byte)0);
        }
        dataOutput.close(); // does not close the method argument stream!!!
    }

    /** Loads and returns new ensemble model, input is NOT closed afterwards.
     * @param in ...
     * @param exec ...
     * @return ...
     * @throws IOException ...
     * @throws CanceledExecutionException ... */
    public static TreeEnsembleModel load(final InputStream in, final ExecutionMonitor exec)
    throws IOException, CanceledExecutionException {
        // wrapping the argument (zip input) stream in a buffered stream
        // reduces read operation from, e.g. 42s to 2s
        DataInputStream input = new DataInputStream(
                new BufferedInputStream(new NonClosableInputStream(in)));
        int version = input.readInt();
        if (version != 20121019) {
            throw new IOException("Tree Ensemble version " + version + " not supported");
        }
        TreeType type = TreeType.load(input);
        TreeMetaData metaData = TreeMetaData.load(input);
        int nrModels = input.readInt();
        AbstractTreeModel[] models = new AbstractTreeModel[nrModels];
        boolean isRegression = metaData.isRegression();
        for (int i = 0; i < nrModels; i++) {
            AbstractTreeModel singleModel;
            try {
                singleModel = isRegression ? TreeModelRegression.load(input, metaData)
                        : TreeModelClassification.load(input, metaData);
                if (input.readByte() != 0) {
                    throw new IOException("Model not terminated by 0 byte");
                }
            } catch (IOException e) {
                throw new IOException("Can't read tree model "
                        + (i + 1) + "/" + nrModels + ": " + e.getMessage(), e);
            }
            models[i] = singleModel;
        }
        input.close(); // does not close the method argument stream!!
        return new TreeEnsembleModel(metaData, models, type);
    }

}
