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

import io.github.linagora.linid.im.sync.model.recompute.RecomputeRightsDTO;
import io.github.linagora.linid.im.sync.service.RecomputeJobService;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller of the recomputations of the rights, called by linid-api only.
 */
@Slf4j
@RestController
public class RecomputeRightsController {

    /**
     * Service starting the recomputations.
     */
    private final RecomputeJobService recomputeJobService;

    /**
     * Creates the controller of the recomputations of the rights.
     *
     * @param recomputeJobService the service starting the recomputations
     */
    public RecomputeRightsController(final RecomputeJobService recomputeJobService) {
        this.recomputeJobService = recomputeJobService;
    }

    /**
     * Starts the recomputation of the rights of an account: its payload is rebuilt, then its rights are computed on
     * the deployed applications whose pair changed, in the background.
     *
     * <p>Responses: {@code 202} as soon as the request is handled, with a {@link RecomputeRightsDTO} whose
     * {@code alreadyRunning} is {@code false} when the job is started, and {@code true} when a recomputation of the
     * account is already running, on any instance, and no job is started. {@code 503} when too many recomputations are
     * already waiting. No {@code 404}: linid-api checks that the account exists before calling this endpoint. No end
     * notification is sent.</p>
     *
     * @param id the identifier of the account
     * @return {@code 202} with whether a recomputation of the account was already running
     */
    @PostMapping("/accounts/{id}/recompute-rights")
    public ResponseEntity<RecomputeRightsDTO> recomputeAccountRights(@PathVariable final UUID id) {
        log.info("Received POST request to recompute the rights of account {}", id);
        return ResponseEntity.accepted().body(recomputeJobService.recomputeAccountRights(id));
    }

    /**
     * Starts the recomputation of the rights of all the accounts on an application: the payloads of all the accounts
     * are rebuilt, then the rights on the application of the accounts whose pair changed are computed, in the
     * background.
     *
     * <p>Responses: {@code 202} as soon as the request is handled, with a {@link RecomputeRightsDTO} whose
     * {@code alreadyRunning} is {@code false} when the job is started, and {@code true} when a recomputation of the
     * application is already running, on any instance, and no job is started. {@code 503} when too many recomputations
     * are already waiting. No {@code 404}: linid-api checks that the application exists and is deployed before calling
     * this endpoint. No end notification is sent.</p>
     *
     * @param id the identifier of the application
     * @return {@code 202} with whether a recomputation of the application was already running
     */
    @PostMapping("/applications/{id}/recompute-rights")
    public ResponseEntity<RecomputeRightsDTO> recomputeApplicationRights(@PathVariable final UUID id) {
        log.info("Received POST request to recompute the rights of application {}", id);
        return ResponseEntity.accepted().body(recomputeJobService.recomputeApplicationRights(id));
    }
}
