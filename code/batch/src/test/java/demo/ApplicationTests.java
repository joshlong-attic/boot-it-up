package demo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
public class ApplicationTests {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job job;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("file:src/test/resources/input/accounts.txt")
    private Resource accountsResource;

    @Value("file:src/test/resources/input/accountsError.txt")
    private Resource accountsErrorResource;

    @Before
    public void setUp() throws Exception {
        jdbcTemplate.update("delete from account");
    }

    @Test
    public void importAccountDataTest() throws Exception {
        int startingCount = jdbcTemplate.queryForInt("select count(*) from account");
        jobLauncher.run(job, new JobParametersBuilder().addString("inputResource", accountsResource.getFile().getAbsolutePath())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters());

        int accountsAdded = 10;
        Assert.assertEquals(startingCount + accountsAdded, jdbcTemplate.queryForInt("select count(*) from account"));
    }

    @Test
    public void importAccountDataErrorTest() throws Exception {
        int startingCount = jdbcTemplate.queryForInt("select count(*) from account");
        jobLauncher.run(job, new JobParametersBuilder().addString("inputResource", accountsErrorResource.getFile().getAbsolutePath())
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters());
        int accountsAdded = 8;
        Assert.assertEquals((startingCount + accountsAdded), jdbcTemplate.queryForInt("select count(*) from account"));
    }
}
