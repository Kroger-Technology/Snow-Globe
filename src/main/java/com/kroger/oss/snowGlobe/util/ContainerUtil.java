package com.kroger.oss.snowGlobe.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

public class ContainerUtil {

    public static void logContainerOutput(String containerName) {
        try {
            System.out.println("\n\tLogging output for container: " + containerName);
            System.out.println("----------------------------------------------------");
            ProcessBuilder processBuilder = new ProcessBuilder("docker", "logs", containerName);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
            System.out.println("----------------------------------------------------");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void runCommandWithLogs(String... command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            processBuilder.inheritIO();
            System.out.println("\n\tRunning Command: " + Arrays.stream(command).collect(Collectors.joining(" ")));
            System.out.println("----------------------------------------------------");
            Process process = processBuilder.start();
            process.waitFor();
            System.out.println("[EXIT CODE: " + process.exitValue() + "]");
            System.out.println("----------------------------------------------------");
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void runCommand(String... command) {
        try {
            new ProcessBuilder(command).start().waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<Integer, Integer> getMappedPorts(String containerId) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("docker", "ps");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            process.waitFor();
            while (bufferedReader.ready()) {
                String line = bufferedReader.readLine();
                if (line.contains(containerId)) {
                    return parseMappedPorts(line);
                }
            }
            return new HashMap<>();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected static Map<Integer, Integer> parseMappedPorts(String psLine) {
        Map<Integer, Integer> mappedPorts = new HashMap<>();
        int startIndex = psLine.indexOf("0.0.0.0");
        int stopIndex = psLine.indexOf("RP-");
        String ports = psLine.substring(startIndex, stopIndex)
                .trim()
                .replaceAll(" ", "")
                .replaceAll("/tcp", "")
                .replaceAll("->", "|")
                .replaceAll("[0-9]+[.][0-9]+[.][0-9]+[.][0-9]+:", "");
        String[] portMappings = ports.split(",");
        Arrays.stream(portMappings).forEach(mapping -> {
            String[] inOutPorts = mapping.split("[|]");
            mappedPorts.put(Integer.parseInt(inOutPorts[1]), Integer.parseInt(inOutPorts[0]));
        });
        return mappedPorts;
    }

    public static boolean isContainerRunning(String containerId) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("docker", "ps");
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            boolean isRunning = false;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            process.waitFor();
            while (bufferedReader.ready()) {
                String line = bufferedReader.readLine();
                if (line.contains(containerId)) {
                    isRunning = line.contains("Up");
                }
            }
            return isRunning;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void restartNginx(String containerId, int reloadWaitMs) {
        try {
            List<String> originalWorkerThreads = getNginxWorkerThreads(containerId);
            runCommandWithLogs("docker", "exec", containerId, "nginx", "-s", "reload");
            Thread.sleep(reloadWaitMs);
            List<String> postReloadWorkerThreads = getNginxWorkerThreads(containerId);
            // We are going to wait until all original worker threads are retired.
            while(hasOriginalWorkerPidsRunning(originalWorkerThreads, postReloadWorkerThreads)) {
                postReloadWorkerThreads = getNginxWorkerThreads(containerId);
                Thread.sleep(reloadWaitMs);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean hasOriginalWorkerPidsRunning(List<String> originalWorkerPids, List<String> postReloadWorkerPids) {
        return originalWorkerPids.stream().anyMatch(pid -> postReloadWorkerPids.contains(pid));
    }

    private static List<String> getNginxWorkerThreads(String containerId) {
        try {
            Process process = new ProcessBuilder("docker", "top", containerId, "-e", "-o", "pid,command").start();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            process.waitFor();
            List<String> allLines = new ArrayList<>();
            while (bufferedReader.ready()) {
                allLines.add(bufferedReader.readLine());
            }
            return allLines.stream()
                    .filter((line) -> line.contains("nginx: worker process"))
                    .map(line -> line.substring(0, line.indexOf("nginx")).trim())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void shutdownContainer(String containerName) {
        runCommand("docker", "rm", "-f", containerName);
    }

    public static void shutdownContainerWithLogs(String containerName) {
        runCommandWithLogs("docker", "rm", "-f", containerName);
    }
}
