# Title
MismatchedInputException when deserializing Error with DGS client due to mistype on field in GraphQLErrorExtensions classification

https://github.com/Netflix/dgs-framework/issues/1146

## Expected behavior

When using the DGS client to query a DGS service, the client should be able to deserialize the Error coming from the
server.

## Actual behavior

The DGS client fail to deserialize the Error from the server.

```bash
caused by: com.fasterxml.jackson.databind.exc.MismatchedInputException: Cannot deserialize value of type `java.lang.String` from Object value (token `JsonToken.START_OBJECT`)
 at [Source: UNKNOWN; byte offset: #UNKNOWN] (through reference chain: java.util.ArrayList[0]->com.netflix.graphql.dgs.client.GraphQLError["extensions"]->com.netflix.graphql.dgs.client.GraphQLErrorExtensions["classification"])
		at com.fasterxml.jackson.databind.exc.MismatchedInputException.from(MismatchedInputException.java:59)
		at com.fasterxml.jackson.databind.DeserializationContext.reportInputMismatch(DeserializationContext.java:1741)
		...
		at com.fasterxml.jackson.databind.deser.std.CollectionDeserializer._deserializeFromArray(CollectionDeserializer.java:355)
		at com.fasterxml.jackson.databind.deser.std.CollectionDeserializer.deserialize(CollectionDeserializer.java:244)
		at com.fasterxml.jackson.databind.deser.std.CollectionDeserializer.deserialize(CollectionDeserializer.java:28)
		at com.fasterxml.jackson.databind.ObjectMapper._convert(ObjectMapper.java:4388)
```

## Steps to reproduce
I have created a project with all the code & tests, reproducing the issue  -> https://github.com/W4lspirit/demo-dgs-extended-validation

Create a graphql project with DGS framework (webflux) and the extended-validation module.

Add this graphql schema and the size directive to the project:

```graphql
directive @Size(
    min: Int = 1
    max: Int = 1000
    message: String = "graphql.validation.Size.message"
) on ARGUMENT_DEFINITION | INPUT_FIELD_DEFINITION

type Query {
    showsMustSucceed(titleFilter: String!): [Show]
    showsMustGoOn(titleFilter: String @Size): [Show]
}

type Show {
    title: String
    releaseYear: Int
}
```

Create these 2 tests :

```java
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
```
Test `dgs_client_should_deserialize_error_when_mandatory_field_is_missing` will work since the json output match the extension structure defined in the dgs client Error class.

```json
{
    "errors": [
        {
            "message": "Variable 'titleFilter' has an invalid value: Variable 'titleFilter' has coerced Null value for NonNull type 'String!'",
            "locations": [
                {
                    "line": 1,
                    "column": 25
                }
            ],
            "extensions": {
                "classification": "ValidationError"
            }
        }
    ]
}
```

Here -> https://github.com/Netflix/dgs-framework/blob/v5.0.5/graphql-dgs-client/src/main/kotlin/com/netflix/graphql/dgs/client/GraphQLError.kt#L42
```kotlin
@JsonIgnoreProperties(ignoreUnknown = true)
data class GraphQLErrorExtensions(
    @JsonProperty val errorType: ErrorType? = null,
    @JsonProperty val errorDetail: String? = null,
    @JsonProperty val origin: String = "",
    @JsonProperty val debugInfo: GraphQLErrorDebugInfo = GraphQLErrorDebugInfo(),
    @JsonProperty val classification: String = ""
)
```

Test `dgs_client_should_deserialize_error_when_directive_size_on_input_argument_is_not_respected` will fail because the json output does not match the structure defined in the dgs client Error class. 

The field extension is a map not a string.

```json
{
    "errors": [
        {
            "message": "/showsMustGoOn/titleFilter size must be between 1 and 1000",
            "locations": [
                {
                    "line": 2,
                    "column": 5
                }
            ],
            "path": [
                "showsMustGoOn"
            ],
            "extensions": {
                "classification": {
                    "type": "ExtendedValidationError",
                    "validatedPath": [
                        "showsMustGoOn",
                        "titleFilter"
                    ],
                    "constraint": "@Size"
                }
            }
        }
    ],
    "data": {
        "showsMustGoOn": null
    }
}
```

These extensions error are build by the extended-validation module -> https://github.com/graphql-java/graphql-java-extended-validation/blob/master/src/main/java/graphql/validation/interpolation/ResourceBundleMessageInterpolator.java#L194
```java
        @Override
        public Object toSpecification(GraphQLError error) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("type", "ExtendedValidationError");
            map.put("validatedPath", fieldOrArgumentPath.toList());
            if (directive != null) {
                map.put("constraint", "@" + directive.getName());
            }
            return map;
        }
```
And implement the interface  ErrorClassification from the graphql java library : https://github.com/graphql-java/graphql-java/blob/master/src/main/java/graphql/ErrorClassification.java
```java 
@PublicApi
public interface ErrorClassification {

    /**
     * This is called to create a representation of the error classification
     * that can be put into the `extensions` map of the graphql error under the key 'classification'
     * when {@link GraphQLError#toSpecification()} is called
     *
     * @param error the error associated with this classification
     *
     * @return an object representation of this error classification
     */
    @SuppressWarnings("unused")
    default Object toSpecification(GraphQLError error) {
        return String.valueOf(this);
    }
}
```

I'm not sure if the field GraphQLErrorExtensions.classification should be a string, if we consider that the toSpecification method return an object. 

Here -> https://github.com/Netflix/dgs-framework/blob/v5.0.5/graphql-dgs-client/src/main/kotlin/com/netflix/graphql/dgs/client/GraphQLError.kt#L42
```kotlin
@JsonIgnoreProperties(ignoreUnknown = true)
data class GraphQLErrorExtensions(
    @JsonProperty val errorType: ErrorType? = null,
    @JsonProperty val errorDetail: String? = null,
    @JsonProperty val origin: String = "",
    @JsonProperty val debugInfo: GraphQLErrorDebugInfo = GraphQLErrorDebugInfo(),
    @JsonProperty val classification: String = ""
)
```
