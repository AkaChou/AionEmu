package com.aionemu.boot.config;

import java.nio.file.Path;
import java.util.Map;
import com.aionemu.boot.i18n.I18n;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.stereotype.Component;

/**
 * 配置源刷新器：重新读取磁盘配置目录并重建 {@code Environment} 中的两个属性源。
 * Configuration source refresher: re-reads the disk-backed configuration directories and rebuilds the two
 * property sources inside the {@code Environment}.
 * <p>
 * 后台控制台改写配置文件后必须调用本刷新器，否则 {@code Environment} 仍持有启动时的快照，遗留装配器
 * 查询 {@code Environment} 时会取到旧值，导致「已保存但未生效」。
 * <p>
 * The admin console must invoke this refresher after rewriting a configuration file; otherwise the
 * {@code Environment} still holds the boot-time snapshot, the legacy assembler resolves the stale value and
 * the change silently fails to take effect.
 * <p>
 * <b>线程安全</b>：刷新以「替换整个属性源」的方式原子完成，读者要么看到旧快照要么看到新快照。
 * <b>Thread safety</b>: a refresh replaces the whole property source, so a reader sees either the old
 * snapshot or the new one, never a mixture.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AionConfigSourceRefresher {

	/**
	 * 应用环境。
	 * Application environment.
	 */
	private final ConfigurableEnvironment environment;

	/**
	 * 重新读取磁盘配置并重建裸键属性源与带前缀的遗留属性源。
	 * <p>
	 * 重建后 {@code Environment} 与磁盘内容重新一致，遗留装配器因而能取到刚写入的值。
	 * <p>
	 * Re-reads the disk-backed configuration and rebuilds both the raw-key property source and the prefixed
	 * legacy property source.
	 * <p>
	 * After the rebuild the {@code Environment} agrees with the files on disk again, so the legacy assembler
	 * resolves the values that were just written.
	 * @param configDir 配置根目录 / configuration root directory
	 */
	public void refresh(Path configDir) {
		MutablePropertySources sources = environment.getPropertySources();

		AionConfigFilePropertySource rawSource =
			new AionConfigFilePropertySource(AionConfigFilePropertySource.readEntries(configDir));
		replace(sources, rawSource);

		Map<String, Object> legacyEntries =
			AionLegacyPropertySourceEnvironmentPostProcessor.collectLegacyEntries(configDir);
		if (legacyEntries.isEmpty()) {
			sources.remove(AionLegacyPropertySourceEnvironmentPostProcessor.PROPERTY_SOURCE_NAME);
		} else {
			replace(sources, new MapPropertySource(
				AionLegacyPropertySourceEnvironmentPostProcessor.PROPERTY_SOURCE_NAME, legacyEntries));
		}

		log.debug(I18n.get("console.web.config_refreshed", configDir.toString()));
	}

	/**
	 * 以最低优先级替换指定名称的属性源。
	 * Replaces the property source with the given name at the lowest precedence.
	 * @param sources 属性源集合 / property sources
	 * @param source 新属性源 / new property source
	 */
	private void replace(MutablePropertySources sources, org.springframework.core.env.PropertySource<?> source) {
		sources.remove(source.getName());
		sources.addLast(source);
	}
}
