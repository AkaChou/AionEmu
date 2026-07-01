package com.aionemu.gameserver.model.skinskill;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class SkillSkinListTest {

	@Test
	void storesSkillSkinsInJdkMap() throws Exception {
		SkillSkinList list = new SkillSkinList();

		Map<Integer, SkillSkin> skillSkins = skillSkins(list);

		assertEquals(HashMap.class, skillSkins.getClass());
	}

	@SuppressWarnings("unchecked")
	private Map<Integer, SkillSkin> skillSkins(SkillSkinList list) throws Exception {
		Field field = SkillSkinList.class.getDeclaredField("skillskins");
		field.setAccessible(true);
		return (Map<Integer, SkillSkin>) field.get(list);
	}
}
