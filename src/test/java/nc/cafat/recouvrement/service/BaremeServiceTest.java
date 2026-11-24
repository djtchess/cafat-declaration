package nc.cafat.recouvrement.service;
import nc.cafat.recouvrement.model.Declaration;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
class BaremeServiceTest {
    private final BaremeService bareme = new BaremeService();
    @Test void givenSalaireBrutWhenCalculThenMontantArrondi() throws Exception {
        assertEquals(new BigDecimal("200.01"), bareme.calculer(new Declaration(1001L, "T1", new BigDecimal("1000.03"))));
    }
    @Test void givenSalaireNegatifWhenCalculThenCalculRefuse() {
        assertThrows(CalculException.class, () -> bareme.calculer(new Declaration(1001L, "T1", new BigDecimal("-1"))));
    }
}
