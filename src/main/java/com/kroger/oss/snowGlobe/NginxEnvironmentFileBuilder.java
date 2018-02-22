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

import com.kroger.oss.snowGlobe.environment.UpstreamAppInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class NginxEnvironmentFileBuilder {

    MessageDigest configurationMD5;
    FrameworkProperties frameworkProperties;
    Map<String, UpstreamAppInfo> upstreamServers = new HashMap<>();

    public NginxEnvironmentFileBuilder() {
        frameworkProperties = new FrameworkProperties();
        try {
            configurationMD5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void addUpstreamServer(String clusterName) {
        upstreamServers.put(clusterName.trim(), new UpstreamAppInfo());
    }

    public String buildClusterFileContents() {
        return upstreamServers.keySet()
                .stream()
                .map(serverName -> buildUpstreamServerEntry(serverName, upstreamServers.get(serverName)))
                .reduce(String::concat)
                .orElse("");
    }


    protected void readNginxConfFile(String confFile, String prefix) {
        if (confFile.contains("*")) {
            getWildCardFiles(confFile).stream().forEach(file -> readNginxConfFile(file, prefix));
        } else {
            readSingleNginxFile(confFile, prefix);
        }
    }

    private void readSingleNginxFile(String confFile, String prefix) {
        try {
            FileReader fileReader = new FileReader(confFile);
            BufferedReader reader = new BufferedReader(fileReader);
            while (reader.ready()) {
                String line = reader.readLine();
                configurationMD5.update(line.getBytes());
                parseNginxFileLine(prefix, line);
            }
            fileReader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void parseNginxFileLine(String prefix, String line) {
        if (line.contains(prefix)) {
            addEmptyCluster(line);
        } else if (line.trim().startsWith("include ")) {
            readNginxConfFile(getIncludeFileName(line), prefix);
        }
    }

    protected String getIncludeFileName(String line) {
        return correctFilePath(line.trim().substring(8).trim().replace(";", "").trim());
    }

    protected String correctFilePath(String filePath) {
        return (filePath.contains(frameworkProperties.getDeployedDirectory())) ?
                filePath.replace(frameworkProperties.getDeployedDirectory(), frameworkProperties.getSourceDirectory()) :
                frameworkProperties.getSourceDirectory() + File.separator + filePath;
    }

    public void computeUpstreamPorts() {
        SortedSet<String> keys = new TreeSet<>(upstreamServers.keySet());
        int currentPort = 10000;
        for (String key : keys) {
            UpstreamAppInfo upstreamInstance = upstreamServers.get(key);
            upstreamInstance.setPort(currentPort);
            currentPort++;
        }
    }

    /**
     * Here, we expect each line to be in a specific format that gives us a hint as to which
     * upstream servers the configuration will be looking for.  We just add an empty one so it doesn't
     * complain and shutdown.
     * <p>
     * NOTE:  Any proxy_pass with a variable set will be ignored since that variable will be resolved at the request.
     *
     * @param line a line from the nginx.conf file that should be of the format:
     *             "   proxy_pass http://[CLUSTER NAME]/..."
     */
    void addEmptyCluster(String line) {
        String prefixRemoved = line.substring(line.indexOf("://") + 3);
        String clusterName = prefixRemoved;
        clusterName = handleClusterNameChar(prefixRemoved, clusterName, "/");
        clusterName = handleClusterNameChar(prefixRemoved, clusterName, ";");
        clusterName = handleClusterNameChar(prefixRemoved, clusterName, "/");
        if (clusterName.contains("$")) {
            return;
        }
        addUpstreamServer(clusterName);
    }

    private String handleClusterNameChar(String prefixRemoved, String clusterName, String character) {
        return (prefixRemoved.indexOf(character) > 0) ?
                prefixRemoved.substring(0, prefixRemoved.indexOf(character)) : clusterName;
    }

    public void addUpstreamServer(AppServiceCluster appServiceCluster) {
        this.addUpstreamServer(appServiceCluster.getClusterName());
    }

    public void readEnvConfig(String envConfig) {
        File envConfigFile = new File(envConfig);
        if (envConfigFile.exists()) {
            if (envConfigFile.isDirectory() && envConfigFile.listFiles() != null) {
                stream(envConfigFile.listFiles())
                        .map(File::getAbsolutePath)
                        .forEach(this::readEnvConfig);
            } else {
                readNginxConfFile(envConfig, "proxy_pass");
            }
        }
    }

    String buildUpstreamServerEntry(String serverName, UpstreamAppInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n")
                .append("  upstream ").append(serverName).append(" { \n");
        if (frameworkProperties.defineUpstreamZones()) {
            sb.append("    zone " + serverName + " 64k;\n");
        }
        sb.append("    server ")
                .append(frameworkProperties.getUpstreamName())
                .append(":")
                .append(info.port())
                .append(" max_fails=0")
                .append(";\n")
                .append("  }")
                .append("\n")
                .append("\n");
        return sb.toString();
    }

    public List<String> getWildCardFiles(String wildcardInclude) {
        String baseDirectory = wildcardInclude.contains("/") ? wildcardInclude.substring(0, wildcardInclude.lastIndexOf("/")) : "";
        String wildCard = wildcardInclude.substring(baseDirectory.length() + 1).replaceAll("\\*", "\\.\\*");
        try {
            return stream(new File(baseDirectory).list())
                    .filter(s -> s.matches(".*" + wildCard))
                    .map(s -> baseDirectory  + "/" + s)
                    .collect(toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    FrameworkProperties getPropertiesForTest() {
        return frameworkProperties;
    }

    private byte[] computeUpstreamHash() {
        try {
            MessageDigest upstreamHash = MessageDigest.getInstance("MD5");
            upstreamHash.update(this.buildClusterFileContents().getBytes());
            return upstreamHash.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public String computeConfigurationHash() {
        try {
            MessageDigest totalHash = MessageDigest.getInstance("MD5");
            totalHash.update(configurationMD5.digest());
            totalHash.update(computeUpstreamHash());
            return new BigInteger(1, totalHash.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public void setUpstreamPorts(AppServiceCluster[] upstreamInstance) {
        Arrays.stream(upstreamInstance).forEach(upstream -> {
            UpstreamAppInfo upstreamAppInfo = upstreamServers.get(upstream.getClusterName());
            if (upstreamAppInfo != null) {
                upstream.assignPort(upstreamAppInfo.port());
            }
        });
    }

    public void registerUpstreams(AppServiceCluster[] clusters) {
        Arrays.stream(clusters).forEach(cluster -> {
            addUpstreamServer(cluster.getClusterName());
        });
    }
}
