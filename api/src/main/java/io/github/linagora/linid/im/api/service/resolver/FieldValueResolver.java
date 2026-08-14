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

package io.github.linagora.linid.im.api.service.resolver;

import io.github.linagora.linid.im.corelib.exception.ApiException;
import io.github.linagora.linid.im.corelib.i18n.I18nMessage;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Utility class for resolving object field values dynamically.
 *
 * <p>Uses Java reflection to retrieve a field by name and convert its value
 * to a string representation. This is primarily used to resolve dynamically
 * configured attributes, such as those required to build Superset RLS rules.</p>
 */
public final class FieldValueResolver {

    private FieldValueResolver() {
        // Utility class
    }

    /**
     * Retrieves a field value from an object using reflection and converts it
     * to a string.
     *
     * @param object    the object from which the field value is retrieved
     * @param fieldName the name of the field to retrieve
     * @param entity    the entity name used in the error message when the field
     *                  cannot be resolved
     * @return the field value converted to a string, or {@code null} if the
     *         field value is {@code null}
     * @throws ApiException if the requested field does not exist or cannot be accessed
     */
    public static String getFieldAsString(
        final Object object,
        final String fieldName,
        final String entity) {
        try {
            Field field = object.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);

            Object value = field.get(object);

            if (value == null) {
                return null;
            }

            return value.toString();
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ApiException(
                HttpStatus.NOT_FOUND.value(),
                I18nMessage.of(
                    "error.attribute.unknown",
                    Map.of("entity", entity, "attribute", fieldName)
                )
            );
        }
    }
}
