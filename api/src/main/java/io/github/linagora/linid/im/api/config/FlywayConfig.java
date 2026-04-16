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

package io.github.linagora.linid.im.api.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Flyway configuration for database migrations.
 *
 * <p>
 * Defines two Flyway beans:
 * <ul>
 *   <li>{@code flywayCore}: main application migrations on the "public" schema.</li>
 *   <li>{@code flywayClient}: client-specific migrations executed after core migrations on a dedicated schema.</li>
 * </ul>
 *
 * <p>
 * The client bean is conditional on {@code spring.flyway.client.location} and depends on {@code flywayCore}.
 */
@Configuration
@EnableConfigurationProperties(FlywayProperties.class)
public class FlywayConfig {

  /**
   * Flyway bean for core application migrations.
   *
   * @param props global Flyway properties injected by Spring Boot
   * @return Flyway instance configured for core migrations
   */
  @Bean(initMethod = "migrate")
  public Flyway flywayCore(final FlywayProperties props) {
    return Flyway.configure()
      .dataSource(
        props.getUrl(),
        props.getUser(),
        props.getPassword()
      )
      .schemas("public")
      .locations("classpath:db/migration")
      .table("flyway_schema_history_core")
      .baselineOnMigrate(props.isBaselineOnMigrate())
      .baselineVersion(props.getBaselineVersion())
      .outOfOrder(props.isOutOfOrder())
      .load();
  }

  /**
   * Flyway bean for client-specific migrations.
   *
   * <p>
   * Runs after {@link #flywayCore(FlywayProperties)}. Only created if {@code spring.flyway.client.location} is
   * defined.
   *
   * @param props    global Flyway properties injected by Spring Boot
   * @param location path to client migrations
   * @param schema   client schema name
   * @return Flyway instance configured for client migrations
   */
  @Bean(initMethod = "migrate")
  @DependsOn("flywayCore")
  public Flyway flywayClient(final FlywayProperties props,
                             final @Value("${spring.flyway.client.location}") String location,
                             final @Value("${spring.flyway.client.schema}") String schema) {
    System.out.println(location);
    return Flyway.configure()
      .dataSource(
        props.getUrl(),
        props.getUser(),
        props.getPassword()
      )
      .schemas(schema)
      .locations(location)
      .table("flyway_schema_history_client")
      .baselineOnMigrate(props.isBaselineOnMigrate())
      .outOfOrder(props.isOutOfOrder())
      .load();
  }
}
