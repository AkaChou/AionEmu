package com.aionemu.gameserver.ai2;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.dataholders.NpcSkillData;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.skill.NpcSkillList;
import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplate;
import com.aionemu.gameserver.model.templates.npcskill.NpcSkillTemplates;
import com.aionemu.gameserver.scriptEngine.ScriptEngine;
import com.aionemu.gameserver.scriptEngine.ScriptNpc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AI2ActionsScriptNpcRoutingTest {

	@AfterEach
	void clearScripts() {
		GameEngineServices.scriptEngine().getRegistry().clear();
	}

	@Test
	void scriptConsumerOwnsCompletedUseWhenItReturnsTrue() {
		ScriptEngine engine = GameEngineServices.scriptEngine();
		engine.getRegistry().registerScriptNpc(new ScriptNpc() {
			@Override
			public int getNpcId() {
				return 700437;
			}

			@Override
			public boolean onItemUseFinish(com.aionemu.gameserver.model.gameobjects.player.Player player,
				com.aionemu.gameserver.model.gameobjects.Npc npc) {
				return true;
			}
		});

		assertTrue(AI2Actions.dispatchScriptNpcItemUseFinish(700437, null, null));
	}

	@Test
	void missingOrNonOwningScriptFallsBackToInstanceHandler() {
		assertFalse(AI2Actions.dispatchScriptNpcItemUseFinish(700437, null, null));
	}

	@Test
	void protectBuffRequiresOneCompleteRetailSkillSlot() throws Exception {
		NpcSkillData previous = DataManager.NPC_SKILL_DATA;
		try {
			NpcSkillList complete = skills(new NpcSkillTemplate(276, 16, 100));
			assertEquals(276, AI2Actions.selectRetailProtectBuffSkill("NPC_AI_ProtectBuff", complete).getSkillId());
			assertNull(AI2Actions.selectRetailProtectBuffSkill("npc", complete));

			assertNull(AI2Actions.selectRetailProtectBuffSkill("NPC_AI_ProtectBuff", skills()));
			assertNull(AI2Actions.selectRetailProtectBuffSkill("NPC_AI_ProtectBuff",
				skills(new NpcSkillTemplate(276, 16, 100), new NpcSkillTemplate(277, 16, 100))));
			assertNull(AI2Actions.selectRetailProtectBuffSkill("NPC_AI_ProtectBuff",
				skills(new NpcSkillTemplate(276, 0, 100))));
		} finally {
			DataManager.NPC_SKILL_DATA = previous;
		}
	}

	private static NpcSkillList skills(NpcSkillTemplate... templates) throws Exception {
		DataManager.NPC_SKILL_DATA = new NpcSkillData(List.of(new NpcSkillTemplates(1, List.of(templates))));
		var constructor = NpcSkillList.class.getDeclaredConstructor(int.class);
		constructor.setAccessible(true);
		return constructor.newInstance(1);
	}
}
