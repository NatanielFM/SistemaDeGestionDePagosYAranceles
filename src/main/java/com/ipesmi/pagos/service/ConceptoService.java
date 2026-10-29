package com.ipesmi.pagos.service;

import com.ipesmi.pagos.dao.ConceptoDAO;
import com.ipesmi.pagos.dao.PagoDAO;
import com.ipesmi.pagos.exception.MontoInvalidoException;
import com.ipesmi.pagos.model.Concepto;
import com.ipesmi.pagos.model.TipoConcepto;
import com.ipesmi.pagos.util.LoggerTransacciones;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/** CRUD de conceptos de pago (matrícula, cuotas, exámenes...). */
public class ConceptoService {

    private final ConceptoDAO dao;
    private final PagoDAO pagoDAO;
    private final LoggerTransacciones log = LoggerTransacciones.getInstance();
    private final Map<String, Concepto> conceptos = new LinkedHashMap<>();

    public ConceptoService(ConceptoDAO dao, PagoDAO pagoDAO) {
        this.dao = dao;
        this.pagoDAO = pagoDAO;
        cargar();
    }

    public final void cargar() {
        conceptos.clear();
        dao.listarTodos().forEach(c -> conceptos.put(c.getCodigo(), c));
    }

    public Concepto registrar(String codigo, String descripcion, BigDecimal monto, TipoConcepto tipo,
                              boolean becable, LocalDate vencimiento) throws MontoInvalidoException {
        String cod = normalizarCodigo(codigo);
        if (conceptos.containsKey(cod)) {
            throw error(new IllegalArgumentException("Ya existe el concepto " + cod));
        }
        Concepto c = new Concepto(cod, descripcion, monto, tipo, becable);
        c.setVencimiento(vencimiento);
        validar(c);
        dao.guardar(c);
        conceptos.put(cod, c);
        log.alta("Concepto " + c + " $" + c.getMonto());
        return c;
    }

    public Concepto actualizar(String codigo, String descripcion, BigDecimal monto, TipoConcepto tipo,
                               boolean becable, LocalDate vencimiento) throws MontoInvalidoException {
        Concepto actual = buscar(codigo)
                .orElseThrow(() -> error(new IllegalArgumentException("No existe el concepto " + codigo)));
        Concepto datos = new Concepto(actual.getCodigo(), descripcion, monto, tipo, becable);
        validar(datos);
        actual.setDescripcion(datos.getDescripcion());
        actual.setMonto(datos.getMonto());
        actual.setTipo(tipo);
        actual.setBecable(becable);
        actual.setVencimiento(vencimiento);
        dao.actualizar(actual);
        log.modificacion("Concepto " + actual + " $" + actual.getMonto());
        return actual;
    }

    public void eliminar(String codigo) {
        Concepto c = buscar(codigo)
                .orElseThrow(() -> error(new IllegalArgumentException("No existe el concepto " + codigo)));
        if (pagoDAO.contarPorConcepto(c.getCodigo()) > 0) {
            throw error(new IllegalStateException("El concepto " + c.getCodigo() + " tiene pagos asociados"));
        }
        dao.eliminar(c.getCodigo());
        conceptos.remove(c.getCodigo());
        log.baja("Concepto " + c);
    }

    public Optional<Concepto> buscar(String codigo) {
        return codigo == null ? Optional.empty() : Optional.ofNullable(conceptos.get(normalizarCodigo(codigo)));
    }

    public List<Concepto> listar() {
        return new ArrayList<>(conceptos.values());
    }

    public Map<String, Concepto> getMapa() {
        return conceptos;
    }

    /** Valida un monto de dinero: obligatorio, mayor a cero y con 2 decimales como máximo. */
    public static BigDecimal validarMonto(BigDecimal monto) throws MontoInvalidoException {
        if (monto == null) {
            throw new MontoInvalidoException("Debe ingresar un monto");
        }
        if (monto.signum() <= 0) {
            throw new MontoInvalidoException("El monto debe ser mayor a cero (ingresó $" + monto.toPlainString() + ")");
        }
        if (monto.stripTrailingZeros().scale() > 2) {
            throw new MontoInvalidoException("El monto admite como máximo 2 decimales");
        }
        return monto.setScale(2, RoundingMode.UNNECESSARY);
    }

    private void validar(Concepto c) throws MontoInvalidoException {
        if (c.getCodigo() == null || c.getCodigo().isBlank()) {
            throw error(new IllegalArgumentException("El código es obligatorio"));
        }
        if (c.getDescripcion() == null || c.getDescripcion().isBlank()) {
            throw error(new IllegalArgumentException("La descripción es obligatoria"));
        }
        if (c.getTipo() == null) {
            throw error(new IllegalArgumentException("Seleccione el tipo de concepto"));
        }
        try {
            c.setMonto(validarMonto(c.getMonto()));
        } catch (MontoInvalidoException e) {
            throw error(e);
        }
        c.setDescripcion(c.getDescripcion().strip());
    }

    private static String normalizarCodigo(String codigo) {
        return codigo == null ? null : codigo.strip().toUpperCase(Locale.ROOT);
    }

    private <X extends Exception> X error(X ex) {
        log.error(ex.getMessage());
        return ex;
    }
}
