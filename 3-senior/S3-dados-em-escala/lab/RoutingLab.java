import java.sql.*;
import java.util.*;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.*;

/** S3.03: read/write routing with Spring (AbstractRoutingDataSource + LazyConnectionDataSourceProxy), a lag/health gate and an LSN token for read-your-writes. */
public class RoutingLab {
    enum Node { PRIMARY, REPLICA }

    static final String PRIMARY_URL = "jdbc:postgresql://127.0.0.1:55501/payflow?connectTimeout=2";
    static final String REPLICA_URL = "jdbc:postgresql://127.0.0.1:55502/payflow?connectTimeout=2";
    static final ThreadLocal<String> MIN_LSN = new ThreadLocal<>();   // "the replica must have replayed at least this"

    static DataSource plain(String url) { return new DriverManagerDataSource(url, "postgres", "lab"); }

    /** Knows whether the replica is usable now (cached probe) and whether it has replayed an LSN. */
    static class ReplicaGate {
        final DataSource replica; long downUntil = 0; int fallbacks = 0;
        ReplicaGate(DataSource replica) { this.replica = replica; }
        boolean usable() {
            long now = System.currentTimeMillis();
            if (now < downUntil) return false;
            try (Connection c = replica.getConnection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("select pg_is_in_recovery()")) {
                return r.next() && r.getBoolean(1);
            } catch (SQLException e) { downUntil = now + 3000; return false; }   // circuit: do not retry for 3 s
        }
        boolean caughtUp(String lsn, long waitMs) {
            long end = System.currentTimeMillis() + waitMs;
            do {
                try (Connection c = replica.getConnection(); PreparedStatement p = c.prepareStatement("select pg_last_wal_replay_lsn() >= ?::pg_lsn")) {
                    p.setString(1, lsn);
                    try (ResultSet r = p.executeQuery()) { if (r.next() && r.getBoolean(1)) return true; }
                } catch (SQLException e) { return false; }
                try { Thread.sleep(20); } catch (InterruptedException e) { return false; }
            } while (System.currentTimeMillis() < end);
            return false;
        }
    }

    static class Router extends AbstractRoutingDataSource {
        final ReplicaGate gate; final boolean useToken;
        Router(DataSource primary, DataSource replica, ReplicaGate gate, boolean useToken) {
            this.gate = gate; this.useToken = useToken;
            setTargetDataSources(Map.of(Node.PRIMARY, primary, Node.REPLICA, replica));
            setDefaultTargetDataSource(primary);
            afterPropertiesSet();
        }
        @Override protected Object determineCurrentLookupKey() {
            if (!TransactionSynchronizationManager.isCurrentTransactionReadOnly()) return Node.PRIMARY;
            if (!gate.usable()) { gate.fallbacks++; return Node.PRIMARY; }
            String min = MIN_LSN.get();
            if (useToken && min != null && !gate.caughtUp(min, 300)) { gate.fallbacks++; return Node.PRIMARY; }
            return Node.REPLICA;
        }
    }

    record Setup(DataSource ds, TransactionTemplate rw, TransactionTemplate ro, JdbcTemplate jdbc, ReplicaGate gate) {
        static Setup of(boolean lazy, boolean useToken) {
            var gate = new ReplicaGate(plain(REPLICA_URL));
            DataSource router = new Router(plain(PRIMARY_URL), plain(REPLICA_URL), gate, useToken);
            DataSource ds = lazy ? new LazyConnectionDataSourceProxy(router) : router;
            var tm = new DataSourceTransactionManager(ds);
            var ro = new TransactionTemplate(tm); ro.setReadOnly(true);
            return new Setup(ds, new TransactionTemplate(tm), ro, new JdbcTemplate(ds), gate);
        }
        boolean inRecovery() { return Boolean.TRUE.equals(ro.execute(s -> jdbc.queryForObject("select pg_is_in_recovery()", Boolean.class))); }
        /** Write on the primary and return the LSN token (the WAL position right after the commit). */
        String write(long id) {
            rw.executeWithoutResult(s -> jdbc.update("insert into rw_probe(id, v) values (?, 'x')", id));
            return rw.execute(s -> jdbc.queryForObject("select pg_current_wal_lsn()::text", String.class));
        }
        boolean visible(long id) { return Boolean.TRUE.equals(ro.execute(s -> jdbc.queryForObject("select exists (select 1 from rw_probe where id = ?)", Boolean.class, id))); }
    }

    public static void main(String[] a) {
        switch (a[0]) {
            case "routing" -> {
                for (boolean lazy : new boolean[] { false, true }) {
                    var s = Setup.of(lazy, false); int onReplica = 0;
                    for (int i = 0; i < 10; i++) if (s.inRecovery()) onReplica++;
                    System.out.printf("%-34s read-only transactions served by the REPLICA: %d of 10%n",
                        lazy ? "with LazyConnectionDataSourceProxy" : "WITHOUT LazyConnectionDataSourceProxy", onReplica);
                }
                var s = Setup.of(true, false);
                boolean wOnPrimary = Boolean.FALSE.equals(s.rw.execute(x -> s.jdbc.queryForObject("select pg_is_in_recovery()", Boolean.class)));
                System.out.println("write transaction served by the PRIMARY: " + wOnPrimary);
            }
            case "stale" -> {   // a[1] = iterations, a[2] = "token" | "notoken"
                int n = Integer.parseInt(a[1]); boolean token = a[2].equals("token");
                var s = Setup.of(true, token); int stale = 0; long totalNs = 0;
                for (int i = 0; i < n; i++) {
                    long id = System.nanoTime();
                    String lsn = s.write(id);
                    if (token) MIN_LSN.set(lsn); else MIN_LSN.remove();
                    long t0 = System.nanoTime(); boolean ok = s.visible(id); totalNs += System.nanoTime() - t0;
                    if (!ok) stale++;
                }
                MIN_LSN.remove();
                System.out.printf("%-8s read right after the write: %d of %d did NOT see it | fallbacks to the primary: %d | read latency avg %.1f ms%n",
                    a[2], stale, n, s.gate.fallbacks, totalNs / 1e6 / n);
            }
            case "down" -> {
                var s = Setup.of(true, false); int ok = 0; long worst = 0;
                for (int i = 0; i < 20; i++) {
                    long t0 = System.nanoTime();
                    try { s.inRecovery(); ok++; } catch (Exception e) { System.out.println("read failed: " + e.getMessage()); }
                    worst = Math.max(worst, System.nanoTime() - t0);
                }
                System.out.printf("replica stopped: %d of 20 reads succeeded | fallbacks to the primary: %d | slowest read %.0f ms%n", ok, s.gate.fallbacks, worst / 1e6);
            }
            default -> throw new IllegalArgumentException(a[0]);
        }
    }
}
