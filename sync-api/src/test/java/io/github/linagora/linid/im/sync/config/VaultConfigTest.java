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

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.configuration.CompatibilityVerifierAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

/**
 * Starts the configuration of application.yaml with Vault enabled against a stub of OpenBao, with the compatibility
 * check of Spring Cloud: catches a break between the versions of Spring Cloud and Spring Boot.
 */
@DisplayName("Test class: Vault configuration")
class VaultConfigTest {

  private HttpServer openBao;

  @BeforeEach
  void startOpenBao() throws IOException {
    openBao = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
    Map<String, String> secrets = Map.of(
        "/v1/secret/data/linid-sync-api", "{\"DATABASE_PASSWORD\":\"from-vault\"}",
        "/v1/secret/data/linid", "{\"SHARED_SECRET\":\"shared\"}");
    openBao.createContext("/", exchange -> {
      String path = exchange.getRequestURI().getPath();
      String data = secrets.get(path);
      String json;
      if (path.startsWith("/v1/sys/internal/ui/mounts/secret/")) {
        // Mount of the secret, read first to know the version of the KV engine
        json = "{\"data\":{\"path\":\"secret/\",\"type\":\"kv\",\"options\":{\"version\":\"2\"}}}";
      } else if (data != null) {
        json = "{\"data\":{\"data\":" + data + ",\"metadata\":{}}}";
      } else {
        json = null;
      }
      byte[] body = (json == null ? "{\"errors\":[]}" : json).getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "application/json");
      exchange.sendResponseHeaders(json == null ? 404 : 200, body.length);
      exchange.getResponseBody().write(body);
      exchange.close();
    });
    openBao.start();
  }

  @AfterEach
  void stopOpenBao() {
    openBao.stop(0);
  }

  @Test
  @DisplayName("Test startup: should read the secrets of OpenBao with the compatibility check of Spring Cloud")
  void testStartupWithVault(@TempDir Path directory) throws IOException {
    // The external configuration is required by application.yaml
    Path externalConfiguration = Files.createFile(directory.resolve("config.yaml"));
    try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TestConfiguration.class)
        .web(WebApplicationType.NONE)
        .properties(
            "EXTERNAL_CONFIGURATION=" + externalConfiguration,
            "VAULT_ENABLED=true",
            "VAULT_URI=http://localhost:" + openBao.getAddress().getPort(),
            "VAULT_TOKEN=test-token")
        .run()) {
      assertEquals("from-vault", context.getEnvironment().getProperty("DATABASE_PASSWORD"));
      assertEquals("shared", context.getEnvironment().getProperty("SHARED_SECRET"));
    }
  }

  @Configuration
  @ImportAutoConfiguration(CompatibilityVerifierAutoConfiguration.class)
  static class TestConfiguration {
  }
}
