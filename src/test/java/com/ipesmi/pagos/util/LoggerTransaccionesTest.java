package com.ipesmi.pagos.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class LoggerTransaccionesTest {

    @Test
    @DisplayName("getInstance devuelve siempre el mismo objeto")
    void unicidad() {
        assertSame(LoggerTransacciones.getInstance(), LoggerTransacciones.getInstance());
    }

    @Test
    @DisplayName("Desde varios hilos a la vez también hay una sola instancia")
    void unicidadConcurrente() throws InterruptedException {
        Set<LoggerTransacciones> instancias = ConcurrentHashMap.newKeySet();
        ExecutorService pool = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 100; i++) {
            pool.submit(() -> instancias.add(LoggerTransacciones.getInstance()));
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS));
        assertEquals(1, instancias.size());
    }

    @Test
    @DisplayName("El constructor es privado")
    void constructorPrivado() throws NoSuchMethodException {
        var constructor = LoggerTransacciones.class.getDeclaredConstructor();
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
    }

    @Test
    @DisplayName("Registrar escribe una línea en el archivo con nivel y mensaje")
    void escribeEnArchivo() throws Exception {
        LoggerTransacciones log = LoggerTransacciones.getInstance();
        String marca = "prueba-" + System.nanoTime();

        log.alta(marca);

        List<String> lineas = Files.readAllLines(log.getArchivo(), StandardCharsets.UTF_8);
        String ultima = lineas.get(lineas.size() - 1);
        assertTrue(ultima.contains("ALTA"));
        assertTrue(ultima.endsWith(marca));
        assertTrue(log.getHistorial().get(log.getHistorial().size() - 1).endsWith(marca));
    }

    @Test
    @DisplayName("Los oyentes reciben cada línea registrada")
    void notificaOyentes() {
        LoggerTransacciones log = LoggerTransacciones.getInstance();
        AtomicReference<String> recibida = new AtomicReference<>();
        java.util.function.Consumer<String> oyente = recibida::set;
        log.agregarOyente(oyente);
        try {
            log.error("monto negativo");
        } finally {
            log.quitarOyente(oyente);
        }
        assertNotNull(recibida.get());
        assertTrue(recibida.get().contains("ERROR"));
    }

    @ParameterizedTest(name = "mensaje [{0}] es rechazado")
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Mensajes nulos, vacíos o en blanco lanzan IllegalArgumentException")
    void mensajeInvalido(String mensaje) {
        LoggerTransacciones log = LoggerTransacciones.getInstance();
        int antes = log.getHistorial().size();
        assertThrows(IllegalArgumentException.class, () -> log.info(mensaje));
        assertEquals(antes, log.getHistorial().size(), "no debe quedar registrado");
    }

    @Test
    @DisplayName("Nivel nulo lanza IllegalArgumentException")
    void nivelNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> LoggerTransacciones.getInstance().registrar(null, "hola"));
    }

    @Test
    @DisplayName("El historial expuesto no se puede modificar")
    void historialSoloLectura() {
        LoggerTransacciones log = LoggerTransacciones.getInstance();
        log.info("algo");
        assertThrows(UnsupportedOperationException.class, () -> log.getHistorial().clear());
    }
}
