package moments4j.settings;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Enables MomentsSettings (settings.py). */
@Configuration
@EnableConfigurationProperties(MomentsSettings.class)
public class SettingsConfig {
}
