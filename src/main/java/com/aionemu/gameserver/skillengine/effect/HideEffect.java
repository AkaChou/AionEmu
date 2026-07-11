package com.aionemu.gameserver.skillengine.effect;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.configs.main.SecurityConfig;
import com.aionemu.gameserver.controllers.attack.AttackUtil;
import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureVisualState;
import com.aionemu.gameserver.network.aion.serverpackets.SM_PLAYER_STATE;
import com.aionemu.gameserver.services.player.PlayerVisualStateService;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.skillengine.model.Skill;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 隐身效果：设置视觉状态与观察者，攻击/施法/对话等可打破隐身。
 * Hide effect: sets visual state and observers; attacks/casts/dialogs can break stealth.
 *
 * @author Sweetkr
 * @author Cura
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "HideEffect")
public class HideEffect extends BuffEffect {

	@XmlAttribute
	protected CreatureVisualState state;
	@XmlAttribute(name = "buffcount")
	protected int buffCount;

	@XmlAttribute
	protected int type = 0;

	/**
	 * 将隐身效果加入目标控制器。
	 * Attaches the hide effect to the target controller.
	 */
	@Override
	public void applyEffect(Effect effect) {
		effect.addToEffectedController();
	}

	/**
	 * 结束隐身并恢复可见状态。
	 * Ends hide and restores the visible state.
	 */
	@Override
	public void endEffect(Effect effect) {
		super.endEffect(effect);

		Creature effected = effect.getEffected();
		effected.getEffectController().unsetAbnormal(AbnormalState.HIDE.getId());

		effected.unsetVisualState(state);

		if ((effected instanceof Player)) {
			ActionObserver observer = effect.getActionObserver(position);
			effect.getEffected().getObserveController().removeObserver(observer);
		}

		PacketSendUtility.broadcastPacketAndReceive(effected, new SM_PLAYER_STATE(effected));
		if ((SecurityConfig.INVIS) && ((effected instanceof Player))) {
			PlayerVisualStateService.hideValidate((Player) effected);
		}
	}

	/**
	 * 设置视觉隐身状态、广播与打破隐身的观察者。
	 * Sets visual hide state, broadcasts, and break-stealth observers.
	 */
	@Override
	public void startEffect(final Effect effect) {
		super.startEffect(effect);

		final Creature effected = effect.getEffected();
		effected.getEffectController().setAbnormal(AbnormalState.HIDE.getId());
		effect.setAbnormal(AbnormalState.HIDE.getId());

		effected.setVisualState(state);

		AttackUtil.cancelCastOn(effected);

		PacketSendUtility.broadcastPacketAndReceive(effected, new SM_PLAYER_STATE(effected));

		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {

			public void run() {
				AttackUtil.removeTargetFrom(effected, true);
			}
		}, 500L);

		if ((effected instanceof Player)) {
			if (SecurityConfig.INVIS) {
				PlayerVisualStateService.hideValidate((Player) effected);
			}

			// 使用技能时移除隐身 / Remove Hide when use skill
			ActionObserver observer = new ActionObserver(ObserverType.SKILLUSE) {

				int bufNumber = 1;

				@Override
				public void skilluse(Skill skill) {
					// [2.5] 允许自身增益 = (buffCount - 1) / [2.5] Allow self buffs = (buffCount - 1)
					if (skill.isSelfBuff() && bufNumber++ < buffCount) {
						return;
					}
					effect.endEffect();
				}
			};
			effected.getObserveController().addObserver(observer);
			effect.setActionObserver(observer, position);

			if (type == 0) {
				effect.setCancelOnDmg(true);
			}

			// 攻击时移除隐身 / Remove Hide when attacking
			effected.getObserveController().attach(new ActionObserver(ObserverType.ATTACK) {

				@Override
				public void attack(Creature creature) {
					effect.endEffect();
				}
			});
			/**
			 * 玩家侧：使用任意物品动作或向 NPC 请求对话时移除隐身。
	 * For player: remove Hide when using any item action or requesting dialog to any NPC
			 */
			effected.getObserveController().attach(new ActionObserver(ObserverType.ITEMUSE) {

				@Override
				public void itemused(Item item) {
					effect.endEffect();
				}
			});
			effected.getObserveController().attach(new ActionObserver(ObserverType.NPCDIALOGREQUEST) {

				@Override
				public void npcdialogrequested(Npc npc) {
					effect.endEffect();
				}

			});
		} else if (type == 0) {
			effect.setCancelOnDmg(true);

			effected.getObserveController().attach(new ActionObserver(ObserverType.ATTACK) {

				public void attack(Creature creature) {
					effect.endEffect();
				}
			});
			effected.getObserveController().attach(new ActionObserver(ObserverType.SKILLUSE) {

				public void skilluse(Skill skill) {
					effect.endEffect();
				}
			});
		}
	}
}
