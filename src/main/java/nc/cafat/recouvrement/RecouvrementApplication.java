package nc.cafat.recouvrement;

import nc.cafat.recouvrement.model.CompteCotisant;
import nc.cafat.recouvrement.repo.CompteRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import java.math.BigDecimal;

@SpringBootApplication
public class RecouvrementApplication {
    public static void main(String[] args) { SpringApplication.run(RecouvrementApplication.class, args); }

    @Bean
    CommandLineRunner initialiserCompte(CompteRepository comptes) {
        return args -> {
            if (!comptes.existsById(1001L)) comptes.save(new CompteCotisant(1001L, BigDecimal.ZERO));
        };
    }
}
