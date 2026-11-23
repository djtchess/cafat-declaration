package nc.cafat.recouvrement.repo;

import nc.cafat.recouvrement.model.Declaration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DeclarationRepository extends JpaRepository<Declaration, Long> {
    List<Declaration> findByEmployeurIdOrderByIdAsc(Long employeurId);
    Optional<Declaration> findByReference(String reference);

    // PostgreSQL arbitre les dépôts concurrents sans invalider la transaction du doublon.
    @Modifying
    @Query(value = "INSERT INTO declaration (employeur_id, reference, salaire_brut) " +
        "VALUES (:employeurId, :reference, :salaireBrut) " +
        "ON CONFLICT (reference) DO NOTHING", nativeQuery = true)
    int insererSiAbsent(@Param("employeurId") Long employeurId, @Param("reference") String reference,
                        @Param("salaireBrut") BigDecimal salaireBrut);
}
