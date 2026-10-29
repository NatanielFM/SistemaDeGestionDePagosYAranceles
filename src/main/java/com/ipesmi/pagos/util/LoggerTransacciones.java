package com.ipesmi.pagos.util;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/**
 * Logger de transacciones implementado con el patrón Singleton.
 * <p>
 * Se usa el idiom "holder" (inicialización perezosa y thread-safe garantizada
 * por la JVM al cargar la clase interna). Cada línea se agrega al archivo
 * {@code logs/transacciones.log} y también queda en memoria para la vista
 * Bitácora.
 */
public final class LoggerTransacciones {

    public enum Nivel { INFO, ALTA, BAJA, MODIFICACION, ERROR }

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Path archivo;
    private final List<String> historial = new ArrayList<>();
    private final List<Consumer<String>> oyentes = new ArrayList<>();

    private LoggerTransacciones() {
        this.archivo = Paths.get(System.getProperty("pagos.log", "logs/transacciones.log"));
    }

    private static class Holder {
        private static final LoggerTransacciones INSTANCIA = new LoggerTransacciones();
    }

    public static LoggerTransacciones getInstance() {
        return Holder.INSTANCIA;
    }

    /**
     * Registra una línea con fecha, nivel y mensaje.
     *
     * @throws IllegalArgumentException si el nivel es nulo o el mensaje es nulo o vacío
     */
    public synchronized void registrar(Nivel nivel, String mensaje) {
        if (nivel == null) {
            throw new IllegalArgumentException("El nivel del log no puede ser nulo");
        }
        if (mensaje == null || mensaje.isBlank()) {
            throw new IllegalArgumentException("El mensaje del log no puede ser nulo ni vacío");
        }
        String linea = String.format("%s [%-12s] %s", LocalDateTime.now().format(FORMATO), nivel, mensaje.strip());
        escribir(linea);
        historial.add(linea);
        oyentes.forEach(o -> o.accept(linea));
    }

    public void info(String mensaje) { registrar(Nivel.INFO, mensaje); }
    public void alta(String mensaje) { registrar(Nivel.ALTA, mensaje); }
    public void baja(String mensaje) { registrar(Nivel.BAJA, mensaje); }
    public void modificacion(String mensaje) { registrar(Nivel.MODIFICACION, mensaje); }
    public void error(String mensaje) { registrar(Nivel.ERROR, mensaje); }

    private void escribir(String linea) {
        try {
            Path carpeta = archivo.toAbsolutePath().getParent();
            if (carpeta != null) {
                Files.createDirectories(carpeta);
            }
            Files.writeString(archivo, linea + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir el log en " + archivo, e);
        }
    }

    /** Líneas registradas durante esta ejecución (solo lectura). */
    public synchronized List<String> getHistorial() {
        return Collections.unmodifiableList(new ArrayList<>(historial));
    }

    public synchronized void agregarOyente(Consumer<String> oyente) {
        oyentes.add(oyente);
    }

    public synchronized void quitarOyente(Consumer<String> oyente) {
        oyentes.remove(oyente);
    }

    public Path getArchivo() {
        return archivo;
    }
}
