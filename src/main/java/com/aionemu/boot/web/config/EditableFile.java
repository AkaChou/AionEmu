package com.aionemu.boot.web.config;

import java.util.ArrayList;
import java.util.List;

/**
 * 可编辑配置文件的描述符：路径、格式、编辑方式，以及改完后触发的定向热应用动作。
 * Descriptor of an editable configuration file: its path, format, editing mode, and the targeted hot-apply
 * action triggered after an edit.
 * <p>
 * 首批覆盖 {@code administration/} 下的五个 {@code .properties} 与一个 {@code .txt}。
 * {@code teleport_names_zh.txt} 刻意不在其中：它与 {@code teleport_location.xml} 的 {@code loc_id} 需保持
 * 对齐，属静态对照表，误删会破坏 {@code //移动} 命令。
 * <p>
 * 描述符采用<b>注册式</b>而非枚举：新增配置目录（{@code main/}、商城等）只需注册新的描述符，不必改动
 * 既有代码，控制台菜单因而可以随配置范围一起扩展。
 * <p>
 * The first batch covers the five {@code .properties} files and one {@code .txt} under
 * {@code administration/}. {@code teleport_names_zh.txt} is deliberately excluded: it has to stay aligned with
 * the {@code loc_id} values of {@code teleport_location.xml}, it is a static lookup table, and deleting rows
 * would break the {@code //移动} command.
 * <p>
 * The descriptors are <b>registry-based</b> rather than an enum: adding a configuration directory
 * ({@code main/}, the shop, ...) means registering new descriptors and touching no existing code, so the
 * console menu can grow together with the configuration scope.
 *
 * @author AionEmu
 */
public final class EditableFile {

	/**
	 * 已注册的全部可编辑文件，按注册顺序排列。
	 * Every registered editable file, in registration order.
	 * <p>
	 * 该字段必须先于各描述符常量声明：静态字段按源码顺序初始化，描述符的构造过程会向本列表登记。
	 * <p>
	 * This field must be declared before the descriptor constants: static fields are initialized in source
	 * order and the construction of a descriptor registers itself into this list.
	 */
	private static final List<EditableFile> FILES = new ArrayList<>();

	/**
	 * GM 权限与外观配置。
	 * GM privilege and appearance configuration.
	 */
	public static final EditableFile ADMIN = new EditableFile("admin", ConfigGroup.ADMINISTRATION,
		"administration/admin.properties", Format.PROPERTIES, ApplyTarget.ADMIN_CONFIG);

	/**
	 * 开发者调试配置。
	 * Developer debugging configuration.
	 */
	public static final EditableFile DEVELOPER = new EditableFile("developer", ConfigGroup.ADMINISTRATION,
		"administration/developer.properties", Format.PROPERTIES, ApplyTarget.DEVELOPER_CONFIG);

	/**
	 * GM 面板权限配置。
	 * GM panel permission configuration.
	 */
	public static final EditableFile PANEL = new EditableFile("panel", ConfigGroup.ADMINISTRATION,
		"administration/panel.properties", Format.PROPERTIES, ApplyTarget.PANEL_CONFIG);

	/**
	 * 命令别名与访问等级配置。
	 * Command alias and access level configuration.
	 */
	public static final EditableFile COMMANDS = new EditableFile("commands", ConfigGroup.ADMINISTRATION,
		"administration/commands.properties", Format.PROPERTIES, ApplyTarget.COMMANDS);

	/**
	 * 数据包洪水过滤配置。
	 * Packet flood filter configuration.
	 */
	public static final EditableFile PFF = new EditableFile("pff", ConfigGroup.ADMINISTRATION,
		"administration/pff.properties", Format.PROPERTIES, ApplyTarget.PACKET_FLOOD_FILTER);

	/**
	 * 受限物品 ID 列表。
	 * Restricted item id list.
	 */
	public static final EditableFile ITEM_RESTRICTION = new EditableFile("item-restriction",
		ConfigGroup.ADMINISTRATION, "administration/item.restriction.txt", Format.ITEM_ID_LIST,
		ApplyTarget.ADMIN_SERVICE);

	/**
	 * 注册主服配置目录下的可编辑文件。
	 * <p>
	 * 该静态块必须位于各描述符常量之后：静态初始化按源码顺序执行，主服文件登记在
	 * administration 之后，控制台菜单的分组顺序因此与菜单展示顺序一致。
	 * <p>
	 * Registers the editable files of the main configuration directory.
	 * <p>
	 * This static block must come after the descriptor constants: static initialization runs in source order,
	 * so the main files are registered after the administration ones and the console menu groups keep the
	 * order the menu displays them in.
	 */
	static {
		MainConfigFiles.registerAll();
	}

	/**
	 * 文件格式。
	 * File format.
	 */
	public enum Format {
		/**
		 * 键值型 properties 文件。
		 * Key-value properties file.
		 */
		PROPERTIES,
		/**
		 * 每行一个物品 ID 的列表。
		 * A list holding one item id per line.
		 */
		ITEM_ID_LIST
	}

	/**
	 * 定向热应用目标。
	 * Targeted hot-apply target.
	 */
	public enum ApplyTarget {
		/**
		 * 重跑管理员配置装配。
		 * Re-assembles the administrator configuration.
		 */
		ADMIN_CONFIG,
		/**
		 * 重跑开发者配置装配。
		 * Re-assembles the developer configuration.
		 */
		DEVELOPER_CONFIG,
		/**
		 * 重跑面板配置装配。
		 * Re-assembles the panel configuration.
		 */
		PANEL_CONFIG,
		/**
		 * 重载聊天命令与权限。
		 * Reloads the chat commands and their permissions.
		 */
		COMMANDS,
		/**
		 * 重载数据包洪水过滤器。
		 * Reloads the packet flood filter.
		 */
		PACKET_FLOOD_FILTER,
		/**
		 * 重载管理员物品限制服务。
		 * Reloads the administrator item restriction service.
		 */
		ADMIN_SERVICE,
		/**
		 * 重跑游戏服主目录的配置装配。
		 * <p>
		 * 只重跑被改文件对应的配置类，不触碰其它配置类与调度器。
		 * <p>
		 * Re-assembles the configuration class matching the edited file only, leaving the other configuration
		 * classes and the schedulers untouched.
		 */
		MAIN_CONFIG,
		/**
		 * 重跑登录服的 VIP 配置装配。
		 * <p>
		 * {@code main/vip.properties} 虽放在主目录，却由登录服读取：它的属性来源是
		 * {@code login/} 加 {@code network/} 加本文件的组合，因此不能走通用的主目录装配。
		 * <p>
		 * Re-assembles the VIP configuration of the login server.
		 * <p>
		 * {@code main/vip.properties} lives in the main directory but is read by the login server: its
		 * properties come from {@code login/} plus {@code network/} plus this file, so the generic main
		 * directory assembly does not apply to it.
		 */
		LOGIN_VIP_CONFIG
	}

	/**
	 * 描述符标识。
	 * The descriptor id.
	 */
	private final String name;

	/**
	 * 所属分组：一个文件可以同时出现在多个分组下（商城开关既属于「主服配置」也属于「商城管理」）。
	 * The groups this file belongs to: a file may show up under several groups at once (the shop switches
	 * belong to both "main" and "shop management").
	 */
	private final List<ConfigGroup> groups;

	/**
	 * 相对配置根目录的路径。
	 * Path relative to the configuration root directory.
	 */
	private final String relativePath;

	/**
	 * 文件格式。
	 * File format.
	 */
	private final Format format;

	/**
	 * 定向热应用目标。
	 * Targeted hot-apply target.
	 */
	private final ApplyTarget applyTarget;

	/**
	 * 热应用时重跑的配置类；仅 {@link ApplyTarget#MAIN_CONFIG} 使用。
	 * The configuration class re-assembled on hot-apply; used by {@link ApplyTarget#MAIN_CONFIG} only.
	 */
	private final Class<?> configClass;

	/**
	 * 以给定标识、分组、路径、格式与应用目标构造描述符。
	 * Constructs a descriptor with the given id, group, path, format and apply target.
	 * @param name 标识 / the id
	 * @param group 所属分组 / the group
	 * @param relativePath 相对路径 / relative path
	 * @param format 格式 / format
	 * @param applyTarget 应用目标 / apply target
	 * @param configClass 热应用时重跑的配置类 / the configuration class re-assembled on hot-apply
	 */
	private EditableFile(String name, List<ConfigGroup> groups, String relativePath, Format format,
			ApplyTarget applyTarget, Class<?> configClass) {
		this(name, groups, relativePath, format, applyTarget, configClass, true);
	}

	/**
	 * 以给定标识、分组、路径、格式、应用目标与配置类构造描述符。
	 * Constructs a descriptor with the given id, group, path, format, apply target and configuration class.
	 * @param name 标识 / the id
	 * @param group 所属分组 / the group
	 * @param relativePath 相对路径 / relative path
	 * @param format 格式 / format
	 * @param applyTarget 应用目标 / apply target
	 * @param configClass 热应用时重跑的配置类 / the configuration class re-assembled on hot-apply
	 * @param register 是否登记到全局列表 / whether the descriptor is registered in the global list
	 */
	private EditableFile(String name, List<ConfigGroup> groups, String relativePath, Format format,
			ApplyTarget applyTarget, Class<?> configClass, boolean register) {
		this.name = name;
		this.groups = List.copyOf(groups);
		this.relativePath = relativePath;
		this.format = format;
		this.applyTarget = applyTarget;
		this.configClass = configClass;
		if (register) {
			FILES.add(this);
		}
	}

	/**
	 * 登记一个描述符到全局列表。
	 * Registers a descriptor in the global list.
	 * @param file 描述符 / the descriptor
	 */
	static void register(EditableFile file) {
		FILES.add(file);
	}

	/**
	 * 创建一个描述符并登记到全局列表。
	 * Creates a descriptor and registers it in the global list.
	 * @param name 标识 / the id
	 * @param group 所属分组 / the group
	 * @param relativePath 相对路径 / relative path
	 * @param format 格式 / format
	 * @param applyTarget 应用目标 / apply target
	 * @param configClass 热应用时重跑的配置类 / the configuration class re-assembled on hot-apply
	 * @return 新建的描述符 / the new descriptor
	 */
	static EditableFile create(String name, List<ConfigGroup> groups, String relativePath, Format format,
			ApplyTarget applyTarget, Class<?> configClass) {
		EditableFile file = new EditableFile(name, groups, relativePath, format, applyTarget, configClass, false);
		FILES.add(file);
		return file;
	}

	/**
	 * 创建一个只属于单个分组的描述符并登记到全局列表。
	 * Creates a descriptor belonging to a single group and registers it in the global list.
	 * @param name 标识 / the id
	 * @param group 所属分组 / the group
	 * @param relativePath 相对路径 / relative path
	 * @param format 格式 / format
	 * @param applyTarget 应用目标 / apply target
	 * @param configClass 热应用时重跑的配置类 / the configuration class re-assembled on hot-apply
	 * @return 新建的描述符 / the new descriptor
	 */
	static EditableFile create(String name, ConfigGroup group, String relativePath, Format format,
			ApplyTarget applyTarget, Class<?> configClass) {
		return create(name, List.of(group), relativePath, format, applyTarget, configClass);
	}

	/**
	 * 以不带配置类的方式构造描述符。
	 * Constructs a descriptor without a configuration class.
	 * @param name 标识 / the id
	 * @param group 所属分组 / the group
	 * @param relativePath 相对路径 / relative path
	 * @param format 格式 / format
	 * @param applyTarget 应用目标 / apply target
	 */
	private EditableFile(String name, ConfigGroup group, String relativePath, Format format,
			ApplyTarget applyTarget) {
		this(name, List.of(group), relativePath, format, applyTarget, null);
	}

	/**
	 * 返回描述符标识。
	 * Returns the descriptor id.
	 * @return 标识 / the id
	 */
	public String name() {
		return name;
	}

	/**
	 * 返回所属分组。
	 * Returns the groups this file belongs to.
	 * @return 分组列表 / the group list
	 */
	public List<ConfigGroup> groups() {
		return groups;
	}

	/**
	 * 返回主分组：文件在菜单里首次归属的分组。
	 * Returns the primary group: the group the file first belongs to in the menu.
	 * @return 主分组 / the primary group
	 */
	public ConfigGroup group() {
		return groups.get(0);
	}

	/**
	 * 返回所属分组标识。
	 * Returns the ids of the groups this file belongs to.
	 * @return 分组标识列表 / the group id list
	 */
	public List<String> groupIds() {
		return groups.stream().map(ConfigGroup::id).toList();
	}

	/**
	 * 返回相对配置根目录的路径。
	 * Returns the path relative to the configuration root directory.
	 * @return 相对路径 / relative path
	 */
	public String relativePath() {
		return relativePath;
	}

	/**
	 * 返回文件格式。
	 * Returns the file format.
	 * @return 格式 / format
	 */
	public Format format() {
		return format;
	}

	/**
	 * 返回定向热应用目标。
	 * Returns the targeted hot-apply target.
	 * @return 应用目标 / apply target
	 */
	public ApplyTarget applyTarget() {
		return applyTarget;
	}

	/**
	 * 返回热应用时重跑的配置类。
	 * Returns the configuration class re-assembled on hot-apply.
	 * @return 配置类；不适用时为 {@code null} / the configuration class, {@code null} when not applicable
	 */
	public Class<?> configClass() {
		return configClass;
	}

	/**
	 * 返回全部可编辑文件。
	 * Returns every editable file.
	 * @return 可编辑文件列表 / editable file list
	 */
	public static List<EditableFile> all() {
		return List.copyOf(FILES);
	}

	/**
	 * 按标识查找可编辑文件。
	 * Finds an editable file by its id.
	 * @param name 标识 / the id
	 * @return 可编辑文件；不存在时为 {@code null} / the editable file, {@code null} when absent
	 */
	public static EditableFile byName(String name) {
		for (EditableFile file : FILES) {
			if (file.name.equalsIgnoreCase(name)) {
				return file;
			}
		}
		return null;
	}
}
