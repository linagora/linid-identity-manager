CREATE ROLE audit_trigger_role NOLOGIN;

CREATE TABLE audit
(
    aud_id     UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    table_name TEXT        NOT NULL,
    record_id  UUID        NOT NULL,
    operation  VARCHAR(10) NOT NULL CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE')),
    payload    JSONB       NOT NULL,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_table_name_record_id ON audit (table_name, record_id);

CREATE OR REPLACE FUNCTION f_audit()
    RETURNS TRIGGER
    SECURITY DEFINER
AS
$$
DECLARE
    -- NEW is NULL on DELETE and OLD is NULL on INSERT: the row is the one that exists.
    row_json           JSONB := to_jsonb(COALESCE(NEW, OLD));
    payload_json       JSONB;
    words              TEXT[];
    payload_key        TEXT;
    foreign_key_column TEXT;
    excluded_columns   TEXT[];
    related_query      TEXT;
    related_json       JSONB;
    primary_key_column TEXT;
BEGIN
    payload_json := jsonb_build_object(
            'row', row_json,
            'changed_by', (SELECT to_jsonb(a)
                           FROM accounts a
                           WHERE a.act_id = COALESCE(row_json ->> 'updated_by', row_json ->> 'created_by')::UUID)); --- to change to use the value of the ticket #436

    FOR i IN 0 .. TG_NARGS - 1
        LOOP
            words := regexp_split_to_array(TG_ARGV[i], '=| -');
            payload_key := words[1];
            foreign_key_column := words[2];
            excluded_columns := words[3:];

            -- The foreign key on this column gives the related table and its referenced column.
            SELECT format('SELECT to_jsonb(t) - $2 FROM %s t WHERE t.%I = $1', c.confrelid::REGCLASS, referenced.attname)
            INTO related_query
            FROM pg_constraint c
                     JOIN pg_attribute referencing ON referencing.attrelid = c.conrelid AND referencing.attnum = c.conkey[1] -- name of the column in the current table
                     JOIN pg_attribute referenced ON referenced.attrelid = c.confrelid AND referenced.attnum = c.confkey[1] -- name of the column in the referenced table
            WHERE c.conrelid = TG_RELID
              AND c.contype = 'f'
              AND referencing.attname = foreign_key_column;

            EXECUTE related_query INTO related_json USING (row_json ->> foreign_key_column)::UUID, excluded_columns;

            payload_json := payload_json || jsonb_build_object(payload_key, related_json);
        END LOOP;

    -- The primary key column gives the record_id.
    SELECT a.attname
    INTO primary_key_column
    FROM pg_constraint c
             JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = c.conkey[1]
    WHERE c.conrelid = TG_RELID
      AND c.contype = 'p';

    INSERT INTO audit(table_name, record_id, operation, payload)
    VALUES (TG_TABLE_NAME, (row_json ->> primary_key_column)::UUID, TG_OP, payload_json);

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

ALTER FUNCTION f_audit() OWNER TO audit_trigger_role;

REVOKE INSERT, UPDATE, DELETE ON audit FROM PUBLIC;

GRANT INSERT ON audit TO audit_trigger_role;

COMMENT ON TABLE audit IS 'Audit table storing every INSERT, UPDATE and DELETE operation performed on the audited tables, the ones with a trigger executing f_audit().';

COMMENT ON COLUMN audit.aud_id IS 'Primary key. UUID automatically generated for each audit record.';
COMMENT ON COLUMN audit.table_name IS 'Name of the audited table where the change happened.';
COMMENT ON COLUMN audit.record_id IS 'Primary key of the changed row. Not enforced as a foreign key to preserve audit history when the source row is deleted.';
COMMENT ON COLUMN audit.operation IS 'Type of SQL operation that produced the audit record (INSERT, UPDATE or DELETE).';
COMMENT ON COLUMN audit.payload IS 'JSONB snapshot of the changed row (row), the account who performed the change (changed_by) and the related rows named by the trigger arguments.';
COMMENT ON COLUMN audit.changed_at IS 'Date and time when the change was recorded. Default is now(). Stored in UTC (TIMESTAMPTZ).';

COMMENT ON INDEX idx_audit_table_name_record_id IS 'Index on table name and row identifier to optimize the lookups of the history of a row.';

COMMENT ON FUNCTION f_audit() IS 'Trigger function recording the changed row in the audit table, with the account found in its updated_by (or created_by) column and the related rows named by the trigger arguments. The audited table needs a single-column UUID primary key.';
