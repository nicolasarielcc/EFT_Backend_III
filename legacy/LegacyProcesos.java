package legacy;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Procesos BATCH LEGACY del Banco XYZ (simulacion del mainframe COBOL + Shell).
 *
 * <p><b>Proposito:</b> representar la "copia legacy" que debe migrarse a Spring
 * Batch (requerimiento 1.a de la EFT). Implementa los 3 procesos originales con
 * Java plano (sin framework, sin normalizacion) para evidenciar las limitaciones
 * que motivan la reescritura:</p>
 *
 * <ul>
 *   <li>Solo entiende un formato de fecha (YYYY-MM-DD); los demas formatos del
 *       dataset (YYYY/MM/DD, DD-MM-YYYY, DD/MM/YYYY) son <b>descartados</b>.</li>
 *   <li>No normaliza montos, no detecta anomalias y no reintenta fallos.</li>
 *   <li>Sin reejecucion, sin paralelismo y sin trazabilidad.</li>
 * </ul>
 *
 * <p>La version <b>reescrita</b> (equivalente) vive en el modulo Spring Batch
 * {@code banco-batch}, que normaliza fechas (4 formatos), aplica retry/skip,
 * detecta anomalias, genera resumenes y permite reejecucion automatica.</p>
 *
 * <p><b>Uso:</b>{@code javac legacy/LegacyProcesos.java} y luego
 * {@code java legacy.LegacyProcesos <dir_datos> <dir_salida>}.</p>
 */
public class LegacyProcesos {

    private static final DateTimeFormatter SOLO_ISO = DateTimeFormatter.ISO_LOCAL_DATE;

    public static void main(String[] args) throws IOException {
        Path dataDir = Paths.get(args.length > 0 ? args[0] : "legacy/data");
        Path outDir = Paths.get(args.length > 1 ? args[1] : "legacy/output");
        Files.createDirectories(outDir);

        System.out.println("=== PROCESOS LEGACY (copia original) ===");

        // 1) Reporte de Transacciones Diarias
        reporteTransaccionesDiarias(dataDir.resolve("movimientos_financieros_diarios.csv"),
                outDir.resolve("legacy_transacciones.csv"));
        // 2) Calculo de Intereses
        calculoIntereses(dataDir.resolve("intereses_trimestrales.csv"),
                outDir.resolve("legacy_intereses.csv"));
        // 3) Estados de Cuenta Anuales
        estadosDeCuentaAnuales(dataDir.resolve("estados_financieros_anuales.csv"),
                outDir.resolve("legacy_estados.csv"));

        System.out.println("=== FIN (ver salidas en " + outDir + ") ===");
    }

    /** Proceso 1: transacciones diarias (solo lee filas ISO; el resto las descarta). */
    private static void reporteTransaccionesDiarias(Path in, Path out) throws IOException {
        int procesadas = 0;
        int descartadas = 0;
        StringBuilder sb = new StringBuilder("id,fecha,monto,tipo\n");
        try (BufferedReader br = Files.newBufferedReader(in, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] c = line.split(",", -1);
                if (c.length < 4) { descartadas++; continue; }
                try {
                    LocalDate fecha = LocalDate.parse(c[1].trim(), SOLO_ISO);
                    new java.math.BigDecimal(c[2]); // valida numero
                    sb.append(c[0]).append(',').append(fecha).append(',').append(c[2]).append(',').append(c[3]).append('\n');
                    procesadas++;
                } catch (RuntimeException e) {
                    // Fecha no ISO (YYYY/MM/DD, DD/MM/YYYY...) o numero invalido -> SE DESCARTA
                    System.err.println("[LEGACY-ERROR] registro descartado: " + line);
                    descartadas++;
                }
            }
        }
        Files.writeString(out, sb.toString());
        System.out.printf("Proceso 1 (transacciones): procesadas=%d descartadas=%d%n", procesadas, descartadas);
    }

    /** Proceso 2: calculo de intereses (copia saldo base; no valida negativos/duplicados). */
    private static void calculoIntereses(Path in, Path out) throws IOException {
        int procesadas = 0;
        StringBuilder sb = new StringBuilder("cuenta_id,nombre,saldo,edad,tipo\n");
        try (BufferedReader br = Files.newBufferedReader(in, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] c = line.split(",", -1);
                if (c.length < 5) continue;
                try {
                    new java.math.BigDecimal(c[2].trim());
                    sb.append(line).append('\n');
                    procesadas++;
                } catch (NumberFormatException e) {
                    System.err.println("[LEGACY-ERROR] saldo invalido: " + line);
                }
            }
        }
        Files.writeString(out, sb.toString());
        System.out.println("Proceso 2 (intereses): procesadas=" + procesadas);
    }

    /** Proceso 3: estados anuales (fusiona fecha ISO; el resto descartado). */
    private static void estadosDeCuentaAnuales(Path in, Path out) throws IOException {
        int procesadas = 0;
        int descartadas = 0;
        StringBuilder sb = new StringBuilder("cuenta_id,fecha,transaccion,monto,descripcion\n");
        try (BufferedReader br = Files.newBufferedReader(in, StandardCharsets.UTF_8)) {
            String line;
            boolean first = true;
            while ((line = br.readLine()) != null) {
                if (first) { first = false; continue; }
                String[] c = line.split(",", -1);
                if (c.length < 5) { descartadas++; continue; }
                try {
                    LocalDate fecha = LocalDate.parse(c[1].trim(), SOLO_ISO);
                    new java.math.BigDecimal(c[3]);
                    sb.append(c[0]).append(',').append(fecha).append(',').append(c[2])
                      .append(',').append(c[3]).append(',').append(c[4]).append('\n');
                    procesadas++;
                } catch (RuntimeException e) {
                    System.err.println("[LEGACY-ERROR] registro descartado: " + line);
                    descartadas++;
                }
            }
        }
        Files.writeString(out, sb.toString());
        System.out.printf("Proceso 3 (estados anuales): procesadas=%d descartadas=%d%n", procesadas, descartadas);
    }
}