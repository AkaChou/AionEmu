package com.aionemu.gameserver.controllers.effect;

import java.util.Collection;
import java.util.Collections;

import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.lifecycle.GameTaskManagerServices;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ABNORMAL_STATE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STANCE;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.SkillTargetSlot;
import com.aionemu.gameserver.skillengine.model.SkillTemplate;
import com.aionemu.gameserver.taskmanager.tasks.PacketBroadcaster.BroadcastMode;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 玩家效果控制器，扩展决斗校验、图标同步与登出效果恢复。
 * Player effect controller extending duel checks, icon sync and logout effect restore.
 *
 * @author ATracer
 */
public class PlayerEffectController extends EffectController {

	/**
	 * 为指定生物构造玩家效果控制器。
	 * Constructs a player effect controller for the given creature.
	 *
	 * @param owner 所有者生物（玩家） / owner creature (player)
	 */
	public PlayerEffectController(Creature owner) {
		super(owner);
	}

	/**
	 * 添加效果；决斗结束后的友好 debuff 将被拒绝。
	 * Adds an effect; friendly debuffs after a duel ends are rejected.
	 *
	 * @param effect 待添加效果 / effect to add
	 */
	@Override
	public void addEffect(Effect effect) {
		if (checkDuelCondition(effect) && !effect.getIsForcedEffect()) {
			return;
		}
		super.addEffect(effect);
		updatePlayerIconsAndGroup(effect);
	}

	/**
	 * 清除效果并刷新玩家/队伍图标。
	 * Clears an effect and refreshes player/team icons.
	 *
	 * @param effect 待清除效果 / effect to clear
	 */
	@Override
	public void clearEffect(Effect effect) {
		super.clearEffect(effect);
		updatePlayerIconsAndGroup(effect);
	}

	/**
	 * 获取所有者玩家。
	 * Gets the owner player.
	 *
	 * @return 所有者玩家 / owner player
	 */
	@Override
	public Player getOwner() {
		return (Player) super.getOwner();
	}

	/**
	 * 非被动效果时更新玩家图标并通知队伍效果更新。
	 * For non-passive effects, updates player icons and notifies team effect updates.
	 *
	 * related effect
	 */
	private void updatePlayerIconsAndGroup(Effect effect) {
		if (!effect.isPassive()) {
			updatePlayerEffectIcons();
			if (getOwner().isInTeam()) {
				GameTaskManagerServices.teamEffectUpdater().startTask(getOwner());
			}
		}
	}

	/**
	 * 标记需要广播玩家效果图标。
	 * Marks player effect icons for broadcast.
	 */
	@Override
	public void updatePlayerEffectIcons() {
		getOwner().addPacketBroadcastMask(BroadcastMode.UPDATE_PLAYER_EFFECT_ICONS);
	}

	/**
	 * 实际发送异常状态包以刷新效果图标。
	 * Actually sends the abnormal-state packet to refresh effect icons.
	 */
	@Override
	public void updatePlayerEffectIconsImpl() {
		Collection<Effect> effects = getAbnormalEffectsToShow();
		PacketSendUtility.sendPacket((Player) getOwner(), new SM_ABNORMAL_STATE(effects, abnormals));
	}

	/**
	 * 决斗结束后对友好单位的 DEBUFF 不应再添加。
	 * Debuffs against a friendly unit after a duel ends must not be added.
	 *
	 * @param effect 待检查效果 / effect to check
	 * @return 若应阻止添加则为 true / true if the effect should be blocked
	 */
	private boolean checkDuelCondition(Effect effect) {
		Creature creature = effect.getEffector();
		if (creature instanceof Player) {
			if (!getOwner().isEnemy(creature) && effect.getTargetSlot() == SkillTargetSlot.DEBUFF.ordinal()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 从持久化数据恢复登出前效果。
	 * Restores a saved effect from logout persistence data.
	 *
	 * skill id
	 * skill level
	 * @param remainingTime 剩余时间（毫秒） / remaining time in ms
	 * @param endTime 结束时间戳 / end timestamp
	 */
	public void addSavedEffect(int skillId, int skillLvl, int remainingTime, long endTime) {
		SkillTemplate template = DataManager.SKILL_DATA.getSkillTemplate(skillId);

		if (remainingTime <= 0 || template == null) {
			return;
		}
		if (CustomConfig.ABYSSXFORM_LOGOUT && template.isDeityAvatar()) {

			if (System.currentTimeMillis() >= endTime) {
				return;
			} else {
				remainingTime = (int) (endTime - System.currentTimeMillis());
			}
		}
		Effect effect = new Effect(getOwner(), getOwner(), template, skillLvl, remainingTime);
		abnormalEffectMap.put(effect.getStack(), effect);
		effect.addAllEffectToSucess();
		effect.startEffect(true);

		if (effect.getSkillTemplate().getTargetSlot() != SkillTargetSlot.NOSHOW) {
			PacketSendUtility.sendPacket(getOwner(),
					new SM_ABNORMAL_STATE(Collections.singletonList(effect), abnormals));
		}
	}

	/**
	 * 广播效果时若处于姿态则同步姿态包。
	 * When broadcasting effects, also syncs stance packet if under stance.
	 */
	@Override
	public void broadCastEffectsImp() {
		super.broadCastEffectsImp();
		Player player = getOwner();
		if (player.getController().isUnderStance()) {
			PacketSendUtility.sendPacket(player, new SM_PLAYER_STANCE(player, 1));
		}
	}
}
