
package io.github.linagora.linid.im.api.service;

import io.github.linagora.linid.im.api.model.superset.SupersetRlsConfig;
import io.github.linagora.linid.im.api.model.superset.SupersetTokenRecord;
import io.github.linagora.linid.im.api.model.user.UserPrincipal;
import io.github.linagora.linid.im.api.persistence.model.AccountView;
import io.github.linagora.linid.im.api.persistence.model.OrganizationalUnitView;
import io.github.linagora.linid.im.api.persistence.repository.AccountViewRepository;
import io.github.linagora.linid.im.api.persistence.repository.OrganizationalUnitViewRepository;
import io.github.linagora.linid.im.corelib.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Test class: SupersetServiceImpl")
class SupersetServiceImplTest {

    @Mock
    private AccountViewRepository accountViewRepository;

    @Mock
    private OrganizationalUnitViewRepository organizationalUnitViewRepository;

    private SupersetServiceImpl service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        service = new SupersetServiceImpl(
            "http://localhost:8088",
            "admin",
            "admin",
            "",
            accountViewRepository,
            organizationalUnitViewRepository
        );
    }

    @Test
    @DisplayName("should return no RLS rules when RLS configuration is empty")
    void shouldReturnNoRlsRulesWhenConfigurationIsEmpty() {
        var tokenRecord = new SupersetTokenRecord(
            "unknown-dashboard",
            UUID.randomUUID(),
            UUID.randomUUID().toString()
        );

        var userPrincipal = mock(UserPrincipal.class);

        var result = service.buildRlsRules(userPrincipal, tokenRecord);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("should return only enabled RLS rules matching the dashboard slug")
    void shouldReturnOnlyMatchingEnabledRlsRules() {
        var dashboardId = UUID.randomUUID();
        var rlsId = UUID.randomUUID().toString();

        var matchingConfig = new SupersetRlsConfig(
            "dashboard",
            true,
            10,
            "ACCOUNT",
            "externalId"
        );

        var disabledConfig = new SupersetRlsConfig(
            "dashboard",
            false,
            20,
            "ACCOUNT",
            "externalId"
        );

        var otherDashboardConfig = new SupersetRlsConfig(
            "other-dashboard",
            true,
            30,
            "ACCOUNT",
            "externalId"
        );

        var account = AccountView.builder().externalId("john.doe").build();

        when(accountViewRepository.findById(UUID.fromString(rlsId)))
            .thenReturn(Optional.of(account));

        setRlsConfigurations(
            matchingConfig,
            disabledConfig,
            otherDashboardConfig
        );

        var tokenRecord = new SupersetTokenRecord(
            "dashboard",
            dashboardId,
            rlsId
        );

        var userPrincipal = mock(UserPrincipal.class);

        var result = service.buildRlsRules(userPrincipal, tokenRecord);

        assertThat(result)
            .hasSize(1)
            .first()
            .satisfies(rule -> {
                assertThat(rule.clause()).isEqualTo("rls_id='john.doe'");
                assertThat(rule.dataset()).isEqualTo(10);
            });
    }

    @Test
    @DisplayName("should build an account RLS rule using the configured account attribute")
    void shouldBuildAccountRlsRule() {
        var rlsId = UUID.randomUUID();
        var config = new SupersetRlsConfig(
            "dashboard",
            true,
            42,
            "ACCOUNT",
            "externalId"
        );

        var account = AccountView.builder().externalId("john.doe").build();

        when(accountViewRepository.findById(rlsId))
            .thenReturn(Optional.of(account));

        var tokenRecord = new SupersetTokenRecord(
            "dashboard",
            UUID.randomUUID(),
            rlsId.toString()
        );

        var result = service.buildRlsRule(
            mock(UserPrincipal.class),
            tokenRecord,
            config
        );

        assertThat(result.clause()).isEqualTo("rls_id='john.doe'");
        assertThat(result.dataset()).isEqualTo(42);
    }

    @Test
    @DisplayName("should build an organizational unit RLS rule using the configured attribute")
    void shouldBuildOrganizationalUnitRlsRule() {
        var rlsId = UUID.randomUUID();
        var config = new SupersetRlsConfig(
            "dashboard",
            true,
            42,
            "ORGANIZATIONAL_UNIT",
            "name"
        );

        var organizationalUnit = OrganizationalUnitView.builder().name("IT").build();

        when(organizationalUnitViewRepository.findById(rlsId))
            .thenReturn(Optional.of(organizationalUnit));

        var tokenRecord = new SupersetTokenRecord(
            "dashboard",
            UUID.randomUUID(),
            rlsId.toString()
        );

        var result = service.buildRlsRule(
            mock(UserPrincipal.class),
            tokenRecord,
            config
        );

        assertThat(result.clause()).isEqualTo("rls_id='IT'");
        assertThat(result.dataset()).isEqualTo(42);
    }

    @Test
    @DisplayName("should reject an unsupported RLS entity")
    void shouldRejectUnsupportedRlsEntity() {
        var config = new SupersetRlsConfig(
            "dashboard",
            true,
            42,
            "UNKNOWN_ENTITY",
            "name"
        );

        var tokenRecord = new SupersetTokenRecord(
            "dashboard",
            UUID.randomUUID(),
            UUID.randomUUID().toString()
        );

        assertThatThrownBy(() ->
            service.buildRlsRule(
                mock(UserPrincipal.class),
                tokenRecord,
                config
            )
        )
            .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("should throw a not found exception when the account does not exist")
    void shouldThrowWhenAccountDoesNotExist() {
        var rlsId = UUID.randomUUID();

        when(accountViewRepository.findById(rlsId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            service.getAccountValue(rlsId.toString(), "username")
        )
            .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("should return the configured account attribute value")
    void shouldReturnAccountAttributeValue() {
        var rlsId = UUID.randomUUID();
        var account = AccountView.builder().externalId("john.doe").build();

        when(accountViewRepository.findById(rlsId))
            .thenReturn(Optional.of(account));

        var result = service.getAccountValue(
            rlsId.toString(),
            "externalId"
        );

        assertThat(result).isEqualTo("john.doe");
    }

    @Test
    @DisplayName("should throw a not found exception when the organizational unit does not exist")
    void shouldThrowWhenOrganizationalUnitDoesNotExist() {
        var rlsId = UUID.randomUUID();

        when(organizationalUnitViewRepository.findById(rlsId))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
            service.getOrganizationalUnitValue(rlsId.toString(), "name")
        )
            .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("should return the configured organizational unit attribute value")
    void shouldReturnOrganizationalUnitAttributeValue() {
        var rlsId = UUID.randomUUID();
        var organizationalUnit = OrganizationalUnitView.builder().name("IT").build();

        when(organizationalUnitViewRepository.findById(rlsId))
            .thenReturn(Optional.of(organizationalUnit));

        var result = service.getOrganizationalUnitValue(
            rlsId.toString(),
            "name"
        );

        assertThat(result).isEqualTo("IT");
    }

    @Test
    @DisplayName("should throw an exception when the RLS configuration file cannot be read")
    void shouldThrowWhenRlsConfigurationFileCannotBeRead(@TempDir final Path tempDir) {
        Path missingFile = tempDir.resolve("missing.yml");

        assertThatThrownBy(() ->
            service.initRlsConfiguration(missingFile.toString())
        )
            .isInstanceOf(ApiException.class);
    }

    @Test
    @DisplayName("should leave RLS configuration empty when the configuration path is blank")
    void shouldLeaveConfigurationEmptyWhenPathIsBlank() {
        service.initRlsConfiguration(" ");

        var result = service.buildRlsRules(
            mock(UserPrincipal.class),
            new SupersetTokenRecord(
                "dashboard",
                UUID.randomUUID(),
                UUID.randomUUID().toString()
            )
        );

        assertThat(result).isEmpty();
    }

    private void setRlsConfigurations(final SupersetRlsConfig... configs) {
        service.initRlsConfiguration("");
        try {
            var field = SupersetServiceImpl.class.getDeclaredField("rlsConfigs");
            field.setAccessible(true);
            field.set(service, List.of(configs));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to configure RLS configurations for test", e);
        }
    }
}