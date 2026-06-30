package com.aionemu.boot.callback;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CallbackWeavingBootstrapTest {

	@TempDir
	private Path tempDir;

	@Test
	void skipsClassesLoadedFromPackagedJarFileSystem() throws Exception {
		Path jarFile = tempDir.resolve("AionEmu.jar");
		URI jarUri = URI.create("jar:" + jarFile.toUri());
		try (FileSystem jarFs = FileSystems.newFileSystem(jarUri, Map.of("create", "true"))) {
			Path classesDirectory = jarFs.getPath("BOOT-INF/classes");
			Files.createDirectories(classesDirectory.resolve("com/aionemu/gameserver/utils/javaagent"));
			Files.write(classesDirectory.resolve("com/aionemu/gameserver/utils/javaagent/JavaAgentUtils.class"), new byte[] {0});

			Class<?> anchorClass = new CodeSourceClassLoader(
				URI.create(jarUri + "!/BOOT-INF/classes/").toURL(),
				anchorClassBytes()
			).loadClass(Anchor.class.getName());

			assertDoesNotThrow(() -> CallbackWeavingBootstrap.weaveExplodedClassesIfNeeded(anchorClass));
		}
	}

	private byte[] anchorClassBytes() throws Exception {
		String resourceName = Anchor.class.getName().replace('.', '/') + ".class";
		try (InputStream input = getClass().getClassLoader().getResourceAsStream(resourceName)) {
			return input.readAllBytes();
		}
	}

	private static final class Anchor {
	}

	private static final class CodeSourceClassLoader extends ClassLoader {
		private final URL codeSourceUrl;
		private final byte[] classBytes;

		private CodeSourceClassLoader(URL codeSourceUrl, byte[] classBytes) {
			super(CallbackWeavingBootstrapTest.class.getClassLoader());
			this.codeSourceUrl = codeSourceUrl;
			this.classBytes = classBytes;
		}

		@Override
		protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
			if (!name.equals(Anchor.class.getName())) {
				return super.loadClass(name, resolve);
			}
			Class<?> loadedClass = findLoadedClass(name);
			if (loadedClass == null) {
				CodeSource codeSource = new CodeSource(codeSourceUrl, (Certificate[]) null);
				ProtectionDomain protectionDomain = new ProtectionDomain(codeSource, null, this, null);
				loadedClass = defineClass(name, classBytes, 0, classBytes.length, protectionDomain);
			}
			if (resolve) {
				resolveClass(loadedClass);
			}
			return loadedClass;
		}
	}
}
