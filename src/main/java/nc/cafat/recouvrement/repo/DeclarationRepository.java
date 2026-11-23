package nc.cafat.recouvrement.repo;

import nc.cafat.recouvrement.model.Declaration;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeclarationRepository extends JpaRepository<Declaration, Long> {
    List<Declaration> findByEmployeurIdOrderByIdAsc(Long employeurId);
}
