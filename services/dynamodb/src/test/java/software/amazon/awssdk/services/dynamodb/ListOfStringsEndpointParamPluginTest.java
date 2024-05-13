/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.services.dynamodb;

import static java.util.Arrays.asList;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.endpoints.Endpoint;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.endpoints.DynamoDbEndpointParams;
import software.amazon.awssdk.services.dynamodb.endpoints.DynamoDbEndpointProvider;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.KeysAndAttributes;

@WireMockTest
class ListOfStringsEndpointParamPluginTest {

    private DynamoDbAsyncClient client;
    private ListOfStringsParamEndpointProvider endpointProvider;

    @BeforeEach
    public void init(WireMockRuntimeInfo wm) {
        AwsBasicCredentials credentials = AwsBasicCredentials.create("key", "secret");
        
        endpointProvider = new ListOfStringsParamEndpointProvider(DynamoDbEndpointProvider.defaultProvider());

        client = DynamoDbAsyncClient.builder()
                                    .region(Region.US_EAST_1)
                                    .endpointOverride(URI.create(wm.getHttpBaseUrl()))
                                    .endpointProvider(endpointProvider)
                                    .credentialsProvider(StaticCredentialsProvider.create(credentials))
                                    .build();
    }

    @Test
    void callingBatchGetItem_requestWithCompleteListOfKey_returnsRightValues() {
        Map<String, KeysAndAttributes> tableMetadataMap =
            IntStream.range(1, 4)
                     .boxed()
                     .collect(Collectors.toMap(table -> "table" + table, this::entries));

        client.batchGetItem(r -> r.requestItems(tableMetadataMap)).join();
        assertThat(endpointProvider.storedKeys()).isEqualTo(asList("table1", "table2", "table3"));
    }

    private KeysAndAttributes entries(Integer tableNumber) {
        List<Map<String, AttributeValue>> collect =
            IntStream.range(10, 12).boxed()
                     .map(attributes -> keysAndAttributesMap(tableNumber)).collect(Collectors.toList());
        return KeysAndAttributes.builder().keys(collect).build();
    }

    private Map<String, AttributeValue> keysAndAttributesMap(int tableNumber) {
        return IntStream.range(20, 22).boxed()
                        .collect(Collectors.toMap(a -> "table" + tableNumber + "attributeKey" + a,
                                                  a -> AttributeValue.builder().s("attributeValue" + a).build()));
    }

    private static class ListOfStringsParamEndpointProvider implements DynamoDbEndpointProvider {

        private List<String> storedKeys;
        DynamoDbEndpointProvider delegate;

        ListOfStringsParamEndpointProvider(DynamoDbEndpointProvider endpointProvider) {
            this.delegate = endpointProvider;
        }

        List<String> storedKeys() {
            return storedKeys;
        }

        @Override
        public CompletableFuture<Endpoint> resolveEndpoint(DynamoDbEndpointParams endpointParams) {
            List<String> keys = endpointParams.tables();
            if (keys != null) {
                storedKeys = keys;
            }
            return delegate.resolveEndpoint(endpointParams);
        }
    }
    

}
