package demo;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.file.transform.LineTokenizer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * This example is based on example in https://github.com/briansjavablog/spring-batch-tutorial.git,
 * but uses the Spring Batch Java Configuration API and Spring Boot and Java 8.
 *
 * @author Brian Hannaway <hannawaybrian@googlemail.com> 
 * @author Josh Long
 */
@SpringBootApplication
@EnableBatchProcessing
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    Job job(JobBuilderFactory jobBuilderFactory, @Qualifier("start") Step start) {
        return jobBuilderFactory.get("importAccountData")
                .start(start)
                .build();
    }

    @Bean
    Step start(StepBuilderFactory stepBuilderFactory,
               FlatFileItemReader<Account> flatFileItemReader,
               ItemWriter<Account> accountItemWriter) {
        return stepBuilderFactory.get("parseAndLoadAccountData").
                <Account, Account>chunk(3)
                .faultTolerant()
                .skip(FlatFileParseException.class)
                .skipLimit(2)
                .reader(flatFileItemReader)
                .writer(accountItemWriter)
                .build();
    }

    @Bean
    @StepScope
    FlatFileItemReader<Account> flatFileItemReader(
            LineMapper<Account> accountDefaultLineMapper,
            @Value("file:#{jobParameters['inputResource']}") Resource resource) {
        FlatFileItemReader<Account> accountFlatFileItemReader = new FlatFileItemReader<>();
        accountFlatFileItemReader.setLinesToSkip(1);
        accountFlatFileItemReader.setLineMapper(accountDefaultLineMapper);
        accountFlatFileItemReader.setResource(resource);
        return accountFlatFileItemReader;
    }

    @Bean
    DelimitedLineTokenizer delimitedLineTokenizer() {
        DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer(",");
        delimitedLineTokenizer.setNames("ACCOUNT_ID,ACCOUNT_HOLDER_NAME,ACCOUNT_CURRENCY,BALANCE".split(","));
        return delimitedLineTokenizer;
    }

    @Bean
    DefaultLineMapper<Account> accountDefaultLineMapper(LineTokenizer lt, FieldSetMapper<Account> accountFieldSetMapper) {
        DefaultLineMapper<Account> accountDefaultLineMapper = new DefaultLineMapper<>();
        accountDefaultLineMapper.setFieldSetMapper(accountFieldSetMapper);
        accountDefaultLineMapper.setLineTokenizer(lt);
        return accountDefaultLineMapper;
    }

    @Bean
    ItemWriter<Account> accountItemWriter(JdbcTemplate jdbcTemplate) {
        return (List<? extends Account> accounts) -> {
            String insertAccount = "insert into account (ACCOUNT_ID, ACCOUNT_HOLDER_NAME, ACCOUNT_CURRENCY, BALANCE) values(?,?,?,?)";
            String updateAccount = "update account set ACCOUNT_HOLDER_NAME=?, ACCOUNT_CURRENCY=?, BALANCE=? where id = ?";
            accounts.forEach(a -> {
                int updated = jdbcTemplate.update(updateAccount, a.getAccountHolderName(), a.getAccountCurrency(), a.getBalance(), a.getId());
                if (updated == 0) {
                    jdbcTemplate.update(insertAccount, a.getId(), a.getAccountHolderName(), a.getAccountCurrency(), a.getBalance());
                }
            });
        };
    }

    @Bean
    FieldSetMapper<Account> accountFieldSetMapper() {
        return (fieldSet) -> new Account(
                fieldSet.readString("ACCOUNT_ID"),
                fieldSet.readString("ACCOUNT_HOLDER_NAME"),
                fieldSet.readString("ACCOUNT_CURRENCY"),
                fieldSet.readBigDecimal("BALANCE"));
    }
}

class Account implements Serializable {

    private static final long serialVersionUID = -3166540015278455392L;
    private String id;
    private String accountHolderName;
    private String accountCurrency;
    private BigDecimal balance;

    public String getId() {
        return id;
    }

    public String getAccountHolderName() {
        return accountHolderName;
    }

    public String getAccountCurrency() {
        return accountCurrency;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public Account(String id, String accountHolderName, String accountCurrency, BigDecimal balance) {
        this.id = id;
        this.accountHolderName = accountHolderName;
        this.accountCurrency = accountCurrency;
        this.balance = balance;
    }

    @Override
    public String toString() {
        return "Account [id=" + id + ", accountHolderName=" + accountHolderName + ", accountCurrency=" + accountCurrency + ", balance=" + balance + "]";
    }
}
