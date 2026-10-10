package cl.duoc.banco.bff.batch.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

/**
 * Punto de entrada para disparar los 3 procesos batch de la EFT.
 */
@RestController
@RequestMapping("/api/batch")
@RequiredArgsConstructor
public class BatchController {

    private final JobLauncher jobLauncher;
    private final Job reporteTransaccionesJob;
    private final Job calculoInteresesJob;
    private final Job estadosAnualesJob;

    @GetMapping("/reporte-transacciones")
    public Map<String, Object> reporteTransacciones() throws Exception {
        return resumen(ejecutar(reporteTransaccionesJob));
    }

    @GetMapping("/calculo-intereses")
    public Map<String, Object> calculoIntereses() throws Exception {
        return resumen(ejecutar(calculoInteresesJob));
    }

    @GetMapping("/estados-anuales")
    public Map<String, Object> estadosAnuales() throws Exception {
        return resumen(ejecutar(estadosAnualesJob));
    }

    @GetMapping("/procesar")
    public Map<String, Object> procesarTodo() throws Exception {
        List<Map<String, Object>> jobs = new ArrayList<>();
        jobs.add(resumen(ejecutar(reporteTransaccionesJob)));
        jobs.add(resumen(ejecutar(calculoInteresesJob)));
        jobs.add(resumen(ejecutar(estadosAnualesJob)));

        long leidos = jobs.stream().mapToLong(j -> ((Number) j.get("leidos")).longValue()).sum();
        long escritos = jobs.stream().mapToLong(j -> ((Number) j.get("escritos")).longValue()).sum();
        long omitidos = jobs.stream().mapToLong(j -> ((Number) j.get("omitidos")).longValue()).sum();

        return Map.of(
                "estado", "COMPLETED",
                "leidos", leidos,
                "escritos", escritos,
                "omitidos", omitidos,
                "jobs", jobs);
    }

    private JobExecution ejecutar(Job job) throws Exception {
        JobParameters parameters = new JobParametersBuilder()
                .addLong("ejecucion", System.currentTimeMillis())
                .toJobParameters();
        return jobLauncher.run(job, parameters);
    }

    private Map<String, Object> resumen(JobExecution execution) {
        long leidos = execution.getStepExecutions().stream().mapToLong(se -> se.getReadCount()).sum();
        long escritos = execution.getStepExecutions().stream().mapToLong(se -> se.getWriteCount()).sum();
        long omitidos = execution.getStepExecutions().stream().mapToLong(se -> se.getSkipCount()).sum();
        return Map.of(
                "job", execution.getJobInstance().getJobName(),
                "estado", execution.getStatus().name(),
                "leidos", leidos,
                "escritos", escritos,
                "omitidos", omitidos);
    }
}
