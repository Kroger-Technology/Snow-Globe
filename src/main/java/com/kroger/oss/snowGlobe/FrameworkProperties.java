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

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.valueOf;
import static java.util.stream.Collectors.joining;

public class FrameworkProperties {

    private static String overrideYmlConfigurationFile;
    public Map<String, Object> properties;

    public FrameworkProperties(String configurationFile) {
        if (overrideYmlConfigurationFile != null) {
            loadFile(overrideYmlConfigurationFile);
        } else {
            loadFile(configurationFile);
        }
        handleLoggingSettings();
    }

    public FrameworkProperties() {
        this("snow-globe.yml");
    }

    public static void setConfigFile(String configFile) {
        overrideYmlConfigurationFile = configFile;
    }

    private void handleLoggingSettings() {
        if (properties.getOrDefault("snowglobe.log.output", "false").toString().equalsIgnoreCase("true")) {
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        }
    }

    @SuppressWarnings("unchecked")
    void loadFile(String path) {
        try {
            properties = (Map<String, Object>) new Yaml().load(new FileInputStream(path));
        } catch (FileNotFoundException e) {
            System.err.println("Unable to find 'snow-globe.yml'.  This is needed to run.");
            throw new RuntimeException(e);
        }
    }

    private boolean getBooleanValue(String key) {
        return properties.get(key) != null &&
                properties.get(key).toString().equalsIgnoreCase("true");
    }

    private String getStringValue(String key) {
        if (properties.get(key) != null) {
            return properties.get(key).toString();
        } else {
            return null;
        }
    }

    private String getStringValue(String key, String defaultValue) {
        return (properties.get(key) != null) ?
                properties.get(key).toString() :
                defaultValue;
    }


    public String getUpstreamBounceImage() {
        return getStringValue("upstream.bounce.image", "krogersnowglobe/upstream-bounce-app2:latest");
    }

    public boolean logContainerOutput() {
        return getBooleanValue("snowglobe.log.output");
    }

    public boolean preserveTempFiles() {
        return getBooleanValue("snowglobe.preserve.temp.files");
    }

    public boolean defineUpstreamZones() {
        return getBooleanValue("nginx.define.upstream.zones");
    }

    public String getDockerNetworkName() {
        return getStringValue("snowglobe.network.name", "snow-globe");
    }

    String getNginxImage() {
        return getStringValue("nginx.container", "nginx");
    }

    String getUpstreamLocation() {
        return getString("upstream.file.path", "/tmp/emptyUpstream.conf");
    }

    @SuppressWarnings("unchecked")
    List<String> getNginxVolumes() {
        return getList("nginx.volume.mounts");
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> getNginxPortMapping() {
        if (properties.containsKey("nginx.url.port.mapping")) {
            return (List<Map<String, Object>>) properties.get("nginx.url.port.mapping");
        } else {
            return getDefaultPortMapping();
        }
    }

    public List<Map<String, Object>> getDefaultPortMapping() {
        Map<String, Object> https = new HashMap<>();
        Map<String, Object> httpsInner = new HashMap<>();
        httpsInner.put("pattern", "https:.*");
        httpsInner.put("port", 443);
        https.put("https", httpsInner);

        Map<String, Object> http = new HashMap<>();
        Map<String, Object> httpInner = new HashMap<>();
        httpInner.put("pattern", "http:.*");
        httpInner.put("port", 80);
        http.put("http", httpInner);
        return Arrays.asList(https, http);
    }


    public List<String> getFilesToScan() {
        return getList("nginx.env.config.files");
    }

    @SuppressWarnings("unchecked")
    private List<String> getList(String key) {
        try {
            return (List<String>) properties.get(key);
        } catch (Exception e) {
            return null;
        }
    }

    private String getString(String key, String defaultValue) {
        try {
            return valueOf(properties.get(key));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    public String getSourceDirectory() {
        return getStringValue("nginx.source.base.directory");
    }

    public String getDeployedDirectory() {
        return getStringValue("nginx.deploy.base.directory");
    }

    @SuppressWarnings("unchecked")
    public String getStartCommand() {
        try {
            List<String> startCommands = (List<String>) properties.get("nginx.start.command");
            return startCommands.stream().collect(joining(" "));
        } catch (Exception e) {
            return defaultStartCommand();
        }
    }

    private String defaultStartCommand() {
        return "nginx -g 'daemon off;'";
    }


    void setPropertyForTesting(String key, String value) {
        properties.put(key, value);
    }

    private int getIntValue(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.get(key).toString());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public int getMaxNginxStartupTime() {
        return getIntValue("nginx.max.startupTime", 10);
    }

    public int getMaxNginxStartupPollingTimeMs() {
        return getIntValue("nginx.startup.PollingTimeMs", 200);
    }

    public byte[] getLocalHostResolvedIp() {
        String rawResolvedIpEnvVariable = getStringValue("snowGlobe.localhost.resolvedIpEnvVariable");
        try {
            String resolvedIp = System.getenv(rawResolvedIpEnvVariable);
            if (resolvedIp.contains(":")) {
                resolvedIp = resolvedIp.substring(0, resolvedIp.indexOf(":"));
            }
            return InetAddress.getByName(resolvedIp).getAddress();
        } catch (Exception e) {
            return new byte[]{127, 0, 0, 1};
        }
    }

    public boolean getShouldRestartNginxOnEachRun() {
        return getBooleanValue("nginx.reload.onEachRun");
    }

    public String[] getNginxReloadCommand() {
        List<String> list = getList("nginx.reload.customCommand");
        if(list != null && list.size() > 0) {
            return list.toArray(new String[0]);
        }
        return new String[]{"nginx", "-s", "reload"};
    }
}
