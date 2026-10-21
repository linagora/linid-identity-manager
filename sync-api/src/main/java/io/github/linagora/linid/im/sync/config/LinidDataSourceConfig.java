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

import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

/**
 * Configuration of the linid-api database, where {@code sync-api} reads the account data and writes the payloads and
 * the computed rights.
 *
 * <p>Every bean is declared with {@code defaultCandidate = false} and the {@value #QUALIFIER} qualifier: it is only
 * injected with this qualifier, and Spring Boot keeps creating the default datasource and transaction manager of the
 * {@code sync-api} database, used by Spring Batch. The Hibernate auto-configuration is excluded by
 * {@link io.github.linagora.linid.im.sync.LinIdSyncApiApplication}, so that no {@code EntityManagerFactory} is
 * created on the {@code sync-api} database.</p>
 *
 * <p>Hibernate never changes the schema of the linid-api database, whose migrations stay in linid-api.</p>
 */
@Configuration
@EnableConfigurationProperties(LinidJpaProperties.class)
@EnableJpaRepositories(
    basePackages = "io.github.linagora.linid.im.sync.persistence.repository",
    entityManagerFactoryRef = "linidEntityManagerFactory",
    transactionManagerRef = "linidTransactionManager")
public class LinidDataSourceConfig {

    /**
     * Qualifier of the beans of the linid-api database.
     */
    public static final String QUALIFIER = "linid";

    /**
     * Package of the entities of the linid-api database.
     */
    static final String MODEL_PACKAGE = "io.github.linagora.linid.im.sync.persistence.model";

    /**
     * Connection settings of the linid-api database, read from {@code linid.datasource}.
     *
     * @return the connection settings
     */
    @Bean(defaultCandidate = false)
    @Qualifier(QUALIFIER)
    @ConfigurationProperties("linid.datasource")
    public DataSourceProperties linidDataSourceProperties() {
        return new DataSourceProperties();
    }

    /**
     * Connection pool of the linid-api database, tuned by {@code linid.datasource.hikari}.
     *
     * @param properties the connection settings of the linid-api database
     * @return the connection pool
     */
    @Bean(defaultCandidate = false)
    @Qualifier(QUALIFIER)
    @ConfigurationProperties("linid.datasource.hikari")
    public HikariDataSource linidDataSource(final @Qualifier(QUALIFIER) DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    /**
     * {@link JdbcTemplate} of the linid-api database, whose transactions are managed by
     * {@link #linidTransactionManager(EntityManagerFactory)}.
     *
     * @param dataSource the linid-api datasource
     * @return the JDBC template
     */
    @Bean(defaultCandidate = false)
    @Qualifier(QUALIFIER)
    public JdbcTemplate linidJdbcTemplate(final @Qualifier(QUALIFIER) DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * {@code EntityManagerFactory} of the entities of the linid-api database.
     *
     * @param dataSource    the linid-api datasource
     * @param jpaProperties the Hibernate properties of the linid-api database
     * @return the factory of the {@code EntityManagerFactory}
     */
    @Bean(defaultCandidate = false)
    @Qualifier(QUALIFIER)
    public LocalContainerEntityManagerFactoryBean linidEntityManagerFactory(
        final @Qualifier(QUALIFIER) DataSource dataSource,
        final LinidJpaProperties jpaProperties) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan(MODEL_PACKAGE);
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setPersistenceUnitName(QUALIFIER);
        factory.setJpaPropertyMap(jpaProperties.properties());
        return factory;
    }

    /**
     * Transaction manager of the linid-api database. It takes the datasource of the {@code EntityManagerFactory}, so
     * it also manages the transactions of {@link #linidJdbcTemplate(DataSource)}.
     *
     * @param entityManagerFactory the {@code EntityManagerFactory} of the linid-api database
     * @return the transaction manager
     */
    @Bean(defaultCandidate = false)
    @Qualifier(QUALIFIER)
    public JpaTransactionManager linidTransactionManager(
        final @Qualifier(QUALIFIER) EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
