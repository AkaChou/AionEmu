package com.aionemu.scripts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StartSilentScriptTest {

    @TempDir
    Path tempDir;

    @Test
    void startSilentKeepsAionDirectoryByDefaultAndUsesAionRuntimePaths() throws Exception {
        Path root = prepareRuntimeRoot();
        Path marker = root.resolve("aion/game/marker.txt");
        Files.createDirectories(marker.getParent());
        Files.writeString(marker, "keep");
        Path javaArgs = root.resolve("java-args.txt");

        ProcessResult result = runScript(root, "start-silent.sh", javaArgs);

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(Files.exists(marker));
        assertTrue(Files.exists(root.resolve("aion/log/aionemu.pid")));
        List<String> args = waitForLines(javaArgs);
        assertTrue(args.contains("-Daion.home=" + root.resolve("aion")));
        assertTrue(args.contains("-Daion.log.dir=" + root.resolve("aion/log")));
        assertTrue(args.contains("-jar"));
        assertTrue(args.contains(root.resolve("target/AionEmu.jar").toString()));
    }

    @Test
    void startSilentCleanOptionRemovesAionDirectoryBeforeStarting() throws Exception {
        Path root = prepareRuntimeRoot();
        Path marker = root.resolve("aion/game/marker.txt");
        Files.createDirectories(marker.getParent());
        Files.writeString(marker, "remove");
        Path javaArgs = root.resolve("java-args.txt");

        ProcessResult result = runScript(root, "start-silent.sh", javaArgs, "-c");

        assertEquals(0, result.exitCode(), result.output());
        assertFalse(Files.exists(marker));
        assertTrue(Files.exists(root.resolve("aion/log/aionemu.pid")));
    }

    @Test
    void stopSilentUsesAionLogPidByDefault() throws Exception {
        Path root = prepareRuntimeRoot();
        Path pidFile = root.resolve("aion/log/aionemu.pid");
        Files.createDirectories(pidFile.getParent());
        Files.writeString(pidFile, "99999999");

        ProcessResult result = runScript(root, "stop-silent.sh", root.resolve("java-args.txt"));

        assertEquals(0, result.exitCode(), result.output());
        assertFalse(Files.exists(pidFile));
    }

    private Path prepareRuntimeRoot() throws IOException {
        Path root = tempDir.resolve("runtime");
        Files.createDirectories(root.resolve("target"));
        Files.writeString(root.resolve("target/AionEmu.jar"), "jar");
        Files.copy(Path.of("start-silent.sh"), root.resolve("start-silent.sh"));
        Files.copy(Path.of("stop-silent.sh"), root.resolve("stop-silent.sh"));
        Path fakeJava = root.resolve("bin/java");
        Files.createDirectories(fakeJava.getParent());
        Files.writeString(fakeJava, """
            #!/usr/bin/env bash
            printf '%s\\n' "$@" > "$AION_FAKE_JAVA_ARGS_FILE"
            exit 0
            """);
        fakeJava.toFile().setExecutable(true);
        return root;
    }

    private ProcessResult runScript(Path root, String script, Path javaArgs, String... args) throws Exception {
        String[] command = new String[args.length + 2];
        command[0] = "bash";
        command[1] = root.resolve(script).toString();
        System.arraycopy(args, 0, command, 2, args.length);

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.directory(root.toFile());
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().put("PATH", root.resolve("bin") + ":" + processBuilder.environment().get("PATH"));
        processBuilder.environment().put("AION_FAKE_JAVA_ARGS_FILE", javaArgs.toString());
        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes());
        boolean finished = process.waitFor(Duration.ofSeconds(10).toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new AssertionError(script + " did not finish. Output:\n" + output);
        }
        return new ProcessResult(process.exitValue(), output);
    }

    private List<String> waitForLines(Path file) throws Exception {
        long deadline = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (!Files.exists(file) && System.nanoTime() < deadline) {
            Thread.sleep(25);
        }
        assertTrue(Files.exists(file), "Missing fake java args file: " + file);
        return Files.readAllLines(file);
    }

    private record ProcessResult(int exitCode, String output) {
    }
}
