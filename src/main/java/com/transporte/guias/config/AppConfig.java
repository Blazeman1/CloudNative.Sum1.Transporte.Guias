package com.transporte.guias.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executor;

@Configuration
@PropertySource(value = "classpath:application.properties", ignoreResourceNotFound = false)
public class AppConfig implements WebMvcConfigurer {

    /**
     * Bean para formato de fechas global
     */
    @Bean
    public DateTimeFormatter dateTimeFormatter() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    }

    /**
     * Bean para formato de fechas cortas
     */
    @Bean
    public DateTimeFormatter dateFormatter() {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd");
    }

    /**
     * Configuración de CORS
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);

        registry.addMapping("/public/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET")
                .maxAge(3600);
    }

    /**
     * ThreadPool para tareas asíncronas
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("async-thread-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * Validador personalizado para guías
     */
    @Bean
    public GuiaValidator guiaValidator() {
        return new GuiaValidator();
    }

    public static class GuiaValidator {

        public boolean isValidRut(String rut) {
            if (rut == null || rut.trim().isEmpty()) return false;
            String cleanRut = rut.replace(".", "").replace("-", "");
            if (cleanRut.length() < 8 || cleanRut.length() > 9) return false;
            String cuerpo = cleanRut.substring(0, cleanRut.length() - 1);
            char dv = cleanRut.charAt(cleanRut.length() - 1);
            int suma = 0;
            int multiplicador = 2;
            for (int i = cuerpo.length() - 1; i >= 0; i--) {
                suma += Character.getNumericValue(cuerpo.charAt(i)) * multiplicador;
                multiplicador = multiplicador == 7 ? 2 : multiplicador + 1;
            }
            int resto = suma % 11;
            int digitoVerificador = 11 - resto;
            char dvCalculado;
            if (digitoVerificador == 11) dvCalculado = '0';
            else if (digitoVerificador == 10) dvCalculado = 'K';
            else dvCalculado = Character.forDigit(digitoVerificador, 10);
            return dv == dvCalculado;
        }

        public boolean isValidPeso(Double pesoKg) {
            return pesoKg != null && pesoKg > 0 && pesoKg < 10000;
        }

        public boolean isValidBultos(Integer bultos) {
            return bultos != null && bultos > 0 && bultos < 1000;
        }

        public boolean isValidValorDeclarado(Double valor) {
            return valor == null || (valor >= 0 && valor < 100_000_000);
        }
    }
}
