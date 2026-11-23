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

package io.github.linagora.linid.im.api.persistence.interceptor;

import io.github.linagora.linid.im.api.config.SuperAdminConfig;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Sets the PostgreSQL Row-Level Security (RLS) context of the current transaction.
 *
 * <p>The RLS policies read this context through the {@code current_account_id()} and {@code is_super_admin()} SQL
 * functions. It only lasts for the current transaction, so it never leaks to the next request using the same
 * connection of the pool. Values are bound as query parameters, never concatenated into the SQL.</p>
 *
 * <p>Both methods must be called within an active transaction, otherwise an {@link IllegalStateException} is
 * thrown.</p>
 */
@Component
@RequiredArgsConstructor
public class RlsInterceptor {

    /**
     * Query setting the variables of the RLS context, local to the current transaction.
     */
    private static final String SET_CONTEXT_QUERY = """
        SELECT set_config('linid.account_id', :accountId, true),
               set_config('linid.is_super_admin', :isSuperAdmin, true),
               set_config('linid.super_admin_sub', :superAdminSub, true)
        """;

    /**
     * Entity manager running the query in the current transaction.
     */
    private final EntityManager entityManager;

    /**
     * Configuration giving the subject of the SUPER_ADMIN, used by the policies to protect its account.
     */
    private final SuperAdminConfig superAdminConfig;

    /**
     * Sets the RLS context of the given user for the current transaction.
     *
     * @param userPrincipal the authenticated user
     * @throws IllegalStateException if called outside of an active transaction
     */
    public void securize(final UserPrincipal userPrincipal) {
        setContext(userPrincipal.getId().toString(), userPrincipal.isSuperAdmin());
    }

    /**
     * Sets a full-access RLS context for the current transaction, for technical operations not bound to a user.
     *
     * <p>Only call it first in a method that starts its own transaction: within a transaction started for a user,
     * the rest of this transaction would run with full access.</p>
     *
     * @throws IllegalStateException if called outside of an active transaction
     */
    public void securizeAsSystem() {
        setContext("", true);
    }

    /**
     * Sets the variables of the RLS context for the current transaction.
     *
     * @param accountId  the id of the calling account, empty in a system context
     * @param superAdmin whether the caller has full access
     * @throws IllegalStateException if called outside of an active transaction
     */
    private void setContext(final String accountId, final boolean superAdmin) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException(
                "No active transaction: the RLS context can only be set inside a transaction"
            );
        }

        entityManager.createNativeQuery(SET_CONTEXT_QUERY)
            .setParameter("accountId", accountId)
            .setParameter("isSuperAdmin", String.valueOf(superAdmin))
            .setParameter("superAdminSub", superAdminConfig.getSub())
            .getSingleResult();
    }
}
