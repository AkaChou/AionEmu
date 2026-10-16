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

        ProcessResult result = runScript(root, "scripts/start-silent.sh", javaArgs);

        assertEquals(0, result.exitCode(), result.output());
        assertTrue(Files.exists(marker));
        assertTrue(Files.exists(root.resolve("aion/log/aionemu.pid")));
        assertTrue(result.output().contains("查看日志: tail -f \"" + root.resolve("aion/log/aionemu.log") + "\""));
        List<String> args = waitForLines(javaArgs);
        assertTrue(args.contains("-Daion.home=" + root.resolve("aion")));
        assertTrue(args.contains("-Daion.log.dir=" + root.resolve("aion/log")));
        assertTrue(args.contains("-jar"));
        assertTrue(args.contains(root.resolve("aion/AionEmu.jar").toString()));
    }

    @Test
    void startSilentCleanOptionRemovesAionDirectoryBeforeStarting() throws Exception {
        Path root = prepareRuntimeRoot();
        Path marker = root.resolve("aion/game/marker.txt");
        Files.createDirectories(marker.getParent());
        Files.writeString(marker, "remove");
        Path javaArgs = root.resolve("java-args.txt");

        ProcessResult result = runScript(root, "scripts/start-silent.sh", javaArgs, "-c");

        assertEquals(0, result.exitCode(), result.output());
        assertFalse(Files.exists(marker));
        assertTrue(Files.exists(root.resolve("aion/log/aionemu.pid")));
    }

    @Test
    void startSilentCopiesMissingRuntimeResourcesWithoutOverwritingExistingFiles() throws Exception {
        Path root = prepareRuntimeRoot();
        Path sourceGameConfig = root.resolve("src/main/resources/aion/config/main/gameserver.properties");
        Path sourceLoginConfig = root.resolve("src/main/resources/aion/config/login/database.properties");
        Path sourceLogback = root.resolve("src/main/resources/logback-spring.xml");
        Files.createDirectories(sourceGameConfig.getParent());
        Files.createDirectories(sourceLoginConfig.getParent());
        Files.writeString(sourceGameConfig, "default-game");
        Files.writeString(sourceLoginConfig, "default-login");
        Files.writeString(sourceLogback, "default-logback");
        Path runtimeGameConfig = root.resolve("aion/config/main/gameserver.properties");
        Files.createDirectories(runtimeGameConfig.getParent());
        Files.writeString(runtimeGameConfig, "custom-game");

        ProcessResult result = runScript(root, "scripts/start-silent.sh", root.resolve("java-args.txt"));

        assertEquals(0, result.exitCode(), result.output());
        assertEquals("custom-game", Files.readString(runtimeGameConfig));
        assertEquals("default-login", Files.readString(root.resolve("aion/config/login/database.properties")));
        assertEquals("default-logback", Files.readString(root.resolve("aion/log/logback-spring.xml")));
    }

    @Test
    void packageCopiesGeoJarAndScriptsIntoAionDirectory() throws Exception {
        Path root = prepareRuntimeRoot();
        Path sourceGeo = root.resolve("src/main/resources/aion/geo/100.geo.gz");
        Path sourcePath = root.resolve("src/main/resources/aion/geo/path/100.path.gz");
        Path sourceIndex = root.resolve("src/main/resources/aion/geo/path/100.idx");
        Path sourceConfig = root.resolve("src/main/resources/aion/config/main/gameserver.properties");
        Files.createDirectories(sourceGeo.getParent());
        Files.createDirectories(sourcePath.getParent());
        Files.createDirectories(sourceConfig.getParent());
        Files.writeString(sourceGeo, "geo");
        Files.writeString(sourcePath, "path");
        Files.writeString(sourceIndex, "index");
        Files.writeString(sourceConfig, "default-config");
        Path runtimeConfig = root.resolve("aion/config/main/gameserver.properties");
        Files.createDirectories(runtimeConfig.getParent());
        Files.writeString(runtimeConfig, "custom-config");
        Path sourcePatch = root.resolve("patch/bin64/Game.dll");
        Files.createDirectories(sourcePatch.getParent());
        Files.writeString(sourcePatch, "patch-binary");

        ProcessResult result = runScript(root, "scripts/package.sh", root.resolve("java-args.txt"));

        assertEquals(0, result.exitCode(), result.output());
        assertEquals("jar", Files.readString(root.resolve("aion/AionEmu.jar")));
        assertEquals("patch-binary", Files.readString(root.resolve("aion/patch/bin64/Game.dll")));
        assertEquals("geo", Files.readString(root.resolve("aion/geo/100.geo.gz")));
        assertEquals("path", Files.readString(root.resolve("aion/geo/path/100.path.gz")));
        assertEquals("index", Files.readString(root.resolve("aion/geo/path/100.idx")));
        assertEquals("default-config", Files.readString(runtimeConfig));
        assertTrue(Files.isExecutable(root.resolve("aion/start-silent.sh")));
        assertTrue(Files.isExecutable(root.resolve("aion/stop-silent.sh")));
        assertTrue(Files.isExecutable(root.resolve("aion/shutdown.sh")));
        assertTrue(Files.exists(root.resolve("aion/windows/deploy.bat")));
        assertTrue(Files.exists(root.resolve("aion/windows/server.ps1")));
        assertTrue(result.output().contains("Start: ./aion/start-silent.sh"));
        assertTrue(result.output().contains("Shutdown: ./aion/shutdown.sh"));
        assertTrue(result.output().contains("Stop:  ./aion/stop-silent.sh"));

        ProcessResult startResult = runScript(root, "aion/start-silent.sh", root.resolve("java-args.txt"));
        assertEquals(0, startResult.exitCode(), startResult.output());
        assertTrue(waitForLines(root.resolve("java-args.txt")).contains(root.resolve("aion/AionEmu.jar").toString()));

        ProcessResult stopResult = runScript(root, "aion/stop-silent.sh", root.resolve("java-args.txt"));
        assertEquals(0, stopResult.exitCode(), stopResult.output());
        assertFalse(Files.exists(root.resolve("aion/log/aionemu.pid")));
    }

    @Test
    void rePackageKeepsExistingConfigAndCopiesMissingConfig() throws Exception {
        Path root = prepareRuntimeRoot();
        Path sourceGameConfig = root.resolve("src/main/resources/aion/config/main/gameserver.properties");
        Path sourceLoginConfig = root.resolve("src/main/resources/aion/config/login/database.properties");
        Path sourceGeo = root.resolve("src/main/resources/aion/geo/100.geo.gz");
        Path sourcePath = root.resolve("src/main/resources/aion/geo/path/100.path.gz");
        Path sourceIndex = root.resolve("src/main/resources/aion/geo/path/100.idx");
        Path sourceLogback = root.resolve("src/main/resources/logback-spring.xml");
        Files.createDirectories(sourceGameConfig.getParent());
        Files.createDirectories(sourceLoginConfig.getParent());
        Files.createDirectories(sourceGeo.getParent());
        Files.createDirectories(sourcePath.getParent());
        Files.writeString(sourceGameConfig, "default-game");
        Files.writeString(sourceLoginConfig, "default-login");
        Files.writeString(sourceGeo, "new-geo");
        Files.writeString(sourcePath, "path");
        Files.writeString(sourceIndex, "index");
        Files.writeString(sourceLogback, "default-logback");
        Path runtimeGameConfig = root.resolve("aion/config/main/gameserver.properties");
        Path runtimeLogback = root.resolve("aion/log/logback-spring.xml");
        Files.createDirectories(runtimeGameConfig.getParent());
        Files.createDirectories(runtimeLogback.getParent());
        Files.writeString(runtimeGameConfig, "custom-game");
        Files.writeString(runtimeLogback, "custom-logback");

        ProcessResult result = runScript(root, "scripts/re-package.sh", root.resolve("java-args.txt"));

        assertEquals(0, result.exitCode(), result.output());
        assertEquals("custom-game", Files.readString(runtimeGameConfig));
        assertEquals("default-login", Files.readString(root.resolve("aion/config/login/database.properties")));
        assertEquals("new-geo", Files.readString(root.resolve("aion/geo/100.geo.gz")));
        assertEquals("custom-logback", Files.readString(runtimeLogback));
        assertEquals("jar", Files.readString(root.resolve("aion/AionEmu.jar")));
    }

    @Test
    void stopSilentUsesAionLogPidByDefault() throws Exception {
        Path root = prepareRuntimeRoot();
        Path pidFile = root.resolve("aion/log/aionemu.pid");
        Files.createDirectories(pidFile.getParent());
        Files.writeString(pidFile, "99999999");

        ProcessResult result = runScript(root, "scripts/stop-silent.sh", root.resolve("java-args.txt"));

        assertEquals(0, result.exitCode(), result.output());
        assertFalse(Files.exists(pidFile));
    }

    @Test
    void shutdownRequestsGracefulSignalWithoutForceKill() throws Exception {
        String script = Files.readString(Path.of("scripts/shutdown.sh"));

        assertTrue(script.contains("kill \"$PID\""));
        assertFalse(script.contains("kill -9"));
    }

    @Test
    void windowsScriptsUseCrlfAndDriveThePortableRuntime() throws Exception {
        for (String name : List.of("deploy.bat", "stop.bat", "start-mysql.bat", "stop-mysql.bat")) {
            String content = Files.readString(Path.of("scripts/windows", name));
            assertTrue(content.contains("\r\n"), name + " must use CRLF line endings");
            assertTrue(content.contains("pause"), name + " must pause so the window stays open");
        }

        String ps1 = Files.readString(Path.of("scripts/windows/server.ps1"));
        assertTrue(ps1.contains("-Daion.home="));
        assertTrue(ps1.contains("-Daion.log.dir="));
        assertTrue(ps1.contains("--initialize-insecure"));
        assertTrue(ps1.contains("aion_emu"));
        assertTrue(ps1.contains("database.properties"));
        assertTrue(ps1.contains("network.properties"));
        assertTrue(ps1.contains("Configure-NetworkAddress"));
        assertTrue(ps1.contains("Configure-CountryCode"));
        // 连接 IP 只问一次：回车用 127.0.0.1，输入即采用；无"是否修改"二次确认。
        // One single prompt for the server IP: ENTER keeps 127.0.0.1, the typed value is used as-is.
        assertFalse(ps1.contains("是否需要修改"));
        // 连接 IP 为 IPv4；直接回车默认 127.0.0.1（本机单机部署）。
        // The server IP is an IPv4; pressing ENTER defaults to 127.0.0.1 (local play).
        assertTrue(ps1.contains("Test-IPv4"));
        assertTrue(ps1.contains("$DefaultIp = '127.0.0.1'"));
        assertTrue(ps1.contains("Send-CtrlC"));
        assertTrue(ps1.contains("--init-file="));
        // 便携 MySQL 监听 0.0.0.0（my.ini），远程客户端可直连 / portable MySQL binds 0.0.0.0 via my.ini.
        assertTrue(ps1.contains("my.ini"));
        assertTrue(ps1.contains("bind-address=0.0.0.0"));
        // 前台运行 + 已在运行时说明原因 / foreground run + explain-and-exit when already running.
        assertTrue(ps1.contains("-NoNewWindow"));
        assertTrue(ps1.contains("已在运行"));
        // start-mysql 需拦截 Ctrl+C 作为控制台输入以触发 finally 停止 MySQL / treat Ctrl+C as input to trigger finally.
        assertTrue(ps1.contains("TreatControlCAsInput"));
        // 国内优先：JRE 走 TUNA，MySQL 走华为云 / China-first mirrors for JRE and MySQL.
        assertTrue(ps1.contains("mirrors.tuna.tsinghua.edu.cn"));
        assertTrue(ps1.contains("mirrors.huaweicloud.com"));

        // Windows PowerShell 5.1 对无 BOM 的 .ps1 按系统 ANSI 解码，中文会乱码，故必须带 BOM。
        // Windows PowerShell 5.1 decodes a BOM-less .ps1 as ANSI, garbling Chinese; a BOM is required.
        byte[] head = Files.readAllBytes(Path.of("scripts/windows/server.ps1"));
        assertTrue(head.length > 3 && head[0] == (byte) 0xEF && head[1] == (byte) 0xBB && head[2] == (byte) 0xBF,
            "server.ps1 must start with a UTF-8 BOM");
    }

    private Path prepareRuntimeRoot() throws IOException {
        Path root = tempDir.resolve("runtime");
        Files.createDirectories(root.resolve("target"));
        Files.writeString(root.resolve("target/AionEmu.jar"), "jar");
        Files.createDirectories(root.resolve("aion"));
        Files.writeString(root.resolve("aion/AionEmu.jar"), "jar");
        Files.createDirectories(root.resolve("scripts"));
        Files.copy(Path.of("scripts/package.sh"), root.resolve("scripts/package.sh"));
        Files.copy(Path.of("scripts/re-package.sh"), root.resolve("scripts/re-package.sh"));
        Files.copy(Path.of("scripts/start-silent.sh"), root.resolve("scripts/start-silent.sh"));
        Files.copy(Path.of("scripts/stop-silent.sh"), root.resolve("scripts/stop-silent.sh"));
        Files.copy(Path.of("scripts/shutdown.sh"), root.resolve("scripts/shutdown.sh"));
        Path sourceWindows = Path.of("scripts/windows");
        Path targetWindows = root.resolve("scripts/windows");
        Files.createDirectories(targetWindows);
        try (var windowsFiles = Files.list(sourceWindows)) {
            for (Path file : windowsFiles.toList()) {
                Files.copy(file, targetWindows.resolve(file.getFileName()));
            }
        }
        Files.createDirectories(root.resolve("patch"));
        Path fakeJava = root.resolve("bin/java");
        Files.createDirectories(fakeJava.getParent());
        Files.writeString(fakeJava, """
            #!/usr/bin/env bash
            printf '%s\\n' "$@" > "$AION_FAKE_JAVA_ARGS_FILE"
            exit 0
            """);
        fakeJava.toFile().setExecutable(true);
        Path fakeMaven = root.resolve("bin/mvn");
        Files.writeString(fakeMaven, "#!/usr/bin/env bash\nexit 0\n");
        fakeMaven.toFile().setExecutable(true);
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
