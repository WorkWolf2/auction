package fr.hyping.hypingauctions.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Format {

    private static final SimpleDateFormat FRENCH_DATE_FORMAT = new SimpleDateFormat("EEE dd MMM yyyy - HH'h'mm",
            Locale.FRENCH);

    // US number format: comma as thousands separator, dot as decimal separator
    private static final DecimalFormat US_NUMBER_FORMAT = new DecimalFormat("#,###.##",
            DecimalFormatSymbols.getInstance(Locale.US));

    public static String formatTime(long time) {
        int days = (int) (time / 86400000);
        int hours = (int) ((time % 86400000) / 3600000);
        int minutes = (int) ((time % 3600000) / 60000);
        int seconds = (int) ((time % 60000) / 1000);

        if (days > 0)
            return days + "j " + hours + "h";
        if (hours > 0)
            return hours + "h " + minutes + "m";
        if (minutes > 0)
            return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    public static String formatTimeDetailed(long time) {
        int days = (int) (time / 86400000);
        int hours = (int) ((time % 86400000) / 3600000);
        int minutes = (int) ((time % 3600000) / 60000);
        int seconds = (int) ((time % 60000) / 1000);

        StringBuilder sb = new StringBuilder();
        if (days > 0) {
            sb.append(days).append("j ");
            sb.append(hours).append("h ");
            sb.append(minutes).append("m"); // No seconds if days > 0
        } else {
            if (hours > 0)
                sb.append(hours).append("h ");
            if (minutes > 0)
                sb.append(minutes).append("m ");
            sb.append(seconds).append("s");
        }
        return sb.toString().trim();
    }

    public static String formatNumber(long value) {
        return US_NUMBER_FORMAT.format(value);
    }

    public static String formatNumber(double value) {
        return US_NUMBER_FORMAT.format(value);
    }

    /**
     * Formats a timestamp to French date format (e.g., "Ven 30 mai 2025 - 14h15")
     *
     * @param timestamp The timestamp in milliseconds
     * @return Formatted date string in French format
     */
    public static String formatDateFrench(long timestamp) {
        return FRENCH_DATE_FORMAT.format(new Date(timestamp));
    }

    /**
     * Format time in milliseconds to a human-readable string (English).
     * Returns formats like "7 days", "14 days", "2 hours", "30 minutes".
     */
    public static String formatTimeReadable(long expirationMillis) {
        if (expirationMillis <= 0) {
            return "Inconnu";
        }

        long seconds = expirationMillis / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (days > 0) {
            if (hours > 0) {
                return days + " jour" + (days > 1 ? "s" : "") + " " +
                        hours + " heure" + (hours > 1 ? "s" : "");
            }
            return days + " jour" + (days > 1 ? "s" : "");
        }
        else if (hours > 0) {
            if (minutes > 0) {
                return hours + " heure" + (hours > 1 ? "s" : "") + " " +
                        minutes + " minute" + (minutes > 1 ? "s" : "");
            }
            return hours + " heure" + (hours > 1 ? "s" : "");
        }
        else if (minutes > 0) {
            return minutes + " minute" + (minutes > 1 ? "s" : "");
        }
        else {
            return secs + " seconde" + (secs > 1 ? "s" : "");
        }
    }

}
