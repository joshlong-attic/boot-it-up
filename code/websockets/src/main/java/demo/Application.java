package demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

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

/*
	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/portfolio").withSockJS();
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
//		registry.enableStompBrokerRelay("/queue/", "/topic/");
		registry.setApplicationDestinationPrefixes("/app");
	}
*/

@EnableScheduling
@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfiguration implements WebSocketMessageBrokerConfigurer, SchedulingConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/notifications").withSockJS();
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration channelRegistration) {
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration channelRegistration) {
    }

    @Override
    public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
        return true;
    }

    @Bean
    ThreadPoolTaskScheduler reservationPool() {
        return new ThreadPoolTaskScheduler();
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setTaskScheduler(this.reservationPool());
    }
}

@Controller
class ReservationNotificationWebsocketController {

    private SimpMessageSendingOperations messagingTemplate;
    private ReservationRepository reservationRepository;
    private TaskScheduler taskScheduler;
    private String notificationsDestination = "/topic/notifications";

    protected void schedule(List<Reservation> res) {
        for (final Reservation r : res) {

            this.taskScheduler.schedule(new Runnable() {
                @Override
                public void run() {
                    triggerReservationNotification(r);
                }
            }, r.getDateAndTime());
        }
    }

    @Autowired
    ReservationNotificationWebsocketController(
            @Qualifier("reservationPool") TaskScheduler taskScheduler,
            ReservationRepository reservationRepository,
            SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        this.taskScheduler = new ThreadPoolTaskScheduler();
        this.reservationRepository = reservationRepository;
    //    this.schedule(this.reservationRepository.findAll());
    }

    protected void triggerReservationNotification(Reservation reservation) {
        messagingTemplate.send(
                this.notificationsDestination, MessageBuilder.withPayload(reservation).build());
    }

    @Scheduled(fixedDelay = 10000)
    public void runEveryTenSeconds() {

        Reservation reservation = new Reservation();
        reservation.setDateAndTime(new Date(System.currentTimeMillis()));
        reservation.setFamilyName(Math.random() > .5 ? "Longs" : "Changs");
        reservation.setGroupSize(2);
        reservation.setId((long) Math.round(100));
        triggerReservationNotification(reservation);
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
