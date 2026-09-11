package com.eaut.canteen.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DBConnection {

    private static final Properties props = new Properties();

    // Env vars (DB_URL/DB_USERNAME/DB_PASSWORD/DB_DRIVER) win when set — lets a hosted deploy
    // (no classpath db.properties baked into the image) point at a DB via platform config,
    // while local dev keeps using db.properties unchanged.
    static {
        try (InputStream in = DBConnection.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                props.load(in);
            } else if (System.getenv("DB_URL") == null) {
                throw new IOException("db.properties not found on classpath and no DB_URL env var set — copy db.properties.example to db.properties, or set DB_URL/DB_USERNAME/DB_PASSWORD env vars");
            }
            String url = resolve("DB_URL", "db.url", null);
            Class.forName(resolve("DB_DRIVER", "db.driver", driverFor(url)));
        } catch (IOException | ClassNotFoundException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DBConnection() {
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

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                resolve("DB_URL", "db.url", null),
                resolve("DB_USERNAME", "db.username", null),
                resolve("DB_PASSWORD", "db.password", null));
    }
}
