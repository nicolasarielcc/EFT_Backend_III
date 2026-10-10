package cl.duoc.banco.bff.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ejecuta los 3 jobs batch automaticamente al arrancar si la propiedad
 * {@code batch.run-on-startup} esta habilitada (util en Docker via
 * {@code BATCH_RUN_ON_STARTUP=true}). De lo contrario, se invocan con los
 * endpoints GET /api/batch/... de {@code BatchController}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BancoBatchRunner implements ApplicationRunner {

    private final JobLauncher jobLauncher;
    private final Job reporteTransaccionesJob;
    private final Job calculoInteresesJob;
    private final Job estadosAnualesJob;

    @Value("${batch.run-on-startup:false}")
    private boolean runOnStartup;

    @Override
    public void run(ApplicationArguments args) {
        if (!runOnStartup) {
            return;
        }
        try {
            ejecutar(reporteTransaccionesJob);
            ejecutar(calculoInteresesJob);
            ejecutar(estadosAnualesJob);
            log.info("Proceso batch completo ejecutado al arrancar");
        } catch (Exception e) {
            log.error("Error ejecutando el batch automatico", e);
        }
    }

    private void ejecutar(Job job) throws Exception {
        JobParameters parameters = new JobParametersBuilder()
                .addLong("arranque", System.currentTimeMillis())
                .toJobParameters();
        JobExecution execution = jobLauncher.run(job, parameters);
        log.info("Job {} -> estado: {}", job.getName(), execution.getStatus());
    }
}
