package com.ipesmi.pagos.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Mantiene un único EntityManagerFactory para toda la aplicación (crearlo es
 * caro) y entrega un EntityManager nuevo por operación.
 * <p>
 * Orden de configuración (cada paso pisa al anterior):
 * <ol>
 *   <li>persistence.xml (MySQL en localhost)</li>
 *   <li>config/db.properties, si existe</li>
 *   <li>propiedades de sistema que empiecen con "jakarta.persistence." o "hibernate."</li>
 *   <li>{@code -Dpagos.demo=true}: base H2 en archivo (data/pagos-demo), sin instalar MySQL</li>
 *   <li>{@link #configurar(Map)}, usado por las pruebas</li>
 * </ol>
 */
public final class JPAUtil {

    public static final String UNIDAD = "pagosPU";
    private static final Path CONFIG = Paths.get("config", "db.properties");

    private static EntityManagerFactory emf;
    private static final Map<String, String> extras = new HashMap<>();

    // Hibernate informa por java.util.logging; solo interesan advertencias y errores.
    private static final java.util.logging.Logger LOG_HIBERNATE = java.util.logging.Logger.getLogger("org.hibernate");

    static {
        LOG_HIBERNATE.setLevel(java.util.logging.Level.WARNING);
    }

    private JPAUtil() {
    }

    public static synchronized EntityManagerFactory getEntityManagerFactory() {
        if (emf == null || !emf.isOpen()) {
            emf = Persistence.createEntityManagerFactory(UNIDAD, propiedades());
        }
        return emf;
    }

    public static EntityManager crearEntityManager() {
        return getEntityManagerFactory().createEntityManager();
    }

    /** Agrega propiedades antes de crear el EntityManagerFactory (por ejemplo una base H2 en memoria). */
    public static synchronized void configurar(Map<String, String> propiedades) {
        cerrar();
        extras.clear();
        extras.putAll(propiedades);
    }

    public static synchronized void cerrar() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
        emf = null;
    }

    /** URL JDBC efectiva, para mostrarla en la interfaz. */
    public static String getUrl() {
        return propiedades().getOrDefault("jakarta.persistence.jdbc.url", "jdbc:mysql://localhost:3306/pagos_aranceles");
    }

    private static Map<String, String> propiedades() {
        Map<String, String> props = new HashMap<>();

        if (Files.exists(CONFIG)) {
            Properties archivo = new Properties();
            try (InputStream in = Files.newInputStream(CONFIG)) {
                archivo.load(in);
            } catch (IOException e) {
                throw new UncheckedIOException("No se pudo leer " + CONFIG, e);
            }
            archivo.stringPropertyNames().forEach(k -> props.put(k, archivo.getProperty(k).strip()));
        }

        System.getProperties().stringPropertyNames().stream()
                .filter(k -> k.startsWith("jakarta.persistence.") || k.startsWith("hibernate."))
                .forEach(k -> props.put(k, System.getProperty(k)));

        if (Boolean.getBoolean("pagos.demo")) {
            props.putAll(h2("jdbc:h2:file:./data/pagos-demo;MODE=MySQL;DATABASE_TO_LOWER=TRUE"));
        }

        props.putAll(extras);
        return props;
    }

    /** Propiedades para una base H2 (pruebas y modo demo). */
    public static Map<String, String> h2(String url) {
        Map<String, String> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
        props.put("jakarta.persistence.jdbc.url", url);
        props.put("jakarta.persistence.jdbc.user", "sa");
        props.put("jakarta.persistence.jdbc.password", "");
        return props;
    }
}
