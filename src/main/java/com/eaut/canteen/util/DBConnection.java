package com.eaut.canteen.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * The application's connections to the database, borrowed from a pool.
 *
 * <h2>Why a pool</h2>
 * Every request used to call {@code DriverManager.getConnection()} and open a brand new connection.
 * Measured against the deployed site from Vietnam, that handshake — TCP, then TLS, then the
 * PostgreSQL startup and authentication exchange, all of it across the link from the Render
 * instance to Neon in us-east-2 — cost about <em>400ms on every page that touches the database</em>.
 * For comparison, one query on that same link costs about 57ms, and the queries themselves execute
 * in well under a millisecond (an EXPLAIN ANALYZE of the catalog's main query reports 0.23ms).
 *
 * <p>So the connection, not the querying and certainly not the SQL, was the dominant cost of a page
 * load. Pooling removes it: a borrowed connection is already open, already authenticated, and
 * already warm.
 *
 * <p>This reverses an earlier deliberate choice to run without a pool, which was reasonable while
 * the database was a local one — there, opening a connection costs a fraction of a millisecond and
 * a pool buys nothing but moving parts.
 *
 * <h2>Why HikariCP rather than something hand-written</h2>
 * Returning a connection to a pool correctly means resetting autoCommit, rolling back anything the
 * borrower left uncommitted, noticing connections the network has silently killed, and doing all of
 * it under concurrency. Several servlets here run {@code setAutoCommit(false)} for a transaction, so
 * a pool that failed to reset it would hand the next request a connection that silently never
 * commits. That is not code worth writing twice.
 *
 * <p>The public API is unchanged — {@link #getConnection()} still returns a {@link Connection} that
 * callers close in a try-with-resources, and closing it now returns it to the pool instead of
 * tearing it down. All 59 call sites already did that, so none of them needed touching.
 */
public final class DBConnection {

    private static final Properties props = new Properties();

    /**
     * Small on purpose. Tomcat here serves a canteen, not a stampede, and Neon's free tier caps how
     * many connections it will hold open — a large pool would sit idle and count against that cap
     * for nothing.
     */
    private static final int MAX_POOL_SIZE = 8;

    /**
     * Two connections stay open even while the site is quiet, so the first visitor after a lull
     * does not pay the handshake this class exists to avoid. Note this also keeps Neon's compute
     * from auto-suspending, which is the point — but it does mean the free tier's compute hours are
     * consumed continuously rather than only when someone is ordering.
     */
    private static final int MIN_IDLE = 2;

    private static final HikariDataSource dataSource;

    static {
        try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                props.load(in);
            } else if (System.getenv("DB_URL") == null) {
                throw new IOException("db.properties not found on classpath and no DB_URL env var set — copy db.properties.example to db.properties, or set DB_URL/DB_USERNAME/DB_PASSWORD env vars");
            }
            String url = resolve("DB_URL", "db.url", null);
            Class.forName(resolve("DB_DRIVER", "db.driver", driverFor(url)));
            dataSource = buildPool(url);
        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBConnection() {
    }

    private static HikariDataSource buildPool(String url) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(resolve("DB_USERNAME", "db.username", null));
        config.setPassword(resolve("DB_PASSWORD", "db.password", null));
        config.setMaximumPoolSize(MAX_POOL_SIZE);
        config.setMinimumIdle(MIN_IDLE);
        config.setPoolName("eaut-canteen");

        // Fail a request in a few seconds rather than letting it hang: a page that errors is
        // diagnosable, a page that spins forever looks like the whole site is down.
        config.setConnectionTimeout(10_000);
        // Neon closes idle connections on its side; retiring ours first means a borrower never
        // receives one the server has already hung up on.
        config.setIdleTimeout(120_000);
        config.setMaxLifetime(900_000);
        // isValid() on the JDBC driver, rather than a test query — one fewer round trip on a link
        // where round trips are the entire problem.
        config.setValidationTimeout(3_000);

        // A connection left half-used is a bug this would otherwise hide until it caused a stall.
        // 20s is far longer than any legitimate request here, so it only fires on a real leak.
        config.setLeakDetectionThreshold(20_000);

        // Do NOT refuse to start when the database is unreachable. Neon's free tier suspends its
        // compute after a few minutes of inactivity, so a container starting up may well find
        // nothing listening yet. With Hikari's default fail-fast that would throw inside this
        // class's static initialiser, which in Java is permanent: the class stays in an errored
        // state for the life of the JVM and every page serves a 500 even after the database comes
        // back. Starting anyway means an early request fails and is retried, which recovers.
        config.setInitializationFailTimeout(-1);

        return new HikariDataSource(config);
    }

    /**
     * DB_DRIVER/db.driver still wins when set explicitly, but both supported databases (MySQL for
     * local dev per README, PostgreSQL for the Render+Neon deploy in render-deploy.md) work with
     * zero extra config by picking the driver class from the JDBC URL's own scheme.
     */
    private static String driverFor(String url) {
        if (url != null && url.startsWith("jdbc:postgresql:")) {
            return "org.postgresql.Driver";
        }
        return "com.mysql.cj.jdbc.Driver";
    }

    private static String resolve(String envKey, String propKey, String fallback) {
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }
        String propVal = props.getProperty(propKey);
        return propVal != null ? propVal : fallback;
    }

    /**
     * A pooled connection. Close it — every caller already does, via try-with-resources — and it
     * goes back to the pool rather than being torn down.
     */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
}
