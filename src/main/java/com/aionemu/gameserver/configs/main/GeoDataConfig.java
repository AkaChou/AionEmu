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

	@Property(key = "gameserver.geo.path.enable", defaultValue = "false")
	public static boolean GEO_PATH_ENABLE;

	@Property(key = "gameserver.geo.path.cache.size", defaultValue = "10")
	public static int GEO_PATH_CACHE_SIZE;

	@Property(key = "gameserver.geo.path.max.nodes", defaultValue = "50000")
	public static int GEO_PATH_MAX_NODES;

	@Property(key = "gameserver.geo.path.timeout.ms", defaultValue = "250")
	public static int GEO_PATH_TIMEOUT_MS;

	@Property(key = "gameserver.geo.path.spatial.step", defaultValue = "2")
	public static float GEO_PATH_SPATIAL_STEP;

}
