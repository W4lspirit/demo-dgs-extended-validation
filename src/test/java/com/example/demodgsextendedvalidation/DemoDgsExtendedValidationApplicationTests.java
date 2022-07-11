package com.example.demodgsextendedvalidation;

import com.netflix.graphql.dgs.client.GraphQLResponse;
import com.netflix.graphql.dgs.client.MonoGraphQLClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoDgsExtendedValidationApplicationTests {
    @LocalServerPort
    private int port;

    @Test
    void dgs_client_should_deserialize_error_when_mandatory_field_is_missing() {
        // Given
        final String query = """
                query Query {
                  showsMustSucceed(titleFilter: null){
                    title
                  }
                }
                """;

        // When
        final WebClient webClient = WebClient.create("http://localhost:" + port + "/graphql");
        final Mono<GraphQLResponse> response = MonoGraphQLClient.createWithWebClient(webClient).reactiveExecuteQuery(query);


        // Then
        StepVerifier.create(response)
                .expectNextCount(1)
                .verifyComplete();

    }

    @Test
    void dgs_client_should_deserialize_error_when_directive_size_on_input_argument_is_not_respected() {
        // Given
        final String query = """
                query Query {
                  showsMustGoOn(titleFilter: ""){
                    title
                  }
                }
                """;

        // When
        final WebClient webClient = WebClient.create("http://localhost:" + port + "/graphql");
        final Mono<GraphQLResponse> response = MonoGraphQLClient.createWithWebClient(webClient).reactiveExecuteQuery(query);


        // Then
        StepVerifier.create(response)
                .expectNextCount(1)
                .verifyComplete();

    }

}
