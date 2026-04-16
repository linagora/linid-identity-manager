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

package io.github.linagora.linid.im.api.controller.handler;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for API-specific errors.
 *
 * <p>This controller advice intercepts all {@link ApiException} instances thrown within the
 * application and returns a standardized error response containing:
 * <ul>
 *   <li>Translated error message</li>
 *   <li>Error key and context</li>
 *   <li>HTTP status code</li>
 *   <li>Timestamp</li>
 *   <li>Additional error details (if any)</li>
 * </ul>
 *
 * <p>This class relies on {@link I18nService} to translate error messages based on locale.
 */
@Slf4j
@ControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

  /**
   * Service for translating internationalized messages from internal error keys.
   */
  private final I18nService i18nService;

  /**
   * Handles all {@link ApiException} instances thrown within the application.
   *
   * <p>Generates a standardized error response containing the translated error message,
   * error key, context, HTTP status code, timestamp, and any additional details.
   *
   * <p>If the exception is marked to be logged, it will be logged as an error with the translated message.
   *
   * @param exception the {@link ApiException} to handle
   * @return a {@link ResponseEntity} containing the structured error body and status
   */
  @ExceptionHandler(ApiException.class)
  public ResponseEntity<Map<String, Object>> handleApiException(final ApiException exception) {
    String message = i18nService.translate(exception.getError());

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("error", message);
    body.put("errorKey", exception.getError().key());
    body.put("errorContext", exception.getError().context());
    body.put("status", exception.getStatusCode());
    body.put("timestamp", Instant.now().toEpochMilli());
    body.putAll(exception.getDetails());

    if (exception.isNeedToBeLogged()) {
      log.error(message, exception);
    }

    return ResponseEntity.status(exception.getStatusCode()).body(body);
  }

  /**
   * Handles validation errors thrown when request body fails {@code @Valid} constraints.
   *
   * <p>Returns a structured response with field-level error messages.
   *
   * @param exception the validation exception
   * @return a {@link ResponseEntity} containing field errors and HTTP 400 status
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidationException(
      final MethodArgumentNotValidException exception) {
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    exception.getBindingResult().getFieldErrors()
        .forEach(error -> fieldErrors.put(error.getField(), error.getDefaultMessage()));

    Map<String, Object> body = new LinkedHashMap<>();
    body.put("error", "Validation failed");
    body.put("errorKey", "error.validation");
    body.put("errorContext", fieldErrors);
    body.put("status", HttpStatus.BAD_REQUEST.value());
    body.put("timestamp", Instant.now().toEpochMilli());

    return ResponseEntity.badRequest().body(body);
  }
}
