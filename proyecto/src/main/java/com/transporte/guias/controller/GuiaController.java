package com.transporte.guias.controller;

import com.transporte.guias.model.ApiResponse;
import com.transporte.guias.model.Guia;
import com.transporte.guias.service.GuiaService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/guias")
@CrossOrigin(origins = "*")
public class GuiaController {
    
    @Autowired
    private GuiaService guiaService;
    
    /**
     * POST /api/guias
     * Crear una nueva guía de despacho
     */
    @PostMapping
    public ResponseEntity<ApiResponse> crearGuia(@Valid @RequestBody Guia guia) {
        log.info("POST /api/guias - Creando guía");
        Guia nuevaGuia = guiaService.crearGuia(guia);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Guía creada exitosamente", Map.of("guia", nuevaGuia)));
    }
    
    /**
     * POST /api/guias/{id}/subir
     * Subir una guía generada a S3
     */
    @PostMapping("/{id}/subir")
    public ResponseEntity<ApiResponse> subirAS3(@PathVariable String id) {
        log.info("POST /api/guias/{}/subir", id);
        String s3Url = guiaService.subirGuiaAS3(id);
        return ResponseEntity.ok(ApiResponse.success("Guía subida a S3 exitosamente", Map.of("s3Url", s3Url)));
    }
    
    /**
     * GET /api/guias/{id}/descargar?transportista=XXX
     * Descargar guía desde S3 con validación de permisos
     */
    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargarGuia(
            @PathVariable String id,
            @RequestParam String transportista) {
        log.info("GET /api/guias/{}/descargar por transportista: {}", id, transportista);
        
        byte[] pdfContent = guiaService.descargarGuia(id, transportista);
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=guia_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfContent);
    }
    
    /**
     * PUT /api/guias/{id}
     * Modificar o actualizar una guía existente
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> actualizarGuia(@PathVariable String id, @RequestBody Guia guia) {
        log.info("PUT /api/guias/{}", id);
        Guia guiaActualizada = guiaService.actualizarGuia(id, guia);
        return ResponseEntity.ok(ApiResponse.success("Guía actualizada exitosamente", Map.of("guia", guiaActualizada)));
    }
    
    /**
     * DELETE /api/guias/{id}
     * Eliminar una guía específica
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> eliminarGuia(@PathVariable String id) {
        log.info("DELETE /api/guias/{}", id);
        guiaService.eliminarGuia(id);
        return ResponseEntity.ok(ApiResponse.success("Guía eliminada exitosamente", null));
    }
    
    /**
     * GET /api/guias/consultar?transportista=XXX&fecha=YYYY-MM-DD
     * Consultar guías por transportista y fecha
     */
    @GetMapping("/consultar")
    public ResponseEntity<ApiResponse> consultarGuias(
            @RequestParam String transportista,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        log.info("GET /api/guias/consultar - transportista: {}, fecha: {}", transportista, fecha);
        
        List<Guia> guias = guiaService.consultarGuias(transportista, fecha);
        
        Map<String, Object> data = new HashMap<>();
        data.put("guias", guias);
        data.put("total", guias.size());
        
        return ResponseEntity.ok(ApiResponse.success("Consulta exitosa", data));
    }
    
    /**
     * GET /api/guias/health
     * Endpoint de salud para verificar que el servicio está funcionando
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse> health() {
        return ResponseEntity.ok(ApiResponse.success("Servicio funcionando correctamente", 
                Map.of("timestamp", LocalDate.now())));
    }
}