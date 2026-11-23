package nc.cafat.recouvrement.support;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/** Protège les tests qui effacent leurs tables, même quand IntelliJ désactive les conditions Enabled. */
public class TestDatabaseCondition implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        String url = System.getenv("TEST_DATABASE_URL");
        if (url == null || url.isBlank()) {
            return ConditionEvaluationResult.disabled("TEST_DATABASE_URL doit désigner une base PostgreSQL réservée aux tests");
        }
        return ConditionEvaluationResult.enabled("Base de test configurée explicitement");
    }
}
