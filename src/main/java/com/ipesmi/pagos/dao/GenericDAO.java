package com.ipesmi.pagos.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * CRUD genérico con JPA. Cada operación abre su propio EntityManager y, si
 * escribe, su propia transacción (que se revierte ante cualquier error).
 */
public abstract class GenericDAO<T, ID> {

    private final Class<T> clase;

    protected GenericDAO(Class<T> clase) {
        this.clase = clase;
    }

    public T guardar(T entidad) {
        enTransaccion(em -> em.persist(entidad));
        return entidad;
    }

    public T actualizar(T entidad) {
        return enTransaccionConResultado(em -> em.merge(entidad));
    }

    public void eliminar(ID id) {
        enTransaccion(em -> {
            T encontrada = em.find(clase, id);
            if (encontrada != null) {
                em.remove(encontrada);
            }
        });
    }

    public Optional<T> buscarPorId(ID id) {
        return consultar(em -> Optional.ofNullable(em.find(clase, id)));
    }

    public List<T> listarTodos() {
        // JPQL: el nombre de la entidad coincide con el de la clase
        String jpql = "SELECT e FROM " + clase.getSimpleName() + " e";
        return consultar(em -> em.createQuery(jpql, clase).getResultList());
    }

    protected <R> R consultar(Function<EntityManager, R> consulta) {
        EntityManager em = JPAUtil.crearEntityManager();
        try {
            return consulta.apply(em);
        } finally {
            em.close();
        }
    }

    protected void enTransaccion(Consumer<EntityManager> operacion) {
        enTransaccionConResultado(em -> {
            operacion.accept(em);
            return null;
        });
    }

    protected <R> R enTransaccionConResultado(Function<EntityManager, R> operacion) {
        EntityManager em = JPAUtil.crearEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            R resultado = operacion.apply(em);
            tx.commit();
            return resultado;
        } catch (RuntimeException e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }
}
