package com.aionemu.gameserver.utils;

import com.aionemu.gameserver.world.WorldPosition;

/**
 * 聊天消息链接格式化工具。
 * Chat message link formatting utility.
 *
 * @author antness
 */
public class ChatUtil {

	/**
	 * 将世界坐标格式化为可点击位置链接。
	 * Formats a world position as a clickable location link.
	 *
	 * @param label 链接显示文本 / Link display label
	 * @param pos 世界坐标 / World position
	 * @return 位置链接字符串 / Position link string
	 */
	public static String position(String label, WorldPosition pos) {
		return position(label, pos.getMapId(), pos.getX(), pos.getY(), pos.getZ());
	}

	/**
	 * 将地图坐标格式化为可点击位置链接。
	 * Formats map coordinates as a clickable location link.
	 *
	 * @param label 链接显示文本 / Link display label
	 * @param worldId 世界地图 ID / World map id
	 * @param x X 坐标 / X coordinate
	 * @param y Y 坐标 / Y coordinate
	 * @param z Z 坐标 / Z coordinate
	 * @return 位置链接字符串 / Position link string
	 */
	public static String position(String label, long worldId, float x, float y, float z) {
		return String.format("[pos:%s;%d %f %f %f %d]", label, worldId, x, y, z, getMapPart(worldId, x, y, z));
	}

	static int getMapPart(long worldId, float x, float y, float z) {
		if (worldId != 400010000) {
			return -1;
		}
		if (z > 1800 && z < 2800 && x > 1600 && x < 2700 && y > 1400 && y < 2500) {
			return 2;
		}
		return z > 2250 ? 3 : 1;
	}

	/**
	 * 将物品 ID 格式化为物品链接。
	 * Formats an item id as an item link.
	 *
	 * @param itemId 物品 ID / Item id
	 * @return 物品链接字符串 / Item link string
	 */
	public static String item(long itemId) {
		return String.format("[item: %d]", itemId);
	}

	/**
	 * 将配方 ID 格式化为配方链接。
	 * Formats a recipe id as a recipe link.
	 *
	 * @param recipeId 配方 ID / Recipe id
	 * @return 配方链接字符串 / Recipe link string
	 */
	public static String recipe(long recipeId) {
		return String.format("[recipe: %d]", recipeId);
	}

	/**
	 * 将任务 ID 格式化为任务链接。
	 * Formats a quest id as a quest link.
	 *
	 * @param questId 任务 ID / Quest id
	 * @return 任务链接字符串 / Quest link string
	 */
	public static String quest(int questId) {
		return String.format("[quest: %d]", questId);
	}

	/**
	 * 从可能带前缀的管理名称中提取真实角色名。
	 * Extracts the real character name from an admin name that may include a prefix.
	 *
	 * @param name 管理名称 / Admin name
	 * @return 真实角色名 / Real character name
	 */
	public static String getRealAdminName(String name) {
		int index = name.lastIndexOf(" ");
		if (index == -1) {
			return name;
		}
		return name.substring(index + 1);
	}
}
