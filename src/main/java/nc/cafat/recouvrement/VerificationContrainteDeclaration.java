package nc.cafat.recouvrement;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class VerificationContrainteDeclaration implements ApplicationRunner {
    private final JdbcTemplate jdbc;

    public VerificationContrainteDeclaration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Hibernate peut continuer après une erreur DDL (par exemple des références
        // déjà en double). Sans contrainte, ON CONFLICT ne protège aucun dépôt.
        Boolean presente = jdbc.queryForObject("""
            SELECT EXISTS (
              SELECT 1 FROM pg_constraint c
              JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attname = 'reference'
              WHERE c.conrelid = to_regclass('declaration')
                AND c.contype = 'u' AND NOT c.condeferrable
                AND c.conkey = ARRAY[a.attnum]::smallint[]
            )
            """, Boolean.class);
        if (!Boolean.TRUE.equals(presente)) {
            throw new IllegalStateException("Contrainte UNIQUE sur declaration.reference absente. " +
                "Vérifier les références en double et créer la contrainte avant de démarrer (voir README).");
        }
    }
}
