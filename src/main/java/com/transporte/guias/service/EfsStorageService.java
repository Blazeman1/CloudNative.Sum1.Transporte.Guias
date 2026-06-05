package com.transporte.guias.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class EfsStorageService {
    
    @Value("${efs.mount.path:/app/efs/guias_tmp}")
    private String efsBasePath;
    
    /**
     * Guarda el contenido de una guía temporalmente en EFS
     * Estructura: /app/efs/guias_tmp/{transportista}/{fecha}/guia_{id}.pdf
     */
    public Path guardarTemporalmente(String transportista, String guiaId, byte[] contenido) throws IOException {
        // Crear estructura de carpetas: transportista/fecha/
        String fechaStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        Path dirPath = Paths.get(efsBasePath, transportista, fechaStr);
        
        // Crear directorios si no existen
        Files.createDirectories(dirPath);
        log.info("Directorio EFS creado/verificado: {}", dirPath);
        
        // Nombre del archivo
        String fileName = String.format("guia_%s.pdf", guiaId);
        Path filePath = dirPath.resolve(fileName);
        
        // Escribir el archivo
        Files.write(filePath, contenido, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        log.info("Archivo guardado en EFS: {}", filePath);
        
        return filePath;
    }
    
    /**
     * Lee un archivo desde EFS
     */
    public byte[] leerTemporal(String pathStr) throws IOException {
        Path path = Paths.get(pathStr);
        if (!Files.exists(path)) {
            throw new IOException("Archivo no encontrado en EFS: " + pathStr);
        }
        return Files.readAllBytes(path);
    }
    
    /**
     * Elimina un archivo temporal de EFS
     */
    public boolean eliminarTemporal(String pathStr) throws IOException {
        Path path = Paths.get(pathStr);
        if (Files.exists(path)) {
            Files.delete(path);
            log.info("Archivo eliminado de EFS: {}", pathStr);
            return true;
        }
        return false;
    }
    
    /**
     * Verifica si un archivo existe en EFS
     */
    public boolean existeArchivo(String pathStr) {
        return Files.exists(Paths.get(pathStr));
    }
    
    /**
     * Obtiene la ruta completa esperada para una guía
     */
    public String getRutaEsperada(String transportista, String guiaId) {
        String fechaStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        return Paths.get(efsBasePath, transportista, fechaStr, String.format("guia_%s.pdf", guiaId)).toString();
    }
}