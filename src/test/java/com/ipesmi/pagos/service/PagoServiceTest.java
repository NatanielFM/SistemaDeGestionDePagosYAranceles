package com.ipesmi.pagos.service;

import com.ipesmi.pagos.dao.ConceptoDAO;
import com.ipesmi.pagos.dao.EstudianteDAO;
import com.ipesmi.pagos.dao.JPAUtil;
import com.ipesmi.pagos.dao.PagoDAO;
import com.ipesmi.pagos.exception.BecaNoAplicableException;
import com.ipesmi.pagos.exception.MontoInvalidoException;
import com.ipesmi.pagos.exception.PagoDuplicadoException;
import com.ipesmi.pagos.factory.SolicitudPago;
import com.ipesmi.pagos.model.Concepto;
import com.ipesmi.pagos.model.EstadoPago;
import com.ipesmi.pagos.model.Estudiante;
import com.ipesmi.pagos.model.MedioPago;
import com.ipesmi.pagos.model.Pago;
import com.ipesmi.pagos.model.TipoConcepto;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración de los servicios con JPA sobre una base H2 en
 * memoria (se recrea antes de cada prueba).
 */
class PagoServiceTest {

    private EstudianteService estudiantes;
    private ConceptoService conceptos;
    private PagoService pagos;

    private Estudiante ana;
    private Concepto matricula;
    private Concepto cuota;

    @BeforeEach
    void preparar() throws Exception {
        Map<String, String> props = new HashMap<>(JPAUtil.h2("jdbc:h2:mem:pagos-test;DB_CLOSE_DELAY=-1;MODE=MySQL"));
        props.put("hibernate.hbm2ddl.auto", "create-drop");
        JPAUtil.configurar(props);

        PagoDAO pagoDAO = new PagoDAO();
        estudiantes = new EstudianteService(new EstudianteDAO(), pagoDAO);
        conceptos = new ConceptoService(new ConceptoDAO(), pagoDAO);
        pagos = new PagoService(pagoDAO, estudiantes, conceptos);

        ana = estudiantes.registrar(new Estudiante(1001, "Ana", "Pérez", "ana@ipesmi.edu.ar"));
        matricula = conceptos.registrar("MAT", "Matrícula", new BigDecimal("45000"), TipoConcepto.MATRICULA, false, null);
        cuota = conceptos.registrar("CUO", "Cuota marzo", new BigDecimal("32000"), TipoConcepto.CUOTA, true, null);
    }

    @AfterAll
    static void cerrar() {
        JPAUtil.cerrar();
    }

    private SolicitudPago solicitud(Concepto c, String monto, String referencia) {
        return SolicitudPago.de(ana, c, new BigDecimal(monto), LocalDate.of(2026, 3, 10), referencia);
    }

    @Test
    @DisplayName("Registrar un pago lo persiste y actualiza el saldo")
    void registrarPago() throws Exception {
        Pago p = pagos.registrarPago(MedioPago.EFECTIVO, solicitud(matricula, "45000", "REC-1"));

        assertNotNull(p.getId());
        assertEquals(1, new PagoDAO().listarTodos().size());
        assertEquals(0, new BigDecimal("32000").compareTo(pagos.saldoPendiente(ana)));
    }

    @Test
    @DisplayName("Los datos sobreviven a una recarga desde la base")
    void persistencia() throws Exception {
        pagos.registrarPago(MedioPago.TARJETA, new SolicitudPago(ana, cuota, new BigDecimal("16000"),
                LocalDate.of(2026, 3, 10), "4821", 3, null));
        PagoService recargado = new PagoService(new PagoDAO(), estudiantes, conceptos);
        assertEquals(1, recargado.listar().size());
        assertEquals(MedioPago.TARJETA, recargado.listar().get(0).getMedio());
        assertEquals(0, new BigDecimal("61000").compareTo(recargado.saldoPendiente(ana)));
    }

    @Test
    @DisplayName("Pagar un concepto ya cancelado lanza PagoDuplicadoException")
    void conceptoYaPagado() throws Exception {
        pagos.registrarPago(MedioPago.EFECTIVO, solicitud(matricula, "45000", "REC-1"));
        assertThrows(PagoDuplicadoException.class,
                () -> pagos.registrarPago(MedioPago.EFECTIVO, solicitud(matricula, "100", "REC-2")));
    }

    @Test
    @DisplayName("Repetir un nro. de operación lanza PagoDuplicadoException")
    void referenciaRepetida() throws Exception {
        pagos.registrarPago(MedioPago.TRANSFERENCIA, solicitud(cuota, "10000", "OP-1"));
        assertThrows(PagoDuplicadoException.class,
                () -> pagos.registrarPago(MedioPago.TRANSFERENCIA, solicitud(cuota, "10000", "op-1")));
    }

    @Test
    @DisplayName("Si se anula el pago, se puede volver a pagar el concepto")
    void anularLiberaSaldo() throws Exception {
        Pago p = pagos.registrarPago(MedioPago.EFECTIVO, solicitud(matricula, "45000", "REC-1"));
        pagos.anular(p);
        assertDoesNotThrow(() -> pagos.registrarPago(MedioPago.EFECTIVO, solicitud(matricula, "45000", "REC-2")));
    }

    @ParameterizedTest(name = "monto {0}")
    @ValueSource(strings = {"0", "-1", "-0.01", "10.005"})
    @DisplayName("Montos cero, negativos o con más de 2 decimales lanzan MontoInvalidoException")
    void montoInvalido(String monto) {
        assertThrows(MontoInvalidoException.class,
                () -> pagos.registrarPago(MedioPago.EFECTIVO, solicitud(cuota, monto, "R")));
    }

    @Test
    @DisplayName("Monto nulo lanza MontoInvalidoException")
    void montoNulo() {
        SolicitudPago s = new SolicitudPago(ana, cuota, null, null, "R", 1, null);
        assertThrows(MontoInvalidoException.class, () -> pagos.registrarPago(MedioPago.EFECTIVO, s));
    }

    @Test
    @DisplayName("Borde: pagar exactamente el saldo es válido, un centavo más no")
    void montoEnElLimite() throws Exception {
        assertThrows(MontoInvalidoException.class,
                () -> pagos.registrarPago(MedioPago.EFECTIVO, solicitud(cuota, "32000.01", "R1")));
        assertDoesNotThrow(() -> pagos.registrarPago(MedioPago.EFECTIVO, solicitud(cuota, "32000.00", "R2")));
        assertEquals(0, BigDecimal.ZERO.compareTo(pagos.saldoConcepto(ana, cuota)));
    }

    @Test
    @DisplayName("Una transferencia pendiente cuenta para no pagar dos veces, pero no baja el saldo hasta acreditarse")
    void transferenciaPendiente() throws Exception {
        Pago t = pagos.registrarPago(MedioPago.TRANSFERENCIA, solicitud(cuota, "32000", "OP-9"));
        assertEquals(EstadoPago.PENDIENTE, t.getEstado());
        assertEquals(0, new BigDecimal("32000").compareTo(pagos.saldoConcepto(ana, cuota)));
        assertThrows(PagoDuplicadoException.class,
                () -> pagos.registrarPago(MedioPago.EFECTIVO, solicitud(cuota, "1", "R")));

        pagos.acreditar(t);
        assertEquals(0, BigDecimal.ZERO.compareTo(pagos.saldoConcepto(ana, cuota)));
        assertThrows(IllegalStateException.class, () -> pagos.acreditar(t));
    }

    @Test
    @DisplayName("Aplicar una beca del 50% reduce solo la cuota")
    void aplicarBeca() throws Exception {
        pagos.aplicarBeca(1001, 50);
        assertEquals(50, estudiantes.buscar(1001).orElseThrow().getPorcentajeBeca());
        assertEquals(0, new BigDecimal("61000").compareTo(pagos.saldoPendiente(ana)));
        assertEquals(50, new EstudianteDAO().buscarPorId(1001).orElseThrow().getPorcentajeBeca(), "persistida");
    }

    @Test
    @DisplayName("Borde: beca del 100% es válida")
    void becaCompleta() throws Exception {
        pagos.aplicarBeca(1001, 100);
        assertEquals(0, BigDecimal.ZERO.compareTo(pagos.saldoConcepto(ana, cuota)));
    }

    @ParameterizedTest(name = "beca {0}%")
    @ValueSource(doubles = {0, -10, 100.01, 150, Double.NaN})
    @DisplayName("Porcentajes fuera de (0, 100] lanzan BecaNoAplicableException")
    void becaFueraDeRango(double porcentaje) {
        assertThrows(BecaNoAplicableException.class, () -> pagos.aplicarBeca(1001, porcentaje));
    }

    @Test
    @DisplayName("Beca a un estudiante inactivo lanza BecaNoAplicableException")
    void becaInactivo() {
        estudiantes.actualizar(1001, "Ana", "Pérez", "ana@ipesmi.edu.ar", false);
        assertThrows(BecaNoAplicableException.class, () -> pagos.aplicarBeca(1001, 30));
    }

    @Test
    @DisplayName("Beca a un legajo inexistente lanza BecaNoAplicableException")
    void becaLegajoInexistente() {
        assertThrows(BecaNoAplicableException.class, () -> pagos.aplicarBeca(9999, 30));
    }

    @Test
    @DisplayName("Si la cuota ya está paga, la beca no es aplicable y no se modifica el estudiante")
    void becaSinSaldoBecable() throws Exception {
        pagos.registrarPago(MedioPago.EFECTIVO, solicitud(cuota, "32000", "R"));
        assertThrows(BecaNoAplicableException.class, () -> pagos.aplicarBeca(1001, 40));
        assertEquals(0, ana.getPorcentajeBeca());
    }

    @Test
    @DisplayName("Modificar un pago valida el monto contra el saldo sin contarse a sí mismo")
    void actualizarPago() throws Exception {
        Pago p = pagos.registrarPago(MedioPago.EFECTIVO, solicitud(cuota, "10000", "R"));
        pagos.actualizarPago(p, new BigDecimal("32000"), null, null);
        assertEquals(0, new BigDecimal("32000").compareTo(p.getMonto()));
        assertThrows(MontoInvalidoException.class, () -> pagos.actualizarPago(p, new BigDecimal("32000.50"), null, null));
    }

    @Test
    @DisplayName("No se elimina un estudiante con pagos")
    void eliminarEstudianteConPagos() throws Exception {
        pagos.registrarPago(MedioPago.EFECTIVO, solicitud(cuota, "100", "R"));
        assertThrows(IllegalStateException.class, () -> estudiantes.eliminar(1001));
    }

    @Test
    @DisplayName("Concepto con monto inválido lanza MontoInvalidoException")
    void conceptoMontoInvalido() {
        assertThrows(MontoInvalidoException.class, () -> conceptos.registrar("X", "Algo",
                new BigDecimal("-5"), TipoConcepto.OTRO, false, null));
    }

    @Test
    @DisplayName("Distribución por medio y por tipo de concepto (JPQL)")
    void distribucion() throws Exception {
        pagos.registrarPago(MedioPago.EFECTIVO, solicitud(matricula, "45000", "R"));
        pagos.registrarPago(MedioPago.TARJETA, new SolicitudPago(ana, cuota, new BigDecimal("2000"), null, "1111", 1, null));
        pagos.registrarPago(MedioPago.TRANSFERENCIA, solicitud(cuota, "1000", "OP-2"));   // pendiente: no cuenta

        assertEquals(2, pagos.distribucionPorMedio().size());
        assertEquals(0, new BigDecimal("45000").compareTo(pagos.distribucionPorTipoConcepto().get(TipoConcepto.MATRICULA)));
        assertEquals(0, new BigDecimal("2000").compareTo(pagos.distribucionPorTipoConcepto().get(TipoConcepto.CUOTA)));
    }
}
