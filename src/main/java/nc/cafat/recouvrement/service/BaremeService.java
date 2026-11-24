package nc.cafat.recouvrement.service;

import nc.cafat.recouvrement.model.Declaration;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class BaremeService {
    private static final BigDecimal TAUX_DEMONSTRATION = new BigDecimal("0.20");
    private static final String ERREUR_SALAIRE_INVALIDE = "Salaire brut invalide : le barème ne peut pas calculer les cotisations";

    public BigDecimal calculer(Declaration declaration) throws CalculException {
        if (declaration.getSalaireBrut() == null || declaration.getSalaireBrut().signum() < 0) {
            throw new CalculException(ERREUR_SALAIRE_INVALIDE);
        }
        return declaration.getSalaireBrut().multiply(TAUX_DEMONSTRATION)
            .setScale(2, RoundingMode.HALF_UP);
    }
}
