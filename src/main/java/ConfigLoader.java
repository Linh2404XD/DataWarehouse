import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {
    private Properties properties;

    public ConfigLoader(String configFile) throws IOException {
        // 1. Load file config.properties
        properties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            properties.load(fis);
        }
    }

    public String getProperty(String key) {
        // Lấy giá trị tương ứng với key trong file config
        return properties.getProperty(key);
    }
}
