package com.aionemu.boot.callback;

import com.aionemu.commons.callbacks.weaver.CallbackBuildTimeWeaver;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.experimental.UtilityClass;

/**
 * Boot 启动前的回调字节码织入引导工具。
 * Bootstrap helper that weaves callback bytecode before Boot starts.
 */
@UtilityClass
public class CallbackWeavingBootstrap {

    private final Path CALLBACK_SUPPORT_CLASS = Path.of(
        "com",
        "aionemu",
        "gameserver",
        "utils",
        "javaagent",
        "JavaAgentUtils.class"
    );

    /**
     * 若锚点类位于可写的 exploded classes 目录且存在回调支持类，则执行编译期织入。
     * Weaves build-time callbacks when the anchor class lives in a writable exploded classes directory
     * that contains the callback support class.
     *
     * @param anchorClass 用于定位 classes 目录的锚点类 / anchor class used to locate the classes directory
     */
    public void weaveExplodedClassesIfNeeded(Class<?> anchorClass) {
        try {
            Path classesDirectory = classesDirectory(anchorClass);
            if (classesDirectory == null || !Files.isRegularFile(classesDirectory.resolve(CALLBACK_SUPPORT_CLASS))) {
                return;
            }
            CallbackBuildTimeWeaver.weaveClasses(classesDirectory);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to prepare callback bytecode weaving for boot startup.", e);
        }
    }

    /**
     * 解析锚点类对应的 exploded classes 目录；非默认文件系统或 jar 场景返回 null。
     * Resolves the exploded classes directory for the anchor class; returns null for jars or non-default filesystems.
     *
     * @param anchorClass 锚点类 / anchor class
     * @return classes 目录路径，不可用时为 null / classes directory path, or null when unavailable
     * @throws Exception 解析代码源位置失败时抛出 / thrown when the code-source location cannot be resolved
     */
    private Path classesDirectory(Class<?> anchorClass) throws Exception {
        if (anchorClass.getProtectionDomain().getCodeSource() == null) {
            return null;
        }
        URI location = anchorClass.getProtectionDomain().getCodeSource().getLocation().toURI();
        Path path = Path.of(location);
        if (!path.getFileSystem().equals(FileSystems.getDefault())) {
            return null;
        }
        return Files.isDirectory(path) ? path : null;
    }
}
