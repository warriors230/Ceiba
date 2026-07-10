package com.medisalud.citas.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper para recuperar mensajes del archivo messages.properties.
 * Usa el Locale del contexto actual, lo que permite i18n en el futuro
 * simplemente añadiendo archivos messages_en.properties, messages_fr.properties, etc.
 */
@Component
@RequiredArgsConstructor
public class MessageService {

    private final MessageSource messageSource;

    /**
     * Recupera un mensaje sin parámetros.
     *
     * @param key clave definida en messages.properties
     * @return mensaje localizado
     */
    public String get(String key) {
        return messageSource.getMessage(key, null, LocaleContextHolder.getLocale());
    }

    /**
     * Recupera un mensaje con parámetros posicionales ({0}, {1}, ...).
     *
     * @param key    clave definida en messages.properties
     * @param params valores a sustituir en el mensaje
     * @return mensaje localizado con parámetros interpolados
     */
    public String get(String key, Object... params) {
        return messageSource.getMessage(key, params, LocaleContextHolder.getLocale());
    }
}
