package com.aionemu.gameserver.model.templates.item.actions;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.configs.main.CustomConfig;
import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ItemUseObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.DescriptionId;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.TaskId;
import com.aionemu.gameserver.model.actions.PlayerMode;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.state.CreatureState;
import com.aionemu.gameserver.model.templates.item.ItemTemplate;
import com.aionemu.gameserver.model.templates.ride.RideInfo;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_ITEM_USAGE_ANIMATION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.effect.AbnormalState;
import com.aionemu.gameserver.skillengine.model.Effect;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.zone.ZoneInstance;

/**
 * Ride 动作模板（静态数据/XML）。
 * XML template. / XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "RideAction")
public class RideAction extends AbstractItemAction {
	@XmlAttribute(name = "npc_id")
	protected int npcId;

	/**
	 * @return 是否 act / 是否 act。 / Whether act / Whether act
	 */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		if (parentItem == null) {
			return false;
		}
		if (CustomConfig.ENABLE_RIDE_RESTRICTION) {
			for (ZoneInstance zone : player.getPosition().getMapRegion().getZones(player)) {
				if (!zone.canRide()) {
					// 此处无法骑乘。 / You cannot ride here.
					// 你被强制下马。 / You are forced to dismount.
					PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_NORIDE_AREA_STOP);
					return false;
				}
			}
		}
		return true;
	}

	/** 执行 / act. */
	@Override
	public void act(final Player player, final Item parentItem, Item targetItem) {
		player.getController().cancelUseItem();
		if (player.isInPlayerMode(PlayerMode.RIDE)) {
			player.unsetPlayerMode(PlayerMode.RIDE);
			return;
		}
		PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
				parentItem.getObjectId(), parentItem.getItemId(), 3000, 0, 0), true);
		final ItemUseObserver observer = new ItemUseObserver() {
			/** 中止 / abort. */
			@Override
			public void abort() {
				player.getController().cancelTask(TaskId.ITEM_USE);
				player.removeItemCoolDown(parentItem.getItemTemplate().getUseLimits().getDelayId());
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE
						.STR_ITEM_CANCELED(new DescriptionId(parentItem.getItemTemplate().getNameId())));
				PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
						parentItem.getObjectId(), parentItem.getItemTemplate().getTemplateId(), 0, 2, 0), true);
				player.getObserveController().removeObserver(this);
			}
		};
		player.getObserveController().attach(observer);
		player.getController().addTask(TaskId.ITEM_USE, GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			/** 运行 / run. */
			@Override
			public void run() {
				player.unsetState(CreatureState.ACTIVE);
				player.setState(CreatureState.RESTING);
				player.getObserveController().removeObserver(observer);
				ItemTemplate itemTemplate = parentItem.getItemTemplate();
				player.setPlayerMode(PlayerMode.RIDE, getRideInfo());
				PacketSendUtility.sendPacket(player,
						SM_SYSTEM_MESSAGE.STR_USE_ITEM(new DescriptionId(itemTemplate.getNameId())));
				PacketSendUtility.broadcastPacket(player, new SM_EMOTION(player, EmotionType.START_EMOTE2, 0, 0), true);
				PacketSendUtility.broadcastPacket(player,
						new SM_EMOTION(player, EmotionType.RIDE, 0, getRideInfo().getNpcId()), true);
				PacketSendUtility.broadcastPacket(player, new SM_ITEM_USAGE_ANIMATION(player.getObjectId(),
						parentItem.getObjectId(), parentItem.getItemId(), 0, 1, 1), true);
				player.getController().cancelTask(TaskId.ITEM_USE);
			}
		}, 3000));
		ActionObserver rideObserver = new ActionObserver(ObserverType.ABNORMALSETTED) {
			/** 异常状态已设置 / abnormalsetted. */
			@Override
			public void abnormalsetted(AbnormalState state) {
				if ((state.getId() & AbnormalState.DISMOUT_RIDE.getId()) > 0) {
					player.unsetPlayerMode(PlayerMode.RIDE);
				}
			}
		};
		player.getObserveController().addObserver(rideObserver);
		player.setRideObservers(rideObserver);
		ActionObserver attackedObserver = new ActionObserver(ObserverType.ATTACKED) {
			/** 受攻击 / attacked. */
			@Override
			public void attacked(Creature creature) {
				if (Rnd.get(1000) < 200) {
					player.unsetPlayerMode(PlayerMode.RIDE);
				}
			}
		};
		player.getObserveController().addObserver(attackedObserver);
		player.setRideObservers(attackedObserver);
		ActionObserver dotAttackedObserver = new ActionObserver(ObserverType.DOT_ATTACKED) {
			/** 持续伤害受击 / dotattacked. */
			@Override
			public void dotattacked(Creature creature, Effect dotEffect) {
				if (Rnd.get(1000) < 200) {
					player.unsetPlayerMode(PlayerMode.RIDE);
				}
			}
		};
		player.getObserveController().addObserver(dotAttackedObserver);
		player.setRideObservers(dotAttackedObserver);
	}

	/** 返回 ride info / Returns the ride info */
	public RideInfo getRideInfo() {
		return DataManager.RIDE_DATA.getRideInfo(npcId);
	}
}
