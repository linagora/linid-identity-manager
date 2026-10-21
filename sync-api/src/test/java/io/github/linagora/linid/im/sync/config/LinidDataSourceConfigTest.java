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
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;

import jakarta.persistence.EntityManagerFactory;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

@DisplayName("Test class: LinidDataSourceConfig")
class LinidDataSourceConfigTest {

    private final LinidDataSourceConfig config = new LinidDataSourceConfig();

    @Test
    @DisplayName("linidDataSourceProperties should return empty connection settings to bind")
    void testLinidDataSourceProperties() {
        assertNotNull(config.linidDataSourceProperties());
    }

    @Test
    @DisplayName("linidDataSource should build a connection pool from the connection settings")
    void testLinidDataSource() {
        var properties = config.linidDataSourceProperties();
        properties.setUrl("jdbc:postgresql://localhost:5432/linid");
        properties.setUsername("sync_api_linid_user");
        properties.setPassword("secret");

        try (var dataSource = config.linidDataSource(properties)) {
            assertEquals("jdbc:postgresql://localhost:5432/linid", dataSource.getJdbcUrl());
            assertEquals("sync_api_linid_user", dataSource.getUsername());
            assertEquals("secret", dataSource.getPassword());
            assertEquals("org.postgresql.Driver", dataSource.getDriverClassName());
        }
    }

    @Test
    @DisplayName("linidJdbcTemplate should use the linid-api datasource")
    void testLinidJdbcTemplate() {
        var dataSource = mock(DataSource.class);

        assertSame(dataSource, config.linidJdbcTemplate(dataSource).getDataSource());
    }

    @Test
    @DisplayName("linidEntityManagerFactory should map the entities of the model package on the linid-api datasource")
    void testLinidEntityManagerFactory() {
        var dataSource = mock(DataSource.class);
        var jpaProperties = new LinidJpaProperties(Map.of("hibernate.jdbc.batch_size", "50"));

        var factory = config.linidEntityManagerFactory(dataSource, jpaProperties);

        assertSame(dataSource, factory.getDataSource());
        assertInstanceOf(HibernateJpaVendorAdapter.class, factory.getJpaVendorAdapter());
        assertEquals("linid", factory.getPersistenceUnitName());
        assertEquals(Map.of("hibernate.jdbc.batch_size", "50"), factory.getJpaPropertyMap());
    }

    @Test
    @DisplayName("linidEntityManagerFactory should scan the entities of the model package")
    void testLinidEntityManagerFactory_packagesToScan() {
        try (var construction = mockConstruction(LocalContainerEntityManagerFactoryBean.class)) {
            config.linidEntityManagerFactory(mock(DataSource.class), new LinidJpaProperties(Map.of()));

            verify(construction.constructed().getFirst()).setPackagesToScan(LinidDataSourceConfig.MODEL_PACKAGE);
        }
    }

    @Test
    @DisplayName("linidEntityManagerFactory should set no Hibernate property when the block is absent")
    void testLinidEntityManagerFactory_noProperties() {
        var factory = config.linidEntityManagerFactory(mock(DataSource.class), new LinidJpaProperties(Map.of()));

        assertTrue(factory.getJpaPropertyMap().isEmpty());
    }

    @Test
    @DisplayName("linidTransactionManager should manage the transactions of the linid-api EntityManagerFactory")
    void testLinidTransactionManager() {
        var entityManagerFactory = mock(EntityManagerFactory.class);

        assertSame(entityManagerFactory, config.linidTransactionManager(entityManagerFactory).getEntityManagerFactory());
    }
}
