package com.ipesmi.pagos.factory;

import com.ipesmi.pagos.model.EstadoPago;
import com.ipesmi.pagos.model.MedioPago;
import com.ipesmi.pagos.model.Pago;
import com.ipesmi.pagos.model.PagoEfectivo;
import com.ipesmi.pagos.model.PagoTarjeta;
import com.ipesmi.pagos.model.PagoTransferencia;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Patrón Factory: centraliza la creación de pagos según el medio. Quien llama
 * no conoce las subclases concretas, solo recibe un {@link Pago}.
 * <ul>
 *   <li>EFECTIVO: se acredita al instante; si no se indica recibo se genera uno.</li>
 *   <li>TARJETA: exige los últimos 4 dígitos y entre 1 y 12 cuotas; se acredita al instante.</li>
 *   <li>TRANSFERENCIA: exige nro. de operación y queda PENDIENTE de acreditación.</li>
 * </ul>
 */
public final class PagoFactory {

    public static final int MAX_CUOTAS = 12;

    private static final AtomicInteger SECUENCIA_RECIBO = new AtomicInteger(1);

    private PagoFactory() {
    }

    /** Variante por texto ("efectivo", "Tarjeta", "TRANSFERENCIA"...). */
    public static Pago crearPago(String tipo, SolicitudPago solicitud) {
        if (tipo == null || tipo.isBlank()) {
            throw new IllegalArgumentException("Debe indicar el tipo de pago");
        }
        MedioPago medio;
        try {
            medio = MedioPago.valueOf(tipo.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de pago no soportado: " + tipo);
        }
        return crearPago(medio, solicitud);
    }

    public static Pago crearPago(MedioPago medio, SolicitudPago s) {
        if (medio == null) {
            throw new IllegalArgumentException("Debe indicar el medio de pago");
        }
        if (s == null || s.estudiante() == null || s.concepto() == null) {
            throw new IllegalArgumentException("El pago necesita estudiante y concepto");
        }
        LocalDate fecha = s.fecha() != null ? s.fecha() : LocalDate.now();
        String ref = s.referencia() == null ? "" : s.referencia().strip();

        Pago pago = switch (medio) {
            case EFECTIVO -> {
                String recibo = ref.isEmpty() ? generarRecibo(fecha) : ref;
                PagoEfectivo p = new PagoEfectivo(s.estudiante(), s.concepto(), s.monto(), fecha, recibo);
                p.setEstado(EstadoPago.ACREDITADO);
                yield p;
            }
            case TARJETA -> {
                if (!ref.matches("\\d{4}")) {
                    throw new IllegalArgumentException("Para tarjeta ingrese los últimos 4 dígitos");
                }
                if (s.cuotas() < 1 || s.cuotas() > MAX_CUOTAS) {
                    throw new IllegalArgumentException("Las cuotas deben estar entre 1 y " + MAX_CUOTAS);
                }
                PagoTarjeta p = new PagoTarjeta(s.estudiante(), s.concepto(), s.monto(), fecha, ref, s.cuotas());
                p.setEstado(EstadoPago.ACREDITADO);
                yield p;
            }
            case TRANSFERENCIA -> {
                if (ref.isEmpty()) {
                    throw new IllegalArgumentException("La transferencia necesita el número de operación");
                }
                PagoTransferencia p = new PagoTransferencia(s.estudiante(), s.concepto(), s.monto(), fecha,
                        ref.toUpperCase(Locale.ROOT), s.banco());
                p.setEstado(EstadoPago.PENDIENTE);
                yield p;
            }
        };
        return pago;
    }

    /** Ej.: REC-20260310-143512-1 (fecha del pago, hora de emisión y secuencia de la sesión). */
    private static String generarRecibo(LocalDate fecha) {
        return String.format("REC-%tY%<tm%<td-%tH%<tM%<tS-%d", fecha, LocalTime.now(),
                SECUENCIA_RECIBO.getAndIncrement());
    }
}
