/*
 * Copyright (C) 2020-2026 Linagora
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version, provided you comply with the Additional Terms applicable for LinID Identity Manager software by
 * LINAGORA pursuant to Section 7 of the GNU Affero General Public License, subsections (b), (c), and (e), pursuant to
 * which these Appropriate Legal Notices must notably (i) retain the display of the "LinID™" trademark/logo at the top
 * of the interface window, the display of the “You are using the Open Source and free version of LinID™, powered by
 * Linagora © 2009–2013. Contribute to LinID R&D by subscribing to an Enterprise offer!” infobox and in the e-mails
 * sent with the Program, notice appended to any type of outbound messages (e.g. e-mail and meeting requests) as well
 * as in the LinID Identity Manager user interface, (ii) retain all hypertext links between LinID Identity Manager
 * and https://linid.org/, as well as between LINAGORA and LINAGORA.com, and (iii) refrain from infringing LINAGORA
 * intellectual property rights over its trademarks and commercial brands. Other Additional Terms apply, see
 * <http://www.linagora.com/licenses/> for more details.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License and its applicable Additional Terms for
 * LinID Identity Manager along with this program. If not, see <http://www.gnu.org/licenses/> for the GNU Affero
 * General Public License version 3 and <http://www.linagora.com/licenses/> for the Additional Terms applicable to the
 * LinID Identity Manager software.
 */

package io.github.linagora.linid.im.sync.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Test class: SslConfig")
class SslConfigTest {

    private static final String TRUST_STORE = "javax.net.ssl.trustStore";

    private static final String TRUST_STORE_PASSWORD = "javax.net.ssl.trustStorePassword";

    private final SslConfig config = new SslConfig();

    @TempDir
    Path tempDir;

    private String previousTrustStore;

    private String previousTrustStorePassword;

    @BeforeEach
    void setUp() {
        previousTrustStore = System.clearProperty(TRUST_STORE);
        previousTrustStorePassword = System.clearProperty(TRUST_STORE_PASSWORD);
        ReflectionTestUtils.setField(config, "resourceLoader", new DefaultResourceLoader());
    }

    @AfterEach
    void tearDown() {
        restore(TRUST_STORE, previousTrustStore);
        restore(TRUST_STORE_PASSWORD, previousTrustStorePassword);
    }

    @Test
    @DisplayName("configureSsl should set the trust store and its password when the trust store exists")
    void testConfigureSsl_existingTrustStore() throws IOException {
        var trustStore = Files.createFile(tempDir.resolve("truststore.jks"));

        configure("file:" + trustStore, "secret");

        assertEquals(trustStore.toAbsolutePath().toString(), System.getProperty(TRUST_STORE));
        assertEquals("secret", System.getProperty(TRUST_STORE_PASSWORD));
    }

    @Test
    @DisplayName("configureSsl should set neither the trust store nor its password when the trust store is missing")
    void testConfigureSsl_missingTrustStore() throws IOException {
        configure("file:" + tempDir.resolve("missing.jks"), "secret");

        assertNull(System.getProperty(TRUST_STORE));
        assertNull(System.getProperty(TRUST_STORE_PASSWORD));
    }

    @Test
    @DisplayName("configureSsl should set nothing when the trust store path and password are empty")
    void testConfigureSsl_emptySettings() throws IOException {
        configure("", "");

        assertNull(System.getProperty(TRUST_STORE));
        assertNull(System.getProperty(TRUST_STORE_PASSWORD));
    }

    private void configure(final String trustStorePath, final String trustStorePassword) throws IOException {
        ReflectionTestUtils.setField(config, "trustStorePath", trustStorePath);
        ReflectionTestUtils.setField(config, "trustStorePassword", trustStorePassword);
        config.configureSsl();
    }

    private static void restore(final String key, final String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }
}
