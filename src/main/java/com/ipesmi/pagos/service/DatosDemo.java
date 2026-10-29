package com.ipesmi.pagos.service;

import com.ipesmi.pagos.exception.ArancelException;
import com.ipesmi.pagos.factory.SolicitudPago;
import com.ipesmi.pagos.model.Concepto;
import com.ipesmi.pagos.model.Estudiante;
import com.ipesmi.pagos.model.MedioPago;
import com.ipesmi.pagos.model.Pago;
import com.ipesmi.pagos.model.TipoConcepto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Carga los mismos datos de ejemplo que sql/pagos_aranceles.sql cuando se usa
 * el modo demo (H2) y la base está vacía. Pasa por los servicios, así que
 * también ejercita las validaciones y el log.
 */
public final class DatosDemo {

    private DatosDemo() {
    }

    public static void cargarSiVacia(AppContext ctx) {
        if (!ctx.estudiantes().listar().isEmpty() || !ctx.conceptos().listar().isEmpty()) {
            return;
        }
        try {
            cargar(ctx);
        } catch (ArancelException e) {
            throw new IllegalStateException("Datos de ejemplo inválidos: " + e.getMessage(), e);
        }
    }

    private static void cargar(AppContext ctx) throws ArancelException {
        EstudianteService es = ctx.estudiantes();
        ConceptoService cs = ctx.conceptos();
        PagoService ps = ctx.pagos();

        Estudiante lucia = es.registrar(new Estudiante(1001, "Lucía", "Benítez", "lucia.benitez@ipesmi.edu.ar"));
        Estudiante matias = es.registrar(new Estudiante(1002, "Matías", "Fernández", "matias.fernandez@ipesmi.edu.ar"));
        Estudiante camila = es.registrar(new Estudiante(1003, "Camila", "Duarte", "camila.duarte@ipesmi.edu.ar"));
        Estudiante joaquin = es.registrar(new Estudiante(1004, "Joaquín", "Ramírez", "joaquin.ramirez@ipesmi.edu.ar"));
        Estudiante valentina = es.registrar(new Estudiante(1005, "Valentina", "Sosa", "valentina.sosa@ipesmi.edu.ar"));
        Estudiante tomas = new Estudiante(1006, "Tomás", "Acosta", "tomas.acosta@ipesmi.edu.ar");
        tomas.setActivo(false);
        es.registrar(tomas);

        Concepto mat = cs.registrar("MAT-2026", "Matrícula anual 2026", new BigDecimal("45000"), TipoConcepto.MATRICULA,
                false, LocalDate.of(2026, 3, 1));
        Concepto cuo3 = cs.registrar("CUO-03", "Cuota marzo", new BigDecimal("32000"), TipoConcepto.CUOTA,
                true, LocalDate.of(2026, 3, 10));
        cs.registrar("CUO-04", "Cuota abril", new BigDecimal("32000"), TipoConcepto.CUOTA, true, LocalDate.of(2026, 4, 10));
        cs.registrar("CUO-05", "Cuota mayo", new BigDecimal("32000"), TipoConcepto.CUOTA, true, LocalDate.of(2026, 5, 10));
        cs.registrar("EXA-JUL", "Derecho de examen julio", new BigDecimal("8500"), TipoConcepto.EXAMEN,
                false, LocalDate.of(2026, 7, 1));
        Concepto cuo4 = cs.buscar("CUO-04").orElseThrow();

        ps.aplicarBeca(1002, 50);
        ps.aplicarBeca(1004, 25);
        ps.aplicarBeca(1005, 100);

        ps.registrarPago(MedioPago.EFECTIVO, s(lucia, mat, "45000", 2026, 2, 20, "REC-0001", 1, null));
        ps.registrarPago(MedioPago.TARJETA, s(lucia, cuo3, "32000", 2026, 3, 8, "4821", 3, null));
        ps.registrarPago(MedioPago.TRANSFERENCIA, s(lucia, cuo4, "32000", 2026, 4, 9, "OP-778812", 1, "Banco Macro"));
        ps.registrarPago(MedioPago.EFECTIVO, s(matias, mat, "45000", 2026, 2, 25, "REC-0002", 1, null));
        Pago t = ps.registrarPago(MedioPago.TRANSFERENCIA, s(matias, cuo3, "16000", 2026, 3, 10, "OP-552101", 1, "Banco Nación"));
        ps.acreditar(t);
        ps.registrarPago(MedioPago.TARJETA, s(camila, mat, "45000", 2026, 2, 28, "0937", 1, null));
        ps.registrarPago(MedioPago.EFECTIVO, s(camila, cuo3, "20000", 2026, 3, 12, "REC-0003", 1, null));
        t = ps.registrarPago(MedioPago.TRANSFERENCIA, s(joaquin, mat, "45000", 2026, 3, 1, "OP-610045", 1, "Banco Galicia"));
        ps.acreditar(t);
        Pago anulado = ps.registrarPago(MedioPago.TARJETA, s(joaquin, cuo3, "24000", 2026, 3, 9, "1150", 1, null));
        ps.anular(anulado);
        ps.registrarPago(MedioPago.EFECTIVO, s(valentina, mat, "45000", 2026, 2, 18, "REC-0004", 1, null));

        // equivalentes a los UPDATE del script
        ps.listar().stream().filter(p -> "OP-778812".equals(p.getReferencia())).findFirst().ifPresent(ps::acreditar);
        ps.aplicarBeca(1003, 30);
    }

    private static SolicitudPago s(Estudiante e, Concepto c, String monto, int a, int m, int d,
                                   String ref, int cuotas, String banco) {
        return new SolicitudPago(e, c, new BigDecimal(monto), LocalDate.of(a, m, d), ref, cuotas, banco);
    }
}
