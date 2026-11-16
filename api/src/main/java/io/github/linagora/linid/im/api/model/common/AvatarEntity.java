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


package io.github.linagora.linid.im.api.model.common;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import java.util.Arrays;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Entity types supporting an avatar image.
 *
 * <p>Each value carries the path segment identifying the entity type in the avatar endpoints and in the
 * avatar storage directory.</p>
 */
@Getter
@RequiredArgsConstructor
public enum AvatarEntity {

    /**
     * The avatar of an account.
     */
    ACCOUNTS("accounts"),

    /**
     * The avatar of an application.
     */
    APPLICATIONS("applications"),

    /**
     * The avatar of an organizational unit.
     */
    ORGANIZATIONAL_UNITS("organizational-units"),

    /**
     * The avatar of a group.
     */
    GROUPS("groups");

    /**
     * Path segment identifying the entity type in the avatar endpoints and in the avatar storage directory.
     */
    private final String path;

    /**
     * Resolves an entity type from its path segment.
     *
     * @param path the path segment received by the avatar endpoints
     * @return the matching entity type
     * @throws ApiException with HTTP 400 status when no entity type matches
     */
    public static AvatarEntity fromPath(final String path) {
        return Arrays.stream(values())
            .filter(entity -> entity.path.equals(path))
            .findFirst()
            .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST.value(),
                I18nMessage.of("error.avatar.entity.unsupported", Map.of("entity", String.valueOf(path)))));
    }
}
