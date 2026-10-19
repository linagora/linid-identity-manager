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

package io.github.linagora.linid.im.sync.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

/**
 * Centralized alert logging utility.
 *
 * <p>Blocking errors (the step cannot proceed) are logged at {@code ERROR} level.
 * Non-blocking errors (the step continues with a fallback) are logged at {@code WARN} level.</p>
 */
@Slf4j
public final class AlertService {

    private AlertService() {
    }

    // ── Blocking ───────────────────────────────────────────────────────────

    /**
     * Logs an invalid {@code mapping} template of the payload, such as a Jinjava syntax error or a value inserted
     * without {@code tojson}: the payload build is aborted before any read.
     *
     * @param context the entities in error
     * @param cause the cause of the error
     */
    public static void invalidMapping(final String context, final Throwable cause) {
        log.error("Invalid mapping template, payload build aborted — {}", StringUtils.normalizeSpace(context), cause);
    }

    /**
     * Logs a source that cannot be read, such as an unknown source, an invalid name, a missing account id column or a
     * database error: the payload build is aborted.
     *
     * @param context the entities in error
     * @param cause the cause of the error
     */
    public static void sourceUnreadable(final String context, final Throwable cause) {
        log.error("Source unreadable, payload build aborted — {}", StringUtils.normalizeSpace(context), cause);
    }

    /**
     * Logs an OPA server that cannot be reached: the authorization check is aborted.
     *
     * @param context the entities in error
     * @param cause the cause of the error
     */
    public static void opaUnreachable(final String context, final Throwable cause) {
        log.error("OPA unreachable, authorization check aborted — {}", StringUtils.normalizeSpace(context), cause);
    }

    // ── Non-blocking ───────────────────────────────────────────────────────

    /**
     * Logs an account in error during the payload build, such as several rows in a source, a template error or an
     * invalid JSON: the account keeps its previous payload.
     *
     * @param context the entities in error
     * @param cause the cause of the error
     */
    public static void payloadError(final String context, final Throwable cause) {
        log.warn("Account in error during payload build, keeping previous payload — {}",
            StringUtils.normalizeSpace(context), cause);
    }

    /**
     * Logs a role returned by OPA and missing from the roles of the application: the result is recorded.
     *
     * @param context the entities in error
     * @param cause the cause of the error, or {@code null} when there is none
     */
    public static void unknownRole(final String context, final Throwable cause) {
        log.warn("Unknown role returned by OPA, missing from application_roles — {}",
            StringUtils.normalizeSpace(context), cause);
    }

    /**
     * Logs a pair in error, such as a failed evaluation, a claims template error or an invalid claims JSON: nothing
     * is written for the pair.
     *
     * @param context the entities in error
     * @param cause the cause of the error
     */
    public static void pairError(final String context, final Throwable cause) {
        log.warn("Pair in error, nothing written — {}", StringUtils.normalizeSpace(context), cause);
    }
}
