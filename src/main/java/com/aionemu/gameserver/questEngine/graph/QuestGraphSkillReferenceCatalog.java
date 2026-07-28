package com.aionemu.gameserver.questEngine.graph;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import com.aionemu.gameserver.dataholders.SkillData;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;

/** 从正式技能静态数据构造 skill-use 引用闭包。 / Builds skill-use reference closure from formal skill static data. */
public final class QuestGraphSkillReferenceCatalog {

	/** 禁止实例化纯静态目录构造器。 / Prevents instantiation of this static catalog builder. */
	private QuestGraphSkillReferenceCatalog() {
	}

	/** 构造全部正数技能模板 ID 的不可变引用集合。 / Builds an immutable reference set of all positive skill-template ids. */
	public static Set<Integer> build(SkillData skills) {
		Objects.requireNonNull(skills, "skills");
		Set<Integer> skillIds = new LinkedHashSet<>();
		for (SkillTemplate skill : Objects.requireNonNull(skills.getSkillTemplates(), "skill templates")) {
			if (skill == null || skill.getSkillId() <= 0) {
				throw new IllegalArgumentException("Skill reference is invalid");
			}
			skillIds.add(skill.getSkillId());
		}
		return Set.copyOf(skillIds);
	}
}
