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

import com.aionemu.gameserver.ai.RetailConditionSpawnEngine;
import com.aionemu.gameserver.lifecycle.GameEngineServices;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;

@XmlRootElement(name = "script_npcs")
@XmlAccessorType(XmlAccessType.FIELD)
public class ScriptNpcData {

	@XmlElement(name = "use_skill")
	private List<UseSkillScriptNpc> useSkills = new ArrayList<>();
	@XmlElement(name = "item_gate_variable")
	private List<ItemGateVariableScriptNpc> itemGateVariables = new ArrayList<>();

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
		for (ItemGateVariableScriptNpc script : itemGateVariables) {
			if (script.npcId <= 0 || script.worldId <= 0 || script.itemId <= 0 || script.itemCount <= 0
					|| script.variable == null || script.variable.isBlank() || script.failureMessageId <= 0
					|| script.successMessageId <= 0 || !npcIds.add(script.npcId)) {
				throw new IllegalStateException("Invalid or duplicate ScriptNpc definition for NPC " + script.npcId);
			}
			registry.registerScriptNpc(script);
		}
	}

	public List<UseSkillScriptNpc> getUseSkills() {
		return useSkills;
	}

	public List<ItemGateVariableScriptNpc> getItemGateVariables() {
		return itemGateVariables;
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

	@XmlAccessorType(XmlAccessType.FIELD)
	public static class ItemGateVariableScriptNpc implements ScriptNpc {

		@XmlAttribute(name = "npc_id", required = true)
		private int npcId;
		@XmlAttribute(name = "world_id", required = true)
		private int worldId;
		@XmlAttribute(name = "item_id", required = true)
		private int itemId;
		@XmlAttribute(name = "item_count")
		private int itemCount = 1;
		@XmlAttribute(name = "variable", required = true)
		private String variable;
		@XmlAttribute(name = "value", required = true)
		private int value;
		@XmlAttribute(name = "failure_message_id", required = true)
		private int failureMessageId;
		@XmlAttribute(name = "success_message_id", required = true)
		private int successMessageId;

		@Override
		public int getNpcId() {
			return npcId;
		}

		public int getWorldId() {
			return worldId;
		}

		public int getItemId() {
			return itemId;
		}

		public int getItemCount() {
			return itemCount;
		}

		public String getVariable() {
			return variable;
		}

		public int getValue() {
			return value;
		}

		public int getFailureMessageId() {
			return failureMessageId;
		}

		public int getSuccessMessageId() {
			return successMessageId;
		}

		@Override
		public boolean onItemUseFinish(Player player, Npc npc) {
			if (!npc.isInInstance() || npc.getPosition().getMapId() != worldId) {
				return false;
			}
			if (!player.getInventory().decreaseByItemId(itemId, itemCount)) {
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(failureMessageId));
				return true;
			}
			RetailConditionSpawnEngine.setVariable(
					npc.getPosition().getWorldMapInstance(), variable, value, 0);
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(successMessageId));
			return true;
		}
	}
}
