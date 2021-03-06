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
 *
 * History
 *   25.10.2011 (hofer): created
 */
package org.knime.base.node.preproc.stringmanipulation.manipulator;

import java.util.Locale;

import org.apache.commons.lang3.StringUtils;


/**
 * Count specific characters in the string.
 *
 * @author Heiko Hofer
 */
public class CountCharsModifiersManipulator implements Manipulator {

    /**
     * Count specific characters in the string.
     *
     * @param str the string
     * @param chars the characters to count
     * @param modifiers modifiers like ignore case
     * @return the count
     */
    public static int countChars(final String str, final String chars,
            final String modifiers) {
        if (str == null || str.isEmpty()) {
            return 0;
        }
        String c = (null != chars) ? chars : "";
        String opt = (null != modifiers) ? modifiers.toLowerCase(Locale.ENGLISH) : "";
        boolean ignoreCase = StringUtils.contains(opt, 'i');
        boolean matchOpposite = StringUtils.contains(opt, 'v');
        int sum = 0;
        for (int i = 0; i < c.length(); i++) {
            String s = c.substring(i, i + 1);
            if (ignoreCase) {
                // search for lower case and upper case
                String lower = s.toLowerCase();
                sum += StringUtils.countMatches(str, lower);
                String upper = s.toUpperCase();
                if (!lower.equals(upper)) {
                    sum += StringUtils.countMatches(str, upper);
                }
            } else {
                sum += StringUtils.countMatches(str, s);
            }
        }
        return matchOpposite ? str.length() - sum : sum;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return "countChars";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDisplayName() {
        return getName() + "(str, chars, modifiers)";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNrArgs() {
        return 3;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCategory() {
        return "Count";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Counts the number of specific characters in a string. "
        + "<i>modifiers</i> gives options to control the method:"
        + "<br/>"
        + "<table>"
        + "<tr><td style=\"padding: 0px 8px 0px 5px;\">i</td> "
        + "<td>ignore case</td></tr>"
        + "<tr><td style=\"padding: 0px 8px 0px 5px;\">v</td> "
        + "<td>count characters no in <i>chars</i></td></tr>"
        + "</table>"
        + ""
        + "<br/>"
        + "<strong>Examples:</strong>"
        + "<br/>"
        + "<table>"
        + "<tr><td>countChars(\"abcABCabc\", \"a\", \"\")</td>"
        + "<td>=&nbsp;2</td></tr>"

        + "<tr><td>countChars(\"abcABCabc\", \"ae\", \"\")</td>"
        + "<td>=&nbsp;2</td></tr>"

        + "<tr><td>countChars(\"abcABCabc\", \"abc\", \"\")</td>"
        + "<td>=&nbsp;6</td></tr>"

        + "<tr><td>countChars(\"abcABCabc\", \"abc\", \"i\")</td>"
        + "<td>=&nbsp;9</td></tr>"

        + "<tr><td>countChars(\"abcABCabc\", \"abc\", \"v\")</td>"
        + "<td>=&nbsp;3</td></tr>"

        + "<tr><td>countChars(\"abcABCabc\", \"abc\", \"iv\")</td>"
        + "<td>=&nbsp;0</td></tr>"

        + "<tr><td>countChars(*, \"\", *)</td>"
        + "<td>=&nbsp;0 or length of string for modifier \"v\"</td></tr>"

        + "<tr><td>countChars(*, null, *)</td>"
        + "<td>=&nbsp;0 or length of string for modifier \"v\"</td></tr>"

        + "<tr><td>countChars(\"\", *, *)</td>"
        + "<td>=&nbsp;0</td></tr>"

        + "<tr><td>countChars(null, *, *)</td>"
        + "<td>=&nbsp;0</td></tr>"

        + "</table>"
        + "* can be any character sequence.";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getReturnType() {
        return Integer.class;
    }
}
