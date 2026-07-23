package com.aionemu.gameserver.scriptEngine;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;

@XmlRootElement(name = "script_npcs")
@XmlAccessorType(XmlAccessType.FIELD)
public class ScriptNpcData {

	@XmlElement(name = "use_skill")
	private List<UseSkillScriptNpc> useSkills = new ArrayList<>();

	public static ScriptNpcData load(File file) {
		try {
			return (ScriptNpcData) JAXBContext.newInstance(ScriptNpcData.class).createUnmarshaller().unmarshal(file);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to load ScriptNpc definitions from " + file, e);
		}
	}

	public void register(ScriptRegistry registry) {
		Set<Integer> npcIds = new HashSet<>();
		for (UseSkillScriptNpc script : useSkills) {
			if (script.npcId <= 0 || script.skillId <= 0 || script.skillLevel <= 0 || !npcIds.add(script.npcId)) {
				throw new IllegalStateException("Invalid or duplicate ScriptNpc definition for NPC " + script.npcId);
			}
			registry.registerScriptNpc(script);
		}
	}

	public List<UseSkillScriptNpc> getUseSkills() {
		return useSkills;
	}

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class UseSkillScriptNpc implements ScriptNpc {

		@XmlAttribute(name = "npc_id", required = true)
		private int npcId;
		@XmlAttribute(name = "skill_id", required = true)
		private int skillId;
		@XmlAttribute(name = "skill_level")
		private int skillLevel = 1;
		@XmlAttribute(name = "despawn_on_success")
		private boolean despawnOnSuccess;

		@Override
		public int getNpcId() {
			return npcId;
		}

		public int getSkillId() {
			return skillId;
		}

		public int getSkillLevel() {
			return skillLevel;
		}

		public boolean isDespawnOnSuccess() {
			return despawnOnSuccess;
		}

		@Override
		public boolean onItemUseFinish(Player player, Npc npc) {
			boolean success = GameEngineServices.skillEngine().getSkill(npc, skillId, skillLevel, player)
					.useNoAnimationSkill();
			if (success && despawnOnSuccess) {
				npc.getController().onDelete();
			}
			return true;
		}
	}
}
