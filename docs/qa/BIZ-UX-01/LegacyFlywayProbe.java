import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;

class LegacyFlywayProbe {
    public static void main(String[] locations) {
        String url = System.getenv("DB_URL");
        if (!url.matches("jdbc:mariadb://127\\.0\\.0\\.1:\\d+/biz_ux_v5_drift")) {
            throw new IllegalArgumentException("Expected an isolated loopback migration fixture.");
        }
        String user = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");
        var old = Flyway.configure().dataSource(url, user, password).locations("filesystem:" + locations[0]).load();
        if (old.migrate().migrationsExecuted != 5) {
            throw new IllegalStateException("Expected a fresh fixture with old V1 through V5.");
        }
        var current = Flyway.configure().dataSource(url, user, password).locations("filesystem:" + locations[1]).load();
        try {
            current.migrate();
            throw new IllegalStateException("Expected the current migrations to reject old V5.");
        } catch (FlywayValidateException exception) {
            if (!exception.getMessage().contains("Migration checksum mismatch for migration version 5")) throw exception;
            old.validate();
            if (old.info().applied().length != 5) throw new IllegalStateException("Expected the original five history rows.");
            System.out.println("PASS: current Flyway rejected the old V5 checksum; five original history rows remain valid.");
        }
    }
}
