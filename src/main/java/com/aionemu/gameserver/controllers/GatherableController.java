package com.aionemu.gameserver.controllers;

import java.util.List;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.CraftConfig;
import com.aionemu.gameserver.configs.main.SecurityConfig;
import com.aionemu.gameserver.controllers.observer.StartMovingListener;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.actions.PlayerMode;
import com.aionemu.gameserver.model.gameobjects.Gatherable;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.RewardType;
import com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState;
import com.aionemu.gameserver.model.templates.gather.GatherableTemplate;
import com.aionemu.gameserver.model.templates.gather.Material;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUEST_ACTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.PunishmentService;
import com.aionemu.gameserver.services.RespawnService;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.task.GatheringTask;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.RndSelector;
import com.aionemu.gameserver.utils.captcha.CAPTCHAUtil;
import com.aionemu.gameserver.world.World;

/**
 * 可采集物控制器，管理采集校验、任务、奖励与采集保护。
 * Gatherable controller managing gather validation, tasks, rewards and gather protection.
 */
public class GatherableController extends VisibleObjectController<Gatherable> {

	/** 当前采集次数。 / Current gather count. */
	private int gatherCount;
	/** 当前采集者对象 ID / Current gatherer's object id */
	private int currentGatherer;
	/** 进行中的采集任务。 / Ongoing gathering task. */
	private GatheringTask task;
	/** 采集状态。 / Gather state. */
	private GatherState state = GatherState.IDLE;
	/** 材料随机选择器。 / Material random selector. */
	private RndSelector<Material> mats;

	/**
	 * 采集状态枚举。
	 * Gather state enumeration.
	 */
	public enum GatherState {
		/** 已采集完成。 / Gather finished. */
		GATHERED,
		/** 正在采集。 / Gathering in progress. */
		GATHERING,
		/** 空闲。 / Idle. */
		IDLE
	}

	/**
	 * 玩家开始采集时的入口：校验等级、技能、工具、CAPTCHA 后启动任务。
	 * Entry when a player starts gathering: validates level, skill, tools and CAPTCHA, then starts the task.
	 *
	 * gathering player
	 */
	public void onStartUse(final Player player) {
		final GatherableTemplate template = this.getOwner().getObjectTemplate();
		int gatherId = template.getTemplateId();
		if (player.getLevel() > 10) {
			switch (gatherId) {
			case 400201: // Impure Iron Ore.
			case 400251: // Impure Iron Ore.
			case 400601: // Young Aria.
			case 400651: // Young Azpha.
			case 400701: // Mela Sapling.
			case 400751: // Raydam Sapling.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GATHER_INCORRECT_SKILL);
				break;
			}
			finishGathering(player);
		}
		if (template.getLevelLimit() > 0) {
			// 提取至少需要等级 %0。 / You must be at least level %0 to perform extraction.
			if (player.getLevel() < template.getLevelLimit()) {
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400737, template.getLevelLimit()));
				return;
			}
		}
		if (player.isInPlayerMode(PlayerMode.RIDE)) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1401096));
			return;
		}
		if (player.getInventory().isFull()) {
			// 采集前背包至少需有一个空位。 / You must have at least one free space in your cube to gather.
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1330036));
			return;
		}
		if (MathUtil.getDistance(getOwner(), player) > 6) {
			return;
		}
		// 检查是否可采集 / check is gatherable
		if (!checkGatherable(player, template)) {
			return;
		}
		if (!checkPlayerSkill(player, template)) {
			return;
		}
		// 检查背包中的提取器 / check for extractor in inventory
		byte result = checkPlayerRequiredExtractor(player, template);
		if (result == 0) {
			return;
		}
		// 验证码 / CAPTCHA
		if (SecurityConfig.CAPTCHA_ENABLE) {
			if (SecurityConfig.CAPTCHA_APPEAR.equals(template.getSourceType()) || SecurityConfig.CAPTCHA_APPEAR.equals("ALL")) {
				int rate = SecurityConfig.CAPTCHA_APPEAR_RATE;
				if (template.getCaptchaRate() > 0) {
					rate = (int) (template.getCaptchaRate() * 0.1f);
				}
				if (Rnd.get(0, 100) < rate) {
					player.setCaptchaWord(CAPTCHAUtil.getRandomWord());
					player.setCaptchaImage(CAPTCHAUtil.createCAPTCHA(player.getCaptchaWord()).array());
					PunishmentService.setIsNotGatherable(player, 0, true, SecurityConfig.CAPTCHA_EXTRACTION_BAN_TIME * 1000);
					// 采集中中毒，暂时无法采集（剩余时间：10 分钟）。 / You were poisoned during extraction and cannot extract for (Time remaining: 10Min)
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_CAPTCHA_RESTRICTED("10"));
					PacketSendUtility.sendPacket(player, new SM_QUEST_ACTION(0, 600));
				}
			}
		}
		List<Material> materials = null;
		switch (result) {
		case 1:
			materials = template.getExtraMaterials().getMaterial();
			break;
		case 2:
			materials = template.getMaterials().getMaterial();
			break;
		}
		mats = new RndSelector<Material>();
		for (Material mat : materials) {
			mats.add(mat, mat.getRate());
		}
		synchronized (state) {
			if (state != GatherState.GATHERING) {
				state = GatherState.GATHERING;
				currentGatherer = player.getObjectId();
				startGatherProtection(player);
				player.getObserveController().attach(new StartMovingListener() {
					@Override
					public void moved() {
						finishGathering(player);
						stopGatherProtection(player);
					}
				});
				int skillLvlDiff = player.getSkillList().getSkillLevel(template.getHarvestSkill()) - template.getSkillLevel();
				task = new GatheringTask(player, getOwner(), getMaterial(), skillLvlDiff);
				task.start();
			}
		}
	}

	/**
	 * 按权重随机选取一种材料。
	 * Randomly selects a material by weight.
	 *
	 * @return 选中的材料，可能为 null / selected material, may be null
	 */
	public Material getMaterial() {
		Material m = mats.select();
		int chance = Rnd.get(m.getRate());
		int current = 0;
		current += m.getRate();
		if (mats != null) {
			if (current >= chance) {
				return m;
			}
		}
		return null;
	}

	/**
	 * 校验玩家是否具备足够的采集技能等级。
	 * Validates that the player has the required gather skill level.
	 *
	 * 玩家 / player
	 * gather template
	 *
	 * @return 校验通过则为 true / true if valid
	 */
	private boolean checkPlayerSkill(final Player player, final GatherableTemplate template) {
		int harvestSkillId = template.getHarvestSkill();
		if (!player.getSkillList().isSkillPresent(harvestSkillId)) {
			if (harvestSkillId == 30001) {
				// 你已是守护者，把这留给人类。 / You are Daeva now, leave this to humans.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_GATHER_INCORRECT_SKILL);
			} else {
				// 须学习 %0 技能才能开始采集。 / You must learn the %0 skill to start gathering.
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1330054, new DescriptionId(DataManager.SKILL_DATA.getSkillTemplate(harvestSkillId).getNameId())));
			}
			return false;
			// 你的 %0 技能等级不够高。 / Your %0 skill level is not high enough.
		}
		if (player.getSkillList().getSkillLevel(harvestSkillId) < template.getSkillLevel()) {
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1330001, new DescriptionId(DataManager.SKILL_DATA.getSkillTemplate(harvestSkillId).getNameId())));
			return false;
		}
		return true;
	}

	/**
	 * 校验玩家是否具备所需采集工具/消耗品。
	 * Validates required extractor tools or consumables on the player.
	 *
	 * 玩家 / player
	 * gather template
	 * @return 0=失败，1=额外材料，2=普通材料 / 0=fail, 1=extra materials, 2=normal materials
	 */
	private byte checkPlayerRequiredExtractor(final Player player, final GatherableTemplate template) {
		if (template.getRequiredItemId() > 0) {
			if (template.getCheckType() == 1) {
				List<Item> items = player.getEquipment().getEquippedItemsByItemId(template.getRequiredItemId());
				boolean condOk = false;
				for (Item item : items) {
					if (item.isEquipped()) {
						condOk = true;
						break;
					}
				}
				return (byte) (condOk ? 1 : 2);
			} else if (template.getCheckType() == 2) {
				if (player.getInventory().getItemCountByItemId(template.getRequiredItemId()) < template.getEraseValue()) {
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400376, new DescriptionId(template.getRequiredItemNameId())));
					return 0;
				} else {
					return 1;
				}
			}
		}
		return 2;
	}

	/**
	 * 校验玩家当前是否允许采集（惩罚/封禁计时）。
	 * Validates whether the player is currently allowed to gather (punish/ban timer).
	 *
	 * 玩家 / player
	 * gather template
	 *
	 * @return 若 allowed 则为 true / true if allowed
	 */
	private boolean checkGatherable(final Player player, final GatherableTemplate template) {
		if (player.isNotGatherable()) {
			// 当前无法提取。（剩余时间：10 分钟） / You are currently unable to extract. (Time remaining: 10Min)
			PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400273, (int) ((player.getGatherableTimer() - (System.currentTimeMillis() - player.getStopGatherable())) / 1000)));
			return false;
		}
		return true;
	}

	/**
	 * 完成一次交互：计数并在达到上限后消失。
	 * Completes one interaction: counts and despawns when the harvest limit is reached.
	 */
	public void completeInteraction() {
		state = GatherState.IDLE;
		gatherCount++;
		if (gatherCount == getOwner().getObjectTemplate().getHarvestCount()) {
			onDespawn();
		}
	}

	/**
	 * 向玩家发放采集经验奖励。
	 * Grants gathering experience rewards to the player.
	 *
	 * @param player 玩家 / player
	 */
	public void rewardPlayer(Player player) {
		if (player != null) {
			int skillLvl = getOwner().getObjectTemplate().getSkillLevel();
			int xpReward = (int) ((0.0031 * (skillLvl + 5.3) * (skillLvl + 1592.8) + 60));
			if (player.getSkillList().addSkillXp(player, getOwner().getObjectTemplate().getHarvestSkill(), (int) RewardType.GATHERING.calcReward(player, xpReward), skillLvl)) {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_EXTRACT_GATHERING_SUCCESS_GETEXP);
				player.getCommonData().addExp(xpReward, RewardType.GATHERING);
			} else {
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_DONT_GET_PRODUCTION_EXP(new DescriptionId(DataManager.SKILL_DATA.getSkillTemplate(getOwner().getObjectTemplate().getHarvestSkill()).getNameId())));
			}
		}
	}

	/**
	 * 结束当前玩家的采集过程。
	 * Finishes the current player's gathering process.
	 *
	 * gathering player
	 */
	public void finishGathering(Player player) {
		if (currentGatherer == player.getObjectId()) {
			if (state == GatherState.GATHERING) {
				task.abort();
			}
			currentGatherer = 0;
			state = GatherState.IDLE;
		}
	}

	/**
	 * 开启采集保护：使玩家对其他玩家隐形，避免 PvP 干扰。
	 * Starts gather protection by hiding the player from others to avoid PvP interference.
	 *
	 * gathering player
	 */
	public void startGatherProtection(Player player) {
		if (CraftConfig.PROTECTION_GATHER_ENABLE) {
			player.setVisualState(CreatureVisualState.HIDE3);
			PacketSendUtility.broadcastPacket(player, new SM_PLAYER_STATE(player), true);
		}
	}

	/**
	 * 关闭采集保护，恢复玩家可视状态。
	 * Stops gather protection and restores the player's visual state.
	 *
	 * gathering player
	 */
	public void stopGatherProtection(Player player) {
        if (CraftConfig.PROTECTION_GATHER_ENABLE) {
		    player.unsetVisualState(CreatureVisualState.HIDE3);
		    PacketSendUtility.broadcastPacket(player, new SM_PLAYER_STATE(player), true);
        }
	}

	/**
	 * 可采集物消失并在非副本中安排重生。
	 * Despawns the gatherable and schedules respawn outside instances.
	 */
	@Override
	public void onDespawn() {
		Gatherable owner = getOwner();
		if (!getOwner().isInInstance()) {
			RespawnService.scheduleRespawnTask(owner);
		}
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().despawn(owner);
	}

	/**
	 * 生成前重置采集计数。
	 * Resets gather count before spawn.
	 */
	@Override
	public void onBeforeSpawn() {
		this.gatherCount = 0;
	}

	/**
	 * 获取所有者可采集物。
	 * Gets the owner gatherable.
	 *
	 * gatherable
	 */
	@Override
	public Gatherable getOwner() {
		return super.getOwner();
	}
}
