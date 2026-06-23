package com.aionemu.commons.callbacks.weaver;

import com.aionemu.commons.callbacks.EnhancedObject;
import com.aionemu.commons.callbacks.enhancer.CallbackClassFileTransformer;
import com.aionemu.commons.callbacks.enhancer.GlobalCallbackEnhancer;
import com.aionemu.commons.callbacks.enhancer.ObjectCallbackEnhancer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.LoaderClassPath;

public final class CallbackBuildTimeWeaver {

	private static final CallbackClassFileTransformer[] TRANSFORMERS = {
		new ObjectCallbackEnhancer(),
		new GlobalCallbackEnhancer()
	};

	private CallbackBuildTimeWeaver() {
	}

	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			throw new IllegalArgumentException("Usage: CallbackBuildTimeWeaver <classes-directory>");
		}
		weaveClasses(Path.of(args[0]));
	}

	public static int weaveClasses(Path classesDirectory) throws Exception {
		if (!Files.isDirectory(classesDirectory)) {
			throw new IllegalArgumentException("Classes directory does not exist: " + classesDirectory);
		}

		try (URLClassLoader loader = new URLClassLoader(new URL[] {classesDirectory.toUri().toURL()},
			Thread.currentThread().getContextClassLoader())) {
			int wovenClasses = 0;
			for (Path classFile : classFiles(classesDirectory)) {
				byte[] originalBytes = Files.readAllBytes(classFile);
				byte[] wovenBytes = weaveClassBytes(loader, originalBytes);
				if (wovenBytes != originalBytes) {
					Files.write(classFile, wovenBytes);
					wovenClasses++;
				}
			}
			return wovenClasses;
		}
	}

	public static byte[] weaveClassBytes(ClassLoader loader, byte[] classBytes) throws Exception {
		if (isAlreadyWoven(loader, classBytes)) {
			return classBytes;
		}

		byte[] wovenBytes = classBytes;
		for (CallbackClassFileTransformer transformer : TRANSFORMERS) {
			byte[] transformedBytes = transformer.transform(loader, null, null, null, wovenBytes);
			if (transformedBytes != null) {
				wovenBytes = transformedBytes;
			}
		}
		return wovenBytes;
	}

	private static Iterable<Path> classFiles(Path classesDirectory) throws IOException {
		try (var paths = Files.walk(classesDirectory)) {
			return paths
				.filter(path -> path.toString().endsWith(".class"))
				.sorted(Comparator.comparing(Path::toString))
				.toList();
		}
	}

	private static boolean isAlreadyWoven(ClassLoader loader, byte[] classBytes) throws IOException {
		ClassPool classPool = new ClassPool();
		classPool.appendClassPath(new LoaderClassPath(loader));
		CtClass clazz = null;
		try {
			clazz = classPool.makeClass(new ByteArrayInputStream(classBytes));
			for (CtClass interfaceType : clazz.getInterfaces()) {
				if (interfaceType.getName().equals(EnhancedObject.class.getName())) {
					return true;
				}
			}
			for (CtField field : clazz.getDeclaredFields()) {
				if (field.getName().startsWith("$$$")) {
					return true;
				}
			}
			return false;
		} catch (Exception e) {
			throw new IOException("Failed to inspect class before callback weaving.", e);
		} finally {
			if (clazz != null) {
				clazz.detach();
			}
		}
	}
}
