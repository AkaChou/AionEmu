package com.aionemu.gameserver.questEngine.runtime;

/**
 * Immutable team-membership facts captured at the quest event boundary.
 *
 * <p>The quest engine must distinguish a player who is known to be solo from
 * a snapshot that did not capture team state at all.  The enclosing snapshot
 * uses {@code null} for the latter; this value therefore represents a
 * successful capture, including the all-false solo case.</p>
 */
public record QuestTeamFacts(boolean inGroup, boolean inAlliance) {
}
