package demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.config.annotation.authentication.configurers.InMemoryClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.OAuth2ServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.OAuth2ServerConfigurer;
import org.springframework.security.oauth2.provider.token.InMemoryTokenStore;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@ComponentScan
@EnableAutoConfiguration
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@Configuration
@EnableWebSecurity
class WebSecurityConfig extends OAuth2ServerConfigurerAdapter {

    private final String applicationName = "reservations";

    // @formatter:off
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                .requestMatchers()
                .and()
                .authorizeRequests()
                .antMatchers("/*html").permitAll()
                .anyRequest().authenticated()
                .and()
                .apply(new OAuth2ServerConfigurer())
                .tokenStore(new InMemoryTokenStore())
                .resourceId(applicationName);
    }
    // @formatter:on

    // @formatter:off
    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {


        List<UserDetails> userDetails = new ArrayList<UserDetails>();
        userDetails.add(new User("user1", "password", AuthorityUtils.createAuthorityList("USER", "read")));
        userDetails.add(new User("user2", "password", AuthorityUtils.createAuthorityList("USER", "read", "write")));

        auth
                .userDetailsService(new InMemoryUserDetailsManager(userDetails))
                .and()
                .apply(new InMemoryClientDetailsServiceConfigurer())
                .withClient("my-client")
                .resourceIds(applicationName)
                .scopes("read", "write")
                .authorities("USER")
                .authorizedGrantTypes("password")
                .secret("123456");
    }
    // @formatter:on


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
