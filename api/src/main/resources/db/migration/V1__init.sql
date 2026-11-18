CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE OR REPLACE FUNCTION update_timestamp()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.update_date = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION update_timestamp() IS 'Trigger function that sets the update_date column to the current timestamp (NOW()) whenever a row in a table is updated.';

CREATE OR REPLACE FUNCTION hierarchy_lock()
    RETURNS TRIGGER AS
$$
BEGIN
    -- One change of a hierarchy at a time: two concurrent changes would otherwise each read the hierarchy from before
    -- the other one, and could close a cycle or miss each other's paths. The closure triggers rely on READ COMMITTED,
    -- where each of their statements reads what was committed while waiting for this lock. Under REPEATABLE READ or
    -- SERIALIZABLE, they would keep reading the snapshot taken before the wait: such a change is rejected.
    -- (PostgreSQL runs READ UNCOMMITTED as READ COMMITTED.)
    IF current_setting('transaction_isolation') NOT IN ('read committed', 'read uncommitted') THEN
        RAISE EXCEPTION 'The hierarchy of % can only be changed in a READ COMMITTED transaction, not in %',
            TG_ARGV[0], upper(current_setting('transaction_isolation'))
            USING ERRCODE = 'invalid_transaction_state';
    END IF;

    -- Taken before the statement locks any row: a transaction waiting for it while holding a row of the hierarchy (a
    -- deleted node, a moved node, a referenced parent) would deadlock with the one holding it and reaching that row.
    -- The OID of the closure table names the lock in pg_locks (classid::regclass) with no registry to keep: user table
    -- OIDs start at 16384, above the namespaces of the sync-api locks, defined in LockNamespaces (sync-api).
    PERFORM pg_advisory_xact_lock(TG_ARGV[0]::regclass::oid::int, 0);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION hierarchy_lock() IS 'Trigger function that takes, until the end of the transaction, the lock of the hierarchy whose closure table is named by its argument, and rejects the change outside READ COMMITTED. For BEFORE ... FOR EACH STATEMENT triggers only: as a row-level BEFORE trigger, its NULL result would skip the row.';

CREATE ROLE audit_trigger_role NOLOGIN;
