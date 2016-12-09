package com.kroger.rp.util;

import com.kroger.rp.util.environment.UpstreamAppInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.net.NetworkInterface.getNetworkInterfaces;
import static java.util.Collections.list;
import static java.util.Collections.singletonList;

public abstract class BaseEnvironmentUtil {
    Map<String, List<UpstreamAppInfo>> upstreamServers = new HashMap<>();

    public void addUpstreamServer(String clusterName, List<UpstreamAppInfo> appInfos) {
        upstreamServers.put(clusterName, appInfos);
    }

    public String buildClusterFileContents() {
        return upstreamServers.keySet()
                .stream()
                .map(serverName -> buildUpstreamServerEntry(serverName, upstreamServers.get(serverName)))
                .reduce(String::concat)
                .get();
    }

    abstract String buildUpstreamServerEntry(String serverName, List<UpstreamAppInfo> infos);

    protected String getHostIp() {
        try (DatagramSocket s = new DatagramSocket())
        {
            s.connect(InetAddress.getByAddress(new byte[]{1, 1, 1, 1}), 0);
            String ifName = NetworkInterface.getByInetAddress(s.getLocalAddress()).getDisplayName();

            NetworkInterface iFace = list(getNetworkInterfaces()).stream()
                    .filter(networkInterface -> networkInterface.getDisplayName().equals(ifName))
                    .findFirst()
                    .orElse(null);

            return list(iFace.getInetAddresses()).stream()
                    .filter(inetAddress -> inetAddress instanceof Inet4Address)
                    .map(InetAddress::getHostAddress)
                    .findFirst()
                    .orElse("127.0.0.1");
        } catch (Exception e) {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            }
            throw new RuntimeException(e);
        }
    }

    protected void readNginxConfFile(String confFile, String prefix) {
        try {
            String baseDirectory = confFile.substring(0, confFile.lastIndexOf("/nginx/") + 6);
            FileReader fileReader = new FileReader(confFile);
            BufferedReader reader = new BufferedReader(fileReader);
            while(reader.ready()) {
                String line = reader.readLine();
                if(line.contains(prefix)) {
                    addEmptyCluster(line);
                } else if(line.trim().startsWith("include ") && line.trim().replaceAll("#.*", "").contains(".conf")) {
                    String subFileName = baseDirectory + File.separator + getIncludeFileName(line);
                    readNginxConfFile(subFileName, prefix);
                }

            }
            fileReader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected String getIncludeFileName(String line) {
        String relativeFilePath = line;
        if(line.contains("/etc/nginx/")) {
            relativeFilePath = line.replace("/etc/nginx/", "");
        }
        return relativeFilePath.trim().substring(8).trim().replace(";", "").trim();
    }

    /**
     * Here, we expect each line to be in a specific format that gives us a hint as to which
     * upstream servers the configuration will be looking for.  We just add an empty one so it doesn't
     * complain and shutdown.
     *
     * @param line
     *      a line from the nginx.conf file that should be of the format:
     *      "   proxy_pass http://[CLUSTER NAME]/..."
     */
    void addEmptyCluster(String line) {
        String prefixRemoved = line.substring(line.indexOf("://") + 3);
        String clusterName = prefixRemoved;
        if(prefixRemoved.indexOf("/") > 0) {
            clusterName = prefixRemoved.substring(0, prefixRemoved.indexOf("/"));
        }

        if(prefixRemoved.indexOf("$") > 0) {
            clusterName = prefixRemoved.substring(0, prefixRemoved.indexOf("$"));
        }
        if(clusterName.contains(";")) {
            clusterName = clusterName.substring(0, clusterName.indexOf(";"));
        }
        if(clusterName.contains("/")) {
            clusterName = clusterName.substring(0, clusterName.indexOf("/"));
        }
        addUpstreamServer(clusterName, singletonList(new UpstreamAppInfo("127.0.0.1", 65534)));
    }

    public void addUpstreamServer(AppServiceCluster appServiceCluster) {
        this.addUpstreamServer(appServiceCluster.getClusterName(), appServiceCluster.getAppInstanceInfos());
    }
}
