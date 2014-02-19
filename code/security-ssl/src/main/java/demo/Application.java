package demo;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.io.Resource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.Column;
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
    public EmbeddedServletContainerCustomizer containerCustomizer(@Value("${keystore.file}") final Resource keystoreFile,
                                                                  @Value("${keystore.pass}") final String keystorePass) throws Exception {
        final String absoluteKeystoreFile = keystoreFile.getFile().getAbsolutePath();
        return new EmbeddedServletContainerCustomizer() {
            @Override
            public void customize(ConfigurableEmbeddedServletContainerFactory factory) {
                if (factory instanceof TomcatEmbeddedServletContainerFactory) {
                    TomcatEmbeddedServletContainerFactory containerFactory =
                            (TomcatEmbeddedServletContainerFactory) factory;
                    containerFactory.addConnectorCustomizers(new TomcatConnectorCustomizer() {
                        @Override
                        public void customize(Connector connector) {
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
                    });
                }
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

    @Column(name = "group_size")
    private int groupSize = 1;

    @Column(name = "date_and_time")
    private Date dateAndTime;

    @Id
    @Column(name = "id")
    @GeneratedValue
    private Long id;

    @Override
    public String toString() {
        return "Reservation{" +
                "groupSize=" + groupSize +
                ", dateAndTime=" + dateAndTime +
                ", id=" + id +
                ", familyName='" + familyName + '\'' +
                '}';
    }

    @Column(name = "family_name")
    private String familyName;

    public int getGroupSize() {
        return groupSize;
    }

    public void setGroupSize(int groupSize) {
        this.groupSize = groupSize;
    }

    public Date getDateAndTime() {
        return dateAndTime;
    }

    public void setDateAndTime(Date dateAndTime) {
        this.dateAndTime = dateAndTime;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }
}
