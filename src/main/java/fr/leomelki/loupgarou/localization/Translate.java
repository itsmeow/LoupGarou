package fr.leomelki.loupgarou.localization;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public class Translate {

    private static String localeString;
    private static ResourceBundle bundle;
    private static Locale locale;

    public static void setLocale(String locale) {
        Translate.localeString = locale;
        Translate.locale = Locale.forLanguageTag(localeString);
        Translate.loadLocale();
    }

    public static String getLocale() {
        return localeString;
    }

    public static String getRaw(String key) {
        return bundle.containsKey(key) ? bundle.getString(key) : key;
    }

    private static void loadLocale() {
        bundle = ResourceBundle.getBundle("messages", locale);
    }

    public static MessageFormat getFormatter(String format) {
        return new MessageFormat(format, locale);
    }

    public static String format(String text, Object... args) {
        return getFormatter(text).format(args);
    }

    public static String getFormatted(String key, Object... args) {
        return bundle.containsKey(key) ? format(getRaw(key), args) : key;
    }

}
