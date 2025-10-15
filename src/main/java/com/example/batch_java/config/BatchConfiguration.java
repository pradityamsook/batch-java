package com.example.batch_java.config;

import com.example.batch_java.model.Coffee;
import com.example.batch_java.processor.CoffeeItemPremiumProcessor;
import com.example.batch_java.processor.CoffeeItemProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;

@Slf4j
@Configuration
@EnableBatchProcessing
// Note: @EnableBatchProcessing is often not needed in Spring Boot 3+
public class BatchConfiguration {
    @Autowired
    JobBuilderFactory jobBuilderFactory;

    @Autowired
    StepBuilderFactory stepBuilderFactory;

    // =====================================================================================
    // JOB 1: Store coffe data in h2 database
    // =====================================================================================
    @Bean
    public FlatFileItemReader<Coffee> reader() {
        log.info("Creating CSV reader bean.");
        return new FlatFileItemReaderBuilder<Coffee>()
                .name("coffeeItemReader")
                .resource(new ClassPathResource("coffee.csv"))
                .delimited()
                .names("brand", "origin", "characteristics")
                .fieldSetMapper(new BeanWrapperFieldSetMapper<Coffee>() {{
                    setTargetType(Coffee.class);
                }})
                .linesToSkip(1)
                .build();
    }

    @Bean
    public CoffeeItemProcessor processor() {
        log.info("Creating processor bean.");
        return new CoffeeItemProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<Coffee> writer(DataSource dataSource) {
        log.info("Creating JDBC writer bean.");
        return new JdbcBatchItemWriterBuilder<Coffee>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .sql("INSERT INTO coffee (brand, origin, characteristics) VALUES (:brand, :origin, :characteristics)")
                .dataSource(dataSource)
                .build(); // .beanMapped() is deprecated and not needed with this setup
    }

    @Bean
    public Step importDataCoffeeStep(JobRepository jobRepository,
                      PlatformTransactionManager transactionManager,
                      FlatFileItemReader<Coffee> reader, // <-- Correctly Injected
                      CoffeeItemProcessor processor,     // <-- Correctly Injected
                      JdbcBatchItemWriter<Coffee> writer) {
        return stepBuilderFactory.get("importDataCoffeeStep")
                .<Coffee, Coffee>chunk(10)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    // Do step job
    @Bean
    public Job importCoffeeJob(@Qualifier("importDataCoffeeStep") Step importDataCoffeeStep, JobCompletionNotificationListener listener) {
        return jobBuilderFactory.get("importCoffeeJob")
                .listener(listener)
                .start(importDataCoffeeStep)
                .build();
    }

    // ========================================================================
    // ===== JOB 2: italianCoffeeReader (Reader-Processor-Writer) =====
    // ========================================================================
    @Bean
    public JdbcCursorItemReader<Coffee> italianCoffeeReader (DataSource dataSource) {
        String query = "SELECT id, brand, origin, characteristics FROM coffee WHERE LOWER(origin) = 'italy'";
        return new JdbcCursorItemReaderBuilder<Coffee>()
                .name("italianCoffeeReader")
                .dataSource(dataSource)
                .sql(query)
                .rowMapper(new BeanPropertyRowMapper<>(Coffee.class))
                .build();
    }

    @Bean
    public CoffeeItemPremiumProcessor processorPremium() {
        log.info("Processor premium coffee.");

        return new CoffeeItemPremiumProcessor();
    }

    @Bean public JdbcBatchItemWriter<Coffee> writerPremiumCoffee(DataSource dataSource) {
        log.info("Update coffee to premium coffee. ");
        return new JdbcBatchItemWriterBuilder<Coffee>()
                .itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
                .dataSource(dataSource)
                .sql("UPDATE coffee SET characteristics = :characteristics WHERE id = :id")
                .beanMapped()
                .build();
    }

    @Bean Step processPremiumCoffeeStep(JdbcCursorItemReader<Coffee> premiumCoffeeReader,
                                        CoffeeItemPremiumProcessor premiumCoffeeProcessor,
                                        @Qualifier("writerPremiumCoffee") JdbcBatchItemWriter<Coffee> writerPremiumCoffee) {
        return stepBuilderFactory.get("processItalianCoffeeStep")
                .<Coffee, Coffee>chunk(10)
                .reader(premiumCoffeeReader)
                .processor(premiumCoffeeProcessor)
                .writer(writerPremiumCoffee)
                .build();
    }

    // =====================================================================================
    // JOB 3: A job to delete 'tea' records
    // =====================================================================================
    @Bean
    public Tasklet deleteTeaTasklet(DataSource dataSource) {
        return (stepContribution, chunkContext) -> {
            JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
            String sql = "DELETE FROM coffee WHERE characteristics = 'tea'";
            int rowAffected = jdbcTemplate.update(sql);
            log.info("DELETE {} ROWS WITH 'tea' CHARACTERISTIC.", rowAffected);

            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step deleteTeaStep(Tasklet deleteTeaTasklet) {
        return stepBuilderFactory.get("deleteTeaStep")
                .tasklet(deleteTeaTasklet)
                .build();
    }

    @Bean
    Job deleteTeaJob(@Qualifier("deleteTeaStep") Step deleteTeaStep) {
        return jobBuilderFactory.get("deleteTeaJob")
                .start(deleteTeaStep)
                .build();
    }

    // =====================================================================================
    // JOB multi: run multi job
    // =====================================================================================
    @Bean
    Job multiStepCoffeeJob(@Qualifier("processPremiumCoffeeStep") Step processPremiumCoffeeStep,
                           @Qualifier("deleteTeaStep") Step deleteTeaStep) {
        return jobBuilderFactory.get("multiStepCoffeeJob")
                .start(processPremiumCoffeeStep)
                .next(deleteTeaStep)
                .build();
    }

}