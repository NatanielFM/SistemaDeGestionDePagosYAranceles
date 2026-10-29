package com.ipesmi.pagos.factory;

import com.ipesmi.pagos.model.Concepto;
import com.ipesmi.pagos.model.EstadoPago;
import com.ipesmi.pagos.model.Estudiante;
import com.ipesmi.pagos.model.MedioPago;
import com.ipesmi.pagos.model.Pago;
import com.ipesmi.pagos.model.PagoEfectivo;
import com.ipesmi.pagos.model.PagoTarjeta;
import com.ipesmi.pagos.model.PagoTransferencia;
import com.ipesmi.pagos.model.TipoConcepto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class PagoFactoryTest {

    private Estudiante estudiante;
    private Concepto concepto;

    @BeforeEach
    void preparar() {
        estudiante = new Estudiante(1, "Ana", "Pérez", "ana@ipesmi.edu.ar");
        concepto = new Concepto("CUO-03", "Cuota marzo", new BigDecimal("30000"), TipoConcepto.CUOTA, true);
    }

    private SolicitudPago solicitud(String referencia, int cuotas) {
        return new SolicitudPago(estudiante, concepto, new BigDecimal("1000.00"), LocalDate.of(2026, 3, 5),
                referencia, cuotas, "Banco Macro");
    }

    @Test
    @DisplayName("EFECTIVO crea PagoEfectivo acreditado")
    void efectivo() {
        Pago p = PagoFactory.crearPago(MedioPago.EFECTIVO, solicitud("REC-1", 1));
        assertInstanceOf(PagoEfectivo.class, p);
        assertEquals(MedioPago.EFECTIVO, p.getMedio());
        assertEquals(EstadoPago.ACREDITADO, p.getEstado());
        assertEquals("REC-1", p.getReferencia());
    }

    @Test
    @DisplayName("EFECTIVO sin recibo genera uno automáticamente y no se repite")
    void efectivoGeneraRecibo() {
        Pago a = PagoFactory.crearPago(MedioPago.EFECTIVO, solicitud("", 1));
        Pago b = PagoFactory.crearPago(MedioPago.EFECTIVO, solicitud(null, 1));
        assertTrue(a.getReferencia().startsWith("REC-20260305-"));
        assertNotEquals(a.getReferencia(), b.getReferencia());
    }

    @Test
    @DisplayName("TARJETA crea PagoTarjeta con cuotas")
    void tarjeta() {
        Pago p = PagoFactory.crearPago(MedioPago.TARJETA, solicitud("4821", 3));
        PagoTarjeta t = assertInstanceOf(PagoTarjeta.class, p);
        assertEquals(3, t.getCuotas());
        assertEquals(EstadoPago.ACREDITADO, t.getEstado());
        assertTrue(t.getDetalleMedio().contains("****4821"));
    }

    @Test
    @DisplayName("TRANSFERENCIA crea PagoTransferencia pendiente de acreditación")
    void transferencia() {
        Pago p = PagoFactory.crearPago(MedioPago.TRANSFERENCIA, solicitud("op-123", 1));
        PagoTransferencia t = assertInstanceOf(PagoTransferencia.class, p);
        assertEquals(EstadoPago.PENDIENTE, t.getEstado());
        assertEquals("OP-123", t.getReferencia());
        assertEquals("Banco Macro", t.getBanco());
    }

    @ParameterizedTest(name = "\"{0}\" es un tipo válido")
    @ValueSource(strings = {"efectivo", "TARJETA", " Transferencia "})
    @DisplayName("El tipo por texto no distingue mayúsculas ni espacios")
    void tipoPorTexto(String tipo) {
        Pago p = PagoFactory.crearPago(tipo, solicitud("1234", 1));
        assertEquals(tipo.strip().toUpperCase(), p.getMedio().name());
    }

    @ParameterizedTest(name = "\"{0}\" es inválido")
    @ValueSource(strings = {"cheque", "bitcoin", "efectivo2"})
    @DisplayName("Un tipo inexistente lanza IllegalArgumentException")
    void tipoInvalido(String tipo) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> PagoFactory.crearPago(tipo, solicitud("1234", 1)));
        assertTrue(ex.getMessage().contains(tipo));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Tipo nulo o vacío lanza IllegalArgumentException")
    void tipoNuloOVacio(String tipo) {
        assertThrows(IllegalArgumentException.class, () -> PagoFactory.crearPago(tipo, solicitud("1234", 1)));
    }

    @Test
    @DisplayName("Medio enum nulo lanza IllegalArgumentException")
    void medioNulo() {
        assertThrows(IllegalArgumentException.class,
                () -> PagoFactory.crearPago((MedioPago) null, solicitud("1234", 1)));
    }

    @ParameterizedTest(name = "tarjeta con dígitos \"{0}\"")
    @ValueSource(strings = {"123", "12345", "abcd", ""})
    @DisplayName("Tarjeta exige exactamente 4 dígitos")
    void tarjetaDigitosInvalidos(String digitos) {
        assertThrows(IllegalArgumentException.class,
                () -> PagoFactory.crearPago(MedioPago.TARJETA, solicitud(digitos, 1)));
    }

    @ParameterizedTest(name = "{0} cuotas")
    @ValueSource(ints = {0, -1, 13})
    @DisplayName("Tarjeta acepta de 1 a 12 cuotas")
    void tarjetaCuotasFueraDeRango(int cuotas) {
        assertThrows(IllegalArgumentException.class,
                () -> PagoFactory.crearPago(MedioPago.TARJETA, solicitud("1234", cuotas)));
    }

    @Test
    @DisplayName("Tarjeta en los bordes: 1 y 12 cuotas son válidas")
    void tarjetaCuotasBorde() {
        assertDoesNotThrow(() -> PagoFactory.crearPago(MedioPago.TARJETA, solicitud("1234", 1)));
        assertDoesNotThrow(() -> PagoFactory.crearPago(MedioPago.TARJETA, solicitud("1234", 12)));
    }

    @Test
    @DisplayName("Transferencia sin número de operación es rechazada")
    void transferenciaSinOperacion() {
        assertThrows(IllegalArgumentException.class,
                () -> PagoFactory.crearPago(MedioPago.TRANSFERENCIA, solicitud("  ", 1)));
    }

    @Test
    @DisplayName("Sin estudiante o concepto no se crea el pago")
    void faltanDatos() {
        SolicitudPago sinEstudiante = SolicitudPago.de(null, concepto, BigDecimal.TEN, null, "X");
        assertThrows(IllegalArgumentException.class, () -> PagoFactory.crearPago(MedioPago.EFECTIVO, sinEstudiante));
        assertThrows(IllegalArgumentException.class, () -> PagoFactory.crearPago(MedioPago.EFECTIVO, null));
    }

    @Test
    @DisplayName("Sin fecha se usa la fecha de hoy")
    void fechaPorDefecto() {
        Pago p = PagoFactory.crearPago(MedioPago.EFECTIVO, SolicitudPago.de(estudiante, concepto, BigDecimal.TEN, null, "R"));
        assertEquals(LocalDate.now(), p.getFecha());
    }
}
