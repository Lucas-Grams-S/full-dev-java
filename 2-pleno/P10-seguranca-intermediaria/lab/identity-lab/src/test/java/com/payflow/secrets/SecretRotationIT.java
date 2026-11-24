package com.payflow.secrets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Rotação de credenciais do banco, no PostgreSQL 18 real: a senha só é conferida quando uma conexão NOVA abre.
 * Compara trocar a senha do único usuário (quebra depois) com a rotação por DOIS usuários alternados (não quebra).
 */
class SecretRotationIT {

    private static final String ADMIN_URL = "jdbc:postgresql://127.0.0.1:55432/postgres";
    private static final String APP_URL = "jdbc:postgresql://127.0.0.1:55432/rotation_lab";

    private HikariDataSource pool;

    private static Connection admin(String url) throws SQLException {
        return DriverManager.getConnection(url, "postgres", "lab-pg-admin");
    }

    @BeforeEach
    void schema() throws Exception {
        try (Connection c = admin(ADMIN_URL); Statement s = c.createStatement()) {
            s.execute("DROP DATABASE IF EXISTS rotation_lab WITH (FORCE)");
            for (String role : new String[] {"app_a", "app_b", "payflow_app", "payflow_migrator", "single_app"}) {
                s.execute("DROP ROLE IF EXISTS " + role);
            }
            s.execute("CREATE ROLE payflow_migrator LOGIN PASSWORD 'migrator-pass-1'");
            s.execute("CREATE ROLE payflow_app NOLOGIN");                         // o grupo: as permissões moram aqui
            s.execute("CREATE ROLE app_a LOGIN PASSWORD 'secret-A-v1' IN ROLE payflow_app");
            s.execute("CREATE ROLE app_b LOGIN PASSWORD 'secret-B-v1' IN ROLE payflow_app");
            s.execute("CREATE ROLE single_app LOGIN PASSWORD 'single-v1'");
            s.execute("CREATE DATABASE rotation_lab OWNER payflow_migrator");
        }
        try (Connection c = DriverManager.getConnection(APP_URL, "payflow_migrator", "migrator-pass-1"); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE accounts (id int PRIMARY KEY, balance numeric(19,2) NOT NULL)");
            s.execute("CREATE TABLE ledger_entries (id serial PRIMARY KEY, account_id int NOT NULL, amount numeric(19,2) NOT NULL)");
            s.execute("INSERT INTO accounts VALUES (1, 100.00)");
            // privilégio mínimo: a aplicação lê e escreve contas, e só LÊ e INSERE lançamentos (regra 5: nunca alterados nem apagados)
            s.execute("GRANT SELECT, INSERT, UPDATE ON accounts TO payflow_app, single_app");
            s.execute("GRANT SELECT, INSERT ON ledger_entries TO payflow_app, single_app");
            s.execute("GRANT USAGE ON SEQUENCE ledger_entries_id_seq TO payflow_app, single_app");
        }
    }

    @AfterEach
    void closePool() {
        if (pool != null) {
            pool.close();
        }
    }

    private HikariDataSource poolFor(String user, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(APP_URL);
        config.setUsername(user);
        config.setPassword(password);
        config.setMaximumPoolSize(3);
        config.setMinimumIdle(3);
        config.setConnectionTimeout(1500);
        config.setInitializationFailTimeout(-1);
        return new HikariDataSource(config);
    }

    private static String currentUser(Connection c) throws SQLException {
        try (Statement s = c.createStatement(); var rs = s.executeQuery("select current_user")) {
            rs.next();
            return rs.getString(1);
        }
    }

    private static void changePassword(String role, String password) throws SQLException {
        try (Connection c = admin(ADMIN_URL); Statement s = c.createStatement()) {
            s.execute("ALTER ROLE " + role + " PASSWORD '" + password + "'");
        }
    }

    /** Executa consultas em laço até a flag pedir para parar; conta as que falharam. */
    private Thread hammer(AtomicInteger ok, AtomicInteger failed, java.util.concurrent.atomic.AtomicBoolean stop) {
        Thread t = Thread.ofPlatform().daemon().start(() -> {
            while (!stop.get()) {
                try (Connection c = pool.getConnection()) {
                    currentUser(c);
                    ok.incrementAndGet();
                } catch (SQLException e) {
                    failed.incrementAndGet();
                }
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        return t;
    }

    @Test
    void changingTheOnlyUsersPassword_breaksTheNextNewConnection_notTheOpenOnes() throws Exception {
        pool = poolFor("single_app", "single-v1");
        try (Connection open = pool.getConnection()) {
            changePassword("single_app", "single-v2");                         // o segredo foi rotacionado no banco...
            System.out.println("T10.07 open connection after the change: " + currentUser(open) + " (still works)");
            assertThat(currentUser(open)).isEqualTo("single_app");             // ...e a conexão aberta continua funcionando
        }
        pool.getHikariPoolMXBean().softEvictConnections();                      // o que o max-lifetime faz, algum dia
        long start = System.nanoTime();
        assertThatThrownBy(() -> pool.getConnection().close())
                .isInstanceOf(SQLException.class);
        System.out.println("T10.07 first NEW connection with the old password failed after "
                + (System.nanoTime() - start) / 1_000_000 + " ms");
    }

    @Test
    void alternatingTwoUsers_rotatesWithZeroFailedQueries_andKillsTheOldCredential() throws Exception {
        pool = poolFor("app_a", "secret-A-v1");
        assertThat(currentOf(pool)).isEqualTo("app_a");

        AtomicInteger ok = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        var stop = new java.util.concurrent.atomic.AtomicBoolean();
        Thread load = hammer(ok, failed, stop);
        Thread.sleep(300);

        // 1) troca a senha do usuário INATIVO (B): ninguém o usa ainda
        changePassword("app_b", "secret-B-v2");
        // 2) o cofre passa a apontar para B; a aplicação relê o segredo e o pool usa o novo usuário nas conexões NOVAS
        pool.getHikariConfigMXBean().setUsername("app_b");
        pool.getHikariConfigMXBean().setPassword("secret-B-v2");
        pool.getHikariPoolMXBean().softEvictConnections();
        Thread.sleep(500);
        System.out.println("T10.07 after the switch, new connections run as: " + currentOf(pool));
        assertThat(currentOf(pool)).isEqualTo("app_b");

        // 3) fim da janela de coexistência: a credencial ANTIGA (A) é invalidada
        changePassword("app_a", "secret-A-v2-never-handed-out");
        Thread.sleep(300);
        stop.set(true);
        load.join();
        System.out.println("T10.07 queries during the rotation: ok=" + ok.get() + " failed=" + failed.get());
        assertThat(ok.get()).isGreaterThan(20);
        assertThat(failed.get()).isZero();

        // o segredo antigo, vazado ontem, não abre mais nada
        assertThatThrownBy(() -> DriverManager.getConnection(APP_URL, "app_a", "secret-A-v1")).isInstanceOf(SQLException.class)
                .hasMessageContaining("password authentication failed");
    }

    /** O mesmo laço de carga, na ORDEM ERRADA (invalida o usuário em uso antes de trocar): prova que a medição enxerga falhas. */
    @Test
    void wrongOrder_invalidatingTheActiveUserBeforeSwitching_makesQueriesFail() throws Exception {
        pool = poolFor("app_a", "secret-A-v1");
        currentOf(pool);
        AtomicInteger ok = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        var stop = new java.util.concurrent.atomic.AtomicBoolean();
        Thread load = hammer(ok, failed, stop);
        Thread.sleep(300);
        changePassword("app_a", "secret-A-v2-invalidated-too-early");
        pool.getHikariPoolMXBean().softEvictConnections();                      // as conexões novas ainda usam A com a senha velha
        Thread.sleep(1500);
        stop.set(true);
        load.join();
        System.out.println("T10.07 WRONG order: ok=" + ok.get() + " failed=" + failed.get());
        assertThat(failed.get()).isGreaterThan(0);
    }

    private static String currentOf(HikariDataSource ds) throws SQLException {
        try (Connection c = ds.getConnection()) {
            return currentUser(c);
        }
    }

    @Test
    void theApplicationRoleHasLeastPrivilege_noDdl_noUpdateOrDeleteOfTheLedger() throws Exception {
        try (Connection c = DriverManager.getConnection(APP_URL, "app_a", "secret-A-v1"); Statement s = c.createStatement()) {
            s.execute("UPDATE accounts SET balance = balance + 1 WHERE id = 1");                    // o que a aplicação faz
            s.execute("INSERT INTO ledger_entries (account_id, amount) VALUES (1, 1.00)");
            for (String forbidden : new String[] {
                    "DROP TABLE accounts", "ALTER TABLE accounts ADD COLUMN x int", "CREATE TABLE evil (id int)",
                    "DELETE FROM ledger_entries", "UPDATE ledger_entries SET amount = 0", "TRUNCATE ledger_entries"}) {
                assertThatThrownBy(() -> s.execute(forbidden)).as(forbidden).isInstanceOf(SQLException.class);
                System.out.println("T10.07 as app_a: " + forbidden + " -> denied");
            }
        }
        // a conta que roda as migrações (Flyway) é OUTRA, e a aplicação não a conhece
        try (Connection c = DriverManager.getConnection(APP_URL, "payflow_migrator", "migrator-pass-1"); Statement s = c.createStatement()) {
            s.execute("ALTER TABLE accounts ADD COLUMN status text");
        }
    }

    @Test
    void passwordsAreStoredAsScramHashes_notReadableEvenToTheAppRole() throws Exception {
        try (Connection c = DriverManager.getConnection(APP_URL, "app_a", "secret-A-v1"); Statement s = c.createStatement()) {
            assertThatThrownBy(() -> s.executeQuery("select rolpassword from pg_authid")).isInstanceOf(SQLException.class);
        }
        try (Connection c = admin(ADMIN_URL); Statement s = c.createStatement(); var rs = s.executeQuery(
                "select substr(rolpassword, 1, 14) from pg_authid where rolname = 'app_a'")) {
            rs.next();
            System.out.println("T10.07 stored verifier starts with: " + rs.getString(1));
            assertThat(rs.getString(1)).startsWith("SCRAM-SHA-256$");
        }
    }
}
