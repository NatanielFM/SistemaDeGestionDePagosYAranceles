package com.ipesmi.pagos.service;

import com.ipesmi.pagos.model.Concepto;
import com.ipesmi.pagos.model.EstadoPago;
import com.ipesmi.pagos.model.Estudiante;
import com.ipesmi.pagos.model.Pago;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/**
 * Reglas de cálculo de saldos, sin acceso a la base de datos (por eso se
 * puede probar de forma aislada).
 * <ul>
 *   <li>Importe a pagar de un concepto = monto, menos el % de beca si el concepto es becable.</li>
 *   <li>Saldo de un concepto = importe - pagos ACREDITADOS (nunca negativo).</li>
 *   <li>Saldo pendiente del estudiante = suma de los saldos de todos los conceptos.
 *       Un estudiante inactivo no acumula deuda.</li>
 * </ul>
 */
public final class CalculadoraSaldos {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private CalculadoraSaldos() {
    }

    public static BigDecimal importeConBeca(Concepto concepto, Estudiante estudiante) {
        BigDecimal monto = concepto.getMonto();
        if (!concepto.isBecable() || !estudiante.tieneBeca()) {
            return monto.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal factor = CIEN.subtract(BigDecimal.valueOf(estudiante.getPorcentajeBeca())).divide(CIEN);
        return monto.multiply(factor).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal descuentoBeca(Concepto concepto, Estudiante estudiante) {
        return concepto.getMonto().setScale(2, RoundingMode.HALF_UP).subtract(importeConBeca(concepto, estudiante));
    }

    public static BigDecimal totalAcreditado(Estudiante e, Concepto c, Collection<Pago> pagos) {
        return sumar(e, c, pagos, true);
    }

    /** Acreditado + pendiente de acreditación: lo que ya está "en camino". */
    public static BigDecimal totalComprometido(Estudiante e, Concepto c, Collection<Pago> pagos) {
        return sumar(e, c, pagos, false);
    }

    public static BigDecimal saldoConcepto(Estudiante e, Concepto c, Collection<Pago> pagos) {
        return importeConBeca(c, e).subtract(totalAcreditado(e, c, pagos)).max(BigDecimal.ZERO);
    }

    /** Máximo que se puede registrar hoy para ese concepto sin pagar de más. */
    public static BigDecimal disponibleParaPagar(Estudiante e, Concepto c, Collection<Pago> pagos) {
        return importeConBeca(c, e).subtract(totalComprometido(e, c, pagos)).max(BigDecimal.ZERO);
    }

    public static BigDecimal saldoPendiente(Estudiante e, Collection<Concepto> conceptos, Collection<Pago> pagos) {
        if (!e.isActivo()) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Concepto c : conceptos) {
            total = total.add(saldoConcepto(e, c, pagos));
        }
        return total;
    }

    private static BigDecimal sumar(Estudiante e, Concepto c, Collection<Pago> pagos, boolean soloAcreditados) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Pago p : pagos) {
            boolean cuenta = soloAcreditados
                    ? p.getEstado() == EstadoPago.ACREDITADO
                    : p.getEstado() != EstadoPago.ANULADO;
            if (cuenta && e.equals(p.getEstudiante()) && c.equals(p.getConcepto())) {
                total = total.add(p.getMonto());
            }
        }
        return total;
    }
}
