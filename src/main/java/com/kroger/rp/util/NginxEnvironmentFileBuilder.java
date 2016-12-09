package com.kroger.rp.util;

import com.kroger.rp.util.environment.UpstreamAppInfo;

import java.util.List;

public class NginxEnvironmentFileBuilder extends BaseEnvironmentUtil{


    public NginxEnvironmentFileBuilder(String nginxConfFile) {
        readNginxConfFile(nginxConfFile, "proxy_pass");
    }

    public void readOverridesFile(String overridesConfFile) {
        readNginxConfFile(overridesConfFile, "proxy_pass");
    }

    String buildUpstreamServerEntry(String serverName, List<UpstreamAppInfo> infos) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n")
                .append("  upstream ").append(serverName).append(" { \n");
        infos.stream()
                .forEach(app ->
                        sb.append("    server ")
                                .append(app.containerName())
                                .append(":")
                                .append(app.port())
                                .append(";\n"));
        sb.append("  }")
                .append("\n")
                .append("\n");
        return sb.toString();
    }
}
