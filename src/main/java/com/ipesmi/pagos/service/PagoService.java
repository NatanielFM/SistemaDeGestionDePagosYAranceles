package com.ipesmi.pagos.service;

import com.ipesmi.pagos.dao.PagoDAO;
import com.ipesmi.pagos.exception.BecaNoAplicableException;
import com.ipesmi.pagos.exception.MontoInvalidoException;
import com.ipesmi.pagos.exception.PagoDuplicadoException;
import com.ipesmi.pagos.factory.PagoFactory;
import com.ipesmi.pagos.factory.SolicitudPago;
import com.ipesmi.pagos.model.Concepto;
import com.ipesmi.pagos.model.EstadoPago;
import com.ipesmi.pagos.model.Estudiante;
import com.ipesmi.pagos.model.MedioPago;
import com.ipesmi.pagos.model.Pago;
import com.ipesmi.pagos.model.TipoConcepto;
import com.ipesmi.pagos.util.LoggerTransacciones;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Lógica de negocio de pagos: registro (vía {@link PagoFactory}), acreditación,
 * anulación, cálculo de saldos y aplicación de becas. Los pagos se mantienen
 * en un ArrayList&lt;Pago&gt; sincronizado con la base de datos.
 */
public class PagoService {

    private final PagoDAO dao;
    private final EstudianteService estudiantes;
    private final ConceptoService conceptos;
    private final LoggerTransacciones log = LoggerTransacciones.getInstance();
    private final List<Pago> pagos = new ArrayList<>();

    public PagoService(PagoDAO dao, EstudianteService estudiantes, ConceptoService conceptos) {
        this.dao = dao;
        this.estudiantes = estudiantes;
        this.conceptos = conceptos;
        cargar();
    }

    /**
     * Lee los pagos y los vuelve a enlazar con las instancias de estudiante y
     * concepto que tienen los otros servicios, así un cambio de nombre o de
     * beca se ve en todos lados sin recargar.
     */
    public final void cargar() {
        pagos.clear();
        for (Pago p : dao.listarTodos()) {
            estudiantes.buscar(p.getEstudiante().getLegajo()).ifPresent(p::setEstudiante);
            conceptos.buscar(p.getConcepto().getCodigo()).ifPresent(p::setConcepto);
            pagos.add(p);
        }
    }

    // ------------------------------------------------------------------ alta

    public Pago registrarPago(MedioPago medio, SolicitudPago s) throws PagoDuplicadoException, MontoInvalidoException {
        if (s == null || s.estudiante() == null || s.concepto() == null) {
            throw error(new IllegalArgumentException("Seleccione estudiante y concepto"));
        }
        Estudiante e = s.estudiante();
        Concepto c = s.concepto();
        if (!e.isActivo()) {
            throw error(new IllegalArgumentException("El estudiante " + e.getNombreCompleto() + " está inactivo"));
        }
        BigDecimal disponible = CalculadoraSaldos.disponibleParaPagar(e, c, pagos);
        if (disponible.signum() == 0) {
            boolean cancelado = CalculadoraSaldos.saldoConcepto(e, c, pagos).signum() == 0;
            throw error(new PagoDuplicadoException(cancelado
                    ? e.getNombreCompleto() + " ya abonó " + c.getDescripcion()
                    : e.getNombreCompleto() + " ya tiene pagos pendientes que cubren " + c.getDescripcion()));
        }
        BigDecimal monto = validarMonto(s.monto());
        if (monto.compareTo(disponible) > 0) {
            throw error(new MontoInvalidoException(
                    "El monto $" + monto + " supera el saldo de $" + disponible + " para " + c.getDescripcion()));
        }

        Pago pago;
        try {
            pago = PagoFactory.crearPago(medio, new SolicitudPago(e, c, monto, s.fecha(), s.referencia(),
                    s.cuotas(), s.banco()));
        } catch (IllegalArgumentException ex) {
            throw error(ex);
        }
        verificarReferencia(pago, null);

        dao.guardar(pago);
        pagos.add(0, pago);
        log.alta(String.format("Pago #%d %s | %s | %s | $%s | %s", pago.getId(), pago.getMedio(),
                e, c.getCodigo(), pago.getMonto(), pago.getEstado()));
        return pago;
    }

    // ------------------------------------------------------- modificaciones

    public Pago actualizarPago(Pago pago, BigDecimal nuevoMonto, LocalDate nuevaFecha, String nuevaReferencia)
            throws MontoInvalidoException, PagoDuplicadoException {
        BigDecimal monto = validarMonto(nuevoMonto);
        if (pago.isAnulado()) {
            throw error(new IllegalStateException("No se puede modificar un pago anulado"));
        }
        // el disponible se calcula sin contar el propio pago
        BigDecimal disponible = CalculadoraSaldos.disponibleParaPagar(pago.getEstudiante(), pago.getConcepto(), pagos)
                .add(pago.getMonto());
        if (monto.compareTo(disponible) > 0) {
            throw error(new MontoInvalidoException("El monto $" + monto + " supera el saldo de $" + disponible));
        }
        String referenciaAnterior = pago.getReferencia();
        if (nuevaReferencia != null && !nuevaReferencia.isBlank()) {
            pago.setReferencia(nuevaReferencia.strip());
        }
        try {
            verificarReferencia(pago, pago);
        } catch (PagoDuplicadoException ex) {
            pago.setReferencia(referenciaAnterior);
            throw ex;
        }
        BigDecimal montoAnterior = pago.getMonto();
        pago.setMonto(monto);
        if (nuevaFecha != null) {
            pago.setFecha(nuevaFecha);
        }
        dao.actualizar(pago);
        log.modificacion(String.format("Pago #%d monto $%s -> $%s", pago.getId(), montoAnterior, monto));
        return pago;
    }

    public void acreditar(Pago pago) {
        if (pago.getEstado() != EstadoPago.PENDIENTE) {
            throw error(new IllegalStateException("Solo se pueden acreditar pagos pendientes"));
        }
        cambiarEstado(pago, EstadoPago.ACREDITADO);
    }

    public void anular(Pago pago) {
        if (pago.isAnulado()) {
            throw error(new IllegalStateException("El pago #" + pago.getId() + " ya está anulado"));
        }
        cambiarEstado(pago, EstadoPago.ANULADO);
    }

    public void eliminar(Pago pago) {
        dao.eliminar(pago.getId());
        pagos.remove(pago);
        log.baja("Pago #" + pago.getId() + " de " + pago.getEstudiante() + " eliminado");
    }

    private void cambiarEstado(Pago pago, EstadoPago nuevo) {
        EstadoPago anterior = pago.getEstado();
        pago.setEstado(nuevo);
        try {
            dao.actualizar(pago);
        } catch (RuntimeException ex) {
            pago.setEstado(anterior);
            throw error(ex);
        }
        log.modificacion("Pago #" + pago.getId() + " " + anterior + " -> " + nuevo);
    }

    // --------------------------------------------------------------- becas

    public void aplicarBeca(Integer legajo, double porcentaje) throws BecaNoAplicableException {
        Estudiante e = estudiantes.buscar(legajo).orElse(null);
        if (e == null) {
            throw error(new BecaNoAplicableException("No existe el estudiante con legajo " + legajo));
        }
        if (Double.isNaN(porcentaje) || porcentaje <= 0 || porcentaje > 100) {
            throw error(new BecaNoAplicableException(
                    "El porcentaje de beca debe ser mayor a 0 y hasta 100 (ingresó " + porcentaje + ")"));
        }
        if (!e.isActivo()) {
            throw error(new BecaNoAplicableException(e.getNombreCompleto() + " está inactivo"));
        }
        List<Concepto> becables = conceptos.listar().stream().filter(Concepto::isBecable).toList();
        if (becables.isEmpty()) {
            throw error(new BecaNoAplicableException("No hay conceptos becables registrados"));
        }
        boolean algunoConSaldo = becables.stream()
                .anyMatch(c -> CalculadoraSaldos.saldoConcepto(e, c, pagos).signum() > 0);
        if (!algunoConSaldo) {
            throw error(new BecaNoAplicableException(
                    e.getNombreCompleto() + " no tiene cuotas becables con saldo pendiente"));
        }
        double anterior = e.getPorcentajeBeca();
        e.setPorcentajeBeca(porcentaje);
        try {
            estudiantes.guardarCambios(e);
        } catch (RuntimeException ex) {
            e.setPorcentajeBeca(anterior);
            throw error(ex);
        }
        log.modificacion(String.format("Beca %s: %.0f%% -> %.0f%%", e, anterior, porcentaje));
    }

    public void quitarBeca(Integer legajo) {
        Estudiante e = estudiantes.buscar(legajo)
                .orElseThrow(() -> error(new IllegalArgumentException("No existe el legajo " + legajo)));
        if (!e.tieneBeca()) {
            return;
        }
        double anterior = e.getPorcentajeBeca();
        e.setPorcentajeBeca(0);
        estudiantes.guardarCambios(e);
        log.modificacion(String.format("Beca %s: %.0f%% -> 0%%", e, anterior));
    }

    // -------------------------------------------------------------- saldos

    public BigDecimal saldoPendiente(Estudiante e) {
        return CalculadoraSaldos.saldoPendiente(e, conceptos.listar(), pagos);
    }

    public BigDecimal saldoConcepto(Estudiante e, Concepto c) {
        return CalculadoraSaldos.saldoConcepto(e, c, pagos);
    }

    public BigDecimal disponibleParaPagar(Estudiante e, Concepto c) {
        return CalculadoraSaldos.disponibleParaPagar(e, c, pagos);
    }

    public BigDecimal importeConBeca(Estudiante e, Concepto c) {
        return CalculadoraSaldos.importeConBeca(c, e);
    }

    public BigDecimal saldoPendienteTotal() {
        return estudiantes.listar().stream().map(this::saldoPendiente).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal totalPorEstado(EstadoPago estado) {
        return pagos.stream().filter(p -> p.getEstado() == estado)
                .map(Pago::getMonto).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Total acreditado por medio de pago (datos del PieChart). */
    public Map<MedioPago, BigDecimal> distribucionPorMedio() {
        Map<MedioPago, BigDecimal> mapa = new EnumMap<>(MedioPago.class);
        for (Pago p : pagos) {
            if (p.isAcreditado()) {
                mapa.merge(p.getMedio(), p.getMonto(), BigDecimal::add);
            }
        }
        return mapa;
    }

    /** Total acreditado por tipo de concepto, resuelto con una consulta JPQL. */
    public Map<TipoConcepto, BigDecimal> distribucionPorTipoConcepto() {
        return dao.totalAcreditadoPorTipoConcepto();
    }

    public List<Pago> pagosDe(Estudiante e) {
        return pagos.stream().filter(p -> e.equals(p.getEstudiante())).toList();
    }

    public List<Pago> listar() {
        return Collections.unmodifiableList(pagos);
    }

    // ------------------------------------------------------------- apoyo

    private BigDecimal validarMonto(BigDecimal monto) throws MontoInvalidoException {
        try {
            return ConceptoService.validarMonto(monto);
        } catch (MontoInvalidoException ex) {
            throw error(ex);
        }
    }

    /**
     * El recibo (efectivo) y el nro. de operación (transferencia) no pueden
     * repetirse entre pagos vigentes. En tarjeta la referencia son 4 dígitos,
     * que sí pueden coincidir.
     */
    private void verificarReferencia(Pago nuevo, Pago excluido) throws PagoDuplicadoException {
        if (nuevo.getMedio() == MedioPago.TARJETA || nuevo.getReferencia() == null) {
            return;
        }
        for (Pago p : pagos) {
            if (p != excluido && !p.isAnulado() && p.getMedio() == nuevo.getMedio()
                    && nuevo.getReferencia().equalsIgnoreCase(p.getReferencia())) {
                throw error(new PagoDuplicadoException(
                        "Ya existe un pago " + p.getMedio() + " con referencia " + p.getReferencia()
                                + " (pago #" + p.getId() + ")"));
            }
        }
    }

    private <X extends Exception> X error(X ex) {
        log.error(ex.getMessage());
        return ex;
    }
}
