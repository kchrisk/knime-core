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
package org.knime.core.node.defaultnodesettings;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.port.PortObjectSpec;

/**
 *
 * @author Lara Gorini
 * @since 3.2
 */
public class SettingsModelAuthentication extends SettingsModel {

    private String m_username;

    private String m_password;

    private String m_credential;

    private Type m_type;

    private final String m_configName;

    private final static String secretKey = "DasAlsoWarDesPudelsKern!VonGoethe";

    private final static String CREDENTIAL = "credential";

    private final static String PASSWORD = "password";

    private final static String USERNAME = "username";

    private final static String SELECTED_TYPE = "selectedType";

    /**
     *
     * @author Lara Gorini
     */
    public enum Type {
            /**
             * Authentication with username and password.
             */
        USER_PWD, /**
                   * Authentication with workflow credentials.
                   */
        CREDENTIALS;
    }

    /**
     * @param configName The identifier the values are stored with in the {@link org.knime.core.node.NodeSettings} object.
     * @param defaultUsername Initial username.
     * @param defaultPassword Initial password.
     * @param defaultCredential Initial credential name.
     */
    public SettingsModelAuthentication(final String configName, final String defaultUsername,
        final String defaultPassword, final String defaultCredential) {
        if ((configName == null) || "".equals(configName)) {
            throw new IllegalArgumentException("The configName must be a " + "non-empty string");
        }

        m_username = defaultUsername == null ? "" : defaultUsername;
        m_password = defaultPassword == null ? "" : defaultPassword;

        m_credential = defaultCredential;
        m_configName = configName;

        m_type = Type.USER_PWD;
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    protected SettingsModelAuthentication createClone() {
        return new SettingsModelAuthentication(m_configName, m_username, m_password, m_credential);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected String getModelTypeID() {
        return "SMID_authentication";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConfigName() {
        return m_configName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        Config config = settings.getConfig(m_configName);
        config.getString(CREDENTIAL);
        config.getString(USERNAME);
        config.getPassword(PASSWORD, secretKey);
        config.getString(SELECTED_TYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        // no default value, throw an exception instead
        Config config = settings.getConfig(m_configName);
        setValues(config.getString(CREDENTIAL), Type.valueOf(config.getString(SELECTED_TYPE)),
            config.getString(USERNAME), config.getPassword(PASSWORD, secretKey));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        // use the current value, if no value is stored in the settings
        final Config config;
        try {
            config = settings.getConfig(m_configName);
            setValues(config.getString(CREDENTIAL, m_credential),
                Type.valueOf(config.getString(SELECTED_TYPE, m_type.name())), config.getString(USERNAME, m_username),
                config.getPassword(PASSWORD, secretKey, m_password));
        } catch (InvalidSettingsException ex) {
            throw new NotConfigurableException(ex.getMessage());
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        Config config = settings.addConfig(m_configName);
        config.addString(CREDENTIAL, m_credential);
        config.addString(USERNAME, m_username);
        config.addPassword(PASSWORD, secretKey, m_password);
        config.addString(SELECTED_TYPE, m_type.name());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + m_configName + "')";
    }

    /**
     * @param selectedCredential Credential that is selected.
     * @param type Type of authentication that is selected.
     * @param userName Given username.
     * @param pwd Given password.
     */
    public void setValues(final String selectedCredential, final Type type, final String userName, final String pwd) {
        boolean changed = false;
        changed = setType(type) || changed;
        changed = setCredential(selectedCredential) || changed;
        changed = setUsername(userName) || changed;
        changed = setPassword(pwd) || changed;
        if (changed) {
            notifyChangeListeners();
        }
    }

    private boolean setType(final Type type) {
        boolean sameValue = type.name().equals(m_type.name());
        m_type = type;
        return !sameValue;
    }

    private boolean setCredential(final String newValue) {

        boolean sameValue;
        if (newValue == null) {
            sameValue = (m_credential == null);
        } else {
            sameValue = newValue.equals(m_credential);
        }
        m_credential = newValue;
        return !sameValue;
    }

    private boolean setUsername(final String newValue) {
        boolean sameValue;
        if (newValue == null) {
            sameValue = (m_username == null);
        } else {
            sameValue = newValue.equals(m_username);
        }
        m_username = newValue;
        return !sameValue;
    }

    private boolean setPassword(final String pwd) {
        boolean sameValue;
        if (pwd == null) {
            sameValue = (m_password == null);
        } else {
            sameValue = pwd.equals(m_password);
        }
        m_password = pwd;
        return !sameValue;
    }

    /**
     * @return credential name.
     */
    public String getCredential() {
        return m_credential;
    }

    /**
     * @return username.
     */
    public String getUsername() {
        return m_username;
    }

    /**
     * @return password.
     */
    public String getPassword() {
        return m_password;
    }

    /**
     * @return selected type.
     */
    public Type getSelectedType() {
        return m_type;
    }

}
