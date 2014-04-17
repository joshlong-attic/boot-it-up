package demo;

import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@ComponentScan
@EnableJpaRepositories
@EnableAutoConfiguration
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public EmbeddedServletContainerCustomizer containerCustomizer(@Value("${keystore.file}") Resource keystoreFile,
                                                                  @Value("${keystore.pass}") String keystorePass) throws Exception {
        String absoluteKeystoreFile = keystoreFile.getFile().getAbsolutePath();
        return (ConfigurableEmbeddedServletContainer container) -> {
            if (container instanceof TomcatEmbeddedServletContainerFactory) {
                TomcatEmbeddedServletContainerFactory tomcat = (TomcatEmbeddedServletContainerFactory) container;
                tomcat.addConnectorCustomizers(
                        (connector) -> {
                            connector.setPort(8443);
                            connector.setSecure(true);
                            connector.setScheme("https");
                            Http11NioProtocol proto = (Http11NioProtocol) connector.getProtocolHandler();
                            proto.setSSLEnabled(true);
                            proto.setKeystoreFile(absoluteKeystoreFile);
                            proto.setKeystorePass(keystorePass);
                            proto.setKeystoreType("PKCS12");
                            proto.setKeyAlias("tomcat");
                        }
                );
            }
        };
    }
}

interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByFamilyName(String familyName);
}

@RestController
@RequestMapping(value = "/reservations")
class ReservationRestController {

    @Autowired
    ReservationRepository reservationRepository;

    @RequestMapping(method = RequestMethod.GET)
    Collection<Reservation> reservations() {
        return this.reservationRepository.findAll();
    }
}

@Entity
class Reservation {

    @Id
    @GeneratedValue
    private Long id;
    private int groupSize = 1;
    private Date dateAndTime;
    private String familyName;

    @Override
    public String toString() {
        return "Reservation{" +
                "groupSize=" + groupSize +
                ", dateAndTime=" + dateAndTime +
                ", id=" + id +
                ", familyName='" + familyName + '\'' +
                '}';
    }

    public int getGroupSize() {
        return groupSize;
    }

    public Date getDateAndTime() {
        return dateAndTime;
    }

    public Long getId() {
        return id;
    }

    public String getFamilyName() {
        return familyName;
    }

}
