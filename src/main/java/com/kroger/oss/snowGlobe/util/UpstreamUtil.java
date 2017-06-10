package com.kroger.oss.snowGlobe.util;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kroger.oss.snowGlobe.AppServiceCluster;
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

public class UpstreamUtil {

    public static void setupUpstreamService() {
        DockerNetworking.createNetwork();
        if (!upstreamRunning()) {
            startUpstream();
        }
    }

    public static int addUpstream(int instance, String clusterName, String matchingPaths, int httpResponseCode,
                           Map<String, String> headers, boolean useHttps) {
        StringEntity json = buildJsonBody(instance, clusterName, matchingPaths, httpResponseCode, headers, useHttps);
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

    public static void stopUpstream(int port) {
        try {
            StringEntity body = new StringEntity(Integer.toString(port));
            CloseableHttpClient client = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost("http://localhost:30010");
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setEntity(body);
            client.execute(httpPost);
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static HttpPost buildRequest(StringEntity json) {
        HttpPost httpPost = new HttpPost("http://localhost:30010/startServer");
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

    private static StringEntity buildJsonBody(int instance, String clusterName, String matchingPaths, int httpResponseCode, Map<String, String> headers, boolean useHttps) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("instanceNumber", instance);
        requestMap.put("clusterName", clusterName);
        requestMap.put("matchingPaths", matchingPaths);
        requestMap.put("responseCode", httpResponseCode);
        requestMap.put("runHTTPS", useHttps);
        requestMap.put("responseHeaders", headers);
        try {
            return new StringEntity(new ObjectMapper().writeValueAsString(requestMap));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void startUpstream() {
        try {
            ProcessBuilder processBuilder =
                    new ProcessBuilder("docker", "run", "-p", "30010:3000", "--network=snowglobe_default",
                            "--name", "upstream", "--detach", "krogersnowglobe/upstream-bounce-app:latest");
            Process process = processBuilder.start();
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean upstreamRunning() {
        try {
            URL url = new URL("http://localhost:30010");
            URLConnection uc = url.openConnection();
            uc.connect();
            return true;

        } catch (Exception e) {
            return false;
        }
    }

    public static void initializeUpstreamInstances(AppServiceCluster[] clusters) {
        Arrays.stream(clusters).forEach(cluster -> {
            IntStream.range(0, cluster.getInstances()).forEach(i -> {
                int port = UpstreamUtil.addUpstream(i, cluster.getClusterName(), cluster.getMatchingPaths(),
                        cluster.getHttpResponseCode(), cluster.getResponseHeaders(), cluster.isUseHttps());
                cluster.assignPort(port);
            });
        });
    }
}
