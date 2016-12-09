package com.kroger.rp.util.call;

import com.kroger.rp.util.NginxRpBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class TestRequest {


    private Map<String, String> headers = new HashMap<>();
    private String url;
    private HttpMethod method;
    private String body;
    private NginxRpBuilder reverseProxy;
    private String userAgent;

    public static TestRequest getRequest(String url) {
        return new TestRequest(HttpMethod.GET, url);
    }

    public static TestRequest postRequest(String url) {
        return new TestRequest(HttpMethod.POST, url);
    }

    public static TestRequest putRequest(String url) {
        return new TestRequest(HttpMethod.PUT, url);
    }

    public static TestRequest deleteRequest(String url) {
        return new TestRequest(HttpMethod.DELETE, url);
    }

    public static TestRequest headRequest(String url) {
        return new TestRequest(HttpMethod.HEAD, url);
    }

    public TestRequest(HttpMethod method, String url) {
        this.method = method;
        this.url = url;
    }

    public TestRequest withHeader(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public TestRequest withBody(String body) {
        this.body = body;
        return this;
    }

    public TestRequest withUserAgent(String userAgent) {
        this.userAgent = userAgent;
        return this;
    }

    public String getUserAgent() {
        return this.userAgent;
    }

    public boolean hasUserAgent() {
        return getUserAgent() != null && getUserAgent().length() > 0;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public String getUrl() {
        try {
            URI o = new URI(this.url);
            URI injected = new URI("http", null, o.getHost(), reverseProxy.getPortForUrl(this.url),
                    o.getPath(), o.getQuery(), o.getFragment());
            return injected.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String getPrettyUrl() {
        return url;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getBody() {
        return body;
    }

    public TestRequest to(NginxRpBuilder reverseProxy) {
        this.reverseProxy = reverseProxy;
        return this;
    }
}
