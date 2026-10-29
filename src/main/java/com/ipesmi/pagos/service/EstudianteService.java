package com.ipesmi.pagos.service;

import com.ipesmi.pagos.dao.EstudianteDAO;
import com.ipesmi.pagos.dao.PagoDAO;
import com.ipesmi.pagos.model.Estudiante;
import com.ipesmi.pagos.util.LoggerTransacciones;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Altas, bajas y modificaciones de estudiantes. Mantiene en memoria un
 * HashMap&lt;Integer, Estudiante&gt; indexado por legajo para búsquedas O(1).
 */
public class EstudianteService {

    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    private final EstudianteDAO dao;
    private final PagoDAO pagoDAO;
    private final LoggerTransacciones log = LoggerTransacciones.getInstance();
    private final Map<Integer, Estudiante> estudiantes = new HashMap<>();

    public EstudianteService(EstudianteDAO dao, PagoDAO pagoDAO) {
        this.dao = dao;
        this.pagoDAO = pagoDAO;
        cargar();
    }

    public final void cargar() {
        estudiantes.clear();
        for (Estudiante e : dao.listarTodos()) {
            estudiantes.put(e.getLegajo(), e);
        }
    }

    public Estudiante registrar(Estudiante e) {
        validar(e);
        if (estudiantes.containsKey(e.getLegajo())) {
            throw error(new IllegalArgumentException("Ya existe un estudiante con legajo " + e.getLegajo()));
        }
        dao.guardar(e);
        estudiantes.put(e.getLegajo(), e);
        log.alta("Estudiante " + e);
        return e;
    }

    /** Copia los datos editables sobre la instancia que ya está en el mapa. */
    public Estudiante actualizar(Integer legajo, String nombre, String apellido, String email, boolean activo) {
        Estudiante actual = buscar(legajo)
                .orElseThrow(() -> error(new IllegalArgumentException("No existe el legajo " + legajo)));
        Estudiante datos = new Estudiante(legajo, nombre, apellido, email);
        validar(datos);
        actual.setNombre(datos.getNombre());
        actual.setApellido(datos.getApellido());
        actual.setEmail(datos.getEmail());
        actual.setActivo(activo);
        dao.actualizar(actual);
        log.modificacion("Estudiante " + actual + (activo ? "" : " (inactivo)"));
        return actual;
    }

    /** Persiste cambios hechos sobre la instancia (por ejemplo el % de beca). */
    void guardarCambios(Estudiante e) {
        dao.actualizar(e);
    }

    public void eliminar(Integer legajo) {
        Estudiante e = buscar(legajo)
                .orElseThrow(() -> error(new IllegalArgumentException("No existe el legajo " + legajo)));
        if (pagoDAO.contarPorEstudiante(legajo) > 0) {
            throw error(new IllegalStateException(
                    "El estudiante " + e.getNombreCompleto() + " tiene pagos registrados. Márquelo como inactivo."));
        }
        dao.eliminar(legajo);
        estudiantes.remove(legajo);
        log.baja("Estudiante " + e);
    }

    public Optional<Estudiante> buscar(Integer legajo) {
        return Optional.ofNullable(estudiantes.get(legajo));
    }

    public List<Estudiante> listar() {
        List<Estudiante> lista = new ArrayList<>(estudiantes.values());
        lista.sort(Comparator.comparing(Estudiante::getApellido, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Estudiante::getNombre, String.CASE_INSENSITIVE_ORDER));
        return lista;
    }

    public List<Estudiante> listarActivos() {
        return listar().stream().filter(Estudiante::isActivo).toList();
    }

    public Map<Integer, Estudiante> getMapa() {
        return estudiantes;
    }

    private void validar(Estudiante e) {
        if (e.getLegajo() == null || e.getLegajo() <= 0) {
            throw error(new IllegalArgumentException("El legajo debe ser un número positivo"));
        }
        if (e.getNombre() == null || e.getNombre().isBlank() || e.getApellido() == null || e.getApellido().isBlank()) {
            throw error(new IllegalArgumentException("Nombre y apellido son obligatorios"));
        }
        e.setNombre(e.getNombre().strip());
        e.setApellido(e.getApellido().strip());
        if (e.getEmail() == null || !EMAIL.matcher(e.getEmail().strip()).matches()) {
            throw error(new IllegalArgumentException("El email no tiene un formato válido"));
        }
        e.setEmail(e.getEmail().strip().toLowerCase());
        boolean repetido = estudiantes.values().stream()
                .anyMatch(o -> !o.getLegajo().equals(e.getLegajo()) && o.getEmail().equalsIgnoreCase(e.getEmail()));
        if (repetido) {
            throw error(new IllegalArgumentException("El email " + e.getEmail() + " ya está registrado"));
        }
    }

    private <X extends RuntimeException> X error(X ex) {
        log.error(ex.getMessage());
        return ex;
    }
}
