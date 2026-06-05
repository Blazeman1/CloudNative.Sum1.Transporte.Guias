package com.transporte.guias.service;

import com.transporte.guias.exception.GuiaNotFoundException;
import com.transporte.guias.model.Guia;
import com.transporte.guias.repository.GuiaRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Servicio principal de negocio para la gestión de Guías de Despacho.
 *
 * Flujo de una guía:
 *   1. crearGuia()         → se persiste en memoria y se genera el PDF en EFS
 *   2. subirGuiaAS3()      → PDF viaja de EFS a S3; se actualiza la URL
 *   3. descargarGuia()     → se descarga desde S3 con validación de permisos
 *   4. actualizarGuia()    → se modifican los datos y se regenera el PDF
 *   5. eliminarGuia()      → se borra de memoria, EFS y S3
 */
@Slf4j
@Service
public class GuiaService {

    private final GuiaRepository guiaRepository;
    private final PdfGeneratorService pdfGeneratorService;
    private final EfsStorageService efsStorageService;
    private final S3StorageService s3StorageService;

    public GuiaService(GuiaRepository guiaRepository,
                       PdfGeneratorService pdfGeneratorService,
                       EfsStorageService efsStorageService,
                       S3StorageService s3StorageService) {
        this.guiaRepository     = guiaRepository;
        this.pdfGeneratorService = pdfGeneratorService;
        this.efsStorageService   = efsStorageService;
        this.s3StorageService    = s3StorageService;
    }

    // ── Crear ────────────────────────────────────────────────────────────────

    /**
     * Crea una nueva guía de despacho.
     * Asigna fecha actual si no se indicó, genera el PDF y lo guarda en EFS.
     *
     * @param guia datos de la guía enviados por el cliente
     * @return guía persistida con ID y ruta EFS asignados
     */
    public Guia crearGuia(Guia guia) {
        log.info("Creando nueva guía para transportista: {}", guia.getTransportista());

        // Asignar fecha actual si no viene en el request
        if (guia.getFecha() == null) {
            guia.setFecha(LocalDate.now());
        }
        guia.setFechaCreacion(LocalDateTime.now());
        guia.setEstado(Guia.EstadoGuia.CREADA);

        // Persistir para obtener el ID (UUID asignado en el repositorio)
        Guia guiaGuardada = guiaRepository.save(guia);

        // Generar PDF con iTextPDF y guardarlo en EFS
        try {
            byte[] pdfBytes = pdfGeneratorService.generarPdfCompleto(guiaGuardada);
            Path rutaEfs = efsStorageService.guardarTemporalmente(
                    guiaGuardada.getTransportista(),
                    guiaGuardada.getId(),
                    pdfBytes);
            guiaGuardada.setEfsPath(rutaEfs.toString());
            guiaRepository.save(guiaGuardada);  // actualizar con la ruta EFS
            log.info("PDF generado y guardado en EFS: {}", rutaEfs);
        } catch (Exception e) {
            log.warn("No se pudo guardar el PDF en EFS (continuando sin EFS): {}", e.getMessage());
            // No fallamos la creación si EFS no está disponible en local/dev
        }

        log.info("Guía creada con ID: {}", guiaGuardada.getId());
        return guiaGuardada;
    }

    // ── Subir a S3 ───────────────────────────────────────────────────────────

    /**
     * Sube la guía a AWS S3.
     * Busca el PDF en EFS; si no existe lo regenera en memoria.
     *
     * @param guiaId ID de la guía a subir
     * @return URL del objeto en S3 (s3://bucket/key)
     */
    public String subirGuiaAS3(String guiaId) {
        log.info("Subiendo guía {} a S3", guiaId);

        Guia guia = guiaRepository.findById(guiaId)
                .orElseThrow(() -> new GuiaNotFoundException(guiaId));

        byte[] pdfBytes;

        // Intentar leer desde EFS; si no está disponible regenerar
        if (guia.getEfsPath() != null && efsStorageService.existeArchivo(guia.getEfsPath())) {
            try {
                pdfBytes = efsStorageService.leerTemporal(guia.getEfsPath());
                log.info("PDF leído desde EFS: {}", guia.getEfsPath());
            } catch (Exception e) {
                log.warn("Error leyendo EFS, regenerando PDF: {}", e.getMessage());
                pdfBytes = pdfGeneratorService.generarPdfCompleto(guia);
            }
        } else {
            log.info("Archivo EFS no disponible, generando PDF en memoria");
            pdfBytes = pdfGeneratorService.generarPdfCompleto(guia);
        }

        // Subir a S3
        String s3Url = s3StorageService.subirGuia(guia.getTransportista(), guiaId, pdfBytes);

        // Actualizar estado y URL en el repositorio
        guia.setS3Url(s3Url);
        guia.setEstado(Guia.EstadoGuia.SUBIDA_S3);
        guia.setFechaModificacion(LocalDateTime.now());
        guiaRepository.save(guia);

        // Limpiar archivo temporal de EFS si existe
        if (guia.getEfsPath() != null) {
            try {
                efsStorageService.eliminarTemporal(guia.getEfsPath());
                log.info("Archivo temporal EFS eliminado tras subida a S3");
            } catch (Exception e) {
                log.warn("No se pudo limpiar EFS: {}", e.getMessage());
            }
        }

        return s3Url;
    }

    // ── Descargar ────────────────────────────────────────────────────────────

    /**
     * Descarga una guía desde S3 validando que el transportista tenga acceso.
     *
     * @param guiaId        ID de la guía
     * @param transportista transportista que solicita la descarga
     * @return bytes del PDF
     * @throws SecurityException     si el transportista no coincide con el de la guía
     * @throws GuiaNotFoundException si la guía no existe
     */
    public byte[] descargarGuia(String guiaId, String transportista) {
        log.info("Descargando guía {} para transportista: {}", guiaId, transportista);

        Guia guia = guiaRepository.findById(guiaId)
                .orElseThrow(() -> new GuiaNotFoundException(guiaId));

        // Validación de permisos: solo el transportista asignado puede descargar
        if (!guia.getTransportista().equalsIgnoreCase(transportista)) {
            log.warn("Acceso denegado: transportista '{}' intentó descargar guía de '{}'",
                    transportista, guia.getTransportista());
            throw new SecurityException(
                    "No tiene permisos para descargar esta guía. " +
                    "Solo el transportista asignado puede acceder.");
        }

        // Si no está en S3, generamos el PDF al vuelo
        if (guia.getEstado() != Guia.EstadoGuia.SUBIDA_S3) {
            log.info("Guía no está en S3, generando PDF al vuelo");
            byte[] pdf = pdfGeneratorService.generarPdfCompleto(guia);
            guia.setEstado(Guia.EstadoGuia.DESCARGADA);
            guia.setFechaModificacion(LocalDateTime.now());
            guiaRepository.save(guia);
            return pdf;
        }

        byte[] pdfBytes = s3StorageService.descargarGuia(transportista, guiaId);

        guia.setEstado(Guia.EstadoGuia.DESCARGADA);
        guia.setFechaModificacion(LocalDateTime.now());
        guiaRepository.save(guia);

        return pdfBytes;
    }

    // ── Actualizar ───────────────────────────────────────────────────────────

    /**
     * Actualiza los datos de una guía existente.
     * Si la guía ya fue subida a S3 se vuelve a subir con el PDF actualizado.
     *
     * @param guiaId ID de la guía a actualizar
     * @param datosNuevos nuevos datos a aplicar
     * @return guía actualizada
     */
    public Guia actualizarGuia(String guiaId, Guia datosNuevos) {
        log.info("Actualizando guía: {}", guiaId);

        Guia guia = guiaRepository.findById(guiaId)
                .orElseThrow(() -> new GuiaNotFoundException(guiaId));

        // Aplicar cambios (solo los campos no nulos del request)
        if (datosNuevos.getDestinatario()         != null) guia.setDestinatario(datosNuevos.getDestinatario());
        if (datosNuevos.getDireccionDestino()      != null) guia.setDireccionDestino(datosNuevos.getDireccionDestino());
        if (datosNuevos.getCiudadDestino()         != null) guia.setCiudadDestino(datosNuevos.getCiudadDestino());
        if (datosNuevos.getDescripcionMercaderia() != null) guia.setDescripcionMercaderia(datosNuevos.getDescripcionMercaderia());
        if (datosNuevos.getBultos()                != null) guia.setBultos(datosNuevos.getBultos());
        if (datosNuevos.getPesoKg()                != null) guia.setPesoKg(datosNuevos.getPesoKg());
        if (datosNuevos.getValorDeclarado()        != null) guia.setValorDeclarado(datosNuevos.getValorDeclarado());
        if (datosNuevos.getFecha()                 != null) guia.setFecha(datosNuevos.getFecha());

        guia.setFechaModificacion(LocalDateTime.now());

        // Regenerar el PDF con los nuevos datos y guardarlo en EFS
        try {
            byte[] pdfActualizado = pdfGeneratorService.generarPdfCompleto(guia);
            Path rutaEfs = efsStorageService.guardarTemporalmente(
                    guia.getTransportista(), guiaId, pdfActualizado);
            guia.setEfsPath(rutaEfs.toString());
        } catch (Exception e) {
            log.warn("No se pudo actualizar EFS: {}", e.getMessage());
        }

        // Si ya estaba en S3, volver a subir con los datos actualizados
        if (guia.getS3Url() != null) {
            try {
                String nuevaUrl = subirGuiaAS3(guiaId);
                guia.setS3Url(nuevaUrl);
                log.info("Guía actualizada y re-subida a S3: {}", nuevaUrl);
            } catch (Exception e) {
                log.warn("No se pudo re-subir a S3: {}", e.getMessage());
                guia.setEstado(Guia.EstadoGuia.CREADA);  // Volver al estado anterior
            }
        }

        return guiaRepository.save(guia);
    }

    // ── Eliminar ─────────────────────────────────────────────────────────────

    /**
     * Elimina una guía de todos los almacenamientos (memoria, EFS y S3).
     *
     * @param guiaId ID de la guía a eliminar
     */
    public void eliminarGuia(String guiaId) {
        log.info("Eliminando guía: {}", guiaId);

        Guia guia = guiaRepository.findById(guiaId)
                .orElseThrow(() -> new GuiaNotFoundException(guiaId));

        // Eliminar de EFS si existe
        if (guia.getEfsPath() != null) {
            try {
                efsStorageService.eliminarTemporal(guia.getEfsPath());
            } catch (Exception e) {
                log.warn("No se pudo eliminar de EFS: {}", e.getMessage());
            }
        }

        // Eliminar de S3 si fue subida
        if (guia.getEstado() == Guia.EstadoGuia.SUBIDA_S3 || guia.getS3Url() != null) {
            try {
                s3StorageService.eliminarGuia(guia.getTransportista(), guiaId);
            } catch (Exception e) {
                log.warn("No se pudo eliminar de S3: {}", e.getMessage());
            }
        }

        // Eliminar del repositorio en memoria
        guiaRepository.deleteById(guiaId);
        log.info("Guía {} eliminada correctamente", guiaId);
    }

    // ── Consultar ────────────────────────────────────────────────────────────

    /**
     * Consulta las guías de un transportista para una fecha específica.
     *
     * @param transportista nombre del transportista
     * @param fecha         fecha de búsqueda
     * @return lista de guías que coincidan
     */
    public List<Guia> consultarGuias(String transportista, LocalDate fecha) {
        log.info("Consultando guías - transportista: {}, fecha: {}", transportista, fecha);
        List<Guia> guias = guiaRepository.findByTransportistaAndFecha(transportista, fecha);
        log.info("Se encontraron {} guías", guias.size());
        return guias;
    }
}
