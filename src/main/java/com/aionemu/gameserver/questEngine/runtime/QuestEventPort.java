package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Set;

/** 权威玩家事实的只读领域边界。 / Read-only domain boundary for authoritative player facts. */
public interface QuestEventPort {
	QuestSnapshot snapshot(Connection connection, int playerId, int questId, QuestEvent event) throws SQLException;

	/**
	 * 不获取数据库连接地捕获只读玩家事实。生产快照来自在线玩家内存；保留旧入口仅供兼容自定义端口。
	 * Captures read-only player facts without acquiring a database connection. Production snapshots come from live
	 * player memory; the legacy entry point remains only for custom-port compatibility.
	 */
	default QuestSnapshot snapshot(int playerId, int questId, QuestEvent event) throws SQLException {
		return snapshot(null, playerId, questId, event);
	}

	/** 仅为实际声明该条件的转换捕获可选开始资格事实。 / Capture optional start-eligibility facts only for a transition that actually declares that condition. */
	default QuestSnapshot snapshot(Connection connection, int playerId, int questId, QuestEvent event,
			boolean includeStartEligibility) throws SQLException {
		return snapshot(connection, playerId, questId, event);
	}

	/** 仅捕获所选转换引用的事件服务事实。 / Captures only event-service facts referenced by the selected transition. */
	default QuestSnapshot snapshot(Connection connection, int playerId, int questId, QuestEvent event,
			boolean includeStartEligibility, Set<Integer> eventActivityQuestIds) throws SQLException {
		return snapshot(connection, playerId, questId, event, includeStartEligibility);
	}

	/** 仅捕获所选转换引用的只读事实，不初始化事务或连接。 / Captures selected read-only facts without initializing a transaction or connection. */
	default QuestSnapshot snapshot(int playerId, int questId, QuestEvent event,
			boolean includeStartEligibility, Set<Integer> eventActivityQuestIds) throws SQLException {
		return snapshot(null, playerId, questId, event, includeStartEligibility, eventActivityQuestIds);
	}

	/** 按需捕获需要遍历当前实例的世界事实。 / Captures world-instance facts only when selected conditions require them. */
	default QuestSnapshot snapshot(int playerId, int questId, QuestEvent event,
			boolean includeStartEligibility, Set<Integer> eventActivityQuestIds,
			boolean includeWorldFacts) throws SQLException {
		return snapshot(playerId, questId, event, includeStartEligibility, eventActivityQuestIds);
	}

	/**
	 * 按 transition 推导出的最小事实集捕获只读事实。
	 * Captures read-only facts according to the minimal fact set derived from the transition.
	 *
	 * <p>默认实现回落到三个旧门控（其余事实族保守全采），因此自定义端口与测试替身无需改动即可继续工作。
	 * The default implementation falls back to the three legacy gates with every other family captured, so
	 * custom ports and test doubles keep working unchanged.</p>
	 */
	default QuestSnapshot snapshot(int playerId, int questId, QuestEvent event,
			QuestFactRequirements requirements) throws SQLException {
		Objects.requireNonNull(requirements, "requirements");
		return snapshot(playerId, questId, event, requirements.startEligibility(),
			requirements.eventActivityQuestIds(), requirements.worldFacts());
	}
}
