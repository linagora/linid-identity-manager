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

package io.github.linagora.linid.im.api.controller;

import io.github.linagora.linid.im.corelib.i18n.I18nService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing endpoints for accessing internationalization (i18n)
 * data.
 *
 * <p>
 * Provides endpoints to retrieve available languages and their associated
 * translations.
 */
@RestController
@RequestMapping("/i18n")
@RequiredArgsConstructor
@Tag(name = "I18n", description = "Internationalization endpoints for retrieving available languages and translations")
public class I18nController {

    /**
     * Service providing access to internationalized messages.
     */
    private final I18nService i18nService;

    /**
     * Retrieves the list of available language codes (e.g., "en", "fr").
     *
     * @return a {@link ResponseEntity} containing the list of languages.
     */
    @GetMapping("/languages")
    @Operation(summary = "Get available languages", description = "Retrieves the list of available language codes.")
    @ApiResponse(responseCode = "200", description = "List of available language codes",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = List.class)))
    public ResponseEntity<List<String>> getLanguages() {
        return ResponseEntity.ok(i18nService.getLanguages());
    }

    /**
     * Retrieves the translation key-value pairs for a given language.
     *
     * @param language the language code (e.g., "en", "fr")
     * @return a {@link ResponseEntity} containing the translation map for the
     *         specified language.
     */
    @GetMapping("/{lang}.json")
    @Operation(summary = "Get translation file", description = "Retrieves the translation key-value pairs.")
    @ApiResponse(responseCode = "200", description = "Translation map for the specified language",
        content = @Content(mediaType = "application/json", schema = @Schema(implementation = Map.class)))
    public ResponseEntity<Map<String, String>> getTranslationFile(
            final @Parameter(description = "Language code", required = true) @PathVariable("lang") String language) {
        return ResponseEntity.ok(i18nService.getTranslations(language));
    }
}
