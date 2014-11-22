package demo;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.core.JmsTemplate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * @author Josh Long
 */
@SpringBootApplication
public class Application {

    @Named
    public static class JerseyConfig extends ResourceConfig {

        public JerseyConfig() {
            this.register(GreetingEndpoint.class);
            this.register(JacksonFeature.class);
        }
    }

    @Named
    @Transactional
    public static class GreetingService {

        @Inject
        private JmsTemplate jmsTemplate;

        @PersistenceContext
        private EntityManager entityManager;

        public void createGreeting(String name, boolean fail) {
            this.jmsTemplate.convertAndSend("greetings", name);
            this.entityManager.persist(new Greeting(name));
            if (fail) {
                throw new RuntimeException("simulated error");
            }
        }

        public void createGreeting(String name) {
            this.createGreeting(name, false);
        }

        public Collection<Greeting> findAll() {
            return this.entityManager.createQuery("select g from " + Greeting.class.getName() +
                    " g", Greeting.class)
                    .getResultList();
        }

        public Greeting find(Long id) {
            return this.entityManager.find(Greeting.class, id);
        }
    }

    @Named
    @Path("/hello/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public static class GreetingEndpoint {

        @Inject
        private GreetingService greetingService;

        @PersistenceContext
        private EntityManager entityManager;

        @POST
        public void post(@QueryParam("name") String name) {
            this.greetingService.createGreeting(name);
        }

        @GET
        public Greeting get(@PathParam("id") Long id) {
            return this.entityManager.find(Greeting.class, id);
        }
    }

    @Entity
    public static class Greeting {

        @Id
        @GeneratedValue
        private Long id;

        @Override
        public String toString() {
            return "Greeting{" +
                    "id=" + id +
                    ", message='" + message + '\'' +
                    '}';
        }

        private String message;

        public String getMessage() {
            return message;
        }

        public Greeting(String name) {
            this.message = "Hi, " + name + "!";
        }

        Greeting() {
        }
    }

    @Bean
    CommandLineRunner runner(GreetingService greetingService) {
        return args -> {
            greetingService.createGreeting("Phil");
            greetingService.createGreeting("Dave");
            try {
                greetingService.createGreeting("Josh", true);
            } catch (RuntimeException re) {
                Logger.getLogger(Application.class.getName()).info("caught exception...");
            }
            greetingService.findAll().forEach(System.out::println);
        };
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

