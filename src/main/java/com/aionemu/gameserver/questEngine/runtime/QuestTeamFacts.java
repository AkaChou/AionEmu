package com.aionemu.gameserver.questEngine.runtime;

/**
 * 在任务事件边界捕获的不可变队伍成员资格事实。
 * Immutable team-membership facts captured at the quest event boundary.
 *
 * <p>任务引擎必须区分「已知 solo 的玩家」与「完全未捕获队伍状态的快照」。
 * 外层快照对后者使用 {@code null}；因此该值代表成功捕获，包括全 false 的 solo 情形。
 * The quest engine must distinguish a player who is known to be solo from
 * a snapshot that did not capture team state at all.  The enclosing snapshot
 * uses {@code null} for the latter; this value therefore represents a
 * successful capture, including the all-false solo case.</p>
 */
public record QuestTeamFacts(boolean inGroup, boolean inAlliance) {
}
