package nc.cafat.recouvrement.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "declaration")
public class Declaration {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long employeurId;
    @Column(nullable = false)
    private String reference;
    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal salaireBrut;

    protected Declaration() {}

    public Declaration(Long employeurId, String reference, BigDecimal salaireBrut) {
        this.employeurId = employeurId;
        this.reference = reference;
        this.salaireBrut = salaireBrut;
    }

    public Long getId() { return id; }
    public Long getEmployeurId() { return employeurId; }
    public String getReference() { return reference; }
    public BigDecimal getSalaireBrut() { return salaireBrut; }
}
