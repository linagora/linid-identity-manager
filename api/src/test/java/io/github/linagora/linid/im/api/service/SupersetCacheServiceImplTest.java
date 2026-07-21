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

package io.github.linagora.linid.im.api.service;

import com.sun.net.httpserver.HttpServer;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Test class: SupersetCacheServiceImpl")
class SupersetCacheServiceImplTest {

    private HttpServer httpServer;
    private String baseUrl;
    private AtomicInteger callCount;

    @Mock
    private Cache cache;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        callCount = new AtomicInteger(0);
    }

    @AfterEach
    void tearDown() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    private void startServer(final int status, final String body) throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/api/v1/security/login", exchange -> {
            callCount.incrementAndGet();
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        httpServer.start();
        baseUrl = "http://localhost:" + httpServer.getAddress().getPort();
    }

    @Test
    @DisplayName("should return the access token when Superset login succeeds")
    void shouldReturnAccessTokenWhenLoginSucceeds() throws IOException {
        startServer(200, "{\"access_token\":\"abc123\"}");

        var service = new SupersetCacheServiceImpl(baseUrl, "admin", "admin");

        var result = service.getAccessToken();

        assertThat(result).isEqualTo("abc123");
        assertThat(callCount.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("should throw an ApiException when Superset does not return an access token")
    void shouldThrowWhenAccessTokenIsNull() throws IOException {
        startServer(200, "{}");

        var service = new SupersetCacheServiceImpl(baseUrl, "admin", "admin");

        assertThatThrownBy(service::getAccessToken)
            .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("should throw an ApiException when Superset returns no response body")
    void shouldThrowWhenResponseBodyIsNull() throws IOException {
        startServer(204, "");

        var service = new SupersetCacheServiceImpl(baseUrl, "admin", "admin");

        assertThatThrownBy(service::getAccessToken)
            .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("should cache the access token so a single login call is made across multiple invocations")
    void shouldCacheAccessTokenAcrossMultipleInvocations() throws IOException {
        startServer(200, "{\"access_token\":\"cached-token\"}");

        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(CachingConfig.class);
            context.registerBean(CacheManager.class, (Supplier<CacheManager>) ConcurrentMapCacheManager::new);
            context.registerBean(
                SupersetCacheServiceImpl.class,
                () -> new SupersetCacheServiceImpl(
                    baseUrl,
                    "admin",
                    "admin"
                )
            );
            context.refresh();

            var service = context.getBean(SupersetCacheServiceImpl.class);

            var firstCall = service.getAccessToken();
            var secondCall = service.getAccessToken();

            assertThat(firstCall).isEqualTo("cached-token");
            assertThat(secondCall).isEqualTo("cached-token");
            assertThat(callCount.get()).isEqualTo(1);
        }
    }

    /**
     * Minimal caching configuration used to obtain a real CGLIB proxy around
     * {@link SupersetCacheServiceImpl}, so that {@code @Cacheable} behavior
     * (including {@code sync = true}) can be verified end-to-end rather than
     * mocked away.
     */
    @Configuration
    @EnableCaching(proxyTargetClass = true)
    static class CachingConfig {
    }
}
