\set ON_ERROR_STOP 0

SET ROLE payflow_owner;
UPDATE audit_events SET outcome = 'DENIED';
DELETE FROM audit_events;
TRUNCATE audit_events;
SELECT count(*) AS audit_rows_still_there FROM audit_events;
\echo '-- ...but the owner can switch the trigger off (this is why the audit trail also needs a copy the owner cannot reach):'
ALTER TABLE audit_events DISABLE TRIGGER audit_events_no_update;
DELETE FROM audit_events;
SELECT count(*) AS audit_rows_after_owner_disabled_the_trigger FROM audit_events;
ALTER TABLE audit_events ENABLE TRIGGER audit_events_no_update;
RESET ROLE;
