package com.aionemu.gameserver.configs.main;

import com.aionemu.commons.configuration.Property;

/**
 * 地理数据与导航寻路相关配置。
 * Geodata and navigation pathfinding related configuration.
 */
public class GeoDataConfig {

	/**
	 * 是否启用地理数据。
	 * Whether geodata is enabled.
	 */
	@Property(key = "gameserver.geodata.enable", defaultValue = "false")
	public static boolean GEO_ENABLE;

	/**
	 * 是否使用地理数据做可见性（canSee）检测。
	 * Whether canSee checks use geodata.
	 */
	@Property(key = "gameserver.geodata.cansee.enable", defaultValue = "true")
	public static boolean CANSEE_ENABLE;

	/**
	 * 是否使用地理数据处理恐惧技能。
	 * Whether Fear skill uses geodata.
	 */
	@Property(key = "gameserver.geodata.fear.enable", defaultValue = "true")
	public static boolean FEAR_ENABLE;

	/**
	 * 是否在 NPC 移动时做地理检测（防飞行怪）。
	 * Whether geo checks run during NPC movement (prevent flying mobs).
	 */
	@Property(key = "gameserver.geo.npc.move", defaultValue = "false")
	public static boolean GEO_NPC_MOVE;

	/**
	 * 是否启用地理材质技能效果。
	 * Whether geo materials using skills are enabled.
	 */
	@Property(key = "gameserver.geo.materials.enable", defaultValue = "false")
	public static boolean GEO_MATERIALS_ENABLE;

	/**
	 * 是否启用地理护盾。
	 * Whether geo shields are enabled.
	 */
	@Property(key = "gameserver.geo.shields.enable", defaultValue = "false")
	public static boolean GEO_SHIELDS_ENABLE;

	/**
	 * 是否启用导航寻路。
	 * Whether navigation pathfinding is enabled.
	 */
	@Property(key = "gameserver.geo.nav.pathfinding.enable", defaultValue = "false")
	public static boolean GEO_NAV_ENABLE;

	/**
	 * 导航路径缓存大小。
	 * Navigation path cache size.
	 */
	@Property(key = "gameserver.geo.nav.cache.size", defaultValue = "50")
	public static int GEO_NAV_CACHE_SIZE;

	/**
	 * 是否启用导航拉拽（pull）。
	 * Whether navigation pull is enabled.
	 */
	@Property(key = "gameserver.geo.nav.pull.enable", defaultValue = "true")
	public static boolean GEO_NAV_PULL_ENABLE;

	/**
	 * 导航搜索最大节点数。
	 * Maximum navigation search nodes.
	 */
	@Property(key = "gameserver.geo.nav.max.nodes", defaultValue = "800")
	public static int GEO_NAV_MAX_NODES;

	/**
	 * 导航到达目标阈值。
	 * Navigation target reach threshold.
	 */
	@Property(key = "gameserver.geo.nav.target.threshold", defaultValue = "5")
	public static float GEO_NAV_TARGET_THRESHOLD;

	/**
	 * 导航路径代价权重。
	 * Navigation path cost weight.
	 */
	@Property(key = "gameserver.geo.nav.path.weight", defaultValue = "0.2")
	public static float GEO_NAV_PATH_WEIGHT;

	/**
	 * 导航目标代价权重。
	 * Navigation target cost weight.
	 */
	@Property(key = "gameserver.geo.nav.target.weight", defaultValue = "20")
	public static float GEO_NAV_TARGET_WEIGHT;

	/**
	 * 导航地面搜索距离。
	 * Navigation ground search distance.
	 */
	@Property(key = "gameserver.geo.nav.ground.search.distance", defaultValue = "5")
	public static float GEO_NAV_GROUND_SEARCH_DISTANCE;

	/**
	 * 导航碰撞盒 XY 扩展。
	 * Navigation collision box extent on XY.
	 */
	@Property(key = "gameserver.geo.nav.box.extent.xy", defaultValue = "0.8")
	public static float GEO_NAV_BOX_EXTENT_XY;

	/**
	 * 导航碰撞盒 Z 向下偏移最小值。
	 * Navigation collision box minimum Z offset.
	 */
	@Property(key = "gameserver.geo.nav.box.offset.z.min", defaultValue = "-1")
	public static float GEO_NAV_BOX_OFFSET_Z_MIN;

	/**
	 * 导航碰撞盒 Z 向上偏移最大值。
	 * Navigation collision box maximum Z offset.
	 */
	@Property(key = "gameserver.geo.nav.box.offset.z.max", defaultValue = "4")
	public static float GEO_NAV_BOX_OFFSET_Z_MAX;

	/**
	 * 导航碰撞盒中心 Z 偏移。
	 * Navigation collision box center Z offset.
	 */
	@Property(key = "gameserver.geo.nav.box.center.z", defaultValue = "0.2")
	public static float GEO_NAV_BOX_CENTER_Z;

	/**
	 * 是否平滑导航路径。
	 * Whether navigation paths are smoothed.
	 */
	@Property(key = "gameserver.geo.nav.smooth.path", defaultValue = "true")
	public static boolean GEO_NAV_SMOOTH_PATH;

	/**
	 * 导航走廊长度。
	 * Navigation corridor length.
	 */
	@Property(key = "gameserver.geo.nav.corridor.length", defaultValue = "800")
	public static int GEO_NAV_CORRIDOR_LENGTH;

}
