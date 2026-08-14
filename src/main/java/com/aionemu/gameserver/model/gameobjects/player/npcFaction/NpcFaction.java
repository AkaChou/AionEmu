package com.aionemu.gameserver.model.gameobjects.player.npcFaction;

import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.gameobjects.PersistentState;

/**
 * NPC 势力游戏对象。
 * Npc Faction game object.
 *
 * @author MrPoke
 */
public class NpcFaction {

	private int id;
	private int time;
	private boolean active;
	private boolean mentor;
	private ENpcFactionQuestState state;
	private int questId;
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
	 * @return 势力 ID / Faction ID
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return 剩余计时（秒） / Remaining time in seconds
	 */
	public int getTime() {
		return time;
	}

	/**
	 * @return 是否激活 / Whether active
	 */
	public boolean isActive() {
		return active;
	}

	/**
	 * @return 是否导师 NPC / Whether mentor
	 */
	public boolean isMentor() {
		return mentor;
	}

	/**
	 * @return 关联任务状态 / Quest state
	 */
	public ENpcFactionQuestState getState() {
		return state;
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
	 * @return 关联任务 ID / Associated quest ID
	 */
	public int getQuestId() {
		return questId;
	}

	/**
	 * @param questId 设置的关联任务 ID / Associated quest ID to set
	 */
	public void setQuestId(int questId) {
		this.questId = questId;
		this.setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/**
	 * @return 持久化状态 / Persistent state
	 */
	public PersistentState getPersistentState() {
		return persistentState;
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
