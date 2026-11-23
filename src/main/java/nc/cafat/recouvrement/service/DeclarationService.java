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

    // Le proxy Spring intercepte cette méthode publique et annule aussi sur exception vérifiée.
    @Transactional(rollbackFor = CalculException.class)
    public Declaration deposer(Declaration d) throws CalculException {
        int inseree = declarationRepo.insererSiAbsent(d.getEmployeurId(), d.getReference(), d.getSalaireBrut());
        Declaration enregistree = declarationRepo.findByReference(d.getReference())
            .orElseThrow(() -> new IllegalStateException("Déclaration introuvable après insertion"));

        // Un dépôt déjà enregistré ne doit pas modifier une seconde fois le solde.
        if (inseree == 0) {
            return enregistree;
        }
        BigDecimal montant = bareme.calculer(d);
        compteRepo.majSolde(d.getEmployeurId(), montant);
        jmsTemplate.convertAndSend("avis-echeance", d.getEmployeurId());
        return enregistree;
    }
}
