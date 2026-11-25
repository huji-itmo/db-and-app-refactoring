package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.flyway.target=3")
class CustomerNameMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureDatabase(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private DataSource dataSource;

    @LocalServerPort
    private int port;

    @Test
    void orderApiRemainsAvailableDuringContractMigration() throws Exception {
        TestRestTemplate api = new TestRestTemplate(new RestTemplateBuilder()
                .rootUri("http://localhost:" + port)
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(3)));
        var order = orderForNewProduct(api);
        createOrder(api, order);

        var ready = new CountDownLatch(1);
        var keepReading = new AtomicBoolean(true);
        var executor = Executors.newSingleThreadExecutor();
        try {
            var traffic = executor.submit(() -> {
                assertOrderIsReadable(api);
                ready.countDown();
                do {
                    assertOrderIsReadable(api);
                } while (keepReading.get());
            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(Flyway.configure().dataSource(dataSource).load().migrate().migrationsExecuted).isEqualTo(1);
            keepReading.set(false);
            traffic.get(10, TimeUnit.SECONDS);
            createOrder(api, order);
            assertOrderIsReadable(api);
        } finally {
            keepReading.set(false);
            executor.shutdownNow();
        }
    }

    private static CreateOrderRequest orderForNewProduct(TestRestTemplate api) {
        var product = api.postForEntity("/products", Map.of("name", "Book", "price", 10), JsonNode.class);
        assertThat(product.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        long productId = product.getBody().required("id").longValue();
        return new CreateOrderRequest("Ivan Petrov", "Main Street", null, "regular", null,
                List.of(new CreateOrderRequest.Item(productId, 1)));
    }

    private static void createOrder(TestRestTemplate api, CreateOrderRequest order) {
        assertThat(api.postForEntity("/orders", order, String.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private static void assertOrderIsReadable(TestRestTemplate api) {
        var response = api.getForEntity("/orders", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Ivan Petrov");
    }
}
