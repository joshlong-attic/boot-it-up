package demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.Date;
import java.util.List;

@ComponentScan
@EnableAutoConfiguration
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@Component
class ReservationCommandLineRunner implements CommandLineRunner {
    @Override
    public void run(String... strings) throws Exception {

        for (Reservation r : reservationRepository.findAll())
            System.out.println(r.toString());
    }

    @Autowired
    ReservationRepository reservationRepository;
}


interface ReservationRepository extends JpaRepository<Reservation, Long> {
    List<Reservation> findByFamilyName(String familyName);
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
