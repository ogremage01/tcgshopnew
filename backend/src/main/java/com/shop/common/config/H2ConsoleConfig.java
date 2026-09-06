package com.shop.common.config;

import org.h2.tools.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("dev")
public class H2ConsoleConfig {

    @Bean
    public Server h2WebConsoleServer() throws java.sql.SQLException {
        return Server.createWebServer(
                "-web",
                "-webAllowOthers",
                "-webPort",
                "8082").start();
    }
}
