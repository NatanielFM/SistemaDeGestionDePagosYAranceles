package com.ipesmi.pagos.util;

import java.text.Normalizer;
import java.util.Locale;

/** Coincidencia de texto para los buscadores: sin tildes, sin mayúsculas y sin importar el orden de las palabras. */
public final class Busqueda {

    private Busqueda() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT)
                .strip();
    }

    /**
     * Verdadero si cada palabra de la consulta aparece en la clave. Una consulta
     * vacía coincide con todo.
     * <pre>coincide("lucia ben", "1001 Benítez Lucía") == true</pre>
     */
    public static boolean coincide(String consulta, String clave) {
        String q = normalizar(consulta);
        if (q.isEmpty()) {
            return true;
        }
        String k = normalizar(clave);
        for (String palabra : q.split("[^\\p{L}\\p{N}]+")) {   // ignora espacios, comas y el "·" del texto elegido
            if (!palabra.isEmpty() && !k.contains(palabra)) {
                return false;
            }
        }
        return true;
    }
}
