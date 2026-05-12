CREATE TABLE organizational_units
(
    oun_id      UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL UNIQUE,
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

CREATE TABLE organizational_unit_relations
(
    our_id      UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    parent_id   UUID REFERENCES organizational_units (oun_id) ON DELETE CASCADE,
    child_id    UUID REFERENCES organizational_units (oun_id) ON DELETE CASCADE,
    created_by  UUID        NOT NULL,
    updated_by  UUID        NOT NULL,
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

CREATE TABLE organizational_unit_closures
(
    ancestor_id   UUID NOT NULL,
    descendant_id UUID NOT NULL,
    depth         INT  NOT NULL,
    PRIMARY KEY (ancestor_id, descendant_id),
    FOREIGN KEY (ancestor_id) REFERENCES organizational_units (oun_id) ON DELETE CASCADE,
    FOREIGN KEY (descendant_id) REFERENCES organizational_units (oun_id) ON DELETE CASCADE,
    CHECK (depth >= 0)
);

COMMENT ON TABLE organizational_unit_closures IS 'Closure table storing all ancestor-descendant relationships for fast read queries.';

COMMENT ON COLUMN organizational_unit_closures.ancestor_id IS 'Ancestor node ID in the hierarchy.';
COMMENT ON COLUMN organizational_unit_closures.descendant_id IS 'Descendant node ID in the hierarchy.';
COMMENT ON COLUMN organizational_unit_closures.depth IS 'Distance between ancestor and descendant (0 = self, 1 = direct relation).';

-- Indexes for fast lookups
CREATE INDEX idx_ou_closure_ancestor ON organizational_unit_closures (ancestor_id);
CREATE INDEX idx_ou_closure_descendant ON organizational_unit_closures (descendant_id);

CREATE OR REPLACE FUNCTION organizational_unit_self_closures()
    RETURNS TRIGGER AS
$$
BEGIN
    -- Each node is its own ancestor (depth = 0)
    -- This is required to make recursive joins consistent and simpler
    INSERT INTO organizational_unit_closures (ancestor_id, descendant_id, depth)
    VALUES (NEW.oun_id, NEW.oun_id, 0)
    ON CONFLICT DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION organizational_unit_self_closures IS 'Automatically inserts self-referential closure (depth=0) when a node is created.';

CREATE TRIGGER trg_ou_self_closure
    AFTER INSERT
    ON organizational_units
    FOR EACH ROW
EXECUTE FUNCTION organizational_unit_self_closures();

CREATE OR REPLACE FUNCTION organizational_unit_closures_insert()
    RETURNS TRIGGER AS
$$
BEGIN
    -- -------------------------------------------------
    -- Cycle protection
    -- -------------------------------------------------
    -- If child is already an ancestor of parent,
    -- inserting this relation would create a cycle.
    IF EXISTS (SELECT 1
               FROM organizational_unit_closures
               WHERE ancestor_id = NEW.child_id
                 AND descendant_id = NEW.parent_id) THEN
        RAISE EXCEPTION 'Cycle detected: % -> %', NEW.parent_id, NEW.child_id;
    END IF;

    -- -------------------------------------------------
    -- Direct relation (explicit insert for clarity & safety)
    -- -------------------------------------------------
    INSERT INTO organizational_unit_closures (ancestor_id, descendant_id, depth)
    VALUES (NEW.parent_id, NEW.child_id, 1)
    ON CONFLICT DO NOTHING;

    -- -------------------------------------------------
    -- Transitive closure propagation
    -- -------------------------------------------------
    -- Combine:
    --  - all ancestors of parent
    --  - all descendants of child
    --
    -- This creates all indirect paths introduced by the new edge.
    INSERT INTO organizational_unit_closures (ancestor_id, descendant_id, depth)
    SELECT p.ancestor_id,
           c.descendant_id,
           p.depth + c.depth + 1
    FROM organizational_unit_closures p
             JOIN organizational_unit_closures c
                  ON c.ancestor_id = NEW.child_id
    WHERE p.descendant_id = NEW.parent_id
    ON CONFLICT DO NOTHING;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION organizational_unit_closures_insert IS 'Maintains closure table after inserting a parent-child relation (handles transitive propagation and cycle detection).';

CREATE TRIGGER trg_ou_relation_insert
    AFTER INSERT
    ON organizational_unit_relations
    FOR EACH ROW
EXECUTE FUNCTION organizational_unit_closures_insert();

CREATE OR REPLACE FUNCTION organizational_unit_closures_delete()
    RETURNS TRIGGER AS
$$
BEGIN
    -- -------------------------------------------------
    -- Full rebuild strategy (safe but expensive)
    -- -------------------------------------------------
    -- In DAG with multiple parents, removing one edge may
    -- invalidate only some paths. Detecting which ones is complex.
    --
    -- This approach guarantees correctness by rebuilding everything.
    DELETE FROM organizational_unit_closures;

    INSERT INTO organizational_unit_closures (ancestor_id, descendant_id, depth)
    WITH RECURSIVE rebuild AS (
        -- Base case: self references
        SELECT oun_id AS ancestor_id, oun_id AS descendant_id, 0 AS depth
        FROM organizational_units

        UNION ALL

        -- Recursive step: traverse relations
        SELECT r.parent_id,
               c.descendant_id,
               c.depth + 1
        FROM organizational_unit_relations r
                 JOIN rebuild c ON c.ancestor_id = r.child_id)
    SELECT *
    FROM rebuild;

    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION organizational_unit_closures_delete IS 'Rebuilds the entire closure table after a relation deletion to ensure consistency in a DAG with multiple parents.';

CREATE TRIGGER trg_ou_relation_delete
    AFTER DELETE
    ON organizational_unit_relations
    FOR EACH STATEMENT
EXECUTE FUNCTION organizational_unit_closures_delete();