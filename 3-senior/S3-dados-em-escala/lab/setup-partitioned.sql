-- The monthly-partitioned ledger used by S3.04/S3.07 (same design as S3.02, but WITHOUT a default partition: DETACH ... CONCURRENTLY needs that).
CREATE TABLE ledger_p (LIKE ledger_entries INCLUDING DEFAULTS INCLUDING CONSTRAINTS) PARTITION BY RANGE (created_at);
CREATE FUNCTION create_month_partitions(parent regclass, first_month date, n int) RETURNS int LANGUAGE plpgsql AS $$
DECLARE m date; made int := 0; BEGIN
  FOR i IN 0 .. n - 1 LOOP
    m := (first_month + make_interval(months => i))::date;
    EXECUTE format('CREATE TABLE IF NOT EXISTS %I PARTITION OF %s FOR VALUES FROM (%L) TO (%L)',
                   replace(parent::text, '.', '_') || '_' || to_char(m, 'YYYY_MM'), parent, m, (m + interval '1 month')::date);
    made := made + 1;
  END LOOP; RETURN made; END $$;
SELECT create_month_partitions('ledger_p', date '2024-11-01', 25);
ALTER TABLE ledger_p ADD CONSTRAINT pk_ledger_p PRIMARY KEY (id, created_at);
CREATE INDEX idx_ledger_p_account_created ON ledger_p (account_id, created_at DESC);
INSERT INTO ledger_p SELECT * FROM ledger_entries;
ANALYZE ledger_p;
