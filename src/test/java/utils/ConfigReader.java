package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * config.properties faylından dəyərləri oxumaq üçün util class.
 */
public class ConfigReader {

    private static final Properties properties = new Properties();

    static {
        try (InputStream input = ConfigReader.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("config.properties tapılmadı (src/test/resources altında olmalıdır)");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("config.properties oxunarkən xəta baş verdi", e);
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }
}
