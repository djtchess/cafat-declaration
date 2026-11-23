package nc.cafat.recouvrement.service;

import nc.cafat.recouvrement.model.Declaration;
import nc.cafat.recouvrement.repo.CompteRepository;
import nc.cafat.recouvrement.repo.DeclarationRepository;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

@Service
public class DeclarationService {
    private final DeclarationRepository declarationRepo;
    private final CompteRepository compteRepo;
    private final BaremeService bareme;
    private final JmsTemplate jmsTemplate;

    public DeclarationService(DeclarationRepository declarationRepo, CompteRepository compteRepo,
                              BaremeService bareme, JmsTemplate jmsTemplate) {
        this.declarationRepo = declarationRepo;
        this.compteRepo = compteRepo;
        this.bareme = bareme;
        this.jmsTemplate = jmsTemplate;
    }

    public void deposer(Declaration d) throws CalculException {
        declarationRepo.save(d);
        calculerCotisations(d);
        jmsTemplate.convertAndSend("avis-echeance", d.getEmployeurId());
    }

    @Transactional
    private void calculerCotisations(Declaration d) throws CalculException {
        BigDecimal montant = bareme.calculer(d);
        compteRepo.majSolde(d.getEmployeurId(), montant);
    }
}
