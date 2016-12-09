package com.kroger.rp.util;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;

public class TestFrameworkProperties {

    public static Map<String, Object> properties;

    static {
        initProperties();
    }

    private static String getStringValue(String key) {
        if(properties.get(key) != null) {
            return properties.get(key).toString();
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    static void initProperties() {
        properties = (Map<String, Object>) new Yaml().load(currentThread().getContextClassLoader().getResourceAsStream("test.yaml"));
    }

    static String getFakeUpstreamImage() {
        return getStringValue("upstream.fake.container");
    }

    static boolean logContainerOutput() {
        return properties.get("framework.log.output") != null &&
                properties.get("framework.log.output").toString().equalsIgnoreCase("true");
    }

    static String getNginxImage() {
        return getStringValue("nginx.container");
    }

    static String getUpstreamLocation() {
        return getStringValue("nginx.upstream.file.path");
    }

    @SuppressWarnings("unchecked")
    static List<String> getNginxVolumes() {
        return (List<String>) properties.get("nginx.volume.mounts");
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> getNginxPortMapping() {
        return (List<Map<String, Object>>) properties.get("nginx.url.port.mapping");
    }
}
