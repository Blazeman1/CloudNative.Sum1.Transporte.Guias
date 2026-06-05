package com.transporte.guias.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Configuración del cliente AWS S3
 *
 * Estrategia de credenciales (DefaultCredentialsProvider resuelve en orden):
 *   1. Variables de entorno: AWS_ACCESS_KEY_ID / AWS_SECRET_ACCESS_KEY
 *   2. Archivo ~/.aws/credentials
 *   3. IAM Role asociado a la instancia EC2 (recomendado en producción)
 *
 * En producción sobre EC2 con un IAM Role que tenga permisos S3,
 * no es necesario ninguna credencial explícita.
 */
@Configuration
public class AwsS3Config {

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    /**
     * Bean del cliente S3 usando el SDK v2 de AWS
     * DefaultCredentialsProvider detecta automáticamente las credenciales
     * disponibles en el entorno (EC2 IAM Role, variables de entorno, etc.)
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(awsRegion))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
