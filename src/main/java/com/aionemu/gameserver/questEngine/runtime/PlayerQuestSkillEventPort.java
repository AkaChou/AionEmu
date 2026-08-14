package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.definition.QuestSkillFacts;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/** 在技能成功回调处捕获施法者/目标身份。 / Captures the caster/target identity at the successful skill callback. */
public final class PlayerQuestSkillEventPort implements QuestSkillEventPort {
	@Override
	public QuestEvent.UseSkill useSkill(QuestEnv env, int skillId) {
		if (skillId <= 0) throw new IllegalArgumentException("skillId must be positive");
		if (env == null || env.getPlayer() == null) throw new IllegalArgumentException("skill caster is required");
		Player player = env.getPlayer();
		if (player.getPosition() == null || !player.isSpawned()
				|| player.getWorldId() <= 0 || player.getInstanceId() <= 0) {
			throw new IllegalStateException("skill caster world/instance is unavailable");
		}
		VisibleObject target = env.getVisibleObject();
		int targetObjectId = target == null ? 0 : target.getObjectId();
		int targetTemplateId = target instanceof Npc npc ? npc.getNpcId() : 0;
		int targetPlayerId = target instanceof Player targetPlayer ? targetPlayer.getObjectId() : 0;
		QuestSkillFacts facts = new QuestSkillFacts(player.getObjectId(), skillId, targetObjectId,
			targetTemplateId, targetPlayerId, player.getWorldId(), player.getInstanceId(), true);
		return new QuestEvent.UseSkill(skillId, facts);
	}
}
