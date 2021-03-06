/*
 * Snow-Globe
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

package com.kroger.oss.snowGlobe;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.stream;

/**
 * This represents the upstream service that will be dynamically created.  This represents a "fake" upstream cluster as
 * it may be more than one instance.  The instance may be customized with a special response code with the ability to
 * send custom headers in the response.
 */
public class AppServiceCluster {

    private final String clusterName;
    private final boolean useHttps;
    private int httpResponseCode = 200;
    private String matchingPaths = "*";
    private Map<String, String> responseHeaders = new HashMap<>();
    private int port;

    /**
     * The constructor that stores the basic state of the service.
     *
     * @param clusterName The name of the cluster that matches the Nginx Cluster Name.
     * @param useHttps    if true, then this cluster should accept https traffic, otherwise if false, then it will only accept http
     *                    traffic
     */
    public AppServiceCluster(String clusterName, boolean useHttps) {
        this.clusterName = clusterName;
        this.useHttps = useHttps;
    }


    protected AppServiceCluster(String clusterName, int httpResponseCode, String matchingPaths,
                                Map<String, String> responseHeaders, boolean useHttps) {
        this(clusterName, useHttps);
        this.httpResponseCode = httpResponseCode;
        this.matchingPaths = matchingPaths;
        this.responseHeaders = responseHeaders;
    }

    /**
     * Helper method that will build a single cluster instance that accepts http traffic.
     *
     * @param clusterName The name of the cluster that matches the Nginx Cluster Name.
     * @return The appServiceCluster object that can be used with the <code>NginxRpBuilder</code> to run as part of the
     * test.
     */
    public static AppServiceCluster makeHttpWebService(String clusterName) {
        return new AppServiceCluster(clusterName, false);
    }

    /**
     * Helper method that will build a single cluster instance that accepts https traffic.
     *
     * @param clusterName The name of the cluster that matches the Nginx Cluster Name.
     * @return The appServiceCluster object that can be used with the <code>NginxRpBuilder</code> to run as part of the
     * test.
     */
    public static AppServiceCluster makeHttpsWebService(String clusterName) {
        return new AppServiceCluster(clusterName, true);
    }

    /**
     * A builder method that defines the default http response code.
     *
     * @param httpResponseCode the default http response code.
     * @return the <code>AppServiceCluster</code> object.
     */
    public AppServiceCluster withHttpResponses(int httpResponseCode) {
        this.httpResponseCode = httpResponseCode;
        return this;
    }

    /**
     * Wraps all headers in quotes for argument on parameter.  Also does the horrible escape of quotes.
     */
    private String getHeadersAsArgument() {
        try {
            return "\"" + new ObjectMapper().writeValueAsString(responseHeaders).replaceAll("\"", "\\\\\"") + "\"";
        } catch (IOException e) {
            System.err.println("Unable to parse response header for upstream application");
            return "";
        }
    }

    public AppServiceCluster clone() {
        return new AppServiceCluster(this.clusterName, httpResponseCode, matchingPaths,
                responseHeaders, useHttps);
    }

    public AppServiceCluster withExpectedPaths(String... matchingPaths) {
        this.matchingPaths = stream(matchingPaths).reduce((s, s2) -> s + "|" + s2).get();
        return this;
    }

    public String getClusterName() {
        return this.clusterName;
    }

    public AppServiceCluster withResponseHeader(String key, String value) {
        responseHeaders.put(key, value);
        return this;
    }

    List<String> buildEnvironmentList() {
        List<String> environmentVariables = new ArrayList<>();
        environmentVariables.add("INSTANCE_NUMBER=1");
        environmentVariables.add("CLUSTER_NAME=" + getClusterName());
        environmentVariables.add("APP_PATHS=" + matchingPaths);
        environmentVariables.add("RESPONSE_CODE=" + httpResponseCode);
        environmentVariables.add("RESPONSE_HEADERS=" + getHeadersAsArgument() + "");
        environmentVariables.add("USE_HTTPS=" + (useHttps ? "https" : "http"));
        return environmentVariables;
    }

    public int getHttpResponseCode() {
        return httpResponseCode;
    }

    public String getMatchingPaths() {
        return matchingPaths;
    }

    public Map<String, String> getResponseHeaders() {
        return responseHeaders;
    }

    public boolean isUseHttps() {
        return useHttps;
    }

    public void assignPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }
}
