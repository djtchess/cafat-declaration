package nc.cafat.recouvrement.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import nc.cafat.recouvrement.model.CompteCotisant;
import nc.cafat.recouvrement.repo.CompteRepository;
import nc.cafat.recouvrement.repo.DeclarationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import nc.cafat.recouvrement.support.TestDatabaseCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Les tests utilisent une base ou un schéma PostgreSQL réservé aux tests.
@SpringBootTest(properties = "spring.datasource.url=${TEST_DATABASE_URL}")
@AutoConfigureMockMvc
@ExtendWith(TestDatabaseCondition.class)
class DeclarationApiIntegrationTest {
    @Autowired MockMvc api;
    @Autowired ObjectMapper json;
    @Autowired DeclarationRepository declarations;
    @Autowired CompteRepository comptes;
    @MockitoBean JmsTemplate jms;
    private static final long EMPLOYEUR = 987654321L;

    @BeforeEach void preparer() {
        declarations.deleteAll();
        comptes.deleteAll();
        comptes.save(new CompteCotisant(EMPLOYEUR, BigDecimal.ZERO));
    }

    private MvcResult deposer(String reference, long employeur, String salaire) throws Exception {
        return api.perform(post("/api/declarations").contentType(MediaType.APPLICATION_JSON)
            .content("{\"employeurId\":" + employeur + ",\"reference\":\"" + reference + "\",\"salaireBrut\":" + salaire + "}"))
            .andReturn();
    }

    @Test void givenDeclarationValideWhenDepotEtRejeuThenDeclarationVisibleEtSoldeNonDouble() throws Exception {
        MvcResult premier = deposer("api-ref", EMPLOYEUR, "1000.00");
        MvcResult rejeu = deposer("api-ref", EMPLOYEUR, "1000");
        assertEquals(201, premier.getResponse().getStatus());
        assertEquals(201, rejeu.getResponse().getStatus());
        assertEquals(json.readTree(premier.getResponse().getContentAsString()).get("declarationId"),
                     json.readTree(rejeu.getResponse().getContentAsString()).get("declarationId"));
        api.perform(get("/api/declarations").param("employeurId", Long.toString(EMPLOYEUR)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].reference").value("api-ref"));
        api.perform(get("/api/comptes/{id}", EMPLOYEUR)).andExpect(status().isOk())
            .andExpect(jsonPath("$.soldeDebiteur").value(200.00));
        verify(jms, times(1)).convertAndSend("avis-echeance", EMPLOYEUR);
    }

    @Test void givenSalaireNegatifWhenDepotThenErreur422EtAucunMouvement() throws Exception {
        MvcResult resultat = deposer("invalide-api", EMPLOYEUR, "-1");
        assertEquals(422, resultat.getResponse().getStatus());
        assertTrue(json.readTree(resultat.getResponse().getContentAsString()).has("erreur"));
        api.perform(get("/api/declarations").param("employeurId", Long.toString(EMPLOYEUR)))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        api.perform(get("/api/comptes/{id}", EMPLOYEUR)).andExpect(jsonPath("$.soldeDebiteur").value(0));
        verifyNoInteractions(jms);
    }

    @Test void givenReferenceExistanteWhenNouveauDepotThenSoldeInchange() throws Exception {
        assertEquals(201, deposer("api-conflit", EMPLOYEUR, "1000").getResponse().getStatus());
        assertEquals(201, deposer("api-conflit", EMPLOYEUR, "2000").getResponse().getStatus());
        assertEquals(201, deposer("api-conflit", 222L, "1000").getResponse().getStatus());
        assertEquals(1, declarations.count());
        api.perform(get("/api/comptes/{id}", EMPLOYEUR)).andExpect(jsonPath("$.soldeDebiteur").value(200.00));
        verify(jms, times(1)).convertAndSend("avis-echeance", EMPLOYEUR);
    }

    @Test void givenCompteAbsentWhenConsultationThenErreur404() throws Exception {
        api.perform(get("/api/comptes/{id}", 123L)).andExpect(status().isNotFound());
    }
}
