package com.medisalud.citas.domain.util;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    /**
     * Elimina espacios al inicio y al final, y remueve las tildes/acentos de la
     * cadena.
     */
    public static String sanitizar(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }

        String trimmed = input.trim();
        String normalized = Normalizer.normalize(trimmed, Normalizer.Form.NFD);
        return normalized.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }

    /**
     * Verifica si una fecha dada es un festivo en Colombia
     */
    public static boolean esFestivoColombia(LocalDate fecha) {
        int year = fecha.getYear();
        List<LocalDate> festivos = calcularFestivosColombia(year);
        return festivos.contains(fecha);
    }

    private static List<LocalDate> calcularFestivosColombia(int year) {
        List<LocalDate> festivos = new ArrayList<>();

        // 1. Festivos Fijos (no se mueven)
        festivos.add(LocalDate.of(year, 1, 1)); // Año Nuevo
        festivos.add(LocalDate.of(year, 5, 1)); // Día del Trabajo
        festivos.add(LocalDate.of(year, 7, 20)); // Día de la Independencia
        festivos.add(LocalDate.of(year, 8, 7)); // Batalla de Boyacá
        festivos.add(LocalDate.of(year, 12, 8)); // Inmaculada Concepción
        festivos.add(LocalDate.of(year, 12, 25)); // Navidad

        // 2. Festivos Trasladables
        festivos.add(siguienteLunes(LocalDate.of(year, 1, 6))); // Reyes Magos
        festivos.add(siguienteLunes(LocalDate.of(year, 3, 19))); // San José
        festivos.add(siguienteLunes(LocalDate.of(year, 6, 29))); // San Pedro y San Pablo
        festivos.add(siguienteLunes(LocalDate.of(year, 8, 15))); // Asunción
        festivos.add(siguienteLunes(LocalDate.of(year, 10, 12))); // Día de la Raza
        festivos.add(siguienteLunes(LocalDate.of(year, 11, 1))); // Todos los Santos
        festivos.add(siguienteLunes(LocalDate.of(year, 11, 11))); // Independencia de Cartagena

        // 3. Festivos relativos al Domingo de Pascua
        LocalDate pascua = calcularPascua(year);

        festivos.add(pascua.minusDays(3)); // Jueves Santo
        festivos.add(pascua.minusDays(2)); // Viernes Santo

        // Ascensión (43 días después de Pascua, trasladable al lunes)
        festivos.add(siguienteLunes(pascua.plusDays(43)));
        // Corpus Christi (64 días después de Pascua, trasladable al lunes)
        festivos.add(siguienteLunes(pascua.plusDays(64)));
        // Sagrado Corazón (71 días después de Pascua, trasladable al lunes)
        festivos.add(siguienteLunes(pascua.plusDays(71)));

        return festivos;
    }

    private static LocalDate siguienteLunes(LocalDate fecha) {
        if (fecha.getDayOfWeek() == DayOfWeek.MONDAY) {
            return fecha;
        }
        return fecha.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    /**
     * Algoritmo de Butcher (Computus) para calcular la fecha del Domingo de Pascua
     * de cualquier año.
     */
    private static LocalDate calcularPascua(int year) {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int mes = (h + l - 7 * m + 114) / 31;
        int dia = ((h + l - 7 * m + 114) % 31) + 1;

        return LocalDate.of(year, mes, dia);
    }
}
