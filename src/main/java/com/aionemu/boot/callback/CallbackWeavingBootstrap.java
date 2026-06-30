package com.aionemu.boot.callback;

import com.aionemu.commons.callbacks.weaver.CallbackBuildTimeWeaver;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CallbackWeavingBootstrap {

    private static final Path CALLBACK_SUPPORT_CLASS = Path.of(
        "com",
        "aionemu",
        "gameserver",
        "utils",
        "javaagent",
        "JavaAgentUtils.class"
    );

    private CallbackWeavingBootstrap() {
    }

    public static void weaveExplodedClassesIfNeeded(Class<?> anchorClass) {
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

    private static Path classesDirectory(Class<?> anchorClass) throws Exception {
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
