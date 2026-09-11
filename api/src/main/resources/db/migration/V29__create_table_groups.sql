CREATE TABLE IF NOT EXISTS groups
(
    grp_id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code             VARCHAR(100) NOT NULL,
    label            VARCHAR(255) NOT NULL,
    parent_id        UUID         REFERENCES groups (grp_id) ON DELETE SET NULL,
    description      TEXT,
    email            VARCHAR(320),
    oun_id           UUID         REFERENCES organizational_units (oun_id) ON DELETE SET NULL,
    app_id           UUID         REFERENCES applications (app_id) ON DELETE SET NULL,
    extra_parameters JSONB        NOT NULL DEFAULT '{}'::JSONB,
    created_by       UUID,
    updated_by       UUID,
    insert_date      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_date      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT ck_groups_parent_id_not_self CHECK (parent_id <> grp_id)
);

CREATE UNIQUE INDEX idx_groups_code ON groups (code);
CREATE INDEX idx_groups_parent_id ON groups (parent_id);
CREATE INDEX idx_groups_oun_id ON groups (oun_id);
CREATE INDEX idx_groups_app_id ON groups (app_id);

CREATE TRIGGER tg_groups_set_update_date
    BEFORE UPDATE
    ON groups
    FOR EACH ROW
EXECUTE FUNCTION update_timestamp();

COMMENT ON TABLE groups IS 'Table storing group definitions, including identification, optional parent group, organizational unit and application relationships, and audit data.';

COMMENT ON COLUMN groups.grp_id IS 'Primary key. UUID automatically generated for each group.';
COMMENT ON COLUMN groups.code IS 'Functional unique identifier of the group. Unique constraint enforced.';
COMMENT ON COLUMN groups.label IS 'Human-readable label of the group.';
COMMENT ON COLUMN groups.parent_id IS 'Optional identifier of the parent group. Foreign key to groups, set to NULL when the parent group is deleted.';
COMMENT ON COLUMN groups.description IS 'Optional free-text description of the group.';
COMMENT ON COLUMN groups.email IS 'Optional email address of the group.';
COMMENT ON COLUMN groups.oun_id IS 'Optional identifier of the organizational unit associated with the group. Foreign key to organizational_units, set to NULL when the organizational unit is deleted.';
COMMENT ON COLUMN groups.app_id IS 'Optional identifier of the application associated with the group. Foreign key to applications, set to NULL when the application is deleted.';
COMMENT ON COLUMN groups.extra_parameters IS 'JSONB column containing custom attributes and metadata defined by the deployment. Intended for customer-specific or integration-specific extensions that are not part of the standard data model.';
COMMENT ON COLUMN groups.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN groups.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN groups.insert_date IS 'Date and time when the group record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN groups.update_date IS 'Date and time when the group record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON CONSTRAINT ck_groups_parent_id_not_self ON groups IS 'Ensures that a group cannot be its own parent.';

COMMENT ON INDEX idx_groups_code IS 'Unique index on code to enforce uniqueness and improve query performance when searching by code.';
COMMENT ON INDEX idx_groups_parent_id IS 'Index on parent group identifier to optimize lookups and joins on groups by parent group.';
COMMENT ON INDEX idx_groups_oun_id IS 'Index on organizational unit identifier to optimize lookups and joins on groups by organizational unit.';
COMMENT ON INDEX idx_groups_app_id IS 'Index on application identifier to optimize lookups and joins on groups by application.';

COMMENT ON TRIGGER tg_groups_set_update_date ON groups IS 'Trigger that invokes the update_timestamp() function before each UPDATE to automatically set update_date to NOW().';
