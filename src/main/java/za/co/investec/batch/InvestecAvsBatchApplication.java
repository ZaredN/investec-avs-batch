package za.co.investec.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.flow.JobExecutionDecider;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.config.Task;

@SpringBootApplication
@EnableBatchProcessing //includes job repo, job launcher, job registry, transaction manager
public class InvestecAvsBatchApplication {

    @Autowired
    public JobBuilderFactory jobBuilderFactory;
    @Autowired
    public StepBuilderFactory stepBuilderFactory;

    @Bean
    public JobExecutionDecider decider(){
        return new DeliveryDecider();
    };

    @Bean
    public JobExecutionDecider invoiceDecider(){
        return new InvoiceDecider();
    };
    @Bean
    public Step giveCustomerRefundStep() {
        return stepBuilderFactory.get("giveCustomerRefundStep").tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                System.out.println(String.format("Here is your refund."));
                return RepeatStatus.FINISHED;
            }
        }).build();
    }

    @Bean
    public Step thankCustomerStep() {
        return stepBuilderFactory.get("thankCustomerStep").tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                System.out.println(String.format("Thank you for your service."));
                return RepeatStatus.FINISHED;
            }
        }).build();
    }
    @Bean
    public Step verifyPackageIsCorrectWithCustomer() {
        return stepBuilderFactory.get("verifyPackageIsCorrectWithCustomer").tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                System.out.println(String.format("Check this is the package the customer ordered."));
                return RepeatStatus.FINISHED;
            }
        }).build();
    }

    @Bean
    public Step leaveAtDoorStep() {
        return stepBuilderFactory.get("leaveAtDoorStep").tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                System.out.println(String.format("Leaving package at the door."));
                return RepeatStatus.FINISHED;
            }
        }).build();
    }

    @Bean
    public Step storePackageStep() {
        return stepBuilderFactory.get("storePackageStep").tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

                System.out.println(String.format("Storing the package while the customer address is located."));
                return RepeatStatus.FINISHED;
            }
        }).build();
    }

    @Bean
    public Step givePackageToCustomerStep() {
        return stepBuilderFactory.get("givePackageToCustomer").tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {

                System.out.println(String.format("We have given the package to the customer."));
                return RepeatStatus.FINISHED;
            }
        }).build();
    }
    @Bean
    public Step driveToAddressStep(){
        Boolean GOT_LOST = true;
        return this.stepBuilderFactory.get("driveToAddressStep").tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {
                if(GOT_LOST){
                    throw new RuntimeException("Got lost driving to the address");
                }
                System.out.println("Successfully arrived at the address");
                return RepeatStatus.FINISHED;
            }
        }).build();
    }

    @Bean
    public Step packageItemStep() {
        return stepBuilderFactory.get("packageItemStep").tasklet(new Tasklet() {
            @Override
            public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
                    String item = chunkContext.getStepContext().getJobParameters().get("item").toString();
                    String date = chunkContext.getStepContext().getJobParameters().get("run.date").toString();
                System.out.println(String.format("The %s has been packaged on %s.",item ,date));
                    return RepeatStatus.FINISHED;
            }
        }).build();
    }

    @Bean
    public Job deliverPackageJob() {
        return this.jobBuilderFactory.get("deliverPackageJob")
                .start(packageItemStep())
                .next(driveToAddressStep())
                    .on("FAILED").stop()
                .from(driveToAddressStep())
                    .on("*").to(decider())
                    .on("PRESENT").to(givePackageToCustomerStep())
                   .from(verifyPackageIsCorrectWithCustomer())
                       .next(invoiceDecider()).on("CORRECT").to(thankCustomerStep())
                       .from(invoiceDecider()).on("INCORRECT").to(giveCustomerRefundStep())
                    .from(decider())
                        .on("NOT PRESENT").to(leaveAtDoorStep())
                .end()
                .build();
    }


    public static void main(String[] args) {
		SpringApplication.run(InvestecAvsBatchApplication.class, args);
	}

}
