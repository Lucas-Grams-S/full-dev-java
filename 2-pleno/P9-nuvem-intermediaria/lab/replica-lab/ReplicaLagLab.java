import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * P9.08: what a read replica does to a read-after-write. Real PostgreSQL 18 streaming replication (two containers).
 * Usage: java -cp postgresql.jar ReplicaLagLab.java <label> <iterations>
 *   - every iteration writes one row on the PRIMARY and reads it back from the REPLICA immediately (autocommit);
 *   - then does the same but waits until the replica has replayed the LSN of the write ("read your writes").
 */
public class ReplicaLagLab {

    static Connection open(int port) throws Exception {
        Connection c = DriverManager.getConnection("jdbc:postgresql://localhost:" + port + "/postgres", "postgres", "lab");
        c.setAutoCommit(true);
        return c;
    }

    public static void main(String[] args) throws Exception {
        String label = args[0];
        int n = Integer.parseInt(args[1]);
        try (Connection primary = open(55432); Connection replica = open(55433); Statement s = primary.createStatement()) {
            s.execute("create table if not exists transfers_lab (id bigserial primary key, note text)");
            Thread.sleep(500); // let the DDL reach the replica
            int stale = 0;
            long maxVisibleMs = 0;
            long sumVisibleMs = 0;
            for (int i = 0; i < n; i++) {
                long id;
                try (PreparedStatement ins = primary.prepareStatement("insert into transfers_lab(note) values (?) returning id")) {
                    ins.setString(1, label + "-" + i);
                    try (ResultSet rs = ins.executeQuery()) {
                        rs.next();
                        id = rs.getLong(1);
                    }
                }
                long t0 = System.nanoTime();
                boolean seen = visible(replica, id);
                if (!seen) {
                    stale++;
                    while (!visible(replica, id)) {
                        Thread.sleep(5);
                    }
                    long ms = (System.nanoTime() - t0) / 1_000_000;
                    maxVisibleMs = Math.max(maxVisibleMs, ms);
                    sumVisibleMs += ms;
                }
            }
            System.out.printf("%-22s immediate read on the replica missed the row in %d of %d writes (%.1f%%)%n",
                    label, stale, n, 100.0 * stale / n);
            if (stale > 0) {
                System.out.printf("%-22s time until the row became visible: avg %d ms, max %d ms%n", label, sumVisibleMs / stale, maxVisibleMs);
            }

            // Fix: remember the WAL position of the write and read from the replica only after it replayed it.
            int staleAfterWait = 0;
            long waitSum = 0;
            for (int i = 0; i < n; i++) {
                long id;
                String lsn;
                try (Statement st = primary.createStatement()) {
                    try (ResultSet rs = st.executeQuery("insert into transfers_lab(note) values ('" + label + "-lsn-" + i + "') returning id")) {
                        rs.next();
                        id = rs.getLong(1);
                    }
                    try (ResultSet rs = st.executeQuery("select pg_current_wal_lsn()")) {
                        rs.next();
                        lsn = rs.getString(1);
                    }
                }
                long t0 = System.nanoTime();
                while (!replayed(replica, lsn)) {
                    Thread.sleep(5);
                }
                waitSum += (System.nanoTime() - t0) / 1_000_000;
                if (!visible(replica, id)) {
                    staleAfterWait++;
                }
            }
            System.out.printf("%-22s with LSN wait: stale reads = %d of %d, average wait %d ms%n", label, staleAfterWait, n, waitSum / n);
            try (ResultSet rs = replica.createStatement().executeQuery(
                    "select coalesce(extract(epoch from now() - pg_last_xact_replay_timestamp()) * 1000, -1)::int")) {
                rs.next();
                System.out.printf("%-22s replay lag measured on the replica now (now - last replayed commit): %d ms%n", label, rs.getInt(1));
            }
        }
    }

    static boolean visible(Connection replica, long id) throws Exception {
        try (PreparedStatement ps = replica.prepareStatement("select 1 from transfers_lab where id = ?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    static boolean replayed(Connection replica, String lsn) throws Exception {
        try (PreparedStatement ps = replica.prepareStatement("select pg_last_wal_replay_lsn() >= ?::pg_lsn")) {
            ps.setString(1, lsn);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBoolean(1);
            }
        }
    }
}
