package com.transporte.guias.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Modelo que representa una Guía de Despacho
 * Se almacena en memoria (GuiaRepository) y en S3 como PDF
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Guia {

    /**
     * Identificador único generado por el repositorio (UUID)
     */
    private String id;

    /**
     * RUT del transportista asignado a esta guía
     * Ejemplo: "12345678-9"
     */
    @NotBlank(message = "El transportista es obligatorio")
    private String transportista;

    /**
     * Nombre o razón social del destinatario
     */
    @NotBlank(message = "El destinatario es obligatorio")
    private String destinatario;

    /**
     * Dirección de destino del despacho
     */
    @NotBlank(message = "La dirección de destino es obligatoria")
    private String direccionDestino;

    /**
     * Ciudad o comuna de destino
     */
    @NotBlank(message = "La ciudad de destino es obligatoria")
    private String ciudadDestino;

    /**
     * Descripción de la mercadería despachada
     */
    @NotBlank(message = "La descripción de la mercadería es obligatoria")
    private String descripcionMercaderia;

    /**
     * Cantidad de bultos del despacho
     */
    @NotNull(message = "La cantidad de bultos es obligatoria")
    @Min(value = 1, message = "Debe haber al menos 1 bulto")
    @Max(value = 999, message = "No puede haber más de 999 bultos")
    private Integer bultos;

    /**
     * Peso total en kilogramos
     */
    @NotNull(message = "El peso es obligatorio")
    @Positive(message = "El peso debe ser positivo")
    private Double pesoKg;

    /**
     * Valor declarado de la mercadería (opcional, en pesos CLP)
     */
    private Double valorDeclarado;

    /**
     * Fecha del despacho (se asigna automáticamente si no se indica)
     */
    private LocalDate fecha;

    /**
     * URL del archivo PDF en S3 (se asigna al subir la guía)
     */
    private String s3Url;

    /**
     * Ruta temporal en EFS donde se guardó el PDF antes de subir a S3
     */
    private String efsPath;

    /**
     * Estado de la guía: CREADA, SUBIDA_S3, DESCARGADA, CANCELADA
     */
    @Builder.Default
    private EstadoGuia estado = EstadoGuia.CREADA;

    /**
     * Timestamp de creación
     */
    private LocalDateTime fechaCreacion;

    /**
     * Timestamp de última modificación
     */
    private LocalDateTime fechaModificacion;

    /**
     * Enumeración de estados posibles para una guía
     */
    public enum EstadoGuia {
        CREADA,
        SUBIDA_S3,
        DESCARGADA,
        CANCELADA
    }
}
