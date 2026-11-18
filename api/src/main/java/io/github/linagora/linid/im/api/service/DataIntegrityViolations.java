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

import java.sql.SQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Reads why PostgreSQL rejected a write, so that the services can turn a rejection caused by a concurrent request
 * into the same API error as the check that ran before the write.
 */
final class DataIntegrityViolations {

    /**
     * PostgreSQL SQLState of a check violation, also raised by the triggers that reject a cycle in a hierarchy.
     */
    static final String CHECK_VIOLATION = "23514";

    /**
     * PostgreSQL SQLState of a unique violation.
     */
    static final String UNIQUE_VIOLATION = "23505";

    /**
     * PostgreSQL SQLState of a foreign key violation.
     */
    static final String FOREIGN_KEY_VIOLATION = "23503";

    /**
     * Utility class.
     */
    private DataIntegrityViolations() {
    }

    /**
     * Tells whether the database rejected a write with the given SQLState.
     *
     * @param exception the exception raised by the flush
     * @param sqlState  the expected PostgreSQL SQLState
     * @return {@code true} if the most specific cause is an {@link SQLException} with this SQLState
     */
    static boolean hasSqlState(final DataIntegrityViolationException exception, final String sqlState) {
        return exception.getMostSpecificCause() instanceof SQLException sqlException
            && sqlState.equals(sqlException.getSQLState());
    }

    /**
     * Reads the name of the constraint violated by a write, as extracted by Hibernate.
     *
     * @param exception the exception raised by the flush
     * @return the constraint name, or {@code null} when no cause carries it
     */
    static String violatedConstraint(final DataIntegrityViolationException exception) {
        for (Throwable cause = exception.getCause(); cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
        }

        return null;
    }
}
