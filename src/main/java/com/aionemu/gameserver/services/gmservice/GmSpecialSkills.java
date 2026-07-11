package com.aionemu.gameserver.services.gmservice;

import com.aionemu.gameserver.configs.administration.AdminConfig;
import com.aionemu.gameserver.model.gameobjects.player.Player;

/**
 * GM 特殊技能枚举：按权限等级定义显示标签与可授予技能列表。
 * GM special skills enum: defines display tags and grantable skill lists by access level.
 */
public enum GmSpecialSkills {
	/** 权限等级 1：GM / Access level 1: GM */
	AccessLevel1(1, AdminConfig.ADMIN_TAG_1, "\ue042GM\ue043", new int[]{240, 241, 282}),
    /** Access level 2: HEAD-GM / Access level 2: HEAD-GM */
    AccessLevel2(2, AdminConfig.ADMIN_TAG_1, "\ue042HEAD-GM\ue043", new int[]{240, 241, 282}),
    /** Access level 3: Admin / Access level 3: Admin */
    AccessLevel3(3, AdminConfig.ADMIN_TAG_1, "\ue042Admin\ue043", new int[]{240, 241, 282}),
    /** Access level 4: Unity-Master / Access level 4: Unity-Master */
    AccessLevel4(4, AdminConfig.ADMIN_TAG_1, "\ue042Unity-Master\ue043", new int[]{240, 241, 282}),
    /** Access level 5: Unity-Management / Access level 5: Unity-Management */
    AccessLevel5(5, AdminConfig.ADMIN_TAG_1, "\ue042Unity-Management\ue043", new int[]{240, 241, 277, 282, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 389, 390, 391, 392, 393, 394, 395, 395, 396}),
    /** Access level 6: Unity-Developer / Access level 6: Unity-Developer */
    AccessLevel6(6, AdminConfig.ADMIN_TAG_1, "\ue042Unity-Developer\ue043", new int[]{240, 241, 277, 282, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 389, 390, 391, 392, 393, 394, 395, 395, 396}),
	/** Access level 7: Unity-Developer / Access level 7: Unity-Developer */
	AccessLevel7(7, AdminConfig.ADMIN_TAG_1, "\ue042Unity-Developer\ue043", new int[]{240, 241, 277, 282, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 389, 390, 391, 392, 393, 394, 395, 395, 396}),
	/** Access level 8: Unity-Developer / Access level 8: Unity-Developer */
	AccessLevel8(8, AdminConfig.ADMIN_TAG_1, "\ue042Unity-Developer\ue043", new int[]{240, 241, 277, 282, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 389, 390, 391, 392, 393, 394, 395, 395, 396}),
	/** Access level 9: Unity-Developer / Access level 9: Unity-Developer */
	AccessLevel9(9, AdminConfig.ADMIN_TAG_1, "\ue042Unity-Developer\ue043", new int[]{240, 241, 277, 282, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 389, 390, 391, 392, 393, 394, 395, 395, 396}),
	/** Access level 10: Unity-Developer / Access level 10: Unity-Developer */
	AccessLevel10(10, AdminConfig.ADMIN_TAG_1, "\ue042Unity-Developer\ue043", new int[]{240, 241, 277, 282, 376, 377, 378, 379, 380, 381, 382, 383, 384, 385, 386, 387, 388, 389, 390, 391, 392, 393, 394, 395, 395, 396});
    private final int level;
    private final String nameLevel;
    private String status;
    private int[] skills;

	/**
	 * 构造权限等级条目。
	 * Construct an access-level entry.
	 *
	 * @param id 权限等级 / Access level
	 * @param name 显示名称标签 / Display name tag
	 * @param status 状态显示名 / Status display name
	 * @param skills 可授予技能 ID 列表 / Grantable skill ids
	 */
    GmSpecialSkills(int id, String name, String status, int[] skills) {
        this.level = id;
        this.nameLevel = name;
        this.status = status;
        this.skills = skills;
    }

	/**
	 * 获取权限显示名称标签。
	 * Get the access-level display name tag.
	 *
	 * Name tag
	 */
    public String getName() {
        return nameLevel;
    }

	/**
	 * 获取权限等级数值。
	 * Get the access level number.
	 *
	 * Access level
	 */
    public int getLevel() {
        return level;
    }

	/**
	 * 获取状态显示名（含 GM 前缀字符）。
	 * Get the status display name (including GM prefix glyphs).
	 *
	 * Status name
	 */
    public String getStatusName() {
        return status;
    }

	/**
	 * 获取该等级可授予的技能 ID 数组。
	 * Get the grantable skill id array for this level.
	 *
	 * Skill id array
	 */
    public int[] getSkills() {
        return skills;
    }

	/**
	 * 按权限等级查找对应枚举项。
	 * Look up the enum constant by access level.
	 *
	 * @param level 权限等级 / Access level
	 * @return 对应枚举，未找到返回 null / Matching enum, or null if not found
	 */
    public static GmSpecialSkills getAlType(int level) {
        for (GmSpecialSkills al : GmSpecialSkills.values()) {
            if (level == al.getLevel()) {
                return al;
            }
        }
        return null;
    }

	/**
	 * 按权限等级获取显示名称标签。
	 * Get the display name tag by access level.
	 *
	 * @param level 权限等级 / Access level
	 * @return 名称标签，未找到返回 "%s" / Name tag, or "%s" if not found
	 */
    public static String getAlName(int level) {
        for (GmSpecialSkills al : GmSpecialSkills.values()) {
            if (level == al.getLevel()) {
                return al.getName();
            }
        }
        return "%s";
    }

	/**
	 * 获取玩家状态显示名：有 GM 权限则用 GM 状态名，否则用军团名。
	 * Get player status display name: GM status if access level &gt; 0, otherwise legion name.
	 *
	 * @param player 玩家 / Player
	 * @return 状态显示名 / Status display name
	 */
    public static String getStatusName(Player player) {
        return player.getAccessLevel() > 0 ? GmSpecialSkills.getAlType(player.getAccessLevel()).getStatusName() : player.getLegion().getLegionName();
    }
}
