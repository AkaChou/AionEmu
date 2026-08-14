package com.aionemu.gameserver.model.skill;

import java.sql.Timestamp;

import com.aionemu.gameserver.configs.main.CraftConfig;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.stats.container.StatEnum;

/**
 * 玩家技能条目，用于技能相关逻辑。
 * Player Skill Entry for skill logic.
 *
 * @author ATracer
 */
public class PlayerSkillEntry extends SkillEntry {
	record TransactionState(int skillLevel, int skinId, Timestamp activeSkinTime, int expireTime,
		boolean activated, int currentXp, PersistentState persistentState) {
	}

	private boolean isStigma;
	private boolean isLinked;

	/**
	 * 制作技能的经验值。
	 * for crafting skills
	 */
	private int currentXp;

	private PersistentState persistentState;

	public PlayerSkillEntry(int skillId, boolean isStigma, boolean isLinked, int skillLvl, int skinId,
			Timestamp activeSkinTime, int expireTime, boolean isActivated, PersistentState persistentState) {
		super(skillId, skillLvl, skinId, activeSkinTime, expireTime, isActivated);
		this.isStigma = isStigma;
		this.isLinked = isLinked;
		this.persistentState = persistentState;
	}

	/**
	 * @return 是否为烙印之石技能 / Whether stigma skill
	 */
	public boolean isStigma() {
		return this.isStigma;
	}

	/**
	 * @return 是否为关联技能 / Whether linked
	 */
	public boolean isLinked() {
		return this.isLinked;
	}

	/** 设置技能等级 / Sets the skill lvl */
	public void setSkillLvl(int skillLevel) {
		super.setSkillLvl(skillLevel);
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/** 设置 skin id / Sets the skin id */
	public void setSkinId(int skinId) {
		super.setSkinId(skinId);
		setPersistentState(PersistentState.UPDATE_REQUIRED);
	}

	/** 设置 skin active time / Sets the skin active time */
	public void setSkinActiveTime(Timestamp creationDate) {
		super.setSkinActiveTime(creationDate);
	}

	/** 设置 skin expire time / Sets the skin expire time */
	public void setSkinExpireTime(int minutes) {
		super.setSkinExpireTime(minutes);
	}

	/**
	 * @return 技能附加等级 / The skill extra lvl
	 */
	public int getExtraLvl() {
		switch (skillId) {
		case 30002:
		case 30003:
			if (skillLevel > 399 && skillLevel < 500) {
				return 4;
			}
		case 40001:
		case 40002:
		case 40003:
		case 40004:
		case 40007:
		case 40008:
		case 40010:
			if (skillLevel > 449 && skillLevel < 500) {
				return 5;
			} else if (skillLevel > 499 && skillLevel < 550) {
				return 6;
			} else {
				return skillLevel / 100;
			}
		}
		return 0;
	}

	/**
	 * @return 当前经验 / Current XP
	 */
	public int getCurrentXp() {
		return currentXp;
	}

	/**
	 * @param currentXp 设置的经验 / Current XP to set
	 */
	public void setCurrentXp(int currentXp) {
		this.currentXp = currentXp;
	}

	/**
	 * 为制作技能添加经验，达到阈值时升级。
	 * Add XP to a craft skill and level it up when the threshold is reached.
	 *
	 * @param player 玩家 / Player
	 * @param xp 获得的经验 / XP gained
	 * @return 是否升级 / Whether the skill leveled up
	 */
	public boolean addSkillXp(Player player, int xp) {
		this.currentXp += xp;
		int requiredExp = (int) (0.23 * (skillLevel + 17.2) * (skillLevel + 17.2));
		StatEnum boostStat = StatEnum.getModifier(skillId);
		if (boostStat != null) {
			float statRate = player.getGameStats().getStat(boostStat, 100).getCurrent() / 100f;
			if (statRate > 0) {
				requiredExp /= statRate;
			}
		}
		if (currentXp > requiredExp) {
			if (CraftConfig.UNABLE_CRAFT_SKILLS_UNRESTRICTED_LEVELUP == true) {
				float skillUpRatio = (currentXp / (0.23f * (skillLevel + 17.2f) * (skillLevel + 17.2f)));
				int skillUp = skillLevel + (int) skillUpRatio;

				if (skillLevel > 0 && skillLevel < 99) {
					if (skillUp > 99) {
						skillUp = 99;
					}
				} else if (skillLevel > 99 && skillLevel < 199) {
					if (skillUp > 199) {
						skillUp = 199;
					}
				} else if (skillLevel > 199 && skillLevel < 299) {
					if (skillUp > 299) {
						skillUp = 299;
					}
				} else if (skillLevel > 299 && skillLevel < 399) {
					if (skillUp > 399) {
						skillUp = 399;
					}
				} else if (skillLevel > 399 && skillLevel < 449) {
					if (skillUp > 449) {
						skillUp = 449;
					}
				} else if (skillLevel > 449 && skillLevel < 499) {
					if (skillUp > 499) {
						skillUp = 499;
					}
				} else if (skillLevel > 499 && skillLevel < 549) {
					if (skillUp > 549) {
						skillUp = 549;
					}
				}
				setSkillLvl(skillUp);
				currentXp = 0;
			} else {
				setSkillLvl(skillLevel + 1);
				currentXp = 0;
			}
			return true;
		}
		return false;
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
			} else
				this.persistentState = PersistentState.DELETED;
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

	TransactionState transactionState() {
		return new TransactionState(skillLevel, skinId, activeSkinTime, expireTime, isActivated,
			currentXp, persistentState);
	}

	void restoreTransactionState(TransactionState state) {
		skillLevel = state.skillLevel();
		skinId = state.skinId();
		activeSkinTime = state.activeSkinTime();
		expireTime = state.expireTime();
		isActivated = state.activated();
		currentXp = state.currentXp();
		persistentState = state.persistentState();
	}
}
