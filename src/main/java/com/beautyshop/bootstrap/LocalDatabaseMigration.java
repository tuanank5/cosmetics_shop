package com.beautyshop.bootstrap;

import org.flywaydb.core.Flyway;

public final class LocalDatabaseMigration {
    private LocalDatabaseMigration() {}

    public static void run() {
        String url = System.getenv("DB_URL");
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");
        if (url == null || url.isBlank() || password == null || password.isBlank()) {
            throw new IllegalStateException("Set DB_URL and DB_PASSWORD before running the migration.");
        }
        migrate(url, user == null || user.isBlank() ? "beautyshop" : user, password);
    }

    static void migrate(String url, String user, String password) {
        Flyway flyway = Flyway.configure()
            .dataSource(url, user, password)
            .locations("classpath:db/migration")
            .baselineOnMigrate(false)
            .ignoreMigrationPatterns(new String[0])
            .load();
        flyway.migrate();
        flyway.validate();
    }
}
