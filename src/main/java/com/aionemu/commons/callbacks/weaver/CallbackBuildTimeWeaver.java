package com.aionemu.commons.callbacks.weaver;

import com.aionemu.commons.callbacks.EnhancedObject;
import com.aionemu.commons.callbacks.enhancer.CallbackClassFileTransformer;
import com.aionemu.commons.callbacks.enhancer.GlobalCallbackEnhancer;
import com.aionemu.commons.callbacks.enhancer.ObjectCallbackEnhancer;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
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
	private static final byte[] OBJECT_CALLBACK_DESCRIPTOR =
		"Lcom/aionemu/commons/callbacks/metadata/ObjectCallback;".getBytes(StandardCharsets.ISO_8859_1);
	private static final byte[] GLOBAL_CALLBACK_DESCRIPTOR =
		"Lcom/aionemu/commons/callbacks/metadata/GlobalCallback;".getBytes(StandardCharsets.ISO_8859_1);

	private CallbackBuildTimeWeaver() {
	}

	/**
	 * 命令行入口，对指定 classes 目录执行回调织入
	 * CLI entry point that weaves callbacks for the given classes directory
	 *
	 * @param args 参数，仅接受一个 classes 目录路径 / Arguments, expects a single classes directory path
	 * @throws Exception 织入失败时抛出 / When weaving fails
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
	 * @param classesDirectory classes 输出目录 / classes output directory
	 * @return 实际被修改的 class 数量 / Number of class files actually modified
	 * @throws Exception 织入或 IO 失败时 / When weaving or IO fails
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
	 * @throws Exception 织入失败时抛出 / When weaving fails
	 */
	public static byte[] weaveClassBytes(ClassLoader loader, byte[] classBytes) throws Exception {
		if (!containsCallbackDescriptor(classBytes)) {
			return classBytes;
		}
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
	 * 通过 class 常量池中的注解描述符筛选候选类，避免对普通类创建 Javassist 模型。
	 * Uses annotation descriptors in the class constant pool to skip ordinary classes before creating Javassist models.
	 *
	 * @param classBytes class 字节码 / class bytecode
	 * @return 可能包含回调注解返回 true / true when callback metadata may be present
	 */
	private static boolean containsCallbackDescriptor(byte[] classBytes) {
		return contains(classBytes, OBJECT_CALLBACK_DESCRIPTOR) || contains(classBytes, GLOBAL_CALLBACK_DESCRIPTOR);
	}

	/**
	 * 在 class 字节数组中查找固定字节序列。
	 * Finds a fixed byte sequence in a class byte array.
	 *
	 * @param source 源字节数组 / source bytes
	 * @param target 待查找字节序列 / byte sequence to find
	 * @return 找到时返回 true / true when found
	 */
	private static boolean contains(byte[] source, byte[] target) {
		if (target.length == 0 || source.length < target.length) {
			return false;
		}
		for (int sourceIndex = 0; sourceIndex <= source.length - target.length; sourceIndex++) {
			int targetIndex = 0;
			while (targetIndex < target.length && source[sourceIndex + targetIndex] == target[targetIndex]) {
				targetIndex++;
			}
			if (targetIndex == target.length) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 收集目录下全部 class 文件路径
	 * Collect all class file paths under the directory
	 *
	 * @param classesDirectory classes 目录 / classes directory
	 * @return 排序后的 class 文件路径 / Sorted class file paths
	 * @throws IOException 遍历目录失败时 / When directory walking fails
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
	 * @param loader 类加载器 / Class loader
	 * @param classBytes class 字节码 / Class bytecode
	 * @return 已织入返回 true / True if already woven
	 * @throws IOException 解析字节码失败时 / When bytecode inspection fails
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
