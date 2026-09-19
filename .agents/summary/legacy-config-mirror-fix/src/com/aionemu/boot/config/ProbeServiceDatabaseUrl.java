package com.aionemu.boot.config;

import com.aionemu.commons.configs.DatabaseConfig;
import com.aionemu.commons.configuration.ConfigSourceResolverHolder;
import com.aionemu.commons.configuration.ConfigurableProcessor;
import com.aionemu.commons.utils.PropertiesUtils;
import java.util.Map;
import java.util.Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

/** 用真实配置目录复现/验证启动期的 database.url 解析结果。 */
public class ProbeServiceDatabaseUrl {

    public static void main(String[] args) throws Exception {
        for (String configDir : args) {
            StandardEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().addFirst(new MapPropertySource(
                "commandLine", Map.of("aion.config.dir", configDir)));
            new AionLegacyPropertySourceEnvironmentPostProcessor()
                .postProcessEnvironment(environment, new SpringApplication());
            ConfigSourceResolverHolder.publish(environment::getProperty);

            System.out.println("=== " + configDir + " ===");
            System.out.println("mirror raw database.url        = " + environment.getProperty("database.url"));
            System.out.println("game prefix database.url       = "
                + environment.getProperty("aion.legacy.game.property.database.url"));
            System.out.println("login prefix database.url      = "
                + environment.getProperty("aion.legacy.login.property.database.url"));
            resolve(configDir + "/network", "game");
            resolve(configDir + "/login", "login");
        }
    }

    private static void resolve(String directory, String service) throws Exception {
        Properties[] props = PropertiesUtils.loadAllFromDirectory(directory);
        ConfigurableProcessor.process(DatabaseConfig.class, props);
        System.out.println(service + " DatabaseConfig.DATABASE_URL = " + DatabaseConfig.DATABASE_URL);
    }
}
