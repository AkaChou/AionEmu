package com.aionemu.gameserver.questEngine;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;

import com.aionemu.gameserver.configs.Config;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * 任务栈数据资源定位：外部运行目录优先，classpath 回退。
 * <p>
 * 打包运行的 jar 不包含 {@code aion/**}（pom 部署 profile 排除，运行期数据由 scripts/package.sh
 * 同步到 AION_HOME），此时数据只在外部目录；IDE 运行与单元测试则依赖 resources 源码树/classpath。
 * 外部目录经掉落等静态数据的同款访问器 {@code Config.dataFile/definitionFile} 解析，目录属性由 boot
 * 层 {@code AionServicePaths} 按启动方式设置：IDEA 检出运行指向 {@code src/main/resources/aion/**}，
 * 打包启动指向 {@code <aion.home>/**}。属性未设置的纯测试环境不启用外部分支，行为与直接 classpath
 * 读取一致；解析顺序与 {@code QuestDialogContract.load} 的生产布局优先约定一致。
 * <p>
 * Quest-stack data resource locator: the external runtime directory comes first and the classpath is the
 * fallback. A packaged jar ships no {@code aion/**} (excluded by the deployment pom profile; the data is
 * synced to AION_HOME by scripts/package.sh), so packaged runs only have the external directory, while IDE
 * runs and unit tests rely on the resources source tree / classpath. The external directory is resolved
 * through the same {@code Config.dataFile/definitionFile} accessors used by drop and static data, with the
 * directory properties configured by the boot layer ({@code AionServicePaths}): IDE checkout runs point at
 * {@code src/main/resources/aion/**} and packaged runs at {@code <aion.home>/**}. A plain test environment
 * without those properties skips the external branch and behaves exactly like a direct classpath read; the
 * lookup order matches the production-layout-first convention of {@code QuestDialogContract.load}.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class QuestDataResources {

	private static final String DATA_PREFIX = "aion/data/";
	private static final String DEFINITIONS_PREFIX = "aion/definitions/";

	/**
	 * 以本类的类加载器定位资源（默认入口）。
	 * Locates a resource with this class's loader (default entry).
	 * @param resourcePath classpath 资源路径 / classpath resource path
	 * @return 资源 URL；任何来源都缺失时为 null / resource URL, or null when no source has it
	 */
	public static URL url(String resourcePath) {
		return url(QuestDataResources.class.getClassLoader(), resourcePath);
	}

	/**
	 * 定位资源：外部运行目录优先，classpath 回退。
	 * Locates a resource: external runtime directory first, classpath fallback.
	 * @param loader classpath 查询使用的类加载器 / class loader used for the classpath lookup
	 * @param resourcePath classpath 资源路径（允许前导 /）/ classpath resource path (leading / allowed)
	 * @return 资源 URL；任何来源都缺失时为 null / resource URL, or null when no source has it
	 */
	public static URL url(ClassLoader loader, String resourcePath) {
		Objects.requireNonNull(loader, "loader");
		Objects.requireNonNull(resourcePath, "resourcePath");
		String path = resourcePath.startsWith("/") ? resourcePath.substring(1) : resourcePath;
		URL external = externalUrl(path);
		if (external != null) {
			return external;
		}
		return loader.getResource(path);
	}

	/**
	 * 以本类的类加载器打开资源流（默认入口）。
	 * Opens a resource stream with this class's loader (default entry).
	 * @param resourcePath classpath 资源路径 / classpath resource path
	 * @return 资源流；缺失或不可读时为 null / resource stream, or null when missing or unreadable
	 */
	public static InputStream open(String resourcePath) {
		return open(QuestDataResources.class.getClassLoader(), resourcePath);
	}

	/**
	 * 打开资源流：外部运行目录优先，classpath 回退；缺失或不可读时返回 null（与
	 * {@code ClassLoader.getResourceAsStream} 语义一致）。
	 * Opens a resource stream: external runtime directory first, classpath fallback; null when missing or
	 * unreadable (matching {@code ClassLoader.getResourceAsStream} semantics).
	 * @param loader classpath 查询使用的类加载器 / class loader used for the classpath lookup
	 * @param resourcePath classpath 资源路径（允许前导 /）/ classpath resource path (leading / allowed)
	 * @return 资源流；缺失或不可读时为 null / resource stream, or null when missing or unreadable
	 */
	public static InputStream open(ClassLoader loader, String resourcePath) {
		URL located = url(loader, resourcePath);
		if (located == null) {
			return null;
		}
		try {
			return located.openStream();
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * 按已知前缀经掉落同款访问器 {@code Config.dataFile/definitionFile} 把资源路径映射到外部运行目录；
	 * 前缀未知或文件不存在时返回 null。
	 * Maps the resource path onto the external runtime directory through the same
	 * {@code Config.dataFile/definitionFile} accessors used by drop and static data; null when the prefix is
	 * unknown or the file does not exist.
	 * @param path 无前导斜杠的资源路径 / resource path without a leading slash
	 * @return 外部文件 URL；不适用时为 null / external file URL, or null when not applicable
	 */
	private static URL externalUrl(String path) {
		File file;
		if (path.startsWith(DATA_PREFIX)) {
			file = Config.dataFile("./data/" + path.substring(DATA_PREFIX.length()));
		} else if (path.startsWith(DEFINITIONS_PREFIX)) {
			file = Config.definitionFile("./definitions/" + path.substring(DEFINITIONS_PREFIX.length()));
		} else {
			return null;
		}
		if (!file.exists()) {
			return null;
		}
		try {
			return file.toURI().toURL();
		} catch (MalformedURLException e) {
			return null;
		}
	}
}
