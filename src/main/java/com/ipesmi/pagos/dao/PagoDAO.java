package com.ipesmi.pagos.dao;

import com.ipesmi.pagos.model.EstadoPago;
import com.ipesmi.pagos.model.Pago;
import com.ipesmi.pagos.model.TipoConcepto;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PagoDAO extends GenericDAO<Pago, Long> {

    public PagoDAO() {
        super(Pago.class);
    }

    @Override
    public List<Pago> listarTodos() {
        return consultar(em -> em.createQuery(
                "SELECT p FROM Pago p JOIN FETCH p.estudiante JOIN FETCH p.concepto ORDER BY p.fecha DESC, p.id DESC",
                Pago.class).getResultList());
    }

    public List<Pago> listarPorEstudiante(Integer legajo) {
        return consultar(em -> em.createQuery(
                        "SELECT p FROM Pago p WHERE p.estudiante.legajo = :legajo ORDER BY p.fecha", Pago.class)
                .setParameter("legajo", legajo)
                .getResultList());
    }

    public long contarPorEstudiante(Integer legajo) {
        return consultar(em -> em.createQuery(
                        "SELECT COUNT(p) FROM Pago p WHERE p.estudiante.legajo = :legajo", Long.class)
                .setParameter("legajo", legajo)
                .getSingleResult());
    }

    public long contarPorConcepto(String codigo) {
        return consultar(em -> em.createQuery(
                        "SELECT COUNT(p) FROM Pago p WHERE p.concepto.codigo = :codigo", Long.class)
                .setParameter("codigo", codigo)
                .getSingleResult());
    }

    /** Total acreditado agrupado por tipo de concepto (JPQL con GROUP BY). */
    public Map<TipoConcepto, BigDecimal> totalAcreditadoPorTipoConcepto() {
        List<Object[]> filas = consultar(em -> em.createQuery(
                        "SELECT p.concepto.tipo, SUM(p.monto) FROM Pago p WHERE p.estado = :estado "
                                + "GROUP BY p.concepto.tipo ORDER BY p.concepto.tipo", Object[].class)
                .setParameter("estado", EstadoPago.ACREDITADO)
                .getResultList());
        Map<TipoConcepto, BigDecimal> totales = new LinkedHashMap<>();
        for (Object[] fila : filas) {
            totales.put((TipoConcepto) fila[0], (BigDecimal) fila[1]);
        }
        return totales;
    }
}
