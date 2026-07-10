package com.medisalud.citas.infrastructure.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

/**
 * Configuración del sistema de mensajes internacionalizado (i18n).
 *
 * <p>
 * Para añadir un nuevo idioma, basta con crear el archivo correspondiente en
 * src/main/resources/, por ejemplo:
 * <ul>
 * <li>{@code messages_en.properties} → inglés</li>
 * <li>{@code messages_fr.properties} → francés</li>
 * </ul>
 * El idioma se resolverá automáticamente a partir del header
 * {@code Accept-Language}
 * de cada petición HTTP.
 */
@Configuration
public class MessageConfig {

    /**
     * Define el MessageSource que Spring usará para resolver mensajes.
     * - Apunta a classpath:messages (= src/main/resources/messages.properties)
     * - Usa UTF-8 para soportar caracteres especiales (tildes, ñ, etc.)
     * - Fallback al locale por defecto si no existe el archivo del idioma
     * solicitado
     */
    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource source = new ReloadableResourceBundleMessageSource();
        source.setBasename("classpath:messages");
        source.setDefaultEncoding("UTF-8");
        source.setUseCodeAsDefaultMessage(false);
        source.setFallbackToSystemLocale(false);
        source.setDefaultLocale(new Locale("es"));
        return source;
    }

    /**
     * Resuelve el Locale de cada request usando el header Accept-Language.
     * Si no se envía el header, usa español (es) como predeterminado.
     */
    @Bean
    public AcceptHeaderLocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(new Locale("es"));
        return resolver;
    }
}
