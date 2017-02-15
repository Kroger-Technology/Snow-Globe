/*
 * Nginx Snow Globe
 *
 * Copyright 2017 The Kroger Co.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kroger.rp.util.call;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

public class CallUtilityTest {

    @Before
    public void setup() {
        initMocks(this);
    }

    @Test
    public void shouldGetHttpEntity() throws UnsupportedEncodingException {
        TestRequest request = mock(TestRequest.class);
        String body = "{ thing: \"yes\"}";
        when(request.getBody()).thenReturn(body);
        HttpEntity expectedEntity = new ByteArrayEntity(body.getBytes("UTF-8"));
        HttpEntity actualResult = CallUtility.getHttpEntity(request);
        assertThat(expectedEntity.toString(), is(actualResult.toString()));
    }

    @Test
    public void shouldExecuteAction() throws IOException {
        CloseableHttpClient client = mock(CloseableHttpClient.class);
        HttpUriRequest action = mock(HttpUriRequest.class);
        CloseableHttpResponse expectedResponse = mock(CloseableHttpResponse.class);
        when(client.execute(action)).thenReturn(expectedResponse);

        CloseableHttpResponse actualResponse = CallUtility.execute(client, action);

        assertThat(expectedResponse, is(actualResponse));
        verify(client, times(1)).execute(action);
    }

    @Test(expected = RuntimeException.class)
    public void shouldExecuteActionWithError() throws IOException {
        CloseableHttpClient client = mock(CloseableHttpClient.class);
        HttpUriRequest action = mock(HttpUriRequest.class);
        when(client.execute(action)).thenThrow(new Exception("Expected Error"));

        CallUtility.execute(client, action);
    }

    @Test
    public void shouldGetResponseBody() throws IOException {
        HttpEntity entity = mock(HttpEntity.class);
        String expectedResponse = "expected response";
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(expectedResponse.getBytes(StandardCharsets.UTF_8)));

        String actualResponse = CallUtility.getResponseBody(entity);

        assertThat(expectedResponse, is(actualResponse));
    }

    @Test(expected = RuntimeException.class)
    public void shouldGetResponseBodyWithError() {
        HttpEntity entity = null;
        CallUtility.getResponseBody(entity);
    }



}