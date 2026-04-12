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

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

/** SSL configuration class that sets up the Java system properties for the SSL trust store. */
@Slf4j
@Configuration
public class SslConfig {

  /** Path to the SSL trust store file, injected from application properties. */
  @Value("${server.ssl.truststore.path}")
  private String trustStorePath;

  /** Password for the SSL trust store, injected from application properties. */
  @Value("${server.ssl.truststore.password}")
  private String trustStorePassword;

  /** ResourceLoader to load the trust store file from the specified path. */
  @Autowired private ResourceLoader resourceLoader;

  /** Configures the SSL trust store. */
  @PostConstruct
  public void configureSsl() throws IOException {
    if (!StringUtils.isBlank(trustStorePath)) {
      Resource resource = resourceLoader.getResource(trustStorePath);
      if (!resource.exists()) {
        log.warn(
            "SSL truststore not found at '{}'. "
                + "HTTPS connections to external services may fail.",
            trustStorePath);
        return;
      }
      System.setProperty("javax.net.ssl.trustStore", resource.getFile().getAbsolutePath());
    }

    if (!StringUtils.isBlank(trustStorePassword)) {
      System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
    }
  }
}
