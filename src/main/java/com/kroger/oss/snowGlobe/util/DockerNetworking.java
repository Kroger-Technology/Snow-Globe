package com.kroger.oss.snowGlobe.util;

import java.io.*;

/**
 * Created by ss19287 on 6/10/17.
 */
public class DockerNetworking {

    public static void createNetwork() {
        if (!networkExists()) {
            makeNetwork();
        }
    }

    private static void makeNetwork() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("docker", "network", "create", "snowGlobe");
            Process process = processBuilder.start();
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean networkExists() {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("docker", "network", "ls");
            Process process = processBuilder.start();
            process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while (reader.ready()) {
                if (reader.readLine().contains("snowGlobe")) {
                    return true;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }
}
