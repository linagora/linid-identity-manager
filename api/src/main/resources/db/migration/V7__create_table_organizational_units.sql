-- =========================================================
-- ORGANIZATIONAL UNITS
-- =========================================================

CREATE TABLE organizational_units
(
    oun_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(100) NOT NULL,
    created_by  UUID,
    updated_by  UUID,
    insert_date TIMESTAMPTZ  NOT NULL DEFAULT now(),
    update_date TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_organizational_units_type_name UNIQUE (type, name)
);

COMMENT ON TABLE organizational_units IS 'Stores all organizational units (nodes of the DAG).';

COMMENT ON COLUMN organizational_units.oun_id IS 'Primary key (UUID) of the organizational unit.';
COMMENT ON COLUMN organizational_units.name IS 'Human-readable name of the organizational unit.';
COMMENT ON COLUMN organizational_units.type IS 'Type of the organizational unit.';
COMMENT ON COLUMN organizational_units.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN organizational_units.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN organizational_units.insert_date IS 'Date and time when the account record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN organizational_units.update_date IS 'Date and time when the account record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON CONSTRAINT uk_organizational_units_type_name ON organizational_units IS 'Ensures that an organizational unit name is unique within a given organizational unit type.';

-- =========================================================
-- RELATIONS (DIRECT EDGES)
-- =========================================================

CREATE TABLE organizational_unit_relations
(
    our_id      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id   UUID NOT NULL REFERENCES organizational_units (oun_id) ON DELETE CASCADE,
    child_id    UUID NOT NULL REFERENCES organizational_units (oun_id) ON DELETE CASCADE,
    created_by  UUID NOT NULL,
    updated_by  UUID NOT NULL,
    insert_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    update_date TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (parent_id <> child_id),
    UNIQUE (parent_id, child_id)
);

COMMENT ON TABLE organizational_unit_relations IS 'Stores direct parent-child relationships. This is the source of truth for the graph structure.';

COMMENT ON COLUMN organizational_unit_relations.our_id IS 'Primary key (UUID) of the organizational unit relation.';
COMMENT ON COLUMN organizational_unit_relations.parent_id IS 'Parent organizational unit ID.';
COMMENT ON COLUMN organizational_unit_relations.child_id IS 'Child organizational unit ID.';
COMMENT ON COLUMN organizational_unit_relations.created_by IS 'Identifier of the creator of this record (user, service, or system).';
COMMENT ON COLUMN organizational_unit_relations.updated_by IS 'Identifier of the last updater of this record (user, service, or system).';
COMMENT ON COLUMN organizational_unit_relations.insert_date IS 'Date and time when the account record was created. Default is now(). Stored in UTC (TIMESTAMPTZ).';
COMMENT ON COLUMN organizational_unit_relations.update_date IS 'Date and time when the account record was last updated. Default is now(). Stored in UTC (TIMESTAMPTZ).';

-- =========================================================
-- CLOSURE TABLE
-- =========================================================

CREATE TABLE organizational_unit_closures
(
    ancestor_id   UUID NOT NULL,
    descendant_id UUID NOT NULL,
    depth         INT  NOT NULL CHECK (depth >= 0),
    PRIMARY KEY (ancestor_id, descendant_id),
    FOREIGN KEY (ancestor_id) REFERENCES organizational_units (oun_id) ON DELETE CASCADE,
    FOREIGN KEY (descendant_id) REFERENCES organizational_units (oun_id) ON DELETE CASCADE
);

CREATE INDEX idx_ou_closure_ancestor ON organizational_unit_closures (ancestor_id);
CREATE INDEX idx_ou_closure_descendant ON organizational_unit_closures (descendant_id);

COMMENT ON TABLE organizational_unit_closures IS 'Closure table storing all ancestor-descendant relationships for fast read queries.';

COMMENT ON COLUMN organizational_unit_closures.ancestor_id IS 'Ancestor node ID in the hierarchy.';
COMMENT ON COLUMN organizational_unit_closures.descendant_id IS 'Descendant node ID in the hierarchy.';
COMMENT ON COLUMN organizational_unit_closures.depth IS 'Distance between ancestor and descendant (0 = self, 1 = direct relation).';

-- =========================================================
-- 1. SELF CLOSURE (depth = 0)
-- =========================================================

CREATE OR REPLACE FUNCTION organizational_unit_self_closures()
    RETURNS TRIGGER AS
$$
BEGIN
    INSERT INTO organizational_unit_closures (ancestor_id, descendant_id, depth)
    VALUES (NEW.oun_id, NEW.oun_id, 0)
    ON CONFLICT DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ou_self_closure
    AFTER INSERT ON organizational_units
    FOR EACH ROW
EXECUTE FUNCTION organizational_unit_self_closures();

-- =========================================================
-- 2. INSERT RELATION → UPDATE CLOSURE
-- =========================================================

CREATE OR REPLACE FUNCTION organizational_unit_closures_insert()
    RETURNS TRIGGER AS
$$
BEGIN
    -- Cycle detection
    IF EXISTS (
        SELECT 1
        FROM organizational_unit_closures
        WHERE ancestor_id = NEW.child_id
          AND descendant_id = NEW.parent_id
    ) THEN
        RAISE EXCEPTION 'Cycle detected: % -> %', NEW.parent_id, NEW.child_id;
    END IF;

    -- Direct relation
    INSERT INTO organizational_unit_closures (ancestor_id, descendant_id, depth)
    VALUES (NEW.parent_id, NEW.child_id, 1)
    ON CONFLICT DO NOTHING;

    -- Transitive closure propagation
    INSERT INTO organizational_unit_closures (ancestor_id, descendant_id, depth)
    SELECT DISTINCT
        p.ancestor_id,
        c.descendant_id,
        p.depth + c.depth + 1
    FROM organizational_unit_closures p
             JOIN organizational_unit_closures c
                  ON p.descendant_id = NEW.parent_id
                      AND c.ancestor_id = NEW.child_id
    ON CONFLICT DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ou_relation_insert
    AFTER INSERT ON organizational_unit_relations
    FOR EACH ROW
EXECUTE FUNCTION organizational_unit_closures_insert();

-- =========================================================
-- 3. DELETE RELATION → CLEAN IMPACTED PATHS ONLY
-- =========================================================

CREATE OR REPLACE FUNCTION organizational_unit_closures_delete()
    RETURNS TRIGGER AS
$$
BEGIN
    DELETE FROM organizational_unit_closures c
    WHERE EXISTS (
        SELECT 1
        FROM organizational_unit_closures a
                 JOIN organizational_unit_closures d ON TRUE
        WHERE a.descendant_id = NEW.parent_id
          AND d.ancestor_id = NEW.child_id
          AND c.ancestor_id = a.ancestor_id
          AND c.descendant_id = d.descendant_id
    );

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ou_relation_delete
    AFTER DELETE ON organizational_unit_relations
    FOR EACH ROW
EXECUTE FUNCTION organizational_unit_closures_delete();
