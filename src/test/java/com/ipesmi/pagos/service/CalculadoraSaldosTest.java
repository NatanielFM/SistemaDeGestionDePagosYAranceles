package com.ipesmi.pagos.service;

import com.ipesmi.pagos.factory.PagoFactory;
import com.ipesmi.pagos.factory.SolicitudPago;
import com.ipesmi.pagos.model.Concepto;
import com.ipesmi.pagos.model.EstadoPago;
import com.ipesmi.pagos.model.Estudiante;
import com.ipesmi.pagos.model.MedioPago;
import com.ipesmi.pagos.model.Pago;
import com.ipesmi.pagos.model.TipoConcepto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Cálculo de saldos sin base de datos. */
class CalculadoraSaldosTest {

    private Estudiante ana;
    private Concepto matricula;   // 45000, no becable
    private Concepto cuota;       // 32000, becable
    private List<Concepto> conceptos;
    private List<Pago> pagos;

    @BeforeEach
    void preparar() {
        ana = new Estudiante(1, "Ana", "Pérez", "ana@ipesmi.edu.ar");
        matricula = new Concepto("MAT", "Matrícula", new BigDecimal("45000"), TipoConcepto.MATRICULA, false);
        cuota = new Concepto("CUO", "Cuota", new BigDecimal("32000"), TipoConcepto.CUOTA, true);
        conceptos = List.of(matricula, cuota);
        pagos = new ArrayList<>();
    }

    private Pago pagar(Concepto c, String monto, EstadoPago estado) {
        Pago p = PagoFactory.crearPago(MedioPago.EFECTIVO,
                SolicitudPago.de(ana, c, new BigDecimal(monto), null, "R" + pagos.size()));
        p.setEstado(estado);
        pagos.add(p);
        return p;
    }

    private static void assertMonto(String esperado, BigDecimal real) {
        assertEquals(0, new BigDecimal(esperado).compareTo(real), () -> "esperado " + esperado + " pero fue " + real);
    }

    @Test
    @DisplayName("Sin pagos el saldo es la suma de todos los conceptos")
    void sinPagos() {
        assertMonto("77000", CalculadoraSaldos.saldoPendiente(ana, conceptos, pagos));
    }

    @Test
    @DisplayName("Sin conceptos el saldo es cero")
    void sinConceptos() {
        assertMonto("0", CalculadoraSaldos.saldoPendiente(ana, List.of(), pagos));
    }

    @Test
    @DisplayName("Un pago parcial reduce el saldo")
    void pagoParcial() {
        pagar(cuota, "12000", EstadoPago.ACREDITADO);
        assertMonto("20000", CalculadoraSaldos.saldoConcepto(ana, cuota, pagos));
        assertMonto("65000", CalculadoraSaldos.saldoPendiente(ana, conceptos, pagos));
    }

    @Test
    @DisplayName("Pagando todo exacto el saldo queda en cero")
    void todoPagado() {
        pagar(matricula, "45000", EstadoPago.ACREDITADO);
        pagar(cuota, "32000", EstadoPago.ACREDITADO);
        assertMonto("0", CalculadoraSaldos.saldoPendiente(ana, conceptos, pagos));
    }

    @Test
    @DisplayName("Pagos pendientes y anulados no descuentan saldo")
    void pendientesYAnulados() {
        pagar(cuota, "10000", EstadoPago.PENDIENTE);
        pagar(cuota, "5000", EstadoPago.ANULADO);
        assertMonto("32000", CalculadoraSaldos.saldoConcepto(ana, cuota, pagos));
    }

    @Test
    @DisplayName("Lo pendiente sí reduce lo disponible para pagar (evita pagar dos veces)")
    void disponibleConsideraPendientes() {
        pagar(cuota, "10000", EstadoPago.PENDIENTE);
        pagar(cuota, "5000", EstadoPago.ANULADO);
        assertMonto("22000", CalculadoraSaldos.disponibleParaPagar(ana, cuota, pagos));
    }

    @ParameterizedTest(name = "beca {0}% -> cuota ${1}")
    @CsvSource({"0, 32000", "25, 24000", "50, 16000", "100, 0", "33.33, 21334.40"})
    @DisplayName("La beca reduce solo los conceptos becables")
    void becaSobreCuota(double porcentaje, String esperado) {
        ana.setPorcentajeBeca(porcentaje);
        assertMonto(esperado, CalculadoraSaldos.importeConBeca(cuota, ana));
        assertMonto("45000", CalculadoraSaldos.importeConBeca(matricula, ana));
    }

    @Test
    @DisplayName("Beca del 100%: solo se debe la matrícula")
    void becaCompleta() {
        ana.setPorcentajeBeca(100);
        assertMonto("45000", CalculadoraSaldos.saldoPendiente(ana, conceptos, pagos));
        assertMonto("32000", CalculadoraSaldos.descuentoBeca(cuota, ana));
    }

    @Test
    @DisplayName("Si pagó de más antes de la beca, el saldo no queda negativo")
    void saldoNuncaNegativo() {
        pagar(cuota, "32000", EstadoPago.ACREDITADO);
        ana.setPorcentajeBeca(50);
        assertMonto("0", CalculadoraSaldos.saldoConcepto(ana, cuota, pagos));
        assertMonto("0", CalculadoraSaldos.disponibleParaPagar(ana, cuota, pagos));
    }

    @Test
    @DisplayName("Un estudiante inactivo no acumula deuda")
    void inactivo() {
        ana.setActivo(false);
        assertMonto("0", CalculadoraSaldos.saldoPendiente(ana, conceptos, pagos));
    }

    @Test
    @DisplayName("Los pagos de otro estudiante no afectan el saldo")
    void otroEstudiante() {
        Estudiante otro = new Estudiante(2, "Luis", "Gómez", "luis@ipesmi.edu.ar");
        Pago p = PagoFactory.crearPago(MedioPago.EFECTIVO,
                SolicitudPago.de(otro, cuota, new BigDecimal("32000"), null, "X"));
        pagos.add(p);
        assertMonto("32000", CalculadoraSaldos.saldoConcepto(ana, cuota, pagos));
    }

    @Test
    @DisplayName("Centavos: 0,01 de diferencia se mantiene como saldo")
    void centavos() {
        pagar(cuota, "31999.99", EstadoPago.ACREDITADO);
        assertMonto("0.01", CalculadoraSaldos.saldoConcepto(ana, cuota, pagos));
    }
}
