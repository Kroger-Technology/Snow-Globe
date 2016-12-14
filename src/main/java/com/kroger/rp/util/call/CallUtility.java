package com.kroger.rp.util.call;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.*;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.BasicHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;

public class CallUtility {

    /**
     * This is the main method used to execute a call to the nginx reverse proxy in the testing framework.  This method
     * will build the custom http client that is set to route all DNS traffic to 127.0.0.1.  This will also handle
     * the response and response body, returning it in a <code>ResponseVerification</code> class, allowing assertions
     * to be made to it.
     *
     * @param testRequest
     *      This is the test request that has all of the information about the call, headers, and body of the request.
     *      This object should change the protocol to http and properly add in the port number to match to the correct
     *      port that was setup by the <code>NginxRpBuilder</code> object.
     *
     * @return
     *      The response object wrapped in the <code>ResponseVerification</code> object.  This will be used by the
     *      tests to assert specific things about the request translation, and the response translation.
     *
     */
    public static ResponseVerification make(TestRequest testRequest) {
        CloseableHttpResponse rawResponse = makeRequest(testRequest, buildHttpClient());
        String jsonResponse = getResponseBody(rawResponse.getEntity());
        ResponseBody responseBody = buildResponseBody(jsonResponse);
        return new ResponseVerification(responseBody, rawResponse, jsonResponse, testRequest);
    }

    /**
     * Builds a custom Http client with custom DNS resolution, disabling persistent cookie stores and with custom
     * timeout values.
     *
     * @return
     *      An http client to be used to execute test requests to nginx.
     */
    private static CloseableHttpClient buildHttpClient() {
        return HttpClients.custom()
                    .setConnectionManager(buildConnectionManager())
                    .setDefaultRequestConfig(RequestConfig.custom()
                            // Waiting for a connection from connection manager
                            .setConnectionRequestTimeout(10000)
                            // Waiting for connection to establish
                            .setConnectTimeout(5000)
                            .setExpectContinueEnabled(false)
                            // Waiting for data
                            .setSocketTimeout(5000)
                            // Do not allow cookies to be stored between calls.
                            .setCookieSpec(CookieSpecs.IGNORE_COOKIES)
                            .build())
                    .setRetryHandler(buildRetryHandler())
                    .disableRedirectHandling().build();
    }

    /**
     * Builds a custom connection manager that will map all DNS entries to the 127.0.0.1 domain.  This used in
     * conjunction with the random ports for the docker compose RP entry allow for all traffic to be properly
     * routed.
     *
     * @return
     *      A connection manager that resolves all DNS names to 127.0.0.1
     */
    private static BasicHttpClientConnectionManager buildConnectionManager() {
        return new BasicHttpClientConnectionManager(getDefaultRegistry(), null,
                    null, host -> new InetAddress[] { InetAddress.getByAddress(new byte[] {127, 0, 0, 1}) });
    }

    /**
     * A copy of the BasicHttpClientConnectionManager default registry to be able to create a custom connection
     * manager.  The implementation in the BasicHttpClientConnectionManager is private and can't be used so we
     * made a copy of it.
     *
     * @return
     *      the default registry for creating sockets based on the protocol.
     */
    private static Registry<ConnectionSocketFactory> getDefaultRegistry() {
        return RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
                .build();
    }

    /**
     * Maps expected JSON formatted response from fake upstream server to the  ResponseBody class.
     *
     * @param body
     *      String representation of the JSON response.
     *
     * @return
     *      The ResponseBody object representing the JSON response.
     */
    private static ResponseBody buildResponseBody(String body) {
        ObjectMapper mapper = new ObjectMapper();
        ResponseBody res;
        try {
            res = mapper.readValue(body, ResponseBody.class);
        }catch(JsonParseException jpe) {
            res = ResponseBody.buildDirectResponseFromRp(body);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return res;
    }

    /**
     * Maps out to make the call based on the request HTTP method.
     *
     * @param testRequest
     *      The request to make.
     * @param httpclient
     *      The http client to use to execute that action.
     * @return
     *      The response that was given from the http client.
     */
    private static CloseableHttpResponse makeRequest(TestRequest testRequest, CloseableHttpClient httpclient) {
        switch(testRequest.getMethod()) {
            case "POST":
                return makePostRequest(testRequest, httpclient);
            case "GET":
                return makeGetRequest(testRequest, httpclient);
            case "PUT":
                return makePutRequest(testRequest, httpclient);
            case "DELETE":
                return makeDeleteRequest(testRequest, httpclient);
            default:
                throw new RuntimeException("Unable to make request with action: " + testRequest.getMethod());
        }

    }

    private static CloseableHttpResponse makeDeleteRequest(TestRequest testRequest, CloseableHttpClient httpclient) {
        HttpDelete delete = new HttpDelete(testRequest.getUrl());
        setHeaders(delete, testRequest);
        return execute(httpclient, delete);
    }

    private static CloseableHttpResponse makePutRequest(TestRequest testRequest, CloseableHttpClient httpclient) {
        HttpPut put = new HttpPut(testRequest.getUrl());
        setHeaders(put, testRequest);
        return execute(httpclient, put);
    }

    private static CloseableHttpResponse makeGetRequest(TestRequest testRequest, CloseableHttpClient httpclient) {
        HttpGet httpGet = new HttpGet(testRequest.getUrl());
        setHeaders(httpGet, testRequest);
        return execute(httpclient, httpGet);
    }

    private static void setHeaders(HttpRequestBase httpMethod, TestRequest testRequest) {
        if(testRequest.hasUserAgent()) {
            httpMethod.setHeader("User-Agent", testRequest.getUserAgent());
        }
    }

    private static CloseableHttpResponse makePostRequest(TestRequest testRequest, CloseableHttpClient httpclient) {
        HttpPost httpPost = new HttpPost(testRequest.getUrl());
        setHeaders(httpPost, testRequest);
        if(testRequest.getBody() != null) {
            httpPost.setEntity(getHttpEntity(testRequest));
        }
        return execute(httpclient, httpPost);
    }

    private static HttpRequestRetryHandler buildRetryHandler() {
        return (exception, executionCount, context) -> {
            if (executionCount > 20) {
                // Do not retry if over max retry count
                return false;
            }
            if (exception instanceof HttpHostConnectException) {
                try {
                    Thread.sleep(250);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                // Retry if the server dropped connection on us
                return true;
            }
            // otherwise do not retry
            return false;
        };
    }

    private static HttpEntity getHttpEntity(TestRequest testRequest) {
        HttpEntity entity = null;
        try {
            entity = new ByteArrayEntity(testRequest.getBody().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return entity;
    }

    private static CloseableHttpResponse execute(CloseableHttpClient client, HttpUriRequest action) {
        try {
            return client.execute(action);
        } catch (IOException e) {
            System.err.println("Error while making call to nginx");
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private static String getResponseBody(HttpEntity entity) {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(entity.getContent()), 65728);
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }
}
