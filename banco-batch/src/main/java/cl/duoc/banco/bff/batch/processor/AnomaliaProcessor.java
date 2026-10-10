package cl.duoc.banco.bff.batch.processor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import cl.duoc.banco.bff.batch.model.Anomalia;
import cl.duoc.banco.bff.batch.util.ParseoUtil;

/**
 * Detecta anomalias en movimientos_financieros_diarios.csv.
 * Devuelve una {@link Anomalia} cuando el registro es invalido y {@code null}
 * cuando es valido (los validos se descartan aqui; se cargan en otro step).
 */
@Component
public class AnomaliaProcessor implements ItemProcessor<String, Anomalia> {

    private static final Set<String> TIPOS_VALIDOS = Set.of("debito", "credito");

    @Override
    public Anomalia process(String line) {
        String[] c = ParseoUtil.split(line);

        Long id = c.length > 0 ? ParseoUtil.parseLong(c[0]) : null;
        LocalDate fecha = c.length > 1 ? ParseoUtil.parseFecha(c[1]) : null;
        BigDecimal monto = c.length > 2 ? ParseoUtil.parseDecimal(c[2]) : null;
        String tipo = c.length > 3 ? ParseoUtil.normalize(c[3]) : null;

        if (id == null || id <= 0) {
            return new Anomalia(null, fecha, monto, tipo, "id invalido");
        }
        if (fecha == null) {
            return new Anomalia(null, null, monto, tipo, "fecha invalida");
        }
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) {
            return new Anomalia(null, fecha, monto, tipo, "monto vacio, cero o negativo");
        }
        if (tipo == null || !TIPOS_VALIDOS.contains(tipo)) {
            return new Anomalia(null, fecha, monto, tipo, "tipo de movimiento invalido");
        }

        return null;
    }
}
