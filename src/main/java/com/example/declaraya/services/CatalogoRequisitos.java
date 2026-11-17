package com.example.declaraya.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.example.declaraya.models.DocumentoCliente;

/**
 * Requisitos documentales de cada tramite. La clave es el nombre exacto del tramite
 * (el mismo catalogo que usa SolicitudController al crear solicitudes).
 */
@Component
public class CatalogoRequisitos {

    /** Un documento que el cliente debe (o puede) aportar para el tramite. */
    public record Requisito(String nombre, boolean obligatorio) {
    }

    /** Estado de un requisito en la lista de verificacion de una solicitud. */
    public record ItemVerificacion(String nombre, boolean obligatorio, boolean entregado) {
    }

    // catalogo de tramites de la plataforma (SolicitudController lo usa para validar las solicitudes)
    public static final List<String> TRAMITES = List.of(
            "Declaración de renta",
            "Declaración de IVA",
            "Retención en la fuente",
            "Impuesto de industria y comercio (ICA)",
            "Régimen Simple de Tributación",
            "Información exógena",
            "Actualización o inscripción del RUT",
            "Corrección de una declaración",
            "Solicitud de devolución de saldo a favor",
            "Respuesta a requerimiento de la DIAN",
            "Asesoría tributaria");

    private static final List<Requisito> POR_DEFECTO = List.of(
            new Requisito("Documento de identidad (si aplica)", false),
            new Requisito("Documentos que indique el contador", false));

    private static final Map<String, List<Requisito>> REQUISITOS = Map.of(
            "Declaración de renta", List.of(
                    new Requisito("Documento de identidad", true),
                    new Requisito("RUT actualizado", true),
                    new Requisito("Certificado de ingresos y retenciones (formulario 220, si es empleado)", false),
                    new Requisito("Extractos o certificados bancarios a 31 de diciembre", true),
                    new Requisito("Certificados de créditos o deudas (si aplica)", false),
                    new Requisito("Soportes de deducciones: medicina prepagada, intereses de vivienda o dependientes (si aplica)", false),
                    new Requisito("Declaración del año anterior (si aplica)", false)),
            "Declaración de IVA", List.of(
                    new Requisito("RUT", true),
                    new Requisito("Facturas de venta del periodo", true),
                    new Requisito("Facturas de compra del periodo", true),
                    new Requisito("Declaración del periodo anterior (si aplica)", false)),
            "Retención en la fuente", List.of(
                    new Requisito("RUT", true),
                    new Requisito("Relación de pagos y nómina del periodo", true),
                    new Requisito("Declaración del periodo anterior (si aplica)", false)),
            "Impuesto de industria y comercio (ICA)", List.of(
                    new Requisito("RUT", true),
                    new Requisito("Relación de ingresos del periodo", true),
                    new Requisito("Declaración anterior de ICA (si aplica)", false)),
            "Actualización o inscripción del RUT", List.of(
                    new Requisito("Documento de identidad", true),
                    new Requisito("Certificado o soporte de la actividad económica (si aplica)", false)));

    /** Requisitos del tramite; los tramites sin lista propia usan la lista por defecto. */
    public List<Requisito> requisitosPara(String tramite) {
        return REQUISITOS.getOrDefault(tramite, POR_DEFECTO);
    }

    public boolean esRequisitoDe(String tramite, String nombreRequisito) {
        return requisitosPara(tramite).stream().anyMatch(r -> r.nombre().equals(nombreRequisito));
    }

    /** Un requisito esta entregado si hay un documento vinculado a la solicitud que lo cumple. */
    public List<ItemVerificacion> verificar(String tramite, List<DocumentoCliente> documentosDeLaSolicitud) {
        List<ItemVerificacion> items = new ArrayList<>();
        for (Requisito r : requisitosPara(tramite)) {
            boolean entregado = documentosDeLaSolicitud.stream().anyMatch(d -> r.nombre().equals(d.getRequisito()));
            items.add(new ItemVerificacion(r.nombre(), r.obligatorio(), entregado));
        }
        return items;
    }

    /** Requisitos obligatorios todavia sin documento (para la advertencia al iniciar la preparacion). */
    public List<String> obligatoriosFaltantes(String tramite, List<DocumentoCliente> documentosDeLaSolicitud) {
        return verificar(tramite, documentosDeLaSolicitud).stream()
                .filter(i -> i.obligatorio() && !i.entregado())
                .map(ItemVerificacion::nombre)
                .toList();
    }
}
