package com.ipesmi.pagos.dao;

import com.ipesmi.pagos.model.Estudiante;

import java.util.List;

public class EstudianteDAO extends GenericDAO<Estudiante, Integer> {

    public EstudianteDAO() {
        super(Estudiante.class);
    }

    @Override
    public List<Estudiante> listarTodos() {
        return consultar(em -> em.createQuery(
                "SELECT e FROM Estudiante e ORDER BY e.apellido, e.nombre", Estudiante.class).getResultList());
    }

    public boolean existeEmail(String email, Integer legajoExcluido) {
        return consultar(em -> em.createQuery(
                        "SELECT COUNT(e) FROM Estudiante e WHERE LOWER(e.email) = LOWER(:email) AND e.legajo <> :legajo",
                        Long.class)
                .setParameter("email", email)
                .setParameter("legajo", legajoExcluido == null ? -1 : legajoExcluido)
                .getSingleResult() > 0);
    }
}
