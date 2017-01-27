package com.kroger.rp.util.call;

import org.apache.http.Header;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.message.BasicHeader;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ResponseVerificationTest {

    @Mock ResponseBody serviceResponseBody;
    @Mock CloseableHttpResponse response;
    @Mock TestRequest testRequest;
    @Mock RequestToService requestToService;
    @Mock StatusLine statusLine;

    String body = new String();

    @Before
    public void setup() {
        initMocks(this);
        when(serviceResponseBody.getRequest()).thenReturn(requestToService);
        when(response.getStatusLine()).thenReturn(statusLine);
    }

    private ResponseVerification buildVerification() {
        return new ResponseVerification(serviceResponseBody, response, body, testRequest);
    }

    @Test
    public void shouldVerifyResponseCode() {
        int expectedCode = 200;
        when(statusLine.getStatusCode()).thenReturn(expectedCode);

        buildVerification().andExpectResponseCode(expectedCode);
        buildVerification().expectResponseCode(expectedCode);
    }

    @Test
    public void shouldVerifyClusterName() {
        String expectedClusterName = "clusterName";
        when(serviceResponseBody.getCluster()).thenReturn(expectedClusterName);

        buildVerification().expectClusterName(expectedClusterName);
        buildVerification().andExpectClusterName(expectedClusterName);
    }

    @Test
    public void shouldVerifyClusterNumber() {
        int clusterNumber = 42;
        when(serviceResponseBody.getInstance()).thenReturn(clusterNumber);

        buildVerification().expectClusterNumber(clusterNumber);
        buildVerification().andExpectClusterNumber(clusterNumber);
    }

    @Test
    public void shouldVerifyAppPath() {
        String appPath = "/app/path";
        when(requestToService.getPath()).thenReturn(appPath);

        buildVerification().expectAppPath(appPath);
        buildVerification().andExpectAppPath(appPath);
    }

    @Test
    public void shouldVerifyAppUrl() {
        String appUrl = "https://app.com/url";
        when(requestToService.getUrlToApplication()).thenReturn(appUrl);

        buildVerification().expectAppUrl(appUrl);
        buildVerification().andExpectAppUrl(appUrl);
    }

    @Test
    public void shouldVerifyResponseHeader() {
        String responseHeader = "headerKey";
        Header header = new BasicHeader(responseHeader, "someValue");
        when(response.getAllHeaders()).thenReturn(new Header[]{header});

        buildVerification().expectResponseHeader(responseHeader);
        buildVerification().andExpectResponseHeader(responseHeader);
    }

    @Test
    public void shouldVerifyRequestToApplicationHeader() {
        String responseHeader = "headerKey";
        Map<String, String> headers = new HashMap<>();
        headers.put(responseHeader, "someValue");
        when(requestToService.getHeaders()).thenReturn(headers);

        buildVerification().expectRequestHeaderToApplication(responseHeader);
        buildVerification().andExpectRequestHeaderToApplication(responseHeader);
    }

    @Test
    public void shouldVerifyRequestToApplicationHeaderMatching() {
        String headerKey = "headerKey";
        String headerValue = "headerValue";
        Map<String, String> headers = new HashMap<>();
        headers.put(headerKey, headerValue);
        when(requestToService.getHeaders()).thenReturn(headers);

        buildVerification().expectRequestHeaderToApplicationMatching(headerKey, ".*Value");
        buildVerification().andExpectRequestHeaderToApplicationMatching(headerKey, ".*Value");
    }

    @Test
    public void shouldVerifyRequestToApplicationNotHaveHeader() {
        String headerKey = "headerKey";
        String headerValue = "headerValue";
        Map<String, String> headers = new HashMap<>();
        headers.put(headerKey, headerValue);
        when(requestToService.getHeaders()).thenReturn(headers);

        buildVerification().andExpectMissingResponseHeader("otherKey");
    }
}