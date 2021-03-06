/*
 * ------------------------------------------------------------------------
 *
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
 * ---------------------------------------------------------------------
 *
 * History
 *   25.01.2017 (Adrian Nembach): created
 */
package org.knime.base.node.mine.regression.logistic.learner4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.knime.base.node.mine.regression.logistic.learner4.LogRegLearnerSettings.Prior;
import org.knime.base.node.mine.regression.logistic.learner4.LogRegLearnerSettings.Solver;
import org.knime.base.node.mine.regression.logistic.learner4.data.ClassificationTrainingRow;
import org.knime.base.node.mine.regression.logistic.learner4.data.DataTableTrainingData;
import org.knime.base.node.mine.regression.logistic.learner4.data.InMemoryData;
import org.knime.base.node.mine.regression.logistic.learner4.data.SparseClassificationTrainingRowBuilder;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingData;
import org.knime.base.node.mine.regression.logistic.learner4.data.TrainingRowBuilder;
import org.knime.base.node.mine.regression.logistic.learner4.sg.SagLogRegLearner;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomain;
import org.knime.core.data.DataColumnProperties;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableDomainCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DomainCreatorColumnSelection;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObjectSpecCreator;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.util.filter.NameFilterConfiguration.FilterResult;

/**
 * An object that coordinates the learning of a logistic regression model.
 *
 * It serves as an interface between the KNIME node and the implementation of the logistic regression learning mechanisms.
 * Largely based on the old LogRegLearner class but renamed because the naming was confusing.
 *
 * @author Adrian Nembach, KNIME.com
 */
class LogRegCoordinator {

    private final LogRegLearnerSettings m_settings;
    private String m_warning;
    private PMMLPortObjectSpec m_pmmlOutSpec;
    private List<DataColumnSpec> m_specialColumns;

    LogRegCoordinator(final DataTableSpec tableSpec, final LogRegLearnerSettings settings) throws InvalidSettingsException {
        m_settings = settings;
        init(tableSpec, Collections.<String> emptySet());
    }

    PortObjectSpec[] getOutputSpecs() {
        DataTableSpec coeffTableOutSpec = createCoeffStatisticsTableSpec(m_settings.isCalcCovMatrix());
        DataTableSpec modelStatsTableOutSpec = LogisticRegressionContent.createModelStatisticsTableSpec();
        return new PortObjectSpec[]{m_pmmlOutSpec, coeffTableOutSpec, modelStatsTableOutSpec};
    }

    static DataTableSpec createCoeffStatisticsTableSpec(final boolean covMatPresent) {
        DataTableSpecCreator tableSpecCreator = new DataTableSpecCreator();
        tableSpecCreator.addColumns(new DataColumnSpec[] {
            new DataColumnSpecCreator("Logit", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Variable", StringCell.TYPE).createSpec(),
            new DataColumnSpecCreator("Coeff.", DoubleCell.TYPE).createSpec()
        });

        if (!covMatPresent) {
            tableSpecCreator.setName("Coefficients");
        } else {
            tableSpecCreator.setName("Coefficients and Statistics");
            tableSpecCreator.addColumns(new DataColumnSpec[] {
                new DataColumnSpecCreator("Std. Err.", DoubleCell.TYPE).createSpec(),
                new DataColumnSpecCreator("z-score", DoubleCell.TYPE).createSpec(),
                new DataColumnSpecCreator("P>|z|", DoubleCell.TYPE).createSpec()
            });
        }
        return tableSpecCreator.createSpec();
    }

    /**
     * Returns a concatenation of all warnings that occurred during the training process.
     * @return the warning message to be displayed by the KNIME node.
     */
    String getWarningMessage() {
        return m_warning;
    }

    /**
     * Performs the learning task by creating the appropriate LogRegLearner and all other objects
     * necessary for a successful training.
     *
     * @param trainingData a DataTable that contains the data on which to learn the logistic regression model
     * @param exec the execution context of the corresponding KNIME node
     * @return the content of the logistic regression model
     * @throws InvalidSettingsException if the settings cause inconsistencies during training
     * @throws CanceledExecutionException if the training is canceled
     */
    LogisticRegressionContent learn(final BufferedDataTable trainingData, final ExecutionContext exec) throws InvalidSettingsException, CanceledExecutionException {
        CheckUtils.checkArgument(trainingData.size() > 0,
                "The input table is empty. Please provide data to learn on.");
        CheckUtils.checkArgument(trainingData.size() <= Integer.MAX_VALUE,
                "The input table contains too many rows.");
        LogRegLearner learner;
        if (m_settings.getSolver() == Solver.IRLS) {
            learner = new IrlsLearner(m_settings.getMaxEpoch(), m_settings.getEpsilon(), m_settings.isCalcCovMatrix());
        } else {
            learner = new SagLogRegLearner(m_settings);
        }
        double calcDomainTime = 1.0 / (5.0 * 2.0 + 1.0);
        exec.setMessage("Analyzing categorical data");
        BufferedDataTable dataTable =
            recalcDomainForTargetAndLearningFields(trainingData, exec.createSubExecutionContext(calcDomainTime));
        checkConstantLearningFields(dataTable);
        exec.setMessage("Building logistic regression model");
        ExecutionMonitor trainExec = exec.createSubProgress(1.0 - calcDomainTime);
        LogRegLearnerResult result;
        TrainingRowBuilder<ClassificationTrainingRow> rowBuilder = new SparseClassificationTrainingRowBuilder(dataTable, m_pmmlOutSpec,
            m_settings.getTargetReferenceCategory(), m_settings.getSortTargetCategories(), m_settings.getSortIncludesCategories());
        TrainingData<ClassificationTrainingRow> data;
        Long seed = m_settings.getSeed();
        if (m_settings.isInMemory()) {
            data = new InMemoryData<ClassificationTrainingRow>(dataTable, seed, rowBuilder);
        } else {
            data = new DataTableTrainingData<ClassificationTrainingRow>(trainingData, seed,
                    rowBuilder, m_settings.getChunkSize(), exec.createSilentSubExecutionContext(0.0));
        }
        checkShapeCompatibility(data);
        result = learner.learn(data, trainExec);

        LogisticRegressionContent content = createContentFromLearnerResult(result, rowBuilder, trainingData.getDataTableSpec());

        addToWarning(learner.getWarningMessage());
        return content;
    }

    private void addToWarning(final String warningMsg) {
        if (warningMsg == null) {
            return;
        }
        StringBuilder sb;
        if (m_warning == null) {
            sb = new StringBuilder();
        } else {
            sb = new StringBuilder(m_warning).append("\n");
        }
        sb.append(warningMsg);
        m_warning = sb.toString();
    }

    private void checkShapeCompatibility(final TrainingData<?> data) throws InvalidSettingsException {
        boolean moreFeaturesThanRows = data.getFeatureCount() > data.getRowCount();
        if (moreFeaturesThanRows) {
            switch (m_settings.getSolver()) {
                case IRLS:
                    throw new IllegalArgumentException("The data contains more features than rows."
                        + " The IRLS solver can't deal with this shape, please use the SAG solver with"
                        + " regularization instead.");
                case SAG:
                    if (m_settings.getPrior() == Prior.Uniform) {
                        addToWarning("The data contains more features than rows. In this case it is recommended to"
                            + " use an informative prior (other than Uniform).");
                    }
                    break;
                default:
                    throw new InvalidSettingsException("The provided solver is unknown.");
            }
        }
    }

    /** Initialize instance and check if settings are consistent. */
    private void init(final DataTableSpec inSpec, final Set<String> exclude) throws InvalidSettingsException {

        List<String> inputCols = new ArrayList<String>();
        FilterResult includedColumns = m_settings.getIncludedColumns().applyTo(inSpec);
        for (String column : includedColumns.getIncludes()) {
            inputCols.add(column);
        }
        inputCols.remove(m_settings.getTargetColumn());
        if (inputCols.isEmpty()) {
            throw new InvalidSettingsException("At least one column must " + "be included.");
        }

        DataColumnSpec targetColSpec = null;
        List<DataColumnSpec> regressorColSpecs = new ArrayList<DataColumnSpec>();

        // Auto configuration when target is not set
        if (null == m_settings.getTargetColumn()
            && m_settings.getIncludedColumns().applyTo(inSpec).getExcludes().length == 0) {
            for (int i = 0; i < inSpec.getNumColumns(); i++) {
                DataColumnSpec colSpec = inSpec.getColumnSpec(i);
                String colName = colSpec.getName();
                inputCols.remove(colName);
                if (colSpec.getType().isCompatible(NominalValue.class)) {
                    m_settings.setTargetColumn(colName);
                }
            }
            // when there is no column with nominal data
            if (null == m_settings.getTargetColumn()) {
                throw new InvalidSettingsException("No column in " + "spec compatible to \"NominalValue\".");
            }
        }

        // remove all columns that should not be used
        inputCols.removeAll(exclude);

        m_specialColumns = new LinkedList<>();
        for (int i = 0; i < inSpec.getNumColumns(); i++) {
            DataColumnSpec colSpec = inSpec.getColumnSpec(i);
            String colName = colSpec.getName();
            final DataType type = colSpec.getType();
            if (m_settings.getTargetColumn().equals(colName)) {
                if (type.isCompatible(NominalValue.class)) {
                    targetColSpec = colSpec;
                } else {
                    throw new InvalidSettingsException("Type of column \"" + colName + "\" is not nominal.");
                }
            } else if (inputCols.contains(colName)) {
                if (type.isCompatible(DoubleValue.class) || type.isCompatible(NominalValue.class)) {
                    regressorColSpecs.add(colSpec);
                } else if (type.isCompatible(BitVectorValue.class) || type.isCompatible(ByteVectorValue.class)
                    || (type.isCollectionType() && type.getCollectionElementType().isCompatible(DoubleValue.class))) {
                    m_specialColumns.add(colSpec);
                    //We change the table spec later to encode it as a string.
                    regressorColSpecs.add(new DataColumnSpecCreator(colSpec.getName(), StringCell.TYPE).createSpec());
                } else {
                    throw new InvalidSettingsException("Type of column \"" + colName
                        + "\" is not one of the allowed types, " + "which are numeric or nomial.");
                }
            }
        }

        if (null != targetColSpec) {
            // Check if target has at least two categories.
            final Set<DataCell> targetValues = targetColSpec.getDomain().getValues();
            if (targetValues != null && targetValues.size() < 2) {
                throw new InvalidSettingsException("The target column \"" + targetColSpec.getName()
                    + "\" has one value, only. " + "At least two target categories are expected.");
            }

            String[] learnerCols = new String[regressorColSpecs.size() + 1];
            for (int i = 0; i < regressorColSpecs.size(); i++) {
                learnerCols[i] = regressorColSpecs.get(i).getName();
            }
            learnerCols[learnerCols.length - 1] = targetColSpec.getName();
            final DataColumnSpec[] updatedSpecs = new DataColumnSpec[inSpec.getNumColumns()];
            for (int i = updatedSpecs.length; i-- > 0;) {
                final DataColumnSpec columnSpec = inSpec.getColumnSpec(i);
                final DataType type = columnSpec.getType();
                if (type.isCompatible(BitVectorValue.class) || type.isCompatible(ByteVectorValue.class)) {
                    final DataColumnSpecCreator colSpecCreator =
                        new DataColumnSpecCreator(columnSpec.getName(), StringCell.TYPE);
                    colSpecCreator.setProperties(new DataColumnProperties(Collections.singletonMap("realType",
                        type.isCompatible(BitVectorValue.class) ? "BitVector" : "ByteVector")));
                    updatedSpecs[i] = colSpecCreator.createSpec();
                } else {
                    updatedSpecs[i] = columnSpec;
                }
            }
            DataTableSpec updated = new DataTableSpec(updatedSpecs);
            PMMLPortObjectSpecCreator creator = new PMMLPortObjectSpecCreator(updated);
            creator.setTargetCols(Arrays.asList(targetColSpec));
            creator.setLearningCols(regressorColSpecs);
            //            creator.addPreprocColNames(m_specialColumns.stream().flatMap(spec -> ));
            m_pmmlOutSpec = creator.createSpec();
        } else {
            throw new InvalidSettingsException("The target is " + "not in the input.");
        }
    }

    private LogisticRegressionContent createContentFromLearnerResult(final LogRegLearnerResult result, final TrainingRowBuilder<ClassificationTrainingRow> rowBuilder,
        final DataTableSpec tableSpec) {
        List<String> factorList = new ArrayList<String>();
        List<String> covariateList = new ArrayList<String>();
        Map<String, List<DataCell>> factorDomainValues =
            new HashMap<String, List<DataCell>>();
        Map<Integer, List<DataCell>> nominalDomainValues = rowBuilder.getNominalDomainValues();
        Map<Integer, Integer> vectorLengths = rowBuilder.getVectorLengths();

        for (int i : rowBuilder.getLearningColumns()) {
            DataColumnSpec columnSpec = tableSpec.getColumnSpec(i);
            if (nominalDomainValues.containsKey(i)) {
                String factor =
                    columnSpec.getName();
                factorList.add(factor);
                List<DataCell> values = nominalDomainValues.get(i);
                factorDomainValues.put(factor, values);
            } else {
                if (columnSpec.getType().isCompatible(BitVectorValue.class) || columnSpec.getType().isCompatible(ByteVectorValue.class) ) {
                    int length = vectorLengths.getOrDefault(i, 0).intValue();
                    for (int j = 0; j < length; ++j) {
                        covariateList.add(columnSpec.getName() + "[" + j + "]");
                    }
                } else {
                    covariateList.add(
                        columnSpec.getName());
                }
            }
        }

        final Map<String, Integer> specialVectorLengths = new LinkedHashMap<String, Integer>();
        for (DataColumnSpec spec: m_specialColumns) {
            int colIndex = tableSpec.findColumnIndex(spec.getName());
            if (colIndex >= 0) {
                specialVectorLengths.put(spec.getName(), vectorLengths.get(colIndex));
            }
        }
        RealMatrix beta = result.getBeta();
        int cols = beta.getColumnDimension();
        RealMatrix betaMat = MatrixUtils.createRealMatrix(1, beta.getRowDimension() * cols);
        for (int i = 0; i < beta.getRowDimension(); i++) {
            for (int j = 0; j < beta.getColumnDimension(); j++) {
                betaMat.setEntry(0, i * cols + j, beta.getEntry(i, j));
            }
        }
        RealMatrix covMat = null;
        if (result.hasCovariateMatrix()) {
            covMat = result.getCovariateMatrix();
        }
        // create content
        LogisticRegressionContent content =
            new LogisticRegressionContent(m_pmmlOutSpec,
                factorList, covariateList, specialVectorLengths,
                m_settings.getTargetReferenceCategory(), m_settings.getSortTargetCategories(), m_settings.getSortIncludesCategories(),
                betaMat, result.getLogLike(), covMat, result.getIter());
        return content;

    }

    private void checkConstantLearningFields(final BufferedDataTable data) throws InvalidSettingsException {
        Set<String> exclude = new HashSet<String>();
        for (DataColumnSpec colSpec : m_pmmlOutSpec.getLearningCols()) {
            if (colSpec.getType().isCompatible(DoubleValue.class)) {
                final DataColumnDomain domain = colSpec.getDomain();
                final DataCell lowerBound = domain.getLowerBound();
                final DataCell upperBound = domain.getUpperBound();
                assert lowerBound != null || data.size() == 0 : "Non empty table must have domain set at this point";
                if (java.util.Objects.equals(lowerBound, upperBound)) {
                    exclude.add(colSpec.getName());
                }
            }
        }
        //TODO check constant values in certain positions of vector columns.
        //Maybe in recalcDomainForTargetAndLearningFields.
        if (!exclude.isEmpty()) {
            StringBuilder warning = new StringBuilder();
            warning.append(exclude.size() == 1 ? "Column " : "Columns ");
            warning.append(ConvenienceMethods.getShortStringFrom(exclude, 5));
            warning.append(exclude.size() == 1 ? " has a constant value " : " have constant values ");
            warning.append(" - will be ignored during training");
//            LOGGER.warn(warning.toString());
            m_warning = (m_warning == null ? "" : m_warning + "\n") + warning.toString();
            // re-init learner so that it has the correct learning columns
            init(data.getDataTableSpec(), exclude);
        }
    }

    private BufferedDataTable recalcDomainForTargetAndLearningFields(final BufferedDataTable data,
        final ExecutionContext exec) throws InvalidSettingsException, CanceledExecutionException {
        final String targetCol = m_pmmlOutSpec.getTargetFields().get(0);
        DataTableDomainCreator domainCreator =
            new DataTableDomainCreator(data.getDataTableSpec(), new DomainCreatorColumnSelection() {

                @Override
                public boolean dropDomain(final DataColumnSpec colSpec) {
                    return false;
                }

                @Override
                public boolean createDomain(final DataColumnSpec colSpec) {
                    return colSpec.getName().equals(targetCol) || (colSpec.getType().isCompatible(NominalValue.class)
                        && m_pmmlOutSpec.getLearningFields().contains(colSpec.getName()));
                }
            }, new DomainCreatorColumnSelection() {

                @Override
                public boolean dropDomain(final DataColumnSpec colSpec) {
                    // drop domain of numeric learning fields so that we can check for constant columns
                    return colSpec.getType().isCompatible(DoubleValue.class)
                        && m_pmmlOutSpec.getLearningFields().contains(colSpec.getName());
                }

                @Override
                public boolean createDomain(final DataColumnSpec colSpec) {
                    return colSpec.getType().isCompatible(DoubleValue.class)
                        && m_pmmlOutSpec.getLearningFields().contains(colSpec.getName());
                }
            });
        domainCreator.updateDomain(data, exec);

        DataTableSpec spec = domainCreator.createSpec();
        CheckUtils
            .checkSetting(spec.getColumnSpec(targetCol).getDomain().hasValues(),
                "Target column '%s' has too many"
                    + " unique values - consider to use domain calucator node before to enforce calculation",
            targetCol);
        BufferedDataTable newDataTable = exec.createSpecReplacerTable(data, spec);
        // bug fix 5580 - ignore columns with too many different values
        Set<String> columnWithTooManyDomainValues = new LinkedHashSet<>();
        for (String learningField : m_pmmlOutSpec.getLearningFields()) {
            DataColumnSpec columnSpec = spec.getColumnSpec(learningField);
            if (columnSpec.getType().isCompatible(NominalValue.class) && !columnSpec.getDomain().hasValues()) {
                columnWithTooManyDomainValues.add(learningField);
            }
        }
        if (!columnWithTooManyDomainValues.isEmpty()) {
            StringBuilder warning = new StringBuilder();
            warning.append(columnWithTooManyDomainValues.size() == 1 ? "Column " : "Columns ");
            warning.append(ConvenienceMethods.getShortStringFrom(columnWithTooManyDomainValues, 5));
            warning.append(columnWithTooManyDomainValues.size() == 1 ? " has " : " have ");
            warning.append("too many different values - will be ignored during training ");
            warning.append("(enforce inclusion by using a domain calculator node before)");
//            LOGGER.warn(warning.toString());
            m_warning = (m_warning == null ? "" : m_warning + "\n") + warning.toString();
        }
        // initialize m_learner so that it has the correct DataTableSpec of the input
        init(newDataTable.getDataTableSpec(), columnWithTooManyDomainValues);
        return newDataTable;
    }



}
