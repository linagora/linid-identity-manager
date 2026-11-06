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

package io.github.linagora.linid.im.sync.controller;

import io.github.linagora.linid.im.sync.model.cycle.CycleStatusDTO;
import io.github.linagora.linid.im.sync.service.CycleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller of the status of the synchronization cycle, called by linid-api only.
 */
@RestController
@RequestMapping("/cycle")
@Tag(name = "Cycle", description = "Status of the synchronization cycle")
public class CycleController {

    /**
     * Service reading the status of the cycle.
     */
    private final CycleService cycleService;

    /**
     * Creates the controller of the status of the cycle.
     *
     * @param cycleService the service reading the status of the cycle
     */
    public CycleController(final CycleService cycleService) {
        this.cycleService = cycleService;
    }

    /**
     * Gives the status of the synchronization cycle, so that the recompute buttons can be blocked while it runs.
     *
     * <p>Responses: {@code 200} with a {@link CycleStatusDTO} whose {@code running} is {@code true} while the cycle
     * lock of the global job is held, by any instance.</p>
     *
     * @return {@code 200} with the status of the cycle
     */
    @GetMapping
    @Operation(summary = "Get the status of the synchronization cycle")
    @ApiResponse(responseCode = "200", description = "Status of the cycle")
    public ResponseEntity<CycleStatusDTO> getStatus() {
        return ResponseEntity.ok(cycleService.getCycleStatus());
    }
}
