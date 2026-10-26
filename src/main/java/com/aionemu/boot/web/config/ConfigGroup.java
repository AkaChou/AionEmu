package com.aionemu.boot.web.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置分组：控制台一级菜单的分类。
 * A configuration group: the category behind a top-level console menu entry.
 * <p>
 * 分组是控制台菜单的骨架，与配置目录结构解耦：一个分组可以覆盖多个目录，一个目录也可以被拆进
 * 多个分组（例如「商城管理」既读 {@code ingameshop/} 的商品定义，也读 {@code main/} 的商城开关）。
 * <p>
 * The group is the skeleton of the console menu and is decoupled from the configuration directory layout: one
 * group may cover several directories, and one directory may be split across several groups (the "shop
 * management" group, for instance, reads the item definitions from {@code ingameshop/} and the shop switches
 * from {@code main/}).
 *
 * @param id 分组标识 / the group id
 * @param directory 该分组的主要配置目录，相对配置根目录 / the main configuration directory of the group,
 *                  relative to the configuration root
 * @param labelZh 中文标签 / the Chinese label
 * @param labelEn 英文标签 / the English label
 * @author AionEmu
 */
public record ConfigGroup(String id, String directory, String labelZh, String labelEn) {

	/**
	 * 已注册的全部分组，按注册顺序排列。
	 * Every registered group, in registration order.
	 */
	private static final List<ConfigGroup> GROUPS = new ArrayList<>();

	/**
	 * 注册一个分组。
	 * Registers a group.
	 * @param group 分组 / the group
	 */
	private static void register(ConfigGroup group) {
		GROUPS.add(group);
	}

	/**
	 * 返回全部分组。
	 * Returns every group.
	 * @return 分组列表 / the group list
	 */
	public static List<ConfigGroup> all() {
		return List.copyOf(GROUPS);
	}

	/**
	 * 返回该分组下的可编辑文件。
	 * Returns the editable files of this group.
	 * @return 文件列表 / the file list
	 */
	public List<EditableFile> files() {
		return EditableFile.all().stream()
			.filter(file -> file.groups().contains(this))
			.toList();
	}

	/**
	 * 管理员分组：GM 权限、开发者开关、面板权限、命令别名与洪水过滤。
	 * The administration group: GM privileges, developer switches, panel permissions, command aliases and the
	 * flood filter.
	 */
	public static final ConfigGroup ADMINISTRATION = new ConfigGroup("administration", "administration",
		"管理配置", "Administration");

	/**
	 * 主服配置分组：游戏服的通用配置。
	 * The main group: the general configuration of the game server.
	 */
	public static final ConfigGroup MAIN = new ConfigGroup("main", "main", "主服配置", "Main");

	/**
	 * 商城管理分组：商品定义与商城开关。
	 * The shop group: the item definitions and the shop switches.
	 */
	public static final ConfigGroup SHOP = new ConfigGroup("shop", "ingameshop", "商城管理", "Shop");

	/**
	 * 静态初始化：按菜单顺序注册全部分组。
	 * Static initialization: registers every group in menu order.
	 */
	static {
		register(ADMINISTRATION);
		register(MAIN);
		register(SHOP);
	}

	/**
	 * 返回分组标识到分组的映射。
	 * Returns the map from group id to group.
	 * @return 分组表 / the group map
	 */
	public static Map<String, ConfigGroup> byId() {
		Map<String, ConfigGroup> groups = new LinkedHashMap<>();
		for (ConfigGroup group : GROUPS) {
			groups.put(group.id(), group);
		}
		return groups;
	}
}
