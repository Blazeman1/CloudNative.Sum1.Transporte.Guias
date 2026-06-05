package com.transporte.guias;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal de la aplicación Spring Boot
 * 
 * @SpringBootApplication = @Configuration + @EnableAutoConfiguration + @ComponentScan
 * Esta anotación hace tres cosas:
 * 1. @Configuration: Permite definir beans de Spring
 * 2. @EnableAutoConfiguration: Configura automáticamente Spring basado en las dependencias
 * 3. @ComponentScan: Escanea componentes en el paquete com.transporte.guias y subpaquetes
 */
@SpringBootApplication
@EnableScheduling  // Opcional: permite tareas programadas (limpieza de EFS, etc.)
public class TransporteGuiasApplication {

    /**
     * Método main - Punto de entrada de la aplicación
     * 
     * @param args Argumentos de línea de comandos
     */
    public static void main(String[] args) {
        // SpringApplication.run() hace lo siguiente:
        // 1. Inicia el contenedor de Spring IoC
        // 2. Escanea y registra todos los beans (@Service, @Controller, @Repository, etc.)
        // 3. Levanta un servidor web embebido (Tomcat por defecto en el puerto 8080)
        // 4. Despliega la aplicación y la deja corriendo
        SpringApplication.run(TransporteGuiasApplication.class, args);
        
        // Mensaje opcional para confirmar que la app está corriendo
        System.out.println("=== SISTEMA DE GESTIÓN DE GUÍAS DE DESPACHO ===");
        System.out.println("Aplicación iniciada correctamente en http://localhost:8080");
        System.out.println("Endpoints disponibles:");
        System.out.println("  POST   /api/guias              - Crear guía");
        System.out.println("  POST   /api/guias/{id}/subir  - Subir a S3");
        System.out.println("  GET    /api/guias/{id}/descargar - Descargar guía");
        System.out.println("  PUT    /api/guias/{id}        - Actualizar guía");
        System.out.println("  DELETE /api/guias/{id}        - Eliminar guía");
        System.out.println("  GET    /api/guias/consultar   - Consultar guías");
        System.out.println("  GET    /api/guias/health      - Health check");
        System.out.println("===============================================");
    }
}