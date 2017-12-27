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

package com.kroger.oss.snowGlobe.util;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.kroger.oss.snowGlobe.AppServiceCluster;
import com.kroger.oss.snowGlobe.TestFrameworkProperties;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.IntStream;

import static com.kroger.oss.snowGlobe.util.DockerNetworking.SNOW_GLOBE_NETWORK;

public class UpstreamUtil {

    private static final String UPSTREAM_SERVICE_PORT = "30010";
    private static final String UPSTREAM_NAME = "upstream";

    public static void setupUpstreamService() {
        DockerNetworking.createNetwork();
        if (!upstreamRunning()) {
            startUpstream();
            setupUpstreamShutdownHook();
        } else {
            resetUpstreams();
        }
    }

    private static void setupUpstreamShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ContainerUtil.shutdownContainer(UPSTREAM_NAME);
        }));
    }

    public static int addUpstream(int instance, String clusterName, String matchingPaths, int httpResponseCode,
                                  Map<String, String> headers, boolean useHttps, int port) {
        StringEntity json = buildJsonBody(instance, clusterName, matchingPaths, httpResponseCode, headers, useHttps, port);
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = buildRequest(json);
        try {
            CloseableHttpResponse res = client.execute(httpPost);
            int upstreamPort = getUpstreamPort(res);
            client.close();
            return upstreamPort;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static HttpPost buildRequest(StringEntity json) {
        HttpPost httpPost = new HttpPost("http://localhost:" + UPSTREAM_SERVICE_PORT + "/startServer");
        httpPost.setHeader("Content-type", "application/json");
        httpPost.setEntity(json);
        return httpPost;
    }

    private static int getUpstreamPort(CloseableHttpResponse res) {
        try {
            HttpEntity entity = res.getEntity();
            return Integer.parseInt(EntityUtils.toString(entity, "UTF-8"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static StringEntity buildJsonBody(int instance, String clusterName, String matchingPaths,
                                              int httpResponseCode, Map<String, String> headers, boolean useHttps,
                                              int port) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("instanceNumber", instance);
        requestMap.put("clusterName", clusterName);
        requestMap.put("matchingPaths", matchingPaths);
        requestMap.put("responseCode", httpResponseCode);
        requestMap.put("runHTTPS", useHttps);
        requestMap.put("responseHeaders", headers);
        requestMap.put("port", port);
        try {
            return new StringEntity(new ObjectMapper().writeValueAsString(requestMap));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void startUpstream() {
        TestFrameworkProperties props = new TestFrameworkProperties();
        try {
            String[] command = {"docker", "run", "-p", UPSTREAM_SERVICE_PORT + ":3000", "--network=" + props.getDockerNetworkName(),
                    "--name", UPSTREAM_NAME, "--detach", props.getUpstreamBounceImage()};
            if (props.logContainerOutput()) {
                ContainerUtil.runCommandWithLogs(command);
            } else {
                ContainerUtil.runCommand(command);
            }
            waitForUpstreamToStart();
        } catch (Exception e) {
            // if we have gotten an exception, there is the possibility that another process was setting up this
            // container. This can happen with multiple parallel forks.
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            if (!upstreamRunning()) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void waitForUpstreamToStart() throws InterruptedException {
        for (int i = 0; i < 25; i++) {
            if (upstreamRunning()) {
                return;
            }
            Thread.sleep(200);
        }
        System.out.println("Timed out waiting on upstream container to start.");
        ContainerUtil.logContainerOutput(UPSTREAM_NAME);
    }

    private static boolean upstreamRunning() {
        try {
            URL url = new URL("http://localhost:" + UPSTREAM_SERVICE_PORT + "/health");
            URLConnection uc = url.openConnection();
            uc.connect();
            String status = uc.getHeaderField(0);
            return status.contains("200");

        } catch (Exception e) {
            return false;
        }
    }

    public static void initializeUpstreamInstances(AppServiceCluster[] clusters) {
        Arrays.stream(clusters).forEach(cluster ->
                UpstreamUtil.addUpstream(0, cluster.getClusterName(), cluster.getMatchingPaths(),
                cluster.getHttpResponseCode(), cluster.getResponseHeaders(), cluster.isUseHttps(),
                cluster.getPort()));
    }

    public static void resetUpstreams() {
        try {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("http://localhost:" + UPSTREAM_SERVICE_PORT + "/reset");
            client.execute(httpPost);
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
