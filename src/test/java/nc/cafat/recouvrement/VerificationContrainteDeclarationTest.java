package nc.cafat.recouvrement;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VerificationContrainteDeclarationTest {
    @Test void demarrageRefuseSiLaContrainteEstAbsente() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class))).thenReturn(false);
        var verification = new VerificationContrainteDeclaration(jdbc);
        assertThrows(IllegalStateException.class, () -> verification.run(null));
    }

    @Test void demarrageAutoriseSiLaContrainteExiste() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.queryForObject(anyString(), eq(Boolean.class))).thenReturn(true);
        assertDoesNotThrow(() -> new VerificationContrainteDeclaration(jdbc).run(null));
    }
}
