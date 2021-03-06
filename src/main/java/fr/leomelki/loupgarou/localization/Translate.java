package fr.leomelki.loupgarou.localization;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.bukkit.ChatColor;

import fr.leomelki.loupgarou.classes.LGPlayer;

public class Translate {

    private static Map<String, ResourceBundle> localeBundles = new HashMap<String, ResourceBundle>();
    private static Map<String, Locale> locales = new HashMap<String, Locale>();

    static {
        addLocale("en-us");
        addLocale("fr-fr");
    }

    public static boolean containsLocale(String localeIn) {
        return locales.containsKey(localeIn);
    }

    private static void addLocale(String localeIn) {
        Locale locale = Locale.forLanguageTag(localeIn);
        locales.put(localeIn, locale);
        localeBundles.put(localeIn, ResourceBundle.getBundle("messages", locale));
    }

    public static Locale getLocale(LGPlayer player) {
        return locales.get(player.getLocale());
    }

    public static ResourceBundle getBundle(String locale) {
        return localeBundles.get(locale);
    }

    public static ResourceBundle getBundle(LGPlayer player) {
        return localeBundles.get(player.getLocale());
    }

    public static String get(LGPlayer player, String key) {
        ResourceBundle bundle = getBundle(player);
        return bundle.containsKey(key) ? bundle.getString(key).replaceAll("''", "'") : key;
    }
    
    private static String get2(LGPlayer player, String key) {
        ResourceBundle bundle = getBundle(player);
        return bundle.containsKey(key) ? bundle.getString(key) : key;
    }

    public static String get(LGPlayer player, String key, Object... args) {
        ResourceBundle bundle = getBundle(player);
        return bundle.containsKey(key) ? format(player, get2(player, key), args) : key;
    }
    
    public static String getColor(LGPlayer player, String key) {
        return ChatColor.translateAlternateColorCodes('&', get(player, key));
    }

    public static String getColor(LGPlayer player, String key, Object... args) {
        return ChatColor.translateAlternateColorCodes('&', get(player, key, args));
    }

    public static MessageFormat getFormatter(LGPlayer player, String format) {
        return new MessageFormat(format, getLocale(player));
    }

    public static String format(LGPlayer player, String text, Object... args) {
        return getFormatter(player, text).format(args);
    }

}
