package com.aionemu.boot.web;

import java.nio.file.Files;
import java.nio.file.Path;
import lombok.experimental.UtilityClass;
import org.springframework.core.env.Environment;

/**
 * 管理控制台侧的配置目录解析：与启动层的判定规则保持一致。
 * Configuration directory resolution for the admin console, following the same rules as the bootstrap layer.
 * <p>
 * 显式路径优先；未设置 {@code aion.home}（本地 IDE/检出运行）时使用源码树
 * {@code src/main/resources/aion/config}；其余情况使用 {@code <aion.home>/config}。
 * <p>
 * An explicit path wins; without {@code aion.home} (a local IDE or checkout run) the source tree
 * {@code src/main/resources/aion/config} is used; otherwise {@code <aion.home>/config}.
 */
@UtilityClass
public class AdminConsoleConfigDir {

	/**
	 * 解析配置根目录。
	 * Resolves the configuration root directory.
	 * @return 配置根目录 / configuration root directory
	 */
	public Path resolve() {
		return resolve(System.getProperty("aion.config.dir"), System.getProperty("aion.home"));
	}

	/**
	 * 从 {@code Environment} 解析配置根目录。
	 * <p>
	 * 环境后处理器运行在 {@code aion.config.dir} 系统属性被设置之前（那发生在服务生命周期阶段），
	 * 因此此处从 {@code Environment} 取值，与遗留属性后处理器的判定规则保持一致。
	 * <p>
	 * Resolves the configuration root directory from the {@code Environment}.
	 * <p>
	 * The environment post-processors run before the {@code aion.config.dir} system property is set (that
	 * happens during the service lifecycle phase), so the value is taken from the {@code Environment} here,
	 * matching the rules of the legacy property post-processor.
	 * @param environment 环境 / environment
	 * @return 配置根目录 / configuration root directory
	 */
	public Path resolve(Environment environment) {
		return resolve(environment.getProperty("aion.config.dir"), environment.getProperty("aion.home"));
	}

	/**
	 * 解析配置根目录。
	 * Resolves the configuration root directory.
	 * @param configuredDir 显式配置目录 / explicitly configured directory
	 * @param aionHome 部署根目录 / deployment root directory
	 * @return 配置根目录 / configuration root directory
	 */
	private Path resolve(String configuredDir, String aionHome) {
		if (configuredDir != null) {
			return Path.of(configuredDir);
		}
		if (aionHome == null) {
			Path sourceDirectory = Path.of("src/main/resources/aion/config");
			if (Files.isDirectory(sourceDirectory)) {
				return sourceDirectory;
			}
		}
		return Path.of(aionHome == null ? "aion" : aionHome).resolve("config");
	}
}
