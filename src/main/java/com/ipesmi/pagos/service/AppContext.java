package com.ipesmi.pagos.service;

import com.ipesmi.pagos.dao.ConceptoDAO;
import com.ipesmi.pagos.dao.EstudianteDAO;
import com.ipesmi.pagos.dao.PagoDAO;

/**
 * Arma los servicios una sola vez y los comparte entre los controladores
 * JavaFX (que crea el FXMLLoader y no reciben parámetros en el constructor).
 */
public final class AppContext {

    private static AppContext instancia;

    private final EstudianteService estudianteService;
    private final ConceptoService conceptoService;
    private final PagoService pagoService;

    private AppContext() {
        PagoDAO pagoDAO = new PagoDAO();
        estudianteService = new EstudianteService(new EstudianteDAO(), pagoDAO);
        conceptoService = new ConceptoService(new ConceptoDAO(), pagoDAO);
        pagoService = new PagoService(pagoDAO, estudianteService, conceptoService);
    }

    public static synchronized AppContext get() {
        if (instancia == null) {
            instancia = new AppContext();
            if (Boolean.getBoolean("pagos.demo")) {
                DatosDemo.cargarSiVacia(instancia);
            }
        }
        return instancia;
    }

    /** Descarta los servicios (se usa en pruebas y al reconectar). */
    public static synchronized void reiniciar() {
        instancia = null;
    }

    public EstudianteService estudiantes() { return estudianteService; }
    public ConceptoService conceptos() { return conceptoService; }
    public PagoService pagos() { return pagoService; }
}
