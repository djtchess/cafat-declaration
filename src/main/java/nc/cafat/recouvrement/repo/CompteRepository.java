package nc.cafat.recouvrement.repo;

import nc.cafat.recouvrement.model.CompteCotisant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.math.BigDecimal;

public interface CompteRepository extends JpaRepository<CompteCotisant, Long> {
    default void majSolde(Long employeurId, BigDecimal montant) {
        CompteCotisant compte = findById(employeurId)
            .orElseThrow(() -> new IllegalArgumentException("Compte inconnu : " + employeurId));
        compte.setSoldeDebiteur(compte.getSoldeDebiteur().add(montant));
        save(compte);
    }
}
