package com.aionemu.gameserver.model.gameobjects.player;

import com.aionemu.gameserver.lifecycle.GameCreativityServices;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.GameServer;
import com.aionemu.gameserver.configs.main.AdvCustomConfig;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.configs.main.GSConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.Gender;
import com.aionemu.gameserver.model.PlayerClass;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.team.legion.LegionJoinRequestState;
import com.aionemu.gameserver.model.templates.BoundRadius;
import com.aionemu.gameserver.model.templates.VisibleObjectTemplate;
import com.aionemu.gameserver.model.templates.event.AtreianPassport;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABYSS_FAVOR;
import com.aionemu.gameserver.network.aion.serverpackets.SM_DP_INFO;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATUPDATE_DP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_STATUPDATE_EXP;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.player.CreativityPanel.CreativityEssenceService;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.stats.XPLossEnum;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldPosition;

/**
 * 玩家公共数据。
 * Player Common Data game object.
 */

@Slf4j
public class PlayerCommonData extends VisibleObjectTemplate {
	private final int playerObjId;
	private Race race;
	private String name;
	private PlayerClass playerClass;
	private int level = 0;
	private long exp = 0;
	private long expRecoverable = 0;
	private Gender gender;
	private Timestamp lastOnline = new Timestamp(Calendar.getInstance().getTime().getTime() - 20);
	private Timestamp lastStamp = new Timestamp(Calendar.getInstance().getTime().getTime() - 20);
	private boolean online;
	private String note;
	private WorldPosition position;
	private int questExpands = 0;
	private int npcExpands = AdvCustomConfig.CUBE_SIZE;
	private int warehouseSize = 0;
	private int AdvancedStigmaSlotSize = 0;
	private int titleId = -1;
	private int bonusTitleId = -1;
	private int dp = 0;
	private int mailboxLetters;
	private int soulSickness = 0;
	private boolean noExp = false;
	private double expMultiplier = 1.0; // 默认100%经验值
	private long reposteCurrent;
	private long reposteMax;
	private long salvationPoint;
	private int mentorFlagTime;
	private int worldOwnerId;
	private BoundRadius boundRadius;
	private long lastTransferTime;
	private int stamps = 0;
	private int passportReward = 0;
	public Map<Integer, AtreianPassport> playerPassports = new HashMap<Integer, AtreianPassport>(1);
	private PlayerPassports completedPassports;
	private boolean isArchDaeva = false;
	private int creativityPoint;
	private int cp_step = 0;
	private int stoneCreativityPoint;
	private int joinRequestLegionId = 0;
	private LegionJoinRequestState joinRequestState = LegionJoinRequestState.NONE;
	private int lunaConsumePoint;
	private int muni_keys;
	private int consumeCount = 0;
	private int wardrobeSlot;
	private PlayerUpgradeArcade upgradeArcade;
	// 成长光环 5.0 / Aura Of Growth 5.0
	private long auraOfGrowth;
	private long auraOfGrowthMax;
	// 伯丁之星 5.1 / Berdin's Star 5.1
	private long berdinStar;
	private long berdinStarMax = 1125000000; // 5.6
	private boolean BerdinStarBoost = false;
	// 欧比斯眷顾 5.3 / Abyss Favor 5.3
	private long abyssFavor;
	private long abyssFavorMax = 1000000;
	private boolean AbyssFavorBoost = false;
	// 挑战之塔 5.6 / Tower Of Challenge 5.6
	private int floor;
	// 术古扫荡 5.1 / Shugo Sweep 5.1
	private int goldenDice;
	private int resetBoard;
	// 阿特雷亚护照创建日期 / Atreian Passport Creation Date
	private Timestamp creationDate;
	private int minionSkillPoints;
	// ponytail: 会话设置；仅当自动充值须在重登后保留时才持久化。 / session setting; persist it only if auto-charge must survive relogging.
	private boolean minionSkillPointsAutoCharge;
	private Timestamp minionFunctionTime;

	public PlayerCommonData(int objId) {
		this.playerObjId = objId;
	}

	public int getPlayerObjId() {
		return playerObjId;
	}

	public long getExp() {
		return this.exp;
	}

	public int getQuestExpands() {
		return this.questExpands;
	}

	public void setQuestExpands(int questExpands) {
		this.questExpands = questExpands;
	}

	public void setNpcExpands(int npcExpands) {
		this.npcExpands = npcExpands;
	}

	public int getNpcExpands() {
		return npcExpands;
	}

	/**
	 * @return the AdvancedStigmaSlotSize
	 */
	public int getAdvancedStigmaSlotSize() {
		return AdvancedStigmaSlotSize;
	}

	/**
	 * @param AdvancedStigmaSlotSize the AdvancedStigmaSlotSize to set
	 */
	public void setAdvancedStigmaSlotSize(int AdvancedStigmaSlotSize) {
		this.AdvancedStigmaSlotSize = AdvancedStigmaSlotSize;
	}

	public long getExpShown() {
		return this.exp - DataManager.PLAYER_EXPERIENCE_TABLE.getStartExpForLevel(this.level);
	}

	public long getExpNeed() {
		if (this.level == DataManager.PLAYER_EXPERIENCE_TABLE.getMaxLevel()) {
			return 0;
		}
		return DataManager.PLAYER_EXPERIENCE_TABLE.getStartExpForLevel(this.level + 1)
				- DataManager.PLAYER_EXPERIENCE_TABLE.getStartExpForLevel(this.level);
	}

	/**
	 * 计算损失经验，须在 setExp 之前调用。
	 * Calculate the lost experience, must be called before setexp.
	 */
	public void calculateExpLoss() {
		long expLost = XPLossEnum.getExpLoss(this.level, this.getExpNeed());
		int unrecoverable = (int) (expLost * 0.33333333);
		int recoverable = (int) expLost - unrecoverable;
		long allExpLost = recoverable + this.expRecoverable;
		if (this.getExpShown() > unrecoverable) {
			this.exp = this.exp - unrecoverable;
		} else {
			this.exp = this.exp - this.getExpShown();
		}
		if (this.getExpShown() > recoverable) {
			this.expRecoverable = allExpLost;
			this.exp = this.exp - recoverable;
		} else {
			this.expRecoverable = this.expRecoverable + this.getExpShown();
			this.exp = this.exp - this.getExpShown();
		}
		if (expRecoverable > getExpNeed() * 0.25D) {
			expRecoverable = Math.round(getExpNeed() * 0.25D);
		}
		if (this.getPlayer() != null) {
			PacketSendUtility.sendPacket(getPlayer(),
					new SM_STATUPDATE_EXP(getExpShown(), getExpRecoverable(), getExpNeed(),
							this.getCurrentReposteEnergy(), this.getMaxReposteEnergy(), this.getBerdinStar(),
							this.getAuraOfGrowth()));
		}
	}

	public void setRecoverableExp(long expRecoverable) {
		this.expRecoverable = expRecoverable;
	}

	public void resetRecoverableExp() {
		long el = this.expRecoverable;
		this.expRecoverable = 0;
		this.setExp(this.exp + el, false);
	}

	public long getExpRecoverable() {
		return this.expRecoverable;
	}

public double getExpMultiplier() {
        return expMultiplier;
    }

    public void setExpMultiplier(double expMultiplier) {
        this.expMultiplier = expMultiplier;
    }

	/**
	 * @param value
	 */
	public void addExp(long value, int npcNameId) {
		this.addExp(value, null, npcNameId, "");
	}

	public void addExp(long value, RewardType rewardType) {
		this.addExp(value, rewardType, 0, "");
	}

	public void addExp(long value, RewardType rewardType, int npcNameId) {
		this.addExp(value, rewardType, npcNameId, "");
	}

	public void addExp(long value, RewardType rewardType, String name) {
		this.addExp(value, rewardType, 0, name);
	}

	public void addExp(long value, RewardType rewardType, int npcNameId, String name) {
		if (this.noExp) {
			return;
		}
		long reward = (long) (value * expMultiplier);
		if ((getPlayer() != null) && (rewardType != null)) {
			reward = rewardType.calcReward(getPlayer(), reward); 
		}
		long repose = 0;
		if ((isReadyForReposteEnergy()) && (getCurrentReposteEnergy() > 0)) {
			repose = (long) (reward / 100.0 * 40.0);
			addReposteEnergy(-repose);
		}
		long salvation = 0;
		if ((isReadyForSalvationPoints()) && (getCurrentSalvationPercent() > 0)) {
			salvation = (long) (reward / 100.0 * getCurrentSalvationPercent());
		}
		long berdinStar = 0;
		long berdinStarBoost = 0;
		if ((isReadyForBerdinStar()) && (getBerdinStar() > 0)) {
			berdinStar = reward;
			addBerdinStar(1575000); // 0.14%
			if (BerdinStarBoost) {
				berdinStarBoost = (long) (reward / 100.0 * 50.0);
			}
		}
		long abyssFavor = 0;
		long abyssFavorBoost = 0;
		if ((isReadyForAbyssFavor()) && (getAbyssFavor() > 0)) {
			abyssFavor = reward;
			addAbyssFavor(1500); // 0.15%
			if (AbyssFavorBoost) {
				abyssFavorBoost = (long) (reward / 100.0 * 50.0);
			}
		}
		long auraOfGrowth = 0;
		if ((isReadyForAuraOfGrowth()) && (getAuraOfGrowth() > 0)) {
			auraOfGrowth = (long) (reward / 100.0 * 60.0);
		}
		if ((getPlayer() != null) && (rewardType != null)) {
			if ((rewardType == RewardType.HUNTING) || (rewardType == RewardType.GROUP_HUNTING)
					|| (rewardType == RewardType.CRAFTING) || (rewardType == RewardType.GATHERING)
					|| (rewardType == RewardType.MONSTER_BOOK)) {
				reward += repose + berdinStar + berdinStarBoost + auraOfGrowth + abyssFavor + abyssFavorBoost;
			} else {
				reward += repose;
			}
		}
		setExp(exp + reward, false);
		if ((getPlayer() != null) && (rewardType != null)) {
			switch (rewardType) {
			case HUNTING:
			case GROUP_HUNTING:
			case CRAFTING:
			case GATHERING:
			case MONSTER_BOOK:
				if (npcNameId == 0) {
					PacketSendUtility.sendPacket(getPlayer(), SM_SYSTEM_MESSAGE.STR_GET_EXP2(reward));
				} else {
					PacketSendUtility.sendPacket(getPlayer(),
							SM_SYSTEM_MESSAGE.STR_GET_EXP_DESC(new DescriptionId(npcNameId * 2 + 1), reward));
					if (repose > 0) {
						PacketSendUtility.sendPacket(getPlayer(),
								SM_SYSTEM_MESSAGE.STR_GET_EXP_DESC(new DescriptionId(2805577), repose));
					}
					if (auraOfGrowth > 0) {
						PacketSendUtility.sendPacket(getPlayer(),
								SM_SYSTEM_MESSAGE.STR_GET_EXP_DESC(new DescriptionId(2806377), auraOfGrowth));
					}
					if (berdinStar > 0) {
						PacketSendUtility.sendPacket(getPlayer(),
								SM_SYSTEM_MESSAGE.STR_GET_EXP_DESC(new DescriptionId(2806671), berdinStar));
					}
					if (abyssFavor > 0) {
						PacketSendUtility.sendPacket(getPlayer(),
								SM_SYSTEM_MESSAGE.STR_GET_EXP_DESC(new DescriptionId(2808053), abyssFavor));
					}
				}
				break;
			case EXACT:
			case QUEST:
				if (npcNameId == 0) {
					PacketSendUtility.sendPacket(getPlayer(), SM_SYSTEM_MESSAGE.STR_GET_EXP2(reward));
				} else if ((repose > 0) && (salvation > 0)) {
					PacketSendUtility.sendPacket(getPlayer(), SM_SYSTEM_MESSAGE.STR_GET_EXP_VITAL_MAKEUP_BONUS_DESC(
							new DescriptionId(npcNameId * 2 + 1), reward, repose, salvation));
				} else if ((repose > 0) && (salvation == 0)) {
					PacketSendUtility.sendPacket(getPlayer(), SM_SYSTEM_MESSAGE
							.STR_GET_EXP_VITAL_BONUS_DESC(new DescriptionId(npcNameId * 2 + 1), reward, repose));
				} else if ((repose == 0) && (salvation > 0)) {
					PacketSendUtility.sendPacket(getPlayer(), SM_SYSTEM_MESSAGE
							.STR_GET_EXP_MAKEUP_BONUS_DESC(new DescriptionId(npcNameId * 2 + 1), reward, salvation));
				} else {
					PacketSendUtility.sendPacket(getPlayer(),
							SM_SYSTEM_MESSAGE.STR_GET_EXP_DESC(new DescriptionId(npcNameId * 2 + 1), reward));
				}
				break;
			case PVP_KILL:
				if ((repose > 0) && (salvation > 0)) {
					PacketSendUtility.sendPacket(getPlayer(),
							SM_SYSTEM_MESSAGE.STR_GET_EXP_VITAL_MAKEUP_BONUS(name, reward, repose, salvation));
				} else if ((repose > 0) && (salvation == 0)) {
					PacketSendUtility.sendPacket(getPlayer(),
							SM_SYSTEM_MESSAGE.STR_GET_EXP_VITAL_BONUS(name, reward, repose));
				} else if ((repose == 0) && (salvation > 0)) {
					PacketSendUtility.sendPacket(getPlayer(),
							SM_SYSTEM_MESSAGE.STR_GET_EXP_MAKEUP_BONUS(name, reward, salvation));
				} else {
					PacketSendUtility.sendPacket(getPlayer(), SM_SYSTEM_MESSAGE.STR_GET_EXP(name, reward));
				}
				break;
			default:
				break;
			}
			if (this.isArchDaeva()) {
				GameCreativityServices.creativityEssenceService().pointPerExp(this.getPlayer());
			}
		}
	}

	public boolean isReadyForSalvationPoints() {
		return level >= 15 && level < GSConfig.PLAYER_MAX_LEVEL + 1;
	}

	public boolean isReadyForReposteEnergy() {
		return CustomConfig.ENERGY_OF_REPOSE_ENABLE && level >= 10;
	}

	public void addReposteEnergy(long add) {
		if (!this.isReadyForReposteEnergy()) {
			return;
		}
		reposteCurrent += add;
		if (reposteCurrent < 0) {
			reposteCurrent = 0;
		} else if (reposteCurrent > getMaxReposteEnergy()) {
			reposteCurrent = getMaxReposteEnergy();
		}
	}

	public void updateMaxReposte() {
		if (!isReadyForReposteEnergy()) {
			reposteCurrent = 0;
			reposteMax = 0;
		} else {
			reposteMax = (long) (getExpNeed() * 0.25f); // 零售版 99% / Retail 99%
		}
	}

	public void setCurrentReposteEnergy(long value) {
		reposteCurrent = value;
	}

	public long getCurrentReposteEnergy() {
		return isReadyForReposteEnergy() ? this.reposteCurrent : 0;
	}

	public long getMaxReposteEnergy() {
		return isReadyForReposteEnergy() ? this.reposteMax : 0;
	}

	public void setExp(long exp, boolean ArchDaeva) {
		int maxLevel = DataManager.PLAYER_EXPERIENCE_TABLE.getMaxLevel();
		long maxExp = DataManager.PLAYER_EXPERIENCE_TABLE.getStartExpForLevel(maxLevel);
		if (getPlayerClass() != null && getPlayerClass().isStartingClass()) {
			maxLevel = 10;
			if (this.getLevel() == 9 && this.getExp() >= 126069) {
				// 可通过转职任务成为守护者。 / You can become a Daeva through the class change mission.
				// 完成任务后将达到 10 级，与当前经验无关。 / Once you complete the mission, you will reach level 10, regardless of your
				// 经验。 / EXP.
				PacketSendUtility.sendPacket(this.getPlayer(), SM_SYSTEM_MESSAGE.STR_MSG_CAN_QUEST_DEVA);
			}
		} else if (this.getLevel() == 65 && !this.isArchDaeva()) {
			boolean isCompleteQuest = false;
			if (this.getPlayer().getRace() == Race.ELYOS) {
				isCompleteQuest = this.getPlayer().isCompleteQuest(10520); // Covert Communiques.
			} else {
				isCompleteQuest = this.getPlayer().isCompleteQuest(20520); // Lost Destiny.
			}
			if (!isCompleteQuest) {
				maxExp = 2066885620;
				if (this.getExp() >= 2066885620) {
					// 可通过转职任务成为高阶守护者。 / You can become an Archdaeva through the class change mission.
					// 完成任务后将达到 66 级，与当前经验无关。 / Once you complete the mission, you will reach level 66, regardless of your
					// 经验。 / EXP.
					PacketSendUtility.sendPacket(this.getPlayer(), SM_SYSTEM_MESSAGE.STR_MSG_CAN_QUEST_HIGHDEVA);
				}
			}
		}
		if (exp > maxExp) {
			exp = maxExp;
		}
		int oldLvl = this.level;
		this.exp = exp;
		boolean up = false;
		while ((this.level + 1) < maxLevel
				&& (up = exp >= DataManager.PLAYER_EXPERIENCE_TABLE.getStartExpForLevel(this.level + 1))
				|| (this.level - 1) >= 0 && exp < DataManager.PLAYER_EXPERIENCE_TABLE.getStartExpForLevel(this.level)) {
			if (up) {
				this.level++;
			} else {
				this.level--;
			}
			upgradePlayerData();
		}

		if (this.getPlayer() != null) {
			if (up && GSConfig.ENABLE_RATIO_LIMITATION) {
				if (this.level >= GSConfig.RATIO_MIN_REQUIRED_LEVEL
						&& getPlayer().getPlayerAccount().getNumberOf(getRace()) == 1) {
					GameServer.updateRatio(getRace(), 1);
				}
				if (this.level >= GSConfig.RATIO_MIN_REQUIRED_LEVEL
						&& getPlayer().getPlayerAccount().getNumberOf(getRace()) == 1) {
					GameServer.updateRatio(getRace(), -1);
				}
			}

			if (oldLvl != level) {
				updateMaxReposte();
				updateMaxAuraOfGrowth();
			}
			PacketSendUtility.sendPacket(this.getPlayer(),
					new SM_STATUPDATE_EXP(getExpShown(), getExpRecoverable(), getExpNeed(),
							this.getCurrentReposteEnergy(), this.getMaxReposteEnergy(), this.getBerdinStar(),
							this.getAuraOfGrowth()));
		}
	}

	private void upgradePlayerData() {
		Player player = getPlayer();
		if (player != null) {
			player.getController().upgradePlayer();
			resetSalvationPoints();
		}
	}

	public void setNoExp(boolean value) {
		this.noExp = value;
	}

	public boolean getNoExp() {
		return noExp;
	}

	/**
	 * @return 模板中的种族 / Race as from template
	 */
	public final Race getRace() {
		return race;
	}

	public Race getOppositeRace() {
		return race == Race.ELYOS ? Race.ASMODIANS : Race.ELYOS;
	}

	/**
	 * @return the mentorFlagTime
	 */
	public int getMentorFlagTime() {
		return mentorFlagTime;
	}

	public boolean isHaveMentorFlag() {
		return mentorFlagTime > System.currentTimeMillis() / 1000;
	}

	/**
	 * @param mentorFlagTime the mentorFlagTime to set
	 */
	public void setMentorFlagTime(int mentorFlagTime) {
		this.mentorFlagTime = mentorFlagTime;
	}

	public void setRace(Race race) {
		this.race = race;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public PlayerClass getPlayerClass() {
		return playerClass;
	}

	public void setPlayerClass(PlayerClass playerClass) {
		this.playerClass = playerClass;
	}

	public boolean isOnline() {
		return online;
	}

	public void setOnline(boolean online) {
		this.online = online;
	}

	public Gender getGender() {
		return gender;
	}

	public void setGender(Gender gender) {
		this.gender = gender;
	}

	public WorldPosition getPosition() {
		return position;
	}

	public Timestamp getLastOnline() {
		return lastOnline;
	}

	public void setLastOnline(Timestamp timestamp) {
		lastOnline = timestamp;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		if (level <= DataManager.PLAYER_EXPERIENCE_TABLE.getMaxLevel()) {
			this.setExp(DataManager.PLAYER_EXPERIENCE_TABLE.getStartExpForLevel(level), false);
		}
	}

	// 高阶守护者更新 / ArchDaeva Update
	public void setArchDaeva() {
		this.setArchDaeva(true);
		if (this.getLevel() < 66) {
			this.setExp(DataManager.PLAYER_EXPERIENCE_TABLE.getStartExpForLevel(66), true);
		} else if (this.getLevel() >= 66) {
			return;
		}
	}

	public String getNote() {
		return note;
	}

	public void setNote(String note) {
		this.note = note;
	}

	public int getTitleId() {
		return titleId;
	}

	public void setTitleId(int titleId) {
		this.titleId = titleId;
	}

	public int getBonusTitleId() {
		return bonusTitleId;
	}

	/** 设置 bonus title id / Sets the bonus title id */
	public void setBonusTitleId(int bonusTitleId) {
		this.bonusTitleId = bonusTitleId;
	}

	/**
	 * @param position 创建本类对象后应恰好调用一次。 / This method should be called exactly once after creating object of this class
	 */
	public void setPosition(WorldPosition position) {
		if (this.position != null) {
			throw new IllegalStateException("position already set");
		}
		this.position = position;
	}

	/**
	 * 获取此公共数据对应的玩家，玩家不在线时返回 null。
	 * Gets the corresponding Player for this common data. Returns null if the player is not online
	 *
	 * @return 玩家或 null / Player or null
	 */
	public Player getPlayer() {
		if (online && getPosition() != null) {
			return com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findPlayer(playerObjId);
		}
		return null;
	}

	/** 添加神圣能量。 / Adds dp. */
	public void addDp(int dp) {
		setDp(this.dp + dp);
	}

	/** 设置神圣能量。 / Sets dp. */
	public void setDp(int dp) {
		setDpSilently(dp);
		publishDp();
	}

	/** 设置神圣能量但不发送客户端协议包。 / Sets dp without publishing client packets. */
	public void setDpSilently(int dp) {
		if (playerClass.isStartingClass()) {
			return;
		}
		Player player = getPlayer();
		int maxDp = player == null ? -1 : player.getGameStats().getMaxDp().getCurrent();
		this.dp = maxDp >= 0 && dp > maxDp ? maxDp : dp;
	}

	/** 提交后发布神圣能量变化。 / Publishes a committed dp change. */
	public void publishDp() {
		if (playerClass.isStartingClass()) {
			return;
		}
		Player player = getPlayer();
		if (player != null) {
			PacketSendUtility.broadcastPacket(player, new SM_DP_INFO(playerObjId, this.dp), true);
			player.getGameStats().updateStatsAndSpeedVisually();
			PacketSendUtility.sendPacket(player, new SM_STATUPDATE_DP(this.dp));
		}
	}

	/** 获取神圣能量。 / Returns the dp. */
	public int getDp() {
		return this.dp;
	}

	/** 返回模板 ID / Returns the template id */
	@Override
	public int getTemplateId() {
		return 100000 + race.getRaceId() * 2 + gender.getGenderId();
	}

	/** 返回名称 ID / Returns the name id */
	@Override
	public int getNameId() {
		return 0;
	}

	/**
	 * @param warehouseSize the warehouseSize to set
	 */
	public void setWarehouseSize(int warehouseSize) {
		this.warehouseSize = warehouseSize;
	}

	/**
	 * @return the warehouseSize
	 */
	public int getWarehouseSize() {
		return warehouseSize;
	}

	/** 设置 mailbox letters / Sets the mailbox letters */
	public void setMailboxLetters(int count) {
		this.mailboxLetters = count;
	}

	/** 返回 mailbox letters / Returns the mailbox letters */
	public int getMailboxLetters() {
		return mailboxLetters;
	}

	/**
	 * @param boundRadius
	 */
	public void setBoundingRadius(BoundRadius boundRadius) {
		this.boundRadius = boundRadius;
	}

	/** 获取边界半径。 / Returns the bound radius. */
	@Override
	public BoundRadius getBoundRadius() {
		return boundRadius;
	}

	/** 设置 death count / Sets the death count */
	public void setDeathCount(int count) {
		this.soulSickness = count;
	}

	/** 返回 death count / Returns the death count */
	public int getDeathCount() {
		return this.soulSickness;
	}

	/**
	 * @return 返回值为经验加成百分比。 / Value returned here means % of exp bonus.
	 */
	public byte getCurrentSalvationPercent() {
		if (salvationPoint <= 0) {
			return 0;
		}
		long per = salvationPoint / 1000;
		if (per > 30) {
			return 30;
		}
		return (byte) per;
	}

	/** 添加 salvation points / Adds salvation points */
	public void addSalvationPoints(long points) {
		salvationPoint += points;
	}

	/** 重置救赎点数 / Reset salvation points */
	public void resetSalvationPoints() {
		salvationPoint = 0;
	}

	/** 设置 last transfer time / Sets the last transfer time */
	public void setLastTransferTime(long value) {
		this.lastTransferTime = value;
	}

	/** 返回 last transfer time / Returns the last transfer time */
	public long getLastTransferTime() {
		return this.lastTransferTime;
	}

	/** 返回世界所有者 ID / Returns the world owner id */
	public int getWorldOwnerId() {
		return worldOwnerId;
	}

	/** 设置 world owner id / Sets the world owner id */
	public void setWorldOwnerId(int worldOwnerId) {
		this.worldOwnerId = worldOwnerId;
	}

	/** 返回上次盖章 / Returns the last stamp*/
	public Timestamp getLastStamp() {
		return lastStamp;
	}

	/** 设置 last stamp / Sets the last stamp */
	public void setLastStamp(Timestamp setTime) {
		lastStamp = setTime;
	}

	/** 返回通行证盖章 / Returns the passport stamps*/
	public int getPassportStamps() {
		return stamps;
	}

	/** 设置 passport stamps / Sets the passport stamps */
	public void setPassportStamps(int stamps) {
		this.stamps = stamps;
	}

	/** 返回玩家通行证 / Returns the player passports*/
	public Map<Integer, AtreianPassport> getPlayerPassports() {
		return playerPassports;
	}

	/** 返回 completed passports / Returns the completed passports */
	public PlayerPassports getCompletedPassports() {
		return completedPassports;
	}

	/** 添加 to completed passports / Adds to completed passports */
	public void addToCompletedPassports(AtreianPassport atreianPassport) {
		completedPassports.addPassport(atreianPassport.getId(), atreianPassport);
	}

	/** 设置 completed passports / Sets the completed passports */
	public void setCompletedPassports(PlayerPassports playerPassports) {
		completedPassports = playerPassports;
	}

	/** 返回通行证奖励 / Returns the passport reward*/
	public int getPassportReward() {
		return passportReward;
	}

	/** 设置 passport reward / Sets the passport reward */
	public void setPassportReward(int passportReward) {
		this.passportReward = passportReward;
	}

	/** 设置 arch daeva / Sets the arch daeva */
	public void setArchDaeva(boolean isArchDaeva) {
		this.isArchDaeva = isArchDaeva;
	}

	/**
	 * @return 是否高阶守护者 / Whether arch daeva
	 */
	public boolean isArchDaeva() {
		return isArchDaeva;
	}

	/** 返回 creativity point / Returns the creativity point */
	public int getCreativityPoint() {
		return creativityPoint;
	}

	/** 设置 creativity point / Sets the creativity point */
	public void setCreativityPoint(int point) {
		this.creativityPoint = point;
	}

	/** 返回 cp step / Returns the cp step */
	public int getCPStep() {
		return cp_step;
	}

	/** 设置 cp step / Sets the cp step */
	public void setCPStep(int step) {
		this.cp_step = step;
	}

	/** 返回 stone creativity point / Returns the stone creativity point */
	public int getStoneCreativityPoint() {
		return stoneCreativityPoint;
	}

	/** 设置 stone creativity point / Sets the stone creativity point */
	public void setStoneCreativityPoint(int point) {
		this.stoneCreativityPoint = point;
	}

	/** 返回 join request legion id / Returns the join request legion id */
	public int getJoinRequestLegionId() {
		return joinRequestLegionId;
	}

	/** 设置 join request legion id / Sets the join request legion id */
	public void setJoinRequestLegionId(int joinRequestLegionId) {
		this.joinRequestLegionId = joinRequestLegionId;
	}

	/** 返回 join request state / Returns the join request state */
	public LegionJoinRequestState getJoinRequestState() {
		return joinRequestState;
	}

	/** 设置 join request state / Sets the join request state */
	public void setJoinRequestState(LegionJoinRequestState joinRequestState) {
		this.joinRequestState = joinRequestState;
	}

	/** 设置 luna consume point / Sets the luna consume point */
	public void setLunaConsumePoint(int point) {
		this.lunaConsumePoint = point;
	}

	/** 返回 luna consume point / Returns the luna consume point */
	public int getLunaConsumePoint() {
		return lunaConsumePoint;
	}

	/** 设置 muni keys / Sets the muni keys */
	public void setMuniKeys(int keys) {
		this.muni_keys = keys;
	}

	/** 返回 muni keys / Returns the muni keys */
	public int getMuniKeys() {
		return muni_keys;
	}

	/** 设置 luna consume count / Sets the luna consume count */
	public void setLunaConsumeCount(int count) {
		this.consumeCount = count;
	}

	/** 返回 luna consume count / Returns the luna consume count */
	public int getLunaConsumeCount() {
		return consumeCount;
	}

	/** 设置衣橱槽位。 / Sets the wardrobe slot. */
	public void setWardrobeSlot(int slot) {
		this.wardrobeSlot = slot;
	}

	/** 获取衣橱槽位。 / Returns the wardrobe slot. */
	public int getWardrobeSlot() {
		return wardrobeSlot;
	}

	/** 获取升级街机。 / Returns the upgrade arcade. */
	public PlayerUpgradeArcade getUpgradeArcade() {
		if (upgradeArcade == null) {
			this.upgradeArcade = new PlayerUpgradeArcade();
		}
		return upgradeArcade;
	}

	/** 设置升级街机。 / Sets the upgrade arcade. */
	public void setUpgradeArcade(PlayerUpgradeArcade upgradeArcade) {
		this.upgradeArcade = upgradeArcade;
	}

	/**
	 * @return 是否已准备好获得成长光环 / Whether ready for aura of growth
	 */
	public boolean isReadyForAuraOfGrowth() {
		return (level >= 66) && (level < GSConfig.PLAYER_MAX_LEVEL + 1);
	}

	/** 添加 aura of growth / Adds aura of growth */
	public void addAuraOfGrowth(long add) {
		if (!isReadyForAuraOfGrowth()) {
			return;
		}
		auraOfGrowth += add;
		if (auraOfGrowth < 0) {
			auraOfGrowth = 0;
		} else if (auraOfGrowth > getMaxAuraOfGrowth()) {
			auraOfGrowth = getMaxAuraOfGrowth();
		}
	}

	/** 更新 max aura of growth / Update max aura of growth */
	public void updateMaxAuraOfGrowth() {
		if (!isReadyForAuraOfGrowth()) {
			auraOfGrowth = 0;
			auraOfGrowthMax = 0;
		} else if (level < 70) {
			auraOfGrowthMax = (77000000 + 7000000 * (level - 66));
		} else if (level == 70) {
			this.auraOfGrowthMax = 106000000;
		} else if (level == 71) {
			auraOfGrowthMax = 127000000;
		} else if (level < 83) {
			auraOfGrowthMax = (127000000 + 11000000 * (level - 71));
		} else {
			auraOfGrowthMax = 175000000;
		}
	}

	/** 设置 aura of growth / Sets the aura of growth */
	public void setAuraOfGrowth(long value) {
		auraOfGrowth = value;
	}

	/** 返回 aura of growth / Returns the aura of growth */
	public long getAuraOfGrowth() {
		return isReadyForAuraOfGrowth() ? auraOfGrowth : 0;
	}

	/** 返回 max aura of growth / Returns the max aura of growth */
	public long getMaxAuraOfGrowth() {
		return isReadyForAuraOfGrowth() ? auraOfGrowthMax : 0;
	}

	/** 返回 aura of growth points / Returns the aura of growth points */
	public long getAuraOfGrowthPoints() {
		long percent = 0;
		switch (level) {
		case 66:
			percent = 770000;
			break;
		case 67:
			percent = 840000;
			break;
		case 68:
			percent = 910000;
			break;
		case 69:
			percent = 980000;
			break;
		case 70:
			percent = 1060000;
			break;
		case 71:
			percent = 1270000;
			break;
		case 72:
			percent = 1380000;
			break;
		case 73:
			percent = 1490000;
			break;
		case 74:
			percent = 1600000;
			break;
		case 75:
			percent = 1750000;
			break;
		default:
			percent = 0;
			break;
		}
		return percent;
	}

	/**
	 * 捕获任务经验、成长光环、DP 与晋升可能修改的字段。
	 * Captures fields changed by quest EXP, aura, DP, and promotion.
	 *
	 * @return 可恢复一次的事务快照 / transaction snapshot that can restore once
	 */
	public TransactionSnapshot transactionSnapshot() {
		return new TransactionSnapshot();
	}

	public final class TransactionSnapshot {
		private final int savedLevel = level;
		private final long savedExp = exp;
		private final long savedExpRecoverable = expRecoverable;
		private final int savedDp = dp;
		private final long savedReposteCurrent = reposteCurrent;
		private final long savedReposteMax = reposteMax;
		private final long savedSalvationPoint = salvationPoint;
		private final long savedAuraOfGrowth = auraOfGrowth;
		private final long savedAuraOfGrowthMax = auraOfGrowthMax;
		private final long savedBerdinStar = berdinStar;
		private final long savedAbyssFavor = abyssFavor;
		private final boolean savedArchDaeva = isArchDaeva;
		private boolean restored;

		private TransactionSnapshot() {
		}

		public void restore() {
			if (restored) {
				return;
			}
			restored = true;
			level = savedLevel;
			exp = savedExp;
			expRecoverable = savedExpRecoverable;
			dp = savedDp;
			reposteCurrent = savedReposteCurrent;
			reposteMax = savedReposteMax;
			salvationPoint = savedSalvationPoint;
			auraOfGrowth = savedAuraOfGrowth;
			auraOfGrowthMax = savedAuraOfGrowthMax;
			berdinStar = savedBerdinStar;
			abyssFavor = savedAbyssFavor;
			isArchDaeva = savedArchDaeva;
		}
	}

	/**
	 * 伯丁之星成长系统（5.1 版本）。
	 * Berdin's Star 5.1
	 */
	public boolean isReadyForBerdinStar() {
		return this.level >= 10;
	}

	/** 添加 berdin star / Adds berdin star */
	public void addBerdinStar(long add) {
		if (!isReadyForBerdinStar()) {
			return;
		}
		berdinStar += add;
		if (this.berdinStar < 0) {
			berdinStar = 0;
		} else if (berdinStar > getMaxBerdinStar()) {
			berdinStar = getMaxBerdinStar();
		}
		checkBerdinStarPercent();
	}

	/** 设置 berdin star / Sets the berdin star */
	public void setBerdinStar(long value) {
		berdinStar = value;
		checkBerdinStarPercent();
	}

	/** 返回 berdin star / Returns the berdin star */
	public long getBerdinStar() {
		return isReadyForBerdinStar() ? berdinStar : 0;
	}

	/** 返回 max berdin star / Returns the max berdin star */
	public long getMaxBerdinStar() {
		return isReadyForBerdinStar() ? berdinStarMax : 0;
	}

	/**
	 * 检查伯丁之星百分比。
	 * Check berdin star percent
	 */
	public void checkBerdinStarPercent() {
		if ((this.getPlayer() != null) && (isReadyForBerdinStar())) {
			int percent = (int) ((float) berdinStar * 100.0 / (float) getMaxBerdinStar());
			if ((!BerdinStarBoost) && (percent > 50)) {
				BerdinStarBoost = true;
				PacketSendUtility.sendPacket(this.getPlayer(),
						new SM_SYSTEM_MESSAGE(1403399, new Object[] { Integer.valueOf(50) }));
			} else if ((BerdinStarBoost) && (percent < 50)) {
				BerdinStarBoost = false;
				PacketSendUtility.sendPacket(this.getPlayer(),
						new SM_SYSTEM_MESSAGE(1403400, new Object[] { Integer.valueOf(50) }));
			} else if (berdinStar <= 0) {
				PacketSendUtility.sendPacket(this.getPlayer(), new SM_SYSTEM_MESSAGE(1403401, new Object[0]));
			}
		}
	}

	/**
	 * 欧比斯恩惠系统（5.3 版本）。
	 * Abyss Favor 5.3
	 */
	public boolean isReadyForAbyssFavor() {
		return this.level >= 45;
	}

	/** 添加 abyss favor / Adds abyss favor */
	public void addAbyssFavor(long add) {
		if (!isReadyForAbyssFavor()) {
			return;
		}
		abyssFavor += add;
		if (this.abyssFavor < 0) {
			abyssFavor = 0;
		} else if (abyssFavor > getMaxAbyssFavor()) {
			abyssFavor = getMaxAbyssFavor();
		}
		checkAbyssFavorPercent();
	}

	/** 设置 abyss favor / Sets the abyss favor */
	public void setAbyssFavor(long value) {
		abyssFavor = value;
		checkAbyssFavorPercent();
	}

	/** 返回 abyss favor / Returns the abyss favor */
	public long getAbyssFavor() {
		return isReadyForAbyssFavor() ? abyssFavor : 0;
	}

	/** 返回 max abyss favor / Returns the max abyss favor */
	public long getMaxAbyssFavor() {
		return isReadyForAbyssFavor() ? abyssFavorMax : 0;
	}

	/**
	 * 检查欧比斯恩惠百分比。
	 * Check abyss favor percent
	 */
	public void checkAbyssFavorPercent() {
		if ((this.getPlayer() != null) && (isReadyForAbyssFavor())) {
			int percent = (int) ((float) abyssFavor * 100.0 / (float) getMaxAbyssFavor());
			if ((!AbyssFavorBoost) && (percent > 50)) {
				AbyssFavorBoost = true;
				PacketSendUtility.sendPacket(this.getPlayer(), new SM_ABYSS_FAVOR());
				PacketSendUtility.sendPacket(this.getPlayer(),
						new SM_SYSTEM_MESSAGE(1404029, new Object[] { Integer.valueOf(50) }));
			} else if ((AbyssFavorBoost) && (percent < 50)) {
				AbyssFavorBoost = false;
				PacketSendUtility.sendPacket(this.getPlayer(), new SM_ABYSS_FAVOR());
				PacketSendUtility.sendPacket(this.getPlayer(),
						new SM_SYSTEM_MESSAGE(1404030, new Object[] { Integer.valueOf(50) }));
			} else if (abyssFavor <= 0) {
				PacketSendUtility.sendPacket(this.getPlayer(), new SM_ABYSS_FAVOR());
				PacketSendUtility.sendPacket(this.getPlayer(), new SM_SYSTEM_MESSAGE(1404031, new Object[0]));
			}
		}
	}

	/** 设置 floor / Sets the floor */
	public void setFloor(int floor) {
		this.floor = floor;
	}

	/** 返回 floor / Returns the floor */
	public int getFloor() {
		return floor;
	}

	private int time;

	/** 返回通行证时间 / Returns the passport time*/
	public int getPassportTime() {
		return time;
	}

	/** 设置 passport time / Sets the passport time */
	public void setPassportTime(int time) {
		this.time = time;
	}

	/** 返回 golden dice / Returns the golden dice */
	public int getGoldenDice() {
		return goldenDice;
	}

	/** 设置 golden dice / Sets the golden dice */
	public void setGoldenDice(int dice) {
		this.goldenDice = dice;
	}

	/** 返回 reset board / Returns the reset board */
	public int getResetBoard() {
		return resetBoard;
	}

	/** 设置 reset board / Sets the reset board */
	public void setResetBoard(int reset) {
		this.resetBoard = reset;
	}

	/** 设置 creation date / Sets the creation date */
	public void setCreationDate(Timestamp date) {
		creationDate = date;
	}

	/** 返回 creation date / Returns the creation date */
	public Timestamp getCreationDate() {
		return creationDate;
	}

	/**
	 * 守护灵相关。
	 * Minions section
	 */
	public int getMinionSkillPoints() {
		return minionSkillPoints;
	}

	/** 设置守护灵技能点。 / Sets the minion skill points. */
	public void setMinionSkillPoints(int minionSkillPoints) {
		this.minionSkillPoints = minionSkillPoints;
	}

	/** 是否自动补充守护灵技能点 / Whether minion skill points auto charge */
	public boolean isMinionSkillPointsAutoCharge() {
		return minionSkillPointsAutoCharge;
	}

	/** 设置 minion skill points auto charge / Sets the minion skill points auto charge */
	public void setMinionSkillPointsAutoCharge(boolean minionSkillPointsAutoCharge) {
		this.minionSkillPointsAutoCharge = minionSkillPointsAutoCharge;
	}

	/** 返回 minion function time / Returns the minion function time */
	public Timestamp getMinionFunctionTime() {
		return minionFunctionTime;
	}

	/** 设置 minion function time / Sets the minion function time */
	public void setMinionFunctionTime(Timestamp minionFunctionTime) {
		this.minionFunctionTime = minionFunctionTime;
	}
}
