package nc.cafat.recouvrement.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "compte_cotisant")
public class CompteCotisant {
    @Id
    private Long employeurId;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal soldeDebiteur;

    protected CompteCotisant() {}

    public CompteCotisant(Long employeurId, BigDecimal soldeDebiteur) {
        this.employeurId = employeurId;
        this.soldeDebiteur = soldeDebiteur;
    }

    public Long getEmployeurId() { return employeurId; }
    public BigDecimal getSoldeDebiteur() { return soldeDebiteur; }
    public void setSoldeDebiteur(BigDecimal soldeDebiteur) { this.soldeDebiteur = soldeDebiteur; }
}
