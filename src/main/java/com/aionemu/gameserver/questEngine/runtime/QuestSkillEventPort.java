package com.aionemu.gameserver.questEngine.runtime;

import com.aionemu.gameserver.questEngine.definition.QuestEvent;
import com.aionemu.gameserver.questEngine.model.QuestEnv;

/** 验证后到达任务回调的技能施放边界。 / Boundary for a skill cast that has reached the quest callback after validation. */
public interface QuestSkillEventPort {
	QuestEvent.UseSkill useSkill(QuestEnv env, int skillId);
}
