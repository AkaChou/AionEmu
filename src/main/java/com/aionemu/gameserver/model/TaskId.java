package com.aionemu.gameserver.model;

/**
 * 任务 ID 枚举。
 * Task Id enumeration.
 */

public enum TaskId {
	/** 腐朽 / Decay. */
	DECAY,
	/** 重生 / Respawn */
	RESPAWN,
	/** 监狱 / Prison */
	PRISON,
	/** 保护激活 / Protection Active */
	PROTECTION_ACTIVE,
	/** 溺水 / Drown */
	DROWN,
	/** 消失 / Despawn */
	DESPAWN,
	/** 任务计时器 / Quest Timer */
	QUEST_TIMER,
	/** 任务跟随 / Quest Follow */
	QUEST_FOLLOW,
	/** 玩家更新 / Player Update */
	PLAYER_UPDATE,
	/** 背包更新 / Inventory Update*/
	INVENTORY_UPDATE,
	/** 禁言 / Gag */
	GAG,
	/** 物品使用 / Item Use */
	ITEM_USE,
	/** 动作物品 NPC / Action Item NPC */
	ACTION_ITEM_NPC,
	/** 房屋对象使用 / House Object Use */
	HOUSE_OBJECT_USE,
	/** 快递邮件使用 / Express Mail Use */
	EXPRESS_MAIL_USE,
	/** 技能使用 / Skill Use */
	SKILL_USE,
	/** 采集物 / Gatherable */
	GATHERABLE,
	/** 宠物更新 / Pet Update*/
	PET_UPDATE,
	/** 召唤物跟随 / Summon Follow */
	SUMMON_FOLLOW,
	/** 区域材质行为 / Zone Material Action */
	ZONE_MATERIAL_ACTION,
	/** 地形材质行为 / Terrain Material Action */
	TERRAIN_MATERIAL_ACTION,
	/** 热点传送 / Hotspot Teleport */
	HOTSPOT_TELEPORT,
	/** 自由混战 / Free For All */
	FFA,
	/** 玩家击杀 / Player Kill */
	PK,
	/** 小偷 / Thieves */
	THIEVES,
	/** 迷你兵更新 / Minion Update */
	MINION_UPDATE,
	/** 迷你兵传送检查 / Minion Teleport Check */
	MINION_TELEPORT_CHECK;
}
