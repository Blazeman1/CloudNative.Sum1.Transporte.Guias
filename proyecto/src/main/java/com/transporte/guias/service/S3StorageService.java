package com.transporte.guias.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Servicio de almacenamiento en AWS S3
 *
 * Gestiona la subida, descarga y eliminación de guías en S3.
 *
 * Estructura de carpetas en S3:
 *   {fecha}/{transportista}/guia_{id}.pdf
 *   Ejemplo: 20240115/transportistaX/guia_abc123.pdf
 */
@Slf4j
@Service
public class S3StorageService {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public S3StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Sube un archivo PDF a S3 usando bytes en memoria.
     * La clave sigue la estructura: {fecha}/{transportista}/guia_{id}.pdf
     *
     * @param transportista nombre del transportista (usado como carpeta)
     * @param guiaId        identificador único de la guía
     * @param contenido     bytes del PDF a subir
     * @return URL pública/privada del objeto en S3
     */
    public String subirGuia(String transportista, String guiaId, byte[] contenido) {
        String key = construirKey(transportista, guiaId);
        log.info("Subiendo guía a S3: bucket={}, key={}", bucketName, key);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/pdf")
                .contentLength((long) contenido.length)
                // Metadatos personalizados para auditoría
                .metadata(java.util.Map.of(
                        "guia-id", guiaId,
                        "transportista", transportista,
                        "upload-date", LocalDate.now().toString()
                ))
                .build();

        s3Client.putObject(request, RequestBody.fromBytes(contenido));

        String s3Url = String.format("s3://%s/%s", bucketName, key);
        log.info("Guía subida exitosamente a S3: {}", s3Url);
        return s3Url;
    }

    /**
     * Sube un archivo PDF a S3 desde una ruta en disco (EFS).
     *
     * @param transportista nombre del transportista
     * @param guiaId        identificador de la guía
     * @param filePath      ruta del archivo en el sistema de archivos (EFS)
     * @return URL del objeto en S3
     */
    public String subirGuiaDesdeEfs(String transportista, String guiaId, Path filePath) {
        String key = construirKey(transportista, guiaId);
        log.info("Subiendo guía desde EFS a S3: archivo={}, key={}", filePath, key);

        PutObjectRequest request = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("application/pdf")
                .build();

        s3Client.putObject(request, RequestBody.fromFile(filePath));

        String s3Url = String.format("s3://%s/%s", bucketName, key);
        log.info("Archivo subido desde EFS a S3: {}", s3Url);
        return s3Url;
    }

    /**
     * Descarga una guía desde S3 validando que pertenezca al transportista indicado.
     *
     * @param transportista nombre del transportista (validación de permisos)
     * @param guiaId        identificador de la guía a descargar
     * @return bytes del PDF descargado
     * @throws SecurityException    si el transportista no tiene acceso a la guía
     * @throws RuntimeException     si el archivo no existe en S3
     */
    public byte[] descargarGuia(String transportista, String guiaId) {
        String key = construirKey(transportista, guiaId);
        log.info("Descargando guía de S3: bucket={}, key={}", bucketName, key);

        // Verificar existencia y permisos antes de descargar
        if (!existeEnS3(key)) {
            throw new RuntimeException(
                    "La guía no existe en S3 para el transportista: " + transportista);
        }

        GetObjectRequest request = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);
        log.info("Guía descargada exitosamente: {} bytes", response.asByteArray().length);
        return response.asByteArray();
    }

    /**
     * Elimina una guía de S3.
     *
     * @param transportista nombre del transportista (para construir la clave)
     * @param guiaId        identificador de la guía
     */
    public void eliminarGuia(String transportista, String guiaId) {
        String key = construirKey(transportista, guiaId);
        log.info("Eliminando guía de S3: bucket={}, key={}", bucketName, key);

        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.deleteObject(request);
        log.info("Guía eliminada de S3: {}", key);
    }

    /**
     * Verifica si un objeto existe en S3.
     *
     * @param key clave del objeto en S3
     * @return true si existe, false en caso contrario
     */
    public boolean existeEnS3(String key) {
        try {
            s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        }
    }

    /**
     * Construye la clave (path) del objeto en S3.
     * Formato: {fecha}/{transportista}/guia_{id}.pdf
     * Ejemplo: 20240115/transportistaEjemplo/guia_abc-123.pdf
     *
     * @param transportista nombre del transportista
     * @param guiaId        ID de la guía
     * @return clave S3
     */
    public String construirKey(String transportista, String guiaId) {
        String fecha = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        // Sanitizar el nombre del transportista para usarlo como nombre de carpeta
        String transportistaSanitizado = transportista
                .toLowerCase()
                .replaceAll("[^a-z0-9_\\-]", "_");
        return String.format("%s/%s/guia_%s.pdf", fecha, transportistaSanitizado, guiaId);
    }
}
