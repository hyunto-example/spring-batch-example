package com.example.springbatchexample.job;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManagerFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.example.springbatchexample.job.model.ClassInformation;
import com.example.springbatchexample.job.model.Teacher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class ProcessorConvertJobConfiguration {

	public static final String JOB_NAME = "ProcessorConvertBatch";
	public static final String BEAN_PREFIX = JOB_NAME + "_";

	private final JobBuilderFactory jobBuilderFactory;
	private final StepBuilderFactory stepBuilderFactory;
	private final EntityManagerFactory entityManagerFactory;

	@Value("${chunkSize:1000}")
	private int chunkSize;

	@Bean(JOB_NAME)
	public Job job() {
		return jobBuilderFactory.get(JOB_NAME)
			.preventRestart()
			.start(filterAndConvertStep())
			.next(txStep())
			.build();
	}

	@Bean(BEAN_PREFIX + "filterAndConvertStep")
	@JobScope
	public Step filterAndConvertStep() {
		return stepBuilderFactory.get(BEAN_PREFIX + "filterAndConvertStep")
			.<Teacher, String>chunk(chunkSize)
			.reader(reader())
			.processor(compositeItemProcessor())
			.writer(writer1())
			.build();
	}

	@Bean(BEAN_PREFIX + "tx")
	@JobScope
	public Step txStep() {
		return stepBuilderFactory.get(BEAN_PREFIX + "tx")
			.<Teacher, ClassInformation>chunk(chunkSize)
			.reader(reader())
			.processor(lazyLoadingProcessor())
			.writer(writer3())
			.build();
	}

	@Bean
	public JpaPagingItemReader<Teacher> reader() {
		return new JpaPagingItemReaderBuilder<Teacher>()
			.name(BEAN_PREFIX + "reader")
			.entityManagerFactory(entityManagerFactory)
			.pageSize(chunkSize)
			.queryString("SELECT t FROM Teacher t")
			.build();
	}

	@Bean
	public CompositeItemProcessor compositeItemProcessor() {
		List<ItemProcessor> delegates = new ArrayList<>();
		delegates.add(filterProcessor());	// converter
		delegates.add(converterProcessor());	// filter

		CompositeItemProcessor processor = new CompositeItemProcessor<>();
		processor.setDelegates(delegates);

		return processor;
	}

	@Bean
	public ItemProcessor<Teacher, String> converterProcessor() {
		return Teacher::getName;
	}

	@Bean
	public ItemProcessor<Teacher, Teacher> filterProcessor() {
		return teacher -> {
			boolean isIgnoreTarget = teacher.getId() % 2 == 0L;
			if (isIgnoreTarget) {
				log.info(">>> Teacher Name={}, isIgnoreTarget={}", teacher.getName(), isIgnoreTarget);
				return null;
			}
			return teacher;
		};
	}

	@Bean
	public ItemProcessor<Teacher, ClassInformation> lazyLoadingProcessor() {
		return teacher -> new ClassInformation("3학년 10반", teacher, teacher.getStudents().size());
	}

	@Bean
	public ItemWriter<String> writer1() {
		return items -> {
			for (String item: items) {
				log.info("Teacher Name={}", item);
			}
		};
	}

	@Bean
	public ItemWriter<ClassInformation> writer3() {
		return items -> {
			for (ClassInformation item: items) {
				log.info("반 정보={}", item);
			}
		};
	}

}
