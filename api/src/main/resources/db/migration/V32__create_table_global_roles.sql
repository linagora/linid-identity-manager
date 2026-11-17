CREATE TABLE global_roles
(
    gro_id      UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    act_id      UUID        NOT NULL REFERENCES accounts (act_id) ON DELETE CASCADE,
    role        VARCHAR(50) NOT NULL,
    created_by  UUID        NOT NULL,
    updated_by  UUID        NOT NULL,
    insert_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    update_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_global_roles_act_id_role UNIQUE (act_id, role),
    CONSTRAINT ck_global_roles_role CHECK (role IN ('ADMINISTRATORS_MANAGER',
                                                    'ACCOUNTS_ADMIN', 'ACCOUNTS_AUDITOR',
                                                    'ORGANIZATIONAL_UNITS_ADMIN', 'ORGANIZATIONAL_UNITS_AUDITOR',
                                                    'GROUPS_ADMIN', 'GROUPS_AUDITOR',
                                                    'ROLES_ADMIN', 'ROLES_AUDITOR',
                                                    'APPLICATIONS_ADMIN', 'APPLICATIONS_AUDITOR'))
);

CREATE TRIGGER tg_global_roles_set_update_date
    BEFORE UPDATE
    ON global_roles
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE global_roles IS 'Stores the global roles held by accounts. An account can hold several roles and gets the union of their rights. The SUPER_ADMIN is declared by configuration and never stored here.';

COMMENT ON COLUMN global_roles.gro_id IS 'Primary key (UUID) of the global role assignment.';
COMMENT ON COLUMN global_roles.act_id IS 'Identifier of the account holding the role.';
COMMENT ON COLUMN global_roles.role IS 'Global role held by the account (ADMINISTRATORS_MANAGER, or the ADMIN and AUDITOR roles of accounts, organizational units, groups, roles and applications).';
COMMENT ON COLUMN global_roles.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN global_roles.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN global_roles.insert_date IS 'Date and time when the role was assigned. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN global_roles.update_date IS 'Date and time when the record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON CONSTRAINT uk_global_roles_act_id_role ON global_roles IS 'Ensures that an account holds a given global role only once.';
COMMENT ON CONSTRAINT ck_global_roles_role ON global_roles IS 'Restricts the role to the known global roles; SUPER_ADMIN is excluded because it is declared by configuration.';

COMMENT ON TRIGGER tg_global_roles_set_update_date ON global_roles IS 'Trigger that invokes the update_timestamp() function before each UPDATE to automatically set update_date to NOW().';
