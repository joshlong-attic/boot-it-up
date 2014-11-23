package demo;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.jms.JMSException;
import javax.persistence.*;
import javax.transaction.Transactional;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.Serializable;
import java.util.Collection;
import java.util.logging.Logger;

/**
 * This class demonstrates some of the many new features in <a href="http://spring.io/projects/spring-boot">Spring Boot 1.2</a> that make the lives of
 * those coming from a Java EE experience much easier. Note that none of this support is  <EM>new</EM> to Spring at
 * large, per-se, but it didn't have easy auto-configuration integration in Spring Boot, specifically.
 * <p/>
 * <OL>
 * <LI>It demonstrates JAX-RS integration (in this case, using Jersey 2.x) in the {@link demo.Application.GreetingEndpoint}</LI>
 * <LI> It uses Hibernate to support JPA. </LI>
 * <LI> It demonstrates doing a global transaction (a.k.a., an XA) using JTA by auto-configuring the Atomikos standalone JTA provider.
 * In this example, JMS and JPA work is done as part of a global transaction. We demonstrate this by creating 3 transactions and simulating a
 * rollback on the third one. You should see printed to the console that there are two records that come back from the JDBC {@link javax.sql.DataSource data source}
 * and two records that are received from the embedded JMS {@link javax.jms.Destination destination}.
 * </LI>
 * <LI> It also uses the Wildfly application server's <EM>awesome</EM> <A href="http://undertow.io/">Undertow embedded HTTP server</A> from RedHat
 * instead of the default Apache Tomcat</LI>.
 * <LI> Just for consistency, I also use JSR 330 (which describes a set of annotations that you can use in proprietary application
 * servers like WebLogic as part of their CDI support, as well as in a portable manner in dependency injection containers like Google Guice
 * or Spring.  I also use a JSR 250 annotation  (defined as part of Java EE 5) to demonstrate lifecycle hooks.
 * </LI>
 * </OL>
 * <p/>
 * This example relies on a Spring Boot auto-configured embedded, in-memory <A href="http://www.h2database.com/html/main.html">H2</A> {@link javax.sql.DataSource} and
 * a Spring Boot auto-configured embedded, in-memory <a href="http://hornetq.jboss.org/">HornetQ</a> {@link javax.jms.ConnectionFactory}.
 * If you wanted to connect to non-embedded instances, it's straightforward to define beans that will be picked up instead.
 * <p/>
 * Though I'm using a lot of fairly familiar Java EE APIs, this is still just typical Spring Boot, so by default you can run this
 * application using {@code java -jar ee.jar} or easily deploy it to process-centric platforms-as-a-service offerings like
 * Heroku or Cloud Foundry. If you want to deploy it to a standalone application server like (like Apache Tomcat, or Websphere, or anything in between),
 * it's straightforward to convert the build into a {@code .war} and deploy it accordingly.
 * <p/>
 * I, personally, would question a lot of these APIs. Do you <EM>really</EM> need distributed, multi-resource transactions? In today's distributed world,
 * consider global transaction managers an architecture smell. Do you <EM>really</EM> want to stay on JAX-RS when Spring
 * offers a much richer, integrated Spring MVC-based stack complete with  MVC, REST, HATEOAS, OAuth and websockets support? It might well
 * be that you do, and -  as always - the choice is yours. That's why this release is so cool! More power, more choice, less (code) fat!
 *
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
            Greeting greeting = new Greeting(name);
            this.entityManager.persist(greeting);
            this.jmsTemplate.convertAndSend("greetings", greeting);
            if (fail) {
                throw new RuntimeException("simulated error");
            }
        }

        public void createGreeting(String name) {
            this.createGreeting(name, false);
        }

        public Collection<Greeting> findAll() {
            return this.entityManager
                    .createQuery("select g from " + Greeting.class.getName() + " g", Greeting.class)
                    .getResultList();
        }

        public Greeting find(Long id) {
            return this.entityManager.find(Greeting.class, id);
        }
    }

    @Named
    @Path("/hello")
    @Produces({MediaType.APPLICATION_JSON})
    public static class GreetingEndpoint {

        @Inject
        private GreetingService greetingService;

        @POST
        public void post(@QueryParam("name") String name) {
            this.greetingService.createGreeting(name);
        }

        @GET
        @Path("/{id}")
        public Greeting get(@PathParam("id") Long id) {
            return this.greetingService.find(id);
        }
    }

    @Entity
    public static class Greeting implements Serializable {

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

    @Named
    public static class TestCommandLineRunner {

        @Inject
        private GreetingService greetingService;

        @PostConstruct
        public void afterPropertiesSet() throws Exception {
            greetingService.createGreeting("Phil");
            greetingService.createGreeting("Dave");
            try {
                greetingService.createGreeting("Josh", true);
            } catch (RuntimeException re) {
                Logger.getLogger(Application.class.getName()).info("caught exception...");
            }
            greetingService.findAll().forEach(System.out::println);
        }
    }

    @Named
    public static class MessageProcessor {

        @JmsListener(destination = "greetings")
        public void processGreeting(Greeting greeting) throws JMSException {
            System.out.println("received message: " + greeting);
        }
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}