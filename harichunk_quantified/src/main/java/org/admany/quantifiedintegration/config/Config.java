package org.admany.quantifiedintegration.config;

public final class Config {

    public static final boolean ENABLE_QUANTIFIED = Boolean.parseBoolean(
            System.getProperty("harichunk.quantified.enabled", "true")
    );

    private Config() {
    }
}
