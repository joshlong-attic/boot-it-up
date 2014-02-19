package demo;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationContextLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.AfterTransaction;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Application.class, loader = SpringApplicationContextLoader.class)
@TransactionConfiguration(defaultRollback = true)
public class ReservationRepositoryTests {

    @Autowired
    ReservationRepository reservationRepository;

    @Autowired
    PlatformTransactionManager platformTransactionManager;

    Log log = LogFactory.getLog(getClass());

    @AfterTransaction
    public void howManyRecordsAreInTheDatabaseAfterTransaction() {
        audit("the transaction");
    }

    @After
    public void howManyRecordsAreInTheDatabaseAfterTest() {
        audit("the test");
    }

    protected void audit(String after) {
        int si = this.reservationRepository.findAll().size();
        log.info(String.format("there are %s results in the DB after %s", si, after));
    }

    @Test
    public void test_findByFamilyName() {
        for (int i = 0; i < 3; i++) {
            String familyName = "FamilyName#" + i;
            Reservation cachedReservation = this.reservation(familyName, new Date(), (int) (Math.random() * 10));
            assertThat(cachedReservation).isNotNull();
            List<Reservation> reservationsFromDatabase = this.reservationRepository.findByFamilyName(familyName);
            assertThat(reservationsFromDatabase).hasSize(1);
            assertThat(cachedReservation).isIn(reservationsFromDatabase);
        }
    }

    @Test
    public void test_platformTransactionManagerIsNotNull() {
        assertThat(this.platformTransactionManager).isNotNull();
    }

    @Test
    public void test_loadsAllReservations() {
        assertThat(this.reservationRepository.findAll()).hasSize(3);
    }

    protected Reservation reservation(String familyName, Date date, int s) {
        Reservation reservation = new Reservation();
        reservation.setGroupSize(s);
        reservation.setFamilyName(familyName);
        reservation.setDateAndTime(date);
        this.reservationRepository.save(reservation);
        return reservation;
    }


}