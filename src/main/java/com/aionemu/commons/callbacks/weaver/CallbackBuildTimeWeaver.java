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

/**
 * 回调编译期织入器，在构建阶段对 class 文件注入回调增强
 * Build-time callback weaver that injects callback enhancements into class files
 */
public final class CallbackBuildTimeWeaver {

	private static final CallbackClassFileTransformer[] TRANSFORMERS = {
		new ObjectCallbackEnhancer(),
		new GlobalCallbackEnhancer()
	};

	private CallbackBuildTimeWeaver() {
	}

	/**
	 * 命令行入口，对指定 classes 目录执行回调织入
	 * CLI entry point that weaves callbacks for the given classes directory
	 *
	 * @param args 参数，仅接受一个 classes 目录路径 / Arguments, expects a single classes directory path
	 * When weaving fails。 / When weaving fails.
	 */
	public static void main(String[] args) throws Exception {
		if (args.length != 1) {
			throw new IllegalArgumentException("Usage: CallbackBuildTimeWeaver <classes-directory>");
		}
		weaveClasses(Path.of(args[0]));
	}

	/**
	 * 遍历目录下全部 class 文件并执行织入
	 * Walk all class files under the directory and weave them
	 *
	 * Classes output directory
	 *
	 * @param classesDirectory @return 实际被修改的 class 数量 / Number of class files actually modified
	 * @return @throws Exception 织入或 IO 失败时 / When weaving or IO fails
	 */
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

	/**
	 * 对单个 class 字节码执行对象/全局回调增强
	 * Weave object/global callback enhancements into a single class bytecode
	 *
	 * @param loader 用于解析依赖的类加载器 / Class loader used to resolve dependencies
	 * @param classBytes 原始 class 字节码 / Original class bytecode
	 * @return 织入后的字节码；若无需修改则返回原数组 / Woven bytecode, or the original array when no change is needed
	 * When weaving fails。 / When weaving fails.
	 */
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

	/**
	 * 收集目录下全部 class 文件路径
	 * Collect all class file paths under the directory
	 *
	 * Classes directory
	 *
	 * @param classesDirectory @return 排序后的 class 文件路径 / Sorted class file paths
	 * @return @throws IOException 遍历目录失败时 / When directory walking fails
	 */
	private static Iterable<Path> classFiles(Path classesDirectory) throws IOException {
		try (var paths = Files.walk(classesDirectory)) {
			return paths
				.filter(path -> path.toString().endsWith(".class"))
				.sorted(Comparator.comparing(Path::toString))
				.toList();
		}
	}

	/**
	 * 检查 class 是否已经织入过回调增强
	 * Check whether the class has already been woven with callback enhancements
	 *
	 * Class loader
	 * Class bytecode
	 *
	 * @param loader @return 已织入返回 true / True if already woven
	 * @param classBytes @throws IOException 解析字节码失败时 / When bytecode inspection fails
	 */
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
