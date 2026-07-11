package com.aionemu.gameserver.spawnengine;

/**
 * 刷怪组处理类型枚举，用于区分不同的特殊刷怪逻辑。
 * Spawn group handler type enum for routing special spawn logic.
 */
public enum SpawnHandlerType {
	/** 猎杀者 / Slayer */
	SLAYER,
	/** 首领 / Chief */
	CHIEF,
	/** 裂隙 / Rift */
	RIFT,
	/** 不稳定裂隙 / Volatile rift */
	VOLATILE_RIFT,
	/** 静态物体 / Static object */
	STATIC,
	/** 旗帜 / Flag */
	FLAG,
}
