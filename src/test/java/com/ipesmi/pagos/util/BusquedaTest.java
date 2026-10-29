package com.ipesmi.pagos.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class BusquedaTest {

    private static final String CLAVE = "1001 Benítez Lucía";

    @ParameterizedTest(name = "\"{0}\" encuentra al estudiante")
    @CsvSource({"1001", "100", "benitez", "BENÍTEZ", "lucia", "lucia ben", "ben lucia", "  lucia   benitez  "})
    @DisplayName("Busca por legajo o nombre, sin tildes, sin mayúsculas y sin importar el orden")
    void coincide(String consulta) {
        assertTrue(Busqueda.coincide(consulta, CLAVE));
    }

    @ParameterizedTest(name = "\"{0}\" no coincide")
    @CsvSource({"1002", "perez", "lucia perez", "x1001"})
    void noCoincide(String consulta) {
        assertFalse(Busqueda.coincide(consulta, CLAVE));
    }

    @Test
    @DisplayName("Consulta vacía o nula coincide con todo; clave nula con nada")
    void bordes() {
        assertTrue(Busqueda.coincide("", CLAVE));
        assertTrue(Busqueda.coincide(null, CLAVE));
        assertTrue(Busqueda.coincide("   ", CLAVE));
        assertFalse(Busqueda.coincide("lucia", null));
    }
}
