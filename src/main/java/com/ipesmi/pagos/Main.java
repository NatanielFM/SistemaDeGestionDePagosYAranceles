package com.ipesmi.pagos;

import javafx.application.Application;

/**
 * Clase lanzadora. Al no extender de Application permite ejecutar el JAR o
 * correr desde NetBeans con JavaFX en el classpath (sin module-info).
 */
public class Main {

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
