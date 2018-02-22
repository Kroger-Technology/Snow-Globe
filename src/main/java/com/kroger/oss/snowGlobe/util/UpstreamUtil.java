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
import com.kroger.oss.snowGlobe.FrameworkProperties;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class UpstreamUtil {

    public static void setupUpstreamService(FrameworkProperties props) {
        DockerNetworking.createNetwork();
        if (!upstreamRunning(props.getUpstreamServicePort())) {
            startUpstream(props);
            setupUpstreamShutdownHook(props.getUpstreamName());
        } else {
            resetUpstreams(props.getUpstreamServicePort());
        }
    }

    private static void setupUpstreamShutdownHook(String upstreamName) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ContainerUtil.shutdownContainer(upstreamName);
        }));
    }

    public static int addUpstream(int instance, AppServiceCluster cluster, FrameworkProperties props) {
        String clusterName = cluster.getClusterName();
        String matchingPaths = cluster.getMatchingPaths();
        int httpResponseCode = cluster.getHttpResponseCode();
        Map<String, String> headers = cluster.getResponseHeaders();
        boolean useHttps = cluster.isUseHttps();
        int port = cluster.getPort();
        StringEntity json = buildJsonBody(instance, clusterName, matchingPaths, httpResponseCode, headers, useHttps, port);
        CloseableHttpClient client = HttpClients.createDefault();
        HttpPost httpPost = buildRequest(json, props.getUpstreamServicePort());
        try {
            CloseableHttpResponse res = client.execute(httpPost);
            int upstreamPort = getUpstreamPort(res);
            client.close();
            return upstreamPort;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static HttpPost buildRequest(StringEntity json, String upstreamServicePort) {
        HttpPost httpPost = new HttpPost("http://" + getUpstreamHost(System.getenv("DOCKER_HOST")) + ":" + upstreamServicePort + "/startServer");
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

    private static void startUpstream(FrameworkProperties props) {
        try {
            String[] command = {"docker", "run", "-p", props.getUpstreamServicePort() + ":3000", "--network=" + props.getDockerNetworkName(),
                    "--name", props.getUpstreamName(), "--detach", props.getUpstreamBounceImage()};
            if (props.logContainerOutput()) {
                ContainerUtil.runCommandWithLogs(command);
            } else {
                ContainerUtil.runCommand(command);
            }
            waitForUpstreamToStart(props.getUpstreamName(), props.getUpstreamServicePort());
        } catch (Exception e) {
            // if we have gotten an exception, there is the possibility that another process was setting up this
            // container. This can happen with multiple parallel forks.
            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {
            }
            if (!upstreamRunning(props.getUpstreamServicePort())) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void waitForUpstreamToStart(String upstreamName, String upstreamServicePort) throws InterruptedException {
        for (int i = 0; i < 25; i++) {
            if (upstreamRunning(upstreamServicePort)) {
                return;
            }
            Thread.sleep(200);
        }
        System.out.println("Timed out waiting on upstream container to start.");
        ContainerUtil.logContainerOutput(upstreamName);
    }

    private static boolean upstreamRunning(String port) {
        try {
            URL url = new URL("http://" + getUpstreamHost(System.getenv("DOCKER_HOST")) + ":" + port + "/health");
            URLConnection uc = url.openConnection();
            uc.connect();
            String status = uc.getHeaderField(0);
            return status.contains("200");

        } catch (Exception e) {
            return false;
        }
    }

    public static void initializeUpstreamInstances(AppServiceCluster[] clusters, FrameworkProperties frameworkProperties) {
        Arrays.stream(clusters).forEach(cluster ->
                UpstreamUtil.addUpstream(0, cluster, frameworkProperties));
    }

    public static void resetUpstreams(String upstreamServicePort) {
        try {
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("http://" + getUpstreamHost(System.getenv("DOCKER_HOST")) + ":" + upstreamServicePort + "/reset");
            client.execute(httpPost);
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getUpstreamHost(String dockerHost) {
        if (dockerHost == null || dockerHost.equals("")) {
            return "localhost";
        }
        if (dockerHost.startsWith("unix://")) {
            return "localhost";
        }

        try {
            URI parsed = new URI(dockerHost);
            return parsed.getHost();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return "localhost";
    }
}
