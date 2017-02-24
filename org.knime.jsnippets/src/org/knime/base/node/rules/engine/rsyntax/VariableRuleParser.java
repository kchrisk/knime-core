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
 * ---------------------------------------------------------------------
 *
 * Created on 2013.08.25. by Gabor Bakos
 */
package org.knime.base.node.rules.engine.rsyntax;

import org.fife.ui.rsyntaxtextarea.TokenMaker;
import org.fife.ui.rsyntaxtextarea.TokenMakerFactory;
import org.fife.ui.rsyntaxtextarea.parser.Parser;
import org.knime.base.node.rules.engine.Rule;
import org.knime.base.node.rules.engine.Rule.Operators;
import org.knime.base.node.rules.engine.RuleNodeSettings;

/**
 * {@link Parser} for the Rule Engine Variable node.
 *
 * @author Gabor Bakos
 */
public class VariableRuleParser extends AbstractRuleParser {
    /** The syntax style key for Rule Engine variable. */
    public static String SYNTAX_STYLE_VARIABLE_RULE = SYNTAX_STYLE_RULE + "+variable";

    /**
     * Language support class for the rule engine variable language.
     *
     * @author Gabor Bakos
     * @since 2.9
     */
    public static class RuleLanguageSupport extends AbstractRuleParser.AbstractRuleLanguageSupport<VariableRuleParser> {
        /**
         * Constructs {@link RuleLanguageSupport}.
         */
        public RuleLanguageSupport() {
            super(VariableRuleParser.SYNTAX_STYLE_VARIABLE_RULE, RuleLanguageSupport.class, KnimeTokenMaker.class);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        protected VariableRuleParser createParser() {
            return new VariableRuleParser(true);
        }
    }

    /**
     * Wraps a {@link TokenMaker} and makes the {@link Operators} {@link Operators#toString()} a keyword.
     *
     * @author Gabor Bakos
     * @since 2.9
     */
    public static class KnimeTokenMaker extends AbstractRuleParser.WrappedTokenMaker {

        /**
         * Constructs a {@link Rule} token maker based on the Java {@link TokenMaker}.
         */
        public KnimeTokenMaker() {
            super(TokenMakerFactory.getDefaultInstance().getTokenMaker("text/java"), new VariableRuleParser(true)
                .getOperators());
        }
    }

    /**
     * Creates the default instance.
     */
    public VariableRuleParser() {
        this(true);
    }

    /**
     * @param warnOnColRefsInStrings Warns when there is a possible reference in a {@link String}.
     */
    public VariableRuleParser(final boolean warnOnColRefsInStrings) {
        super(warnOnColRefsInStrings, RuleNodeSettings.VariableRule);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean isNotApplicable(final String style) {
        return !style.equals(SYNTAX_STYLE_VARIABLE_RULE);
    }
}