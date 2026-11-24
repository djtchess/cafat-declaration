package nc.cafat.recouvrement.service;

import nc.cafat.recouvrement.model.CompteCotisant;
import nc.cafat.recouvrement.model.Declaration;
import nc.cafat.recouvrement.repo.CompteRepository;
import nc.cafat.recouvrement.repo.DeclarationRepository;
import org.junit.jupiter.api.*;
import nc.cafat.recouvrement.support.TestDatabaseCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

// Exécuter sur une base PostgreSQL dédiée aux tests, jamais sur une base de production.
@SpringBootTest(properties = "spring.datasource.url=${TEST_DATABASE_URL}")
@ExtendWith(TestDatabaseCondition.class)
class DeclarationIntegrationTest {
    @Autowired DeclarationService service;
    @Autowired DeclarationRepository declarations;
    @Autowired CompteRepository comptes;
    @Autowired TransactionTemplate transactions;
    @MockitoBean JmsTemplate jms;
    private static final long EMPLOYEUR = 987654321L;

    @BeforeEach void preparer() {
        declarations.deleteAll();
        comptes.deleteAll();
        comptes.save(new CompteCotisant(EMPLOYEUR, BigDecimal.ZERO));
    }

    @Test void givenSalaireNegatifWhenDepotThenInsertionAnnulee() {
        assertThrows(CalculException.class, () -> service.deposer(new Declaration(EMPLOYEUR, "invalide", new BigDecimal("-1"))));
        assertTrue(declarations.findByReference("invalide").isEmpty());
        assertEquals(0, comptes.findById(EMPLOYEUR).orElseThrow().getSoldeDebiteur().signum());
        verifyNoInteractions(jms);
    }

    @Test void givenDeclarationExistanteWhenDepotIdentiqueThenAucunNouveauMouvement() throws Exception {
        Declaration premiere = service.deposer(new Declaration(EMPLOYEUR, "unique", new BigDecimal("1000")));
        Declaration rejeu = service.deposer(new Declaration(EMPLOYEUR, "unique", new BigDecimal("1000.00")));
        assertEquals(premiere.getId(), rejeu.getId());
        assertEquals(premiere.getId(), service.deposer(new Declaration(EMPLOYEUR, "unique", new BigDecimal("2000"))).getId());
        assertEquals(premiere.getId(), service.deposer(new Declaration(123L, "unique", new BigDecimal("1000"))).getId());
        assertEquals(0, new BigDecimal("200.00").compareTo(comptes.findById(EMPLOYEUR).orElseThrow().getSoldeDebiteur()));
        verify(jms, times(1)).convertAndSend("avis-echeance", EMPLOYEUR);
    }

    @Test void givenCompteAbsentWhenDepotThenDeclarationAnnulee() {
        assertThrows(org.springframework.dao.InvalidDataAccessApiUsageException.class, () -> service.deposer(new Declaration(123L, "sans-compte", new BigDecimal("1000"))));
        assertTrue(declarations.findByReference("sans-compte").isEmpty());
        verifyNoInteractions(jms);
    }

    @Test void givenEchecEnvoiWhenDepotThenDeclarationEtSoldeAnnules() {
        doThrow(new org.springframework.jms.JmsException("envoi impossible") {})
            .when(jms).convertAndSend("avis-echeance", EMPLOYEUR);
        assertThrows(org.springframework.jms.JmsException.class,
            () -> service.deposer(new Declaration(EMPLOYEUR, "jms-erreur", new BigDecimal("1000"))));
        assertTrue(declarations.findByReference("jms-erreur").isEmpty());
        assertEquals(0, comptes.findById(EMPLOYEUR).orElseThrow().getSoldeDebiteur().signum());
    }
    @Test void givenCompteExistantWhenDepotsIdentiquesEtPaiementConcurrentsThenAucunMouvementPerdu() throws Exception {
        ExecutorService executant = Executors.newFixedThreadPool(3);
        CountDownLatch depart = new CountDownLatch(1);
        try {
            Callable<Declaration> depot = () -> { depart.await(); return service.deposer(new Declaration(EMPLOYEUR, "concurrent", new BigDecimal("1000"))); };
            Future<Declaration> premier = executant.submit(depot);
            Future<Declaration> second = executant.submit(depot);
            Future<?> paiement = executant.submit(() -> {
                try { depart.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
                transactions.executeWithoutResult(status -> comptes.majSolde(EMPLOYEUR, new BigDecimal("-50")));
            });
            depart.countDown();
            assertEquals(premier.get(15, TimeUnit.SECONDS).getId(), second.get(15, TimeUnit.SECONDS).getId());
            paiement.get(15, TimeUnit.SECONDS);
            assertEquals(1, declarations.count());
            assertEquals(0, new BigDecimal("150.00").compareTo(comptes.findById(EMPLOYEUR).orElseThrow().getSoldeDebiteur()));
            verify(jms, times(1)).convertAndSend("avis-echeance", EMPLOYEUR);
        } finally { executant.shutdownNow(); }
    }
}
