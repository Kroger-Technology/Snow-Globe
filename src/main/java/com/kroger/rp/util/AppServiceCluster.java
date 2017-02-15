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

package com.kroger.rp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kroger.rp.util.environment.UpstreamAppInfo;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;

/**
 * This represents the upstream service that will be dynamically created.  This represents a "fake" upstream cluster as
 * it may be more than one instance.  The instance may be customized with a special response code with the ability to
 * send custom headers in the response.
 */
public class AppServiceCluster {

    private final int randomNamePrefix = Math.abs(new Random(System.currentTimeMillis()).nextInt());

    private final String clusterName;
    private final int instances;
    private int httpResponseCode = 200;
    private String matchingPaths = "*";
    private Map<String, String> responseHeaders = new HashMap<>();
    private final boolean useHttps;

    /**
     * Helper method that will build a single cluster instance that accepts http traffic.
     *
     * @param clusterName
     *      The name of the cluster that matches the Nginx Cluster Name.
     * @return
     *      The appServiceCluster object that can be used with the <code>NginxRpBuilder</code> to run as part of the
     *      test.
     */
    public static AppServiceCluster makeHttpWebService(String clusterName) {
        return new AppServiceCluster(clusterName, 1, false);
    }

    /**
     * Helper method that will build a cluster that accepts http traffic.
     *
     * @param clusterName
     *      The name of the cluster that matches the Nginx Cluster Name.
     * @param instances
     *      The number of instances to create in the cluster.
     * @return
     *      The appServiceCluster object that can be used with the <code>NginxRpBuilder</code> to run as part of the
     *      test.
     */
    public static AppServiceCluster makeHttpWebService(String clusterName, int instances) {
        return new AppServiceCluster(clusterName, instances, false);
    }

    /**
     * Helper method that will build a single cluster instance that accepts https traffic.
     *
     * @param clusterName
     *      The name of the cluster that matches the Nginx Cluster Name.
     * @return
     *      The appServiceCluster object that can be used with the <code>NginxRpBuilder</code> to run as part of the
     *      test.
     */
    public static AppServiceCluster makeHttpsWebService(String clusterName) {
        return new AppServiceCluster(clusterName, 1, true);
    }

    /**
     * Helper method that will build a cluster that accepts https traffic.
     *
     * @param clusterName
     *      The name of the cluster that matches the Nginx Cluster Name.
     * @param instances
     *      The number of instances to create in the cluster.
     * @return
     *      The appServiceCluster object that can be used with the <code>NginxRpBuilder</code> to run as part of the
     *      test.
     */
    public static AppServiceCluster makeHttpsWebService(String clusterName, int instances) {
        return new AppServiceCluster(clusterName, instances, true);
    }

    /**
     * The constructor that stores the basic state of the service.
     *
     * @param clusterName
     *      The name of the cluster that matches the Nginx Cluster Name.
     * @param instances
     *      The number of instances to create in the cluster.
     * @param useHttps
     *      if true, then this cluster should accept https traffic, otherwise if false, then it will only accept http
     *      traffic
     */
    public AppServiceCluster(String clusterName, int instances, boolean useHttps) {
        this.clusterName = clusterName;
        this.instances = instances;
        this.useHttps = useHttps;
    }

    /**
     * A builder method that defines the default http response code.
     * @param httpResponseCode
     *      the default http response code.
     * @return
     *      the <code>AppServiceCluster</code> object.
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

    public AppServiceCluster withExpectedPaths(String... matchingPaths) {
        this.matchingPaths = stream(matchingPaths).reduce((s, s2) -> s + "|" + s2).get();
        return this;
    }

    String buildContainerId(int instance) {
        return "CLUSTER-" + randomNamePrefix + "-" + clusterName + "-" + Integer.toString(instance);
    }

    public String getClusterName() {
        return this.clusterName;
    }

    public AppServiceCluster withResponseHeader(String key, String value) {
        responseHeaders.put(key, value);
        return this;
    }

    public Map<String, Object> buildComposeMap(TestFrameworkProperties testFrameworkProperties) {
        Map<String, Object> composeMap = new HashMap<>();
        IntStream.range(0, instances).forEach(instance -> {
            Map<String, Object> appArgsMap = new HashMap<>();
            appArgsMap.put("container_name", buildContainerId(instance));
            appArgsMap.put("image", testFrameworkProperties.getFakeUpstreamImage());
            appArgsMap.put("environment", buildEnvironmentList(instance));
            appArgsMap.put("expose", singletonList(3000));
            composeMap.put(buildContainerId(instance), appArgsMap);
        });
        return composeMap;
    }

    List<String> buildEnvironmentList(int instance) {
        List<String> environmentVariables = new ArrayList<>();
        environmentVariables.add("INSTANCE_NUMBER=" + instance);
        environmentVariables.add("CLUSTER_NAME=" + getClusterName());
        environmentVariables.add("APP_PATHS=" + matchingPaths);
        environmentVariables.add("RESPONSE_CODE=" + httpResponseCode);
        environmentVariables.add("RESPONSE_HEADERS=" + getHeadersAsArgument() + "");
        environmentVariables.add("USE_HTTPS=" + (useHttps ? "https" : "http"));
        return environmentVariables;
    }

    List<String> getInstanceNames() {
        return IntStream.range(0, instances)
                .mapToObj(this::buildContainerId)
                .collect(Collectors.toList());
    }

    public List<UpstreamAppInfo> getAppInstanceInfos() {
        return IntStream.range(0, instances)
                .mapToObj(instance -> new UpstreamAppInfo(buildContainerId(instance), 3000))
                .collect(Collectors.toList());
    }

    public int getRandomNamePrefix() {
        return randomNamePrefix;
    }

    public int getInstances() {
        return instances;
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
}
