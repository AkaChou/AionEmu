package com.aionemu.boot.config;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.springframework.core.env.EnumerablePropertySource;

/**
 * 指向磁盘配置目录的属性源：直接把 {@code .properties} 文件的裸键暴露给 Spring {@code Environment}。
 * Disk-backed property source exposing the raw keys of the {@code .properties} files to the Spring
 * {@code Environment}.
 * <p>
 * 该属性源取代了此前「启动时快照进 Environment」的裸键镜像。镜像的致命缺陷是：遗留装配器
 * ({@code ConfigurableProcessor}) 优先查询 {@code Environment}，于是 {@code Config.reload()} 从磁盘
 * 重读到的值永远用不上 —— 手改配置文件后执行 {@code //reload config} 对任何 {@code gameserver.*} 键
 * 都无效。本属性源直接读取文件本身，且支持运行时重建，因此改盘后立即可见。
 * <p>
 * This property source replaces the raw-key mirror that used to be snapshotted into the {@code Environment}
 * at boot. The mirror was fatally flawed: the legacy assembler ({@code ConfigurableProcessor}) queries
 * the {@code Environment} first, so the values re-read from disk by {@code Config.reload()} were never
 * reachable — hand-editing a configuration file and then running {@code //reload config} had no effect on
 * any {@code gameserver.*} key. This source reads the files themselves and supports runtime rebuilding,
 * so an on-disk change is visible immediately.
 * <p>
 * 注册优先级为最低（{@code addLast}），因此命令行参数、操作系统环境变量与 {@code application.yml}
 * 仍然压过文件值。
 * <p>
 * It is registered at the lowest precedence ({@code addLast}), so command-line arguments, operating-system
 * environment variables and {@code application.yml} still outrank the file values.
 * <p>
 * <b>线程安全</b>：实例不可变，键值在构造时一次性读入；重建由调用方替换整个属性源实例完成。
 * <b>Thread safety</b>: the instance is immutable and its entries are read once at construction; rebuilding
 * replaces the whole property source instance.
 */
public class AionConfigFilePropertySource extends EnumerablePropertySource<Map<String, Object>> {

	/**
	 * 属性源名称，注册到 {@code Environment} 时用于标识与替换。
	 * Property source name, used to identify and replace the source inside the {@code Environment}.
	 */
	public static final String NAME = "aionConfigFiles";

	/**
	 * 裸键键值表。
	 * Raw key to value map.
	 */
	private final Map<String, Object> source;

	/**
	 * 以给定键值构造属性源。
	 * Constructs a property source with the given entries.
	 * @param source 裸键键值表 / raw key to value map
	 */
	public AionConfigFilePropertySource(Map<String, Object> source) {
		super(NAME, source);
		this.source = Collections.unmodifiableMap(new LinkedHashMap<>(source));
	}

	/**
	 * 读取配置目录下的全部 {@code .properties} 文件，返回裸键键值表。
	 * <p>
	 * 目录缺失或文件不可读时静默跳过：该属性源是「尽力而为」的补充来源，缺失不应阻断启动。
	 * <p>
	 * Reads every {@code .properties} file under the configuration directories and returns the raw key map.
	 * <p>
	 * A missing directory or an unreadable file is skipped silently: this source is a best-effort
 * supplement and its absence must not block startup.
	 * @param configDir 配置根目录 / configuration root directory
	 * @return 裸键键值表 / raw key to value map
	 */
	public static Map<String, Object> readEntries(Path configDir) {
		Map<String, Object> entries = new LinkedHashMap<>();
		Map<String, String> owners = new LinkedHashMap<>();
		Map<String, Object> conflicting = new LinkedHashMap<>();
		for (ServiceFiles service : serviceFiles(configDir)) {
			for (Path file : service.files()) {
				for (Map.Entry<String, Object> entry : readFileEntries(file).entrySet()) {
					String key = entry.getKey();
					String owner = owners.get(key);
					if (owner == null) {
						owners.put(key, service.name());
						entries.put(key, entry.getValue());
						continue;
					}
					if (owner.equals(service.name())) {
						// 同一服务内后加载的文件覆盖先前值。 / A later file of the same service wins.
						entries.put(key, entry.getValue());
						continue;
					}
					if (!entry.getValue().equals(entries.get(key))) {
						// 跨服务取值不一致：扁平命名空间无法同时表达两者，该键交由各服务自己的加载器读取本地文件。
						// The services disagree on one key: a flat namespace cannot express both, so the key is
						// left to each service's own loader reading its own file.
						conflicting.put(key, entry.getValue());
					}
				}
			}
		}
		conflicting.keySet().forEach(entries::remove);
		return entries;
	}

	/**
	 * 列出各服务各自读取的配置文件。
	 * Lists the configuration files each service reads on its own.
	 * @param configDir 配置根目录 / configuration root directory
	 * @return 服务文件集合 / the service file sets
	 */
	private static List<ServiceFiles> serviceFiles(Path configDir) {
		List<ServiceFiles> services = new ArrayList<>();
		services.add(new ServiceFiles("game", filesOf(configDir.resolve("administration"), configDir.resolve("main"),
			configDir.resolve("network"), configDir.resolve("mygs.properties"))));
		// 共享的 network/network.properties 只由游戏服贡献裸键：登录服与聊天服读取它时只取带前缀的键，
		// 否则同一文件会被算成「两个服务为同一原始键给出不同值」而被剔除。
		// The shared network/network.properties contributes raw keys through the game server only: the login
		// and chat servers read it for their prefixed keys alone, otherwise one file would count as "two
		// services disagreeing on one raw key" and the key would be dropped.
		services.add(new ServiceFiles("login", filesOf(configDir.resolve("login"),
			configDir.resolve("login/myls.properties"))));
		services.add(new ServiceFiles("chat", filesOf(configDir.resolve("chat"),
			configDir.resolve("chat/mycs.properties"))));
		return services;
	}

	/**
	 * 收集目录下的 {@code .properties} 文件与给定的单文件。
	 * Collects the {@code .properties} files under the directories together with the given single files.
	 * @param directories 目录 / directories
	 * @param files 单文件 / single files
	 * @return 文件列表 / the file list
	 */
	private static List<Path> filesOf(Path... directoriesAndFiles) {
		List<Path> files = new ArrayList<>();
		for (Path path : directoriesAndFiles) {
			if (Files.isDirectory(path)) {
				files.addAll(listPropertiesFiles(path));
			} else if (Files.isRegularFile(path)) {
				files.add(path);
			}
		}
		return files;
	}

	/**
	 * 列出目录下的全部 {@code .properties} 文件（按文件名排序，保证覆盖顺序稳定）。
	 * Lists the {@code .properties} files under a directory, sorted by file name so the override order is
	 * stable.
	 * @param directory 目录 / directory
	 * @return 按文件名排序的文件集合 / files sorted by name
	 */
	private static Collection<Path> listPropertiesFiles(Path directory) {
		try (var stream = Files.list(directory)) {
			return stream
				.filter(Files::isRegularFile)
				.filter(path -> path.getFileName().toString().endsWith(".properties"))
				.sorted()
				.toList();
		} catch (IOException e) {
			return List.of();
		}
	}

	/**
	 * 读取单个 {@code .properties} 文件。
	 * <p>
	 * 以 UTF-8 读取而非 {@code Properties.load(InputStream)} 的 ISO-8859-1 默认编码：配置文件中存在中文键
	 * （如 {@code 移动}）与中文值。
	 * <p>
	 * Reads a single {@code .properties} file.
	 * <p>
	 * The file is read as UTF-8 instead of the ISO-8859-1 default of {@code Properties.load(InputStream)},
 * because the configuration files carry Chinese keys (such as {@code 移动}) and Chinese values.
	 * @param file 文件 / file
	 * @return 键值表 / key to value map
	 */
	private static Map<String, Object> readFileEntries(Path file) {
		Properties properties = new Properties();
		try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
			properties.load(reader);
		} catch (IOException e) {
			return Map.of();
		}
		Map<String, Object> entries = new LinkedHashMap<>();
		for (String name : properties.stringPropertyNames()) {
			entries.put(name, properties.getProperty(name));
		}
		return entries;
	}

	/**
	 * 返回可枚举的裸键键值表。
	 * Returns the enumerable raw key map.
	 * @return 键值表 / key to value map
	 */
	@Override
	public Map<String, Object> getSource() {
		return source;
	}

	/**
	 * 返回指定键的值。
	 * Returns the value of the given key.
	 * @param name 键名 / property name
	 * @return 值；键不存在时为 {@code null} / the value, {@code null} when the key is absent
	 */
	@Override
	public Object getProperty(String name) {
		return source.get(name);
	}

	/**
	 * 返回可枚举的键名数组。
	 * Returns the enumerable property names.
	 * @return 键名数组 / property names
	 */
	@Override
	public String[] getPropertyNames() {
		return source.keySet().toArray(new String[0]);
	}

	/**
	 * 一个服务读取的配置文件集合。
	 * The set of configuration files one service reads.
	 *
	 * @param name 服务标识 / service identity
	 * @param files 该服务读取的文件 / the files the service reads
	 */
	private record ServiceFiles(String name, List<Path> files) {
	}
}
