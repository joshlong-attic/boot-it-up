package demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
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


@EnableScheduling
@EnableWebSocketMessageBroker
@Configuration
class WebSocketConfiguration
        extends AbstractWebSocketMessageBrokerConfigurer
        implements SchedulingConfigurer {

    @Bean
    ThreadPoolTaskScheduler reservationPool() {
        return new ThreadPoolTaskScheduler();
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/notifications").withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor().corePoolSize(4).maxPoolSize(10);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/queue/", "/topic/");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(reservationPool());
    }
}

@Controller
class ReservationNotificationWebsocketController {

    private SimpMessageSendingOperations messagingTemplate;
    private ReservationRepository reservationRepository;
    private TaskScheduler taskScheduler;

    protected void schedule(List<Reservation> res) {
        for (final Reservation r : res) {
            this.taskScheduler.schedule(
                    new Runnable() {
                        @Override
                        public void run() {
                            triggerReservationNotification(r);
                        }
                    }, r.getDateAndTime());
        }
    }

    protected void triggerReservationNotification(Reservation reservation) {
        System.out.println(reservation.toString());
        messagingTemplate.convertAndSend("/topic/alarms", reservation);
    }

    @Autowired
    ReservationNotificationWebsocketController(
            @Qualifier("reservationPool") TaskScheduler taskScheduler,
            final ReservationRepository reservationRepository,
            SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.taskScheduler = (taskScheduler);
        this.reservationRepository = reservationRepository;

        taskScheduler.schedule(new Runnable() {
            @Override
            public void run() {
                schedule(reservationRepository.findAll());
            }
        }, new Date(System.currentTimeMillis() + (15 * 1000)));

    }


}

@Controller
class ReservationMvcController {

    @Autowired
    ReservationRepository reservationRepository;

    @RequestMapping("/reservations.html")
    String reservations(Model model) {
        model.addAttribute("reservations", this.reservationRepository.findAll());
        return "reservations";
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
