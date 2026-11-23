package nc.cafat.recouvrement.repo;

import nc.cafat.recouvrement.model.CompteCotisant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;

public interface CompteRepository extends JpaRepository<CompteCotisant, Long> {
    // La base additionne sur la ligne verrouillée, sans écraser un mouvement concurrent.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update CompteCotisant c set c.soldeDebiteur = c.soldeDebiteur + :montant " +
        "where c.employeurId = :employeurId")
    int ajouterAuSolde(@Param("employeurId") Long employeurId, @Param("montant") BigDecimal montant);

    default void majSolde(Long employeurId, BigDecimal montant) {
        if (ajouterAuSolde(employeurId, montant) != 1) {
            throw new IllegalArgumentException("Compte inconnu : " + employeurId);
        }
    }
}
