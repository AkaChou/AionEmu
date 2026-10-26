package com.aionemu.boot.web.config;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import com.aionemu.boot.config.AionConfigSourceRefresher;
import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.configuration.ConfigurableProcessor;
import com.aionemu.commons.utils.PropertiesUtils;
import com.aionemu.gameserver.configs.Config;
import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.configs.administration.DeveloperConfig;
import com.aionemu.gameserver.configs.administration.PanelConfig;
import com.aionemu.gameserver.network.PacketFloodFilter;
import com.aionemu.gameserver.services.AdminService;
import com.aionemu.gameserver.utils.chathandlers.ChatProcessor;
import com.aionemu.loginserver.configs.VipConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 管理控制台的配置读写服务：读取可编辑文件、预览差异、带回滚地写入并定向热应用。
 * Admin console configuration service: reads the editable files, previews the diff, writes with rollback
 * and applies the change in a targeted way.
 * <p>
 * <b>写入不落盘前先备份</b>，热应用失败时把文件恢复原样并再应用一次旧值，保证服务端永远不会停在
 * 「文件已改、内存未改」的分裂状态。
 * <p>
 * <b>A backup is taken before writing</b>; when the hot-apply fails the file is restored and the old values
 * are applied again, so the server never ends up in a split state where the file changed but the memory did
 * not.
 * <p>
 * 热应用是<b>定向</b>的：只重跑被改文件对应的配置装配或服务重载，绝不调用完整的
 * {@code Config.reload()}，避免连带刷新副本、攻城与日常调度等二十余个调度器。
 * <p>
 * The hot-apply is <b>targeted</b>: only the configuration assembly or service reload matching the edited
 * file runs; the full {@code Config.reload()} is never called, so the two dozen instance, siege and daily
 * schedulers are left untouched.
 *
 * @author AionEmu
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminConfigService {

	/**
	 * 配置根目录。
	 * Configuration root directory.
	 */
	private final Path configDir;

	/**
	 * 配置源刷新器：改盘后重建 {@code Environment} 中的属性源。
	 * Configuration source refresher: rebuilds the property sources in the {@code Environment} after a write.
	 */
	private final AionConfigSourceRefresher sourceRefresher;

	/**
	 * 生效值解析器：改盘后需重读运维覆盖层，使「生效值」与「覆盖来源」保持准确。
	 * Effective value resolver: the operator override layer has to be re-read after a write so the effective
	 * values and their sources stay accurate.
	 */
	private final EffectiveValueResolver valueResolver;

	/**
	 * 读取可编辑文件的内容与元信息。
	 * Reads the content and metadata of an editable file.
	 * @param file 可编辑文件 / editable file
	 * @return 文件视图 / file view
	 */
	public FileView read(EditableFile file) {
		Path path = configDir.resolve(file.relativePath());
		if (!Files.isRegularFile(path)) {
			throw new IllegalArgumentException("Configuration file is missing: " + file.relativePath());
		}
		String text = readText(path);
		Map<String, String> values = file.format() == EditableFile.Format.PROPERTIES
			? PropertiesDocument.parse(text).values()
			: parseItemIdList(text);
		Map<String, ConfigKeyComment> comments = file.format() == EditableFile.Format.PROPERTIES
			? ConfigCommentParser.parse(text)
			: Map.of();
		return new FileView(file, values, comments, text);
	}

	/**
	 * 生成改动后的差异预览，不落盘。
	 * Builds the diff preview of a change without touching the disk.
	 * @param file 可编辑文件 / editable file
	 * @param changes 键值改动 / key-value changes
	 * @return 差异预览 / diff preview
	 */
	public DiffPreview preview(EditableFile file, Map<String, String> changes) {
		FileView view = read(file);
		String updated = applyChanges(file, view.text(), changes);
		List<DiffLine> diff = diff(view.text(), updated);
		return new DiffPreview(file, diff, changedKeys(view.values(), changes));
	}

	/**
	 * 写入改动并定向热应用；失败时回滚到写入前的状态。
	 * Writes the change and applies it in a targeted way; rolls back to the pre-write state on failure.
	 * @param file 可编辑文件 / editable file
	 * @param changes 键值改动 / key-value changes
	 * @return 应用结果 / apply result
	 */
	public ApplyResult apply(EditableFile file, Map<String, String> changes) {
		Path path = configDir.resolve(file.relativePath());
		if (!Files.isRegularFile(path)) {
			throw new IllegalArgumentException("Configuration file is missing: " + file.relativePath());
		}
		String original = readText(path);
		String updated = applyChanges(file, original, changes);
		if (updated.equals(original)) {
			return new ApplyResult(file, true, false, Map.of());
		}

		Path backup = writeBackup(path);
		try {
			Files.writeString(path, updated, StandardCharsets.UTF_8);
			sourceRefresher.refresh(configDir);
			valueResolver.reloadOverrideLayer();
			applyTarget(file);
		} catch (Exception e) {
			log.error(I18n.get("console.web.reload_failed", file.relativePath(), e.getMessage()), e);
			rollback(path, backup, file);
			return new ApplyResult(file, false, true, changedKeys(readValues(file), changes));
		} finally {
			deleteQuietly(backup);
		}
		return new ApplyResult(file, true, false, changedKeys(readValues(file), changes));
	}

	/**
	 * 回滚：把文件恢复为写入前的内容并重新应用旧值。
	 * Rolls back: restores the file to its pre-write content and applies the old values again.
	 * @param path 目标文件 / target file
	 * @param backup 备份文件 / backup file
	 * @param file 可编辑文件 / editable file
	 */
	private void rollback(Path path, Path backup, EditableFile file) {
		try {
			Files.copy(backup, path, StandardCopyOption.REPLACE_EXISTING);
			sourceRefresher.refresh(configDir);
			valueResolver.reloadOverrideLayer();
			applyTarget(file);
			log.warn(I18n.get("console.web.rollback_done", file.relativePath()));
		} catch (Exception rollbackFailure) {
			log.error(I18n.get("console.web.reload_failed", file.relativePath(), rollbackFailure.getMessage()), rollbackFailure);
		}
	}

	/**
	 * 写备份文件。
	 * Writes the backup file.
	 * @param path 目标文件 / target file
	 * @return 备份文件路径 / backup file path
	 */
	private Path writeBackup(Path path) {
		try {
			Path backup = Files.createTempFile(path.getParent(), "admin-console-", ".bak");
			Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
			return backup;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * 删除临时文件，失败不抛。
	 * Deletes a temporary file, ignoring failures.
	 * @param path 文件路径 / file path
	 */
	private void deleteQuietly(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			log.debug("Failed to delete the admin console backup " + path, e);
		}
	}

	/**
	 * 按改动生成新文本。
	 * Produces the new text from the changes.
	 * @param file 可编辑文件 / editable file
	 * @param original 原始文本 / original text
	 * @param changes 键值改动 / key-value changes
	 * @return 新文本 / new text
	 */
	private String applyChanges(EditableFile file, String original, Map<String, String> changes) {
		if (file.format() == EditableFile.Format.ITEM_ID_LIST) {
			return applyItemIdChanges(original, changes);
		}
		PropertiesDocument document = PropertiesDocument.parse(original);
		for (Map.Entry<String, String> change : changes.entrySet()) {
			document = document.withAllValues(change.getKey(), change.getValue());
		}
		return document.render();
	}

	/**
	 * 按改动生成新的物品 ID 列表文本：只替换出现在列表中的 ID。
	 * Produces the new item id list text from the changes: only ids present in the list are replaced.
	 * @param original 原始文本 / original text
	 * @param changes 键值改动 / key-value changes
	 * @return 新文本 / new text
	 */
	private String applyItemIdChanges(String original, Map<String, String> changes) {
		String[] lines = original.split("\n", -1);
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			String trimmed = line.strip();
			if (i > 0) {
				builder.append('\n');
			}
			if (trimmed.isEmpty() || trimmed.startsWith("#")) {
				builder.append(line);
				continue;
			}
			String replacement = changes.get(trimmed);
			builder.append(replacement == null ? line : line.replace(trimmed, replacement));
		}
		return builder.toString();
	}

	/**
	 * 解析物品 ID 列表。
	 * Parses the item id list.
	 * @param text 原始文本 / original text
	 * @return 键值表：键为 ID 文本，值为 ID 文本 / key-value map: both key and value are the id text
	 */
	private Map<String, String> parseItemIdList(String text) {
		Map<String, String> values = new LinkedHashMap<>();
		for (String line : text.split("\n", -1)) {
			String trimmed = line.strip();
			if (trimmed.isEmpty() || trimmed.startsWith("#")) {
				continue;
			}
			values.put(trimmed, trimmed);
		}
		return values;
	}

	/**
	 * 定向热应用：只重跑被改文件对应的装配或服务重载。
	 * Targeted hot-apply: only the assembly or service reload matching the edited file runs.
	 * @param target 应用目标 / apply target
	 */
	private void applyTarget(EditableFile file) {
		switch (file.applyTarget()) {
			case ADMIN_CONFIG -> ConfigurableProcessor.process(AdminConfig.class, administrationProperties());
			case DEVELOPER_CONFIG -> ConfigurableProcessor.process(DeveloperConfig.class, administrationProperties());
			case PANEL_CONFIG -> ConfigurableProcessor.process(PanelConfig.class, administrationProperties());
			case COMMANDS -> ChatProcessor.getInstance().reload();
			case PACKET_FLOOD_FILTER -> PacketFloodFilter.getInstance().reload();
			case ADMIN_SERVICE -> AdminService.getInstance().reload();
			case MAIN_CONFIG -> applyMainConfig(file);
			case LOGIN_VIP_CONFIG -> applyLoginVipConfig();
		}
	}

	/**
	 * 重跑登录服的 VIP 配置装配。
	 * <p>
	 * {@code main/vip.properties} 的属性来源与游戏服主目录不同：登录服把 {@code login/} 目录、
	 * {@code network/network.properties} 与该文件合并后再装配，因此这里照搬登录服的组合方式。
	 * <p>
	 * Re-assembles the VIP configuration of the login server.
	 * <p>
	 * The properties of {@code main/vip.properties} do not come from the game server main directory: the
	 * login server merges the {@code login/} directory, {@code network/network.properties} and that file
	 * before the assembly, so the same combination is reproduced here.
	 */
	private void applyLoginVipConfig() {
		Properties[] loginProperties = loadAllFromDirectory(configDir.resolve("login"));
		Properties[] properties = new Properties[loginProperties.length + 2];
		properties[0] = loadProperties(configDir.resolve("network/network.properties"));
		properties[1] = loadProperties(configDir.resolve("main/vip.properties"));
		System.arraycopy(loginProperties, 0, properties, 2, loginProperties.length);
		ConfigurableProcessor.process(VipConfig.class, properties);
		VipConfig.validate();
	}

	/**
	 * 读取目录下的全部属性文件。
	 * Reads every property file under a directory.
	 * @param directory 目录 / the directory
	 * @return 属性数组 / the properties array
	 */
	private Properties[] loadAllFromDirectory(Path directory) {
		if (!Files.isDirectory(directory)) {
			return new Properties[0];
		}
		try {
			return PropertiesUtils.loadAllFromDirectory(directory.toFile());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * 读取单个属性文件；文件缺失时返回空属性。
	 * Reads a single property file; an absent file yields empty properties.
	 * @param path 文件路径 / the file path
	 * @return 属性对象 / the properties
	 */
	private Properties loadProperties(Path path) {
		if (!Files.isRegularFile(path)) {
			return new Properties();
		}
		try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
			Properties properties = new Properties();
			properties.load(reader);
			return properties;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * 重跑主服配置目录里某个文件对应的配置类。
	 * <p>
	 * 只重跑被改文件对应的配置类：装配本身是纯字段赋值，不会触发其它配置类与调度器，
	 * 因此「改一个文件只影响一个配置类」这一契约得以保持。
	 * <p>
	 * Re-assembles the configuration class matching the edited file in the main configuration directory.
	 * <p>
	 * Only the configuration class matching the edited file runs: the assembly is a plain field assignment and
	 * triggers neither the other configuration classes nor the schedulers, so the contract "editing one file
	 * affects one configuration class" holds.
	 * @param file 可编辑文件 / editable file
	 */
	private void applyMainConfig(EditableFile file) {
		Class<?> configClass = file.configClass();
		if (configClass == null) {
			return;
		}
		ConfigurableProcessor.process(configClass, mainProperties());
		if (MainConfigFiles.REFRESH_REQUIRED.contains(configClass)) {
			invokeRefresh(configClass);
		}
	}

	/**
	 * 调用配置类上的静态 {@code refresh()}：部分配置类在装配之外还需要重算派生状态。
	 * Invokes the static {@code refresh()} of a configuration class: some of them recompute derived state
	 * beyond the assembly.
	 * @param configClass 配置类 / the configuration class
	 */
	private void invokeRefresh(Class<?> configClass) {
		try {
			configClass.getMethod("refresh").invoke(null);
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Cannot refresh the configuration class " + configClass.getName(), e);
		}
	}

	/**
	 * 读取主服配置目录下的全部属性，供配置装配使用。
	 * Reads every property file under the main directory for the configuration assembly.
	 * @return 属性数组 / properties array
	 */
	private Properties[] mainProperties() {
		try {
			return PropertiesUtils.loadAllFromDirectory(configDir.resolve("main").toFile());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * 读取 administration 目录下的全部属性，供配置装配使用。
	 * Reads every property file under the administration directory for the configuration assembly.
	 * @return 属性数组 / properties array
	 */
	private Properties[] administrationProperties() {
		try {
			return PropertiesUtils.loadAllFromDirectory(configDir.resolve("administration").toFile());
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * 读取文件文本。
	 * Reads the file text.
	 * @param path 文件路径 / file path
	 * @return 文本 / text
	 */
	private String readText(Path path) {
		try {
			return Files.readString(path, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * 重新读取文件的键值。
	 * Re-reads the file's key-value pairs.
	 * @param file 可编辑文件 / editable file
	 * @return 键值表 / key-value map
	 */
	private Map<String, String> readValues(EditableFile file) {
		return read(file).values();
	}

	/**
	 * 计算真正发生变化的键。
	 * Computes the keys that actually changed.
	 * @param original 原键值表 / original key-value map
	 * @param changes 键值改动 / key-value changes
	 * @return 变化后的键值表 / the key-value map after the change
	 */
	private Map<String, String> changedKeys(Map<String, String> original, Map<String, String> changes) {
		Map<String, String> changed = new LinkedHashMap<>();
		for (Map.Entry<String, String> change : changes.entrySet()) {
			String previous = original.get(change.getKey());
			if (previous == null || !previous.equals(change.getValue())) {
				changed.put(change.getKey(), change.getValue());
			}
		}
		return changed;
	}

	/**
	 * 生成逐行差异。
	 * Builds the line-by-line diff.
	 * @param original 原始文本 / original text
	 * @param updated 新文本 / new text
	 * @return 差异行列表 / diff lines
	 */
	private List<DiffLine> diff(String original, String updated) {
		String[] before = original.split("\n", -1);
		String[] after = updated.split("\n", -1);
		List<DiffLine> lines = new ArrayList<>();
		int max = Math.max(before.length, after.length);
		for (int i = 0; i < max; i++) {
			String oldLine = i < before.length ? before[i] : null;
			String newLine = i < after.length ? after[i] : null;
			if (java.util.Objects.equals(oldLine, newLine)) {
				continue;
			}
			lines.add(new DiffLine(i + 1, oldLine, newLine));
		}
		return lines;
	}

	/**
	 * 文件视图：可编辑文件的内容与键值。
	 * A file view: the content and key-value pairs of an editable file.
	 *
	 * @param file 可编辑文件 / editable file
	 * @param values 键值表 / key-value map
	 * @param comments 键备注表：键的中英文说明与默认值 / key comments: the bilingual description and the
	 *                 default value of every key
	 * @param text 原始文本 / original text
	 */
	public record FileView(EditableFile file, Map<String, String> values, Map<String, ConfigKeyComment> comments,
			String text) {
	}

	/**
	 * 差异行。
	 * A diff line.
	 *
	 * @param lineNumber 行号（从 1 开始）/ line number (1-based)
	 * @param before 原行文本；新增行为 {@code null} / the original line text, {@code null} for added lines
	 * @param after 新行文本；删除行为 {@code null} / the new line text, {@code null} for removed lines
	 */
	public record DiffLine(int lineNumber, String before, String after) {
	}

	/**
	 * 差异预览。
	 * A diff preview.
	 *
	 * @param file 可编辑文件 / editable file
	 * @param lines 差异行 / diff lines
	 * @param changedKeys 变化的键值 / the changed key-value pairs
	 */
	public record DiffPreview(EditableFile file, List<DiffLine> lines, Map<String, String> changedKeys) {
	}

	/**
	 * 应用结果。
	 * An apply result.
	 *
	 * @param file 可编辑文件 / editable file
	 * @param applied 是否应用成功 / whether the apply succeeded
	 * @param rolledBack 是否已回滚 / whether the change was rolled back
	 * @param changedKeys 变化的键值 / the changed key-value pairs
	 */
	public record ApplyResult(EditableFile file, boolean applied, boolean rolledBack, Map<String, String> changedKeys) {
	}
}
