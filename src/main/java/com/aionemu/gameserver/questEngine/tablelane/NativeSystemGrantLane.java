package com.aionemu.gameserver.questEngine.tablelane;

import java.util.Set;

import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.retail.RetailGrantKind;

/**
 * 原生车道的系统发放面（计划 §7 P4 收口）。
 * <p>
 * 真端族表把「接取」写成类别哨兵（{@code _faction_} / {@code _area_} / {@code _challengetask_}）时，
 * 任务不由 NPC 对话受理，而由对应子系统发放（阵营日常轮换、进区域、挑战任务）。该面在真端是**引擎级**
 * 单一入口，与家族无关；本接口把已切换家族各自的行集暴露给同一个上层消费点
 * （{@link NativeSystemGrantLanes} → {@code NpcFactions}），避免每切一族就要在上层加一次分支。
 * <p>
 * System-grant surface of the native lane: the retail family tables express "acquire" as a category
 * sentinel for subsystem-granted quests. The retail engine serves that surface uniformly, so every
 * switched family exposes its rows through this interface and the caller ({@link NativeSystemGrantLanes})
 * aggregates them instead of growing one branch per family.
 */
public interface NativeSystemGrantLane {

	/** 该行是否属于本车道的注册集。 / Whether the row belongs to this lane's registration set. */
	boolean owns(int questId);

	/** 注册集（表全量行）。 / The registration set (every table row of this family). */
	Set<Integer> ownedQuestIds();

	/** 路由集（注册集 − XML-only − 不可路由）。 / The routing set (registration minus XML-owned and unroutable). */
	Set<Integer> routedQuestIds();

	/** 该行是否由本车道路由（注册集 − XML-only − 不可路由）。 / Whether this lane routes the row. */
	boolean routes(int questId);

	/** 接取名类别（真端哨兵 = 系统发放）。 / The acquire-name category (a sentinel means system-granted). */
	RetailGrantKind grantKind(int questId);

	/** 真端 {@code quest.xml} 的势力 id（{@code npcfaction_name}；无则 0）。 / The retail faction id, or 0. */
	int factionId(int questId);

	/** 指定势力当前可轮换的已路由任务 id。 / Faction-rotation candidates of one faction. */
	Set<Integer> factionRotationCandidates(int factionId);

	/** 是否系统发放（哨兵行且本服确有该发放入口）。 / Whether the row is system-granted here. */
	boolean isSystemGranted(int questId);

	/** 阵营日常轮换资格（真端 {@code quest.xml} 轴直读）。 / Faction-rotation eligibility from the retail axes. */
	boolean factionRotationEligible(Player player, int questId, int factionId);

	/** 系统发放入口：无进度时直接建档到 START。 / The grant entry point: create the row at START. */
	boolean grantSystemStart(Player player, int questId);
}
