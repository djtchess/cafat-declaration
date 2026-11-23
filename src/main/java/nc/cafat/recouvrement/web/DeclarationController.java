package nc.cafat.recouvrement.web;

import nc.cafat.recouvrement.model.CompteCotisant;
import nc.cafat.recouvrement.model.Declaration;
import nc.cafat.recouvrement.repo.CompteRepository;
import nc.cafat.recouvrement.repo.DeclarationRepository;
import nc.cafat.recouvrement.service.CalculException;
import nc.cafat.recouvrement.service.DeclarationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DeclarationController {
    private final DeclarationService service;
    private final DeclarationRepository declarations;
    private final CompteRepository comptes;

    public DeclarationController(DeclarationService service, DeclarationRepository declarations, CompteRepository comptes) {
        this.service = service;
        this.declarations = declarations;
        this.comptes = comptes;
    }

    public record DepotRequest(Long employeurId, String reference, BigDecimal salaireBrut) {}
    public record DepotResponse(Long declarationId, String reference, String message) {}

    @PostMapping("/declarations")
    @ResponseStatus(HttpStatus.CREATED)
    public DepotResponse deposer(@RequestBody DepotRequest request) throws CalculException {
        Declaration d = new Declaration(request.employeurId(), request.reference(), request.salaireBrut());
        service.deposer(d);
        return new DepotResponse(d.getId(), d.getReference(), "Déclaration déposée (version initiale)");
    }

    @GetMapping("/declarations")
    public List<Declaration> lister(@RequestParam Long employeurId) {
        return declarations.findByEmployeurIdOrderByIdAsc(employeurId);
    }

    @GetMapping("/comptes/{employeurId}")
    public CompteCotisant consulterCompte(@PathVariable Long employeurId) {
        return comptes.findById(employeurId).orElseThrow(() ->
            new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte cotisant introuvable"));
    }

    @ExceptionHandler(CalculException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Map<String, String> erreurCalcul(CalculException ex) { return Map.of("erreur", ex.getMessage()); }
}
