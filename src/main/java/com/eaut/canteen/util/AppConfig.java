package com.eaut.canteen.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class AppConfig {

    private static final Properties props = new Properties();

    static {
        try (InputStream in = AppConfig.class.getResourceAsStream("/app.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private AppConfig() {
    }

    /**
     * Env var override wins when set (e.g. "google.clientId" -> "GOOGLE_CLIENTID"), so a hosted
     * deploy with no classpath app.properties can be configured via platform env vars while local
     * dev keeps using app.properties unchanged.
     */
    public static String get(String key) {
        String envKey = key.toUpperCase().replace('.', '_');
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal;
        }
        return props.getProperty(key);
    }
}
