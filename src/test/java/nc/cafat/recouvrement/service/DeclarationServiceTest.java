package nc.cafat.recouvrement.service;

import nc.cafat.recouvrement.model.Declaration;
import nc.cafat.recouvrement.repo.CompteRepository;
import nc.cafat.recouvrement.repo.DeclarationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.jms.core.JmsTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeclarationServiceTest {
    @Mock DeclarationRepository declarations;
    @Mock CompteRepository comptes;
    @Mock BaremeService bareme;
    @Mock JmsTemplate jms;
    DeclarationService service;
    Declaration depot;

    @BeforeEach void preparer() {
        service = new DeclarationService(declarations, comptes, bareme, jms);
        depot = new Declaration(1001L, "REF", new BigDecimal("1000.00"));
    }

    @Test void nouveauDepotCalculeCrediteEtEnvoieUnAvis() throws Exception {
        when(declarations.insererSiAbsent(1001L, "REF", depot.getSalaireBrut())).thenReturn(1);
        when(declarations.findByReference("REF")).thenReturn(Optional.of(depot));
        when(bareme.calculer(depot)).thenReturn(new BigDecimal("200.00"));
        assertSame(depot, service.deposer(depot));
        verify(comptes).majSolde(1001L, new BigDecimal("200.00"));
        verify(jms).convertAndSend("avis-echeance", 1001L);
    }

    @Test void rejeuIdentiqueNeRecalculePasEtNeRenvoiePasAvis() throws Exception {
        Declaration initiale = new Declaration(1001L, "REF", new BigDecimal("1000"));
        when(declarations.insererSiAbsent(1001L, "REF", depot.getSalaireBrut())).thenReturn(0);
        when(declarations.findByReference("REF")).thenReturn(Optional.of(initiale));
        assertSame(initiale, service.deposer(depot));
        verifyNoInteractions(bareme, comptes, jms);
    }

    @Test void referenceExistanteNeDeclencheAucunNouveauMouvement() throws Exception {
        Declaration initiale = new Declaration(1002L, "REF", new BigDecimal("1001"));
        when(declarations.insererSiAbsent(1001L, "REF", depot.getSalaireBrut())).thenReturn(0);
        when(declarations.findByReference("REF")).thenReturn(Optional.of(initiale));
        assertSame(initiale, service.deposer(depot));
        verifyNoInteractions(bareme, comptes, jms);
    }
    @Test void echecDuCalculInterromptLeTraitement() throws Exception {
        when(declarations.insererSiAbsent(1001L, "REF", depot.getSalaireBrut())).thenReturn(1);
        when(declarations.findByReference("REF")).thenReturn(Optional.of(depot));
        when(bareme.calculer(depot)).thenThrow(new CalculException("calcul impossible"));
        assertThrows(CalculException.class, () -> service.deposer(depot));
        verifyNoInteractions(comptes, jms);
    }

    @Test void echecMiseAJourCompteInterromptEnvoi() throws Exception {
        when(declarations.insererSiAbsent(1001L, "REF", depot.getSalaireBrut())).thenReturn(1);
        when(declarations.findByReference("REF")).thenReturn(Optional.of(depot));
        when(bareme.calculer(depot)).thenReturn(new BigDecimal("200.00"));
        doThrow(new IllegalArgumentException("Compte inconnu")).when(comptes).majSolde(1001L, new BigDecimal("200.00"));
        assertThrows(IllegalArgumentException.class, () -> service.deposer(depot));
        verifyNoInteractions(jms);
    }
}
