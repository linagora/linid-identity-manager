/*
 * Copyright (C) 2020-2026 Linagora
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU Affero General
 * Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option)
 * any later version, provided you comply with the Additional Terms applicable for LinID Identity Manager software by
 * LINAGORA pursuant to Section 7 of the GNU Affero General Public License, subsections (b), (c), and (e), pursuant to
 * which these Appropriate Legal Notices must notably (i) retain the display of the "LinID™" trademark/logo at the top
 * of the interface window, the display of the "You are using the Open Source and free version of LinID™, powered by
 * Linagora © 2009–2013. Contribute to LinID R&D by subscribing to an Enterprise offer!" infobox and in the e-mails
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

package io.github.linagora.linid.im.api.controller.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import io.github.linagora.linid.im.corelib.i18n.I18nService;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

  @Mock
  private I18nService i18nService;

  @InjectMocks
  private GlobalExceptionHandler handler;

  private ListAppender<ILoggingEvent> logAppender;

  @BeforeEach
  void setUp() {
    logAppender = new ListAppender<>();
    logAppender.start();
    ((Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class)).addAppender(logAppender);
  }

  @Test
  @DisplayName("Should return structured response with correct fields")
  void testHandleApiException_shouldReturnStructuredResponse() {
    var exception = new ApiException(400, I18nMessage.of("error.key", Map.of("field", "email")));
    Mockito.when(i18nService.translate(Mockito.any())).thenReturn("Translated message");

    ResponseEntity<Map<String, Object>> response = handler.handleApiException(exception);

    assertNotNull(response);
    assertEquals(400, response.getStatusCode().value());

    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertEquals("Translated message", body.get("error"));
    assertEquals("error.key", body.get("errorKey"));
    assertEquals(Map.of("field", "email"), body.get("errorContext"));
    assertEquals(400, body.get("status"));
    assertNotNull(body.get("timestamp"));
  }

  @Test
  @DisplayName("Should include exception details in response body")
  void testHandleApiException_shouldIncludeDetails() {
    var exception = new ApiException(
        422,
        I18nMessage.of("error.validation"),
        Map.of("errors", Map.of("name", "required"))
    );
    Mockito.when(i18nService.translate(Mockito.any())).thenReturn("Validation failed");

    ResponseEntity<Map<String, Object>> response = handler.handleApiException(exception);

    Map<String, Object> body = response.getBody();
    assertNotNull(body);
    assertEquals(422, response.getStatusCode().value());
    assertEquals(Map.of("name", "required"), body.get("errors"));
  }

  @Test
  @DisplayName("Should not log when needToBeLogged is false")
  void testHandleApiException_shouldNotLogWhenDisabled() {
    var exception = new ApiException(401, I18nMessage.of("error.unauthorized"), false);
    Mockito.when(i18nService.translate(Mockito.any())).thenReturn("Unauthorized");

    handler.handleApiException(exception);

    assertTrue(logAppender.list.stream()
        .noneMatch(event -> event.getLevel() == Level.ERROR));
  }

  @Test
  @DisplayName("Should log error when needToBeLogged is true")
  void testHandleApiException_shouldLogWhenEnabled() {
    var exception = new ApiException(500, I18nMessage.of("error.internal"));
    Mockito.when(i18nService.translate(Mockito.any())).thenReturn("Internal server error");

    handler.handleApiException(exception);

    assertEquals(1, logAppender.list.stream()
        .filter(event -> event.getLevel() == Level.ERROR)
        .count());
    assertEquals("Internal server error", logAppender.list.getFirst().getMessage());
  }
}
