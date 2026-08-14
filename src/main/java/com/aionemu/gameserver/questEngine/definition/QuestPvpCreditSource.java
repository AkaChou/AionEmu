package com.aionemu.gameserver.questEngine.definition;

/**
 * 由 {@code PvpService} 选定的权威 PvP 任务积分分发来源。
 * Authoritative PvP quest-credit fanout selected by {@code PvpService}.
 */
public enum QuestPvpCreditSource {
	SOLO,
	GROUP,
	ALLIANCE
}
