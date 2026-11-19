CREATE TABLE IF NOT EXISTS groups
(
    grp_id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id        UUID         REFERENCES groups (grp_id) ON DELETE SET NULL,
    oun_id           UUID         REFERENCES organizational_units (oun_id) ON DELETE SET NULL,
    app_id           UUID         REFERENCES applications (app_id) ON DELETE SET NULL,
    code             VARCHAR(100) NOT NULL UNIQUE,
    name             VARCHAR(255) NOT NULL,
    description      TEXT,
    email            VARCHAR(320),
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

COMMENT ON TABLE groups IS 'Table storing group definitions, including optional parent group, organizational unit and application relationships, identification, and audit data.';

COMMENT ON COLUMN groups.grp_id IS 'Primary key. UUID automatically generated for each group.';
COMMENT ON COLUMN groups.parent_id IS 'Optional identifier of the parent group. Foreign key to groups, set to NULL when the parent group is deleted.';
COMMENT ON COLUMN groups.oun_id IS 'Optional identifier of the organizational unit associated with the group. Foreign key to organizational_units, set to NULL when the organizational unit is deleted.';
COMMENT ON COLUMN groups.app_id IS 'Optional identifier of the application associated with the group. Foreign key to applications, set to NULL when the application is deleted.';
COMMENT ON COLUMN groups.code IS 'Functional unique identifier of the group. Unique constraint enforced.';
COMMENT ON COLUMN groups.name IS 'Human-readable name of the group.';
COMMENT ON COLUMN groups.description IS 'Optional free-text description of the group.';
COMMENT ON COLUMN groups.email IS 'Optional email address of the group.';
COMMENT ON COLUMN groups.extra_parameters IS 'JSONB column containing custom attributes and metadata defined by the deployment. Intended for customer-specific or integration-specific extensions that are not part of the standard data model.';
COMMENT ON COLUMN groups.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN groups.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN groups.insert_date IS 'Date and time when the group record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN groups.update_date IS 'Date and time when the group record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON CONSTRAINT ck_groups_parent_id_not_self ON groups IS 'Ensures that a group cannot be its own parent. Longer cycles are rejected by the group_closures_refresh() trigger function.';

COMMENT ON INDEX idx_groups_code IS 'Unique index on code to enforce uniqueness and improve query performance when searching by code.';
COMMENT ON INDEX idx_groups_parent_id IS 'Index on parent group identifier to optimize lookups and joins on groups by parent group.';
COMMENT ON INDEX idx_groups_oun_id IS 'Index on organizational unit identifier to optimize lookups and joins on groups by organizational unit.';
COMMENT ON INDEX idx_groups_app_id IS 'Index on application identifier to optimize lookups and joins on groups by application.';

COMMENT ON TRIGGER tg_groups_set_update_date ON groups IS 'Trigger that invokes the update_timestamp() function before each UPDATE to automatically set update_date to NOW().';

-- =========================================================
-- CLOSURE TABLE
-- =========================================================

CREATE TABLE group_closures
(
    ancestor_id   UUID NOT NULL REFERENCES groups (grp_id) ON DELETE CASCADE,
    descendant_id UUID NOT NULL REFERENCES groups (grp_id) ON DELETE CASCADE,
    depth         INT  NOT NULL,
    CONSTRAINT pk_group_closures PRIMARY KEY (ancestor_id, descendant_id),
    CONSTRAINT ck_group_closures_depth_positive CHECK (depth >= 0)
);

-- The primary key already serves the lookups by ancestor.
CREATE INDEX idx_group_closures_descendant_id ON group_closures (descendant_id);

COMMENT ON TABLE group_closures IS 'Closure table of the group hierarchy: one row per (ancestor, descendant) pair at any depth, including each group as its own ancestor. Maintained by triggers on groups, which remains the source of truth.';

COMMENT ON COLUMN group_closures.ancestor_id IS 'Identifier of the ancestor group. Foreign key to groups, the row is deleted with the group.';
COMMENT ON COLUMN group_closures.descendant_id IS 'Identifier of the descendant group. Foreign key to groups, the row is deleted with the group.';
COMMENT ON COLUMN group_closures.depth IS 'Distance between the ancestor and the descendant (0 = the group itself, 1 = direct parent).';

COMMENT ON CONSTRAINT pk_group_closures ON group_closures IS 'Ensures that an (ancestor, descendant) pair is stored once, and serves the lookups by ancestor.';
COMMENT ON CONSTRAINT ck_group_closures_depth_positive ON group_closures IS 'Ensures that the depth is never negative.';

COMMENT ON INDEX idx_group_closures_descendant_id IS 'Index on descendant group identifier to optimize the lookups of the ancestors of a group.';

-- =========================================================
-- REBUILD THE ANCESTOR PATHS OF GROUPS
-- =========================================================

CREATE OR REPLACE FUNCTION group_closures_rebuild(group_ids UUID[])
    RETURNS VOID AS
$$
BEGIN
    DELETE FROM group_closures
    WHERE descendant_id = ANY (group_ids)
      AND depth > 0;

    INSERT INTO group_closures (ancestor_id, descendant_id, depth)
    WITH RECURSIVE paths AS (
        SELECT parent_id AS ancestor_id,
               grp_id    AS descendant_id,
               1         AS depth
        FROM groups
        WHERE grp_id = ANY (group_ids)
          AND parent_id IS NOT NULL
        UNION ALL
        SELECT g.parent_id,
               p.descendant_id,
               p.depth + 1
        FROM paths p
                 JOIN groups g ON g.grp_id = p.ancestor_id
        WHERE g.parent_id IS NOT NULL
    ) CYCLE ancestor_id SET is_cycle USING path
    SELECT ancestor_id, descendant_id, depth
    FROM paths
    WHERE NOT is_cycle;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION group_closures_rebuild(UUID[]) IS 'Recomputes from groups.parent_id every ancestor path of the given groups, keeping their depth 0 rows.';

-- =========================================================
-- INSERT OR PARENT CHANGE → CHECK CYCLES, REBUILD THE SUBTREE
-- =========================================================

CREATE OR REPLACE FUNCTION group_closures_refresh()
    RETURNS TRIGGER AS
$$
BEGIN
    -- One change of the hierarchy at a time: two concurrent changes would otherwise each read the hierarchy from before
    -- the other one, and could close a cycle or miss each other's paths. Relies on READ COMMITTED, where each statement
    -- below reads what was committed while waiting. The OID of group_closures as namespace names the lock in pg_locks
    -- (classid::regclass) with no registry to keep: user table OIDs start at 16384, above the namespaces of the
    -- sync-api locks.
    PERFORM pg_advisory_xact_lock('group_closures'::regclass::oid::int, 0);

    IF TG_OP = 'INSERT' THEN
        INSERT INTO group_closures (ancestor_id, descendant_id, depth)
        VALUES (NEW.grp_id, NEW.grp_id, 0);
    END IF;

    -- Walks up groups and not group_closures, which misses the other groups written by the same statement.
    IF NEW.parent_id IS NOT NULL AND EXISTS (
        WITH RECURSIVE chain AS (
            SELECT grp_id, parent_id
            FROM groups
            WHERE grp_id = NEW.parent_id
            UNION ALL
            SELECT g.grp_id, g.parent_id
            FROM chain c
                     JOIN groups g ON g.grp_id = c.parent_id
        ) CYCLE grp_id SET is_cycle USING path
        SELECT 1
        FROM chain
        WHERE grp_id = NEW.grp_id
           OR is_cycle
    ) THEN
        RAISE EXCEPTION 'Cycle detected in the group hierarchy: % -> %', NEW.parent_id, NEW.grp_id
            USING ERRCODE = 'check_violation';
    END IF;

    -- Only the ancestors of the group and of its descendants can change.
    PERFORM group_closures_rebuild(ARRAY(
        SELECT descendant_id
        FROM group_closures
        WHERE ancestor_id = NEW.grp_id
    ));

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION group_closures_refresh() IS 'Trigger function that takes the lock of the group hierarchy until the end of the transaction, rejects a parent closing a cycle in the group hierarchy, then rebuilds the ancestor paths of the group and of its descendants. On insert, it also adds the depth 0 row of the group.';

CREATE TRIGGER tg_groups_closures_insert
    AFTER INSERT
    ON groups
    FOR EACH ROW
EXECUTE FUNCTION group_closures_refresh();

-- Also fired on the children of a deleted group, whose parent_id is set to NULL by the foreign key.
CREATE TRIGGER tg_groups_closures_update
    AFTER UPDATE OF parent_id
    ON groups
    FOR EACH ROW
    WHEN (OLD.parent_id IS DISTINCT FROM NEW.parent_id)
EXECUTE FUNCTION group_closures_refresh();

COMMENT ON TRIGGER tg_groups_closures_insert ON groups IS 'Trigger that invokes group_closures_refresh() after each INSERT to add the paths of the new group to group_closures.';
COMMENT ON TRIGGER tg_groups_closures_update ON groups IS 'Trigger that invokes group_closures_refresh() after each change of parent_id, including the one made by the deletion of the parent, to rebuild the paths of the moved subtree.';
