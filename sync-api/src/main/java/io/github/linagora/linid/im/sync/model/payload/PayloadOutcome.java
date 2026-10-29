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

package io.github.linagora.linid.im.sync.model.payload;

import java.util.Locale;

/**
 * Outcome of an account in {@code buildPayloadsStep}, counted in the execution context of the step and logged at its
 * end, in the order of the values.
 */
public enum PayloadOutcome {

    /**
     * Payload inserted, the account having none yet.
     */
    INSERTED("inserted"),

    /**
     * Payload updated, its checksum having changed.
     */
    UPDATED("updated"),

    /**
     * Payload unchanged, not written.
     */
    UNCHANGED("unchanged"),

    /**
     * Account in error, skipped: it keeps its previous payload.
     */
    IN_ERROR("in error"),

    /**
     * Account deleted since its page was read, not written.
     */
    DELETED("deleted");

    /**
     * Label of the outcome in the log of the step.
     */
    private final String label;

    /**
     * Creates an outcome with its label.
     *
     * @param label the label of the outcome in the log of the step
     */
    PayloadOutcome(final String label) {
        this.label = label;
    }

    /**
     * Gives the label of the outcome in the log of the step.
     *
     * @return the label of the outcome
     */
    public String label() {
        return label;
    }

    /**
     * Gives the key of the counter of the outcome in the execution context of the step.
     *
     * @return the key of the counter, such as {@code payloads.inserted}
     */
    public String key() {
        return "payloads." + name().toLowerCase(Locale.ROOT);
    }
}
