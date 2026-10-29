package com.ipesmi.pagos.dao;

import com.ipesmi.pagos.model.Concepto;

import java.util.List;

public class ConceptoDAO extends GenericDAO<Concepto, String> {

    public ConceptoDAO() {
        super(Concepto.class);
    }

    @Override
    public List<Concepto> listarTodos() {
        return consultar(em -> em.createQuery(
                "SELECT c FROM Concepto c ORDER BY c.vencimiento, c.codigo", Concepto.class).getResultList());
    }
}
