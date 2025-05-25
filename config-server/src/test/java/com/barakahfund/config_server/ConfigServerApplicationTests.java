package com.barakahfund.config_server;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Integration tests for Config Server
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "spring.cloud.config.server.git.uri=https://github.com/spring-cloud-samples/config-repo",
    "spring.security.user.name=test-user",
    "spring.security.user.password=test-pass"
})
class ConfigServerApplicationTests {

    @LocalServerPort
    private int port;

    @Test
    void contextLoads() {
        assertThat(port).isGreaterThan(0);
    }

    @Test
    void configServerIsRunning() {
        assertThat(port).isNotZero();
    }
}
