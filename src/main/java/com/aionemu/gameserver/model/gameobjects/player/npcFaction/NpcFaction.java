package com.aionemu.gameserver.model.gameobjects.player.npcFaction;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import lombok.Getter;

/**
 * NPC 势力游戏对象。
 * Npc Faction game object.
 *
 * @author MrPoke
 */
public class NpcFaction {

	/**
	 * @return 势力 ID / Faction ID
	 */
	@Getter
	private final int id;
	/**
	 * @return 剩余计时（秒） / Remaining time in seconds
	 */
	@Getter
	private int time;
	/**
	 * @return 是否激活 / Whether active
	 */
	@Getter
	private boolean active;
	/**
	 * @return 是否导师 NPC / Whether mentor
	 */
	@Getter
	private final boolean mentor;
	/**
	 * @return 关联任务状态 / Quest state
	 */
	@Getter
	private ENpcFactionQuestState state;
	/**
	 * @return 关联任务 ID / Associated quest ID
	 */
	@Getter
	private int questId;
	/**
	 * @return 持久化状态 / Persistent state
	 */
	@Getter
	private PersistentState persistentState;

	/**
	 * 创建 NPC 势力实例。
	 * Create a NpcFaction instance.
	 *
	 * @param id 势力 ID / Faction ID
	 * @param time 剩余计时（秒） / Remaining time in seconds
	 * @param active 是否激活 / Whether active
	 * @param state 关联任务状态 / Quest state
	 * @param questId 关联任务 ID / Associated quest ID
	 */
	public NpcFaction(int id, int time, boolean active, ENpcFactionQuestState state, int questId) {
		this.id = id;
		this.time = time;
		this.active = active;
		this.state = state;
		this.mentor = DataManager.NPC_FACTIONS_DATA.getNpcFactionById(id).isMentor();
		this.questId = questId;
		this.persistentState = PersistentState.NEW;
	}

	/**
	 * @param time 设置的剩余计时（秒） / Remaining time to set in seconds
	 */
	public void setTime(int time) {
		this.time = time;
		this.setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * @param active 设置的激活状态 / Active state to set
	 */
	public void setActive(boolean active) {
		this.active = active;
		this.setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * @param state 设置的任务状态 / Quest state to set
	 */
	public void setState(ENpcFactionQuestState state) {
		this.setPersistentState(PersistentState.UPDATE_REQUIRED);
		this.state = state;
	}

	/**
	 * @param questId 设置的关联任务 ID / Associated quest ID to set
	 */
	public void setQuestId(int questId) {
		this.questId = questId;
		this.setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * @param persistentState 设置的持久化状态 / Persistent state to set
	 */
	public void setPersistentState(PersistentState persistentState) {
		switch (persistentState) {
		case DELETED:
			if (this.persistentState == PersistentState.NEW) {
				this.persistentState = PersistentState.NOACTION;
			} else {
				this.persistentState = PersistentState.DELETED;
			}
			break;
		case UPDATE_REQUIRED:
			if (this.persistentState != PersistentState.NEW) {
				this.persistentState = PersistentState.UPDATE_REQUIRED;
			}
			break;
		case NOACTION:
			break;
		default:
			this.persistentState = persistentState;
		}
	}
}
