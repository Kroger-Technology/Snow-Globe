package com.kroger.rp.util.call;

import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ResponseVerification {

    private final int responseCode;
    private final String clusterName;
    private final int clusterNumber;
    private final String appPath;
    private final int latencyMillis;
    private final int roundTripTimeMillis;
    private final String body;
    private final Map<String, String> responseHeaders;
    private final ResponseBody serviceResponseBody;
    private final TestRequest testRequest;
    private final String urlToApplication;


    public ResponseVerification(ResponseBody serviceResponseBody, CloseableHttpResponse response, String body, TestRequest testRequest) {
        this.testRequest = testRequest;
        this.responseCode = response.getStatusLine().getStatusCode();
        this.clusterName = serviceResponseBody.getCluster();
        this.clusterNumber = serviceResponseBody.getInstance();
        this.appPath = serviceResponseBody.getRequest().getPath();
        this.urlToApplication = serviceResponseBody.getRequest().getUrlToApplication();
        this.latencyMillis = 0;
        this.roundTripTimeMillis = 0;
        this.body = body;
        this.responseHeaders = buildHeaders(response.getAllHeaders());
        this.serviceResponseBody = serviceResponseBody;
    }

    private Map<String, String> buildHeaders(Header[] allHeaders) {
        return stream(allHeaders).collect(toMap(Header::getName, Header::getValue));
    }

    public ResponseVerification expectResponseCode(int responseCode) {
        assertThat("The response code for: "  + this.testRequest.getPrettyUrl() + " did not match what we expected.", this.responseCode, is(responseCode));
        return this;
    }

    public ResponseVerification andExpectResponseCode(int responseCode) {
        return expectResponseCode(responseCode);
    }

    public ResponseVerification expectClusterName(String clusterName) {
        assertThat("The request:"  + this.testRequest.getPrettyUrl() + " was routed to the wrong upstream cluster!",
                this.clusterName, is(clusterName));
        return this;
    }

    public ResponseVerification andExpectClusterName(String clusterName) {
        return expectClusterName(clusterName);
    }

    public ResponseVerification expectClusterNumber(int clusterNumber) {
        assertThat(this.clusterNumber, is(clusterNumber));
        return this;
    }

    public ResponseVerification andExpectClusterNumber(int clusterNumber) {
        return expectClusterNumber(clusterNumber);
    }

    public ResponseVerification expectAppPath(String appPath) {
        assertThat("The path for: "   + this.testRequest.getPrettyUrl() +  " that was sent to the service did not match what we expected.",
                this.appPath, is(appPath));
        return this;
    }

    public ResponseVerification andExpectAppPath(String appPath) {
        return expectAppPath(appPath);
    }

    public ResponseVerification andExpectAppUrl(String url) {
        assertThat("The url for: "   + this.testRequest.getPrettyUrl() +  " that was sent to the service did not match what we expected.",
            this.urlToApplication, is(url));
        return this;
    }

    @Deprecated
    public ResponseVerification andHasResponseHeader(String headerKey) {
        return andExpectResponseHeader(headerKey);
    }

    @Deprecated
    public ResponseVerification hasResponseHeader(String headerKey) {
        return andExpectResponseHeader(headerKey);
    }

    public ResponseVerification andExpectResponseHeader(String headerKey) {
        assertThat("Call from Reverse Proxy to: " +  this.serviceResponseBody.getRequest().getUrlToApplication()
                        + " originating from: " + this.testRequest.getPrettyUrl()
                        + " did not contain the header in the response sent from the application.",
                this.responseHeaders, hasKey(headerKey));
        return this;
    }

    @Deprecated
    public ResponseVerification hasRequestHeaderToApplication(String headerKey) {
        return andExpectRequestHeaderToApplication(headerKey);
    }

    public ResponseVerification andExpectRequestHeaderToApplication(String headerKey) {
        assertThat("Call from Reverse Proxy to: " +  this.serviceResponseBody.getRequest().getUrlToApplication()
                        + " originating from: " + this.testRequest.getPrettyUrl()
                        + " did not contain the header in the request sent to the application.",
                this.serviceResponseBody.getRequest().getHeaders(), hasKey(headerKey));
        return this;
    }

    @Deprecated
    public ResponseVerification hasRequestHeaderToApplicationMatching(String headerKey, String matchingRegex) {
        return andExpectRequestHeaderToApplicationMatching(headerKey, matchingRegex);
    }

    public ResponseVerification andExpectRequestHeaderToApplicationMatching(String headerKey, String matchingRegex) {
        andExpectRequestHeaderToApplication(headerKey);
        assertThat("Call from Reverse Proxy to: " +  this.serviceResponseBody.getRequest().getUrlToApplication()
                        + " originating from: " + this.testRequest.getPrettyUrl()
                        + " has a value '" + this.serviceResponseBody.getRequest().getHeaders().get(headerKey)
                        + "' that does not match " + matchingRegex,
                this.serviceResponseBody.getRequest().getHeaders().get(headerKey), matchesPattern(matchingRegex));
        return this;
    }

    @Deprecated
    public ResponseVerification andHasRequestHeaderToApplication(String headerKey) {
        return andExpectRequestHeaderToApplication(headerKey);
    }

    public ResponseVerification hasRequestHeaderToApplication(String headerKey, String headerValue) {
        assertThat("Call from Reverse Proxy to: " +  this.serviceResponseBody.getRequest().getUrlToApplication()
                 + " originating from: " + this.testRequest.getPrettyUrl()
                + " did not contain the header in the request sent to the application.",
                this.serviceResponseBody.getRequest().getHeaders(), hasEntry(headerKey, headerValue));
        return this;
    }

    public ResponseVerification andHasRequestHeaderToApplication(String headerKey, String headerValue) {
        return hasRequestHeaderToApplication(headerKey, headerValue);
    }

    public ResponseVerification andDoesNotHaveRequestHeaderToApplication(String headerKey) {
        assertThat("Call from Reverse Proxy to: " +  this.serviceResponseBody.getRequest().getUrlToApplication()
                        + " originating from: " + this.testRequest.getPrettyUrl()
                        + " does have the header + " + headerKey + " in the request sent to the application.",
                this.serviceResponseBody.getRequest().getHeaders(), not(hasKey(headerKey)));
        return this;
    }

    public int getLatencyMillis() {
        return latencyMillis;
    }

    public int getRoundTripTimeMillis() {
        return roundTripTimeMillis;
    }

    @Deprecated
    public ResponseVerification andHasResponseHeader(String key, String value) {
        return expectResponseHeader(key, value);
    }

    public ResponseVerification andExpectResponseHeader(String key, String value) {
        return expectResponseHeader(key, value);
    }

    @Deprecated
    public ResponseVerification hasResponseHeader(String key, String value) {
        assertThat("Call to " + this.testRequest.getPrettyUrl() + " did not have the matching response header.",
                this.responseHeaders, hasEntry(key, value));
        return this;
    }

    public ResponseVerification expectResponseHeader(String key, String value) {
        assertThat("Call to " + this.testRequest.getPrettyUrl() + " did not have the matching response header.",
                this.responseHeaders, hasEntry(key, value));
        return this;
    }

    @Deprecated
    public ResponseVerification andResponseBodyMatchesFileContents(String expectedBodyPath) {
        return andExpectResponseBodyContent(removeNewLines(readFile(System.getProperty("user.dir") + "/" + expectedBodyPath)));
    }

    public ResponseVerification andExpectResponseBodyMatchesFileContents(String expectedBodyPath) {
        return andExpectResponseBodyContent(removeNewLines(readFile(System.getProperty("user.dir") + "/" + expectedBodyPath)));
    }

    private String removeNewLines(String bodyContent) {
        return bodyContent.replaceAll("\n|\r", "");
    }

    private String readFile(String filePath) {
        byte[] encoded;
        try {
            encoded = Files.readAllBytes(Paths.get(filePath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new String(encoded);
    }

    @Deprecated
    public ResponseVerification andDoesNotHaveResponseHeader(String key) {
        assertThat(this.responseHeaders, not(hasKey(key)));
        return this;
    }

    public ResponseVerification andExpectMissingResponseHeader(String key) {
        assertThat(this.responseHeaders, not(hasKey(key)));
        return this;
    }

    public ResponseVerification andExpectResponseBodyContent(String expectedBody) {
        assertThat(this.serviceResponseBody.getRawResponse(), is(expectedBody));
        return this;
    }

    public ResponseVerification andExpectToHaveQueryParam(String param, String value) {
        assertThat("Call to " + this.testRequest.getPrettyUrl() + " did not have the matching query params to upstream server.",
                this.serviceResponseBody.getRequest().getQuery(), hasEntry(param, value));
        return this;
    }
}
