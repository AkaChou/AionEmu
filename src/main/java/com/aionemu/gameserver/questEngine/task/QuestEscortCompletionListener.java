package com.aionemu.gameserver.questEngine.task;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/**
 * 接收一次护送检查的唯一终态，并允许 graph owner 在不改变旧 Handler 分发的情况下接入 typed 信号。
 * Receives the single terminal outcome of an escort check and lets graph owners attach typed signals without changing
 * legacy Handler dispatch.
 */
public interface QuestEscortCompletionListener {

	/** 护送 NPC 已到达目标。 / The escort NPC reached its destination. */
	void onReached(QuestEnv env, Npc follower);

	/** 护送 NPC 已死亡、玩家死亡或超出允许距离。 / The escort NPC died, its player died, or it exceeded the allowed range. */
	void onLost(QuestEnv env, Npc follower);

	/** 返回保持现有 QuestEngine 行为的 listener。 / Returns the listener preserving existing QuestEngine behavior. */
	static QuestEscortCompletionListener legacyQuestEngine() {
		return new QuestEscortCompletionListener() {
			@Override
			public void onReached(QuestEnv env, Npc follower) {
				GameEngineServices.questEngine().onNpcReachTarget(env);
			}

			@Override
			public void onLost(QuestEnv env, Npc follower) {
				GameEngineServices.questEngine().onNpcLostTarget(env);
			}
		};
	}

	/**
	 * 先执行旧 Handler 分发，再执行 typed owner listener；typed 失败不会被吞掉。
	 * Runs legacy Handler dispatch first and then the typed-owner listener; typed failures are not swallowed.
	 */
	static QuestEscortCompletionListener legacyAnd(QuestEscortCompletionListener typedOwner) {
		if (typedOwner == null) {
			throw new IllegalArgumentException("Typed escort completion listener is missing");
		}
		QuestEscortCompletionListener legacy = legacyQuestEngine();
		return new QuestEscortCompletionListener() {
			@Override
			public void onReached(QuestEnv env, Npc follower) {
				legacy.onReached(env, follower);
				typedOwner.onReached(env, follower);
			}

			@Override
			public void onLost(QuestEnv env, Npc follower) {
				legacy.onLost(env, follower);
				typedOwner.onLost(env, follower);
			}
		};
	}
}
