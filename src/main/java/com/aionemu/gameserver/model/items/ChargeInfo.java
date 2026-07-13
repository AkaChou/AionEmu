package com.aionemu.gameserver.model.items;

import com.aionemu.gameserver.controllers.observer.ActionObserver;
import com.aionemu.gameserver.controllers.observer.ObserverType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.PersistentState;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_INVENTORY_UPDATE_ITEM;
import com.aionemu.gameserver.utils.PacketSendUtility;
import lombok.Getter;
import lombok.Setter;

/**
 * Charge 信息，用于物品相关逻辑。
 * Charge Info for items logic.
 */

public class ChargeInfo extends ActionObserver {
	public static final int LEVEL2 = 1000000;
	public static final int LEVEL1 = 500000;

	@Getter
	private int chargePoints;
	private final int attackBurn;
	private final int defendBurn;
	private final Item item;
	@Setter
	private Player player;

	public ChargeInfo(int chargePoints, Item item) {
		super(ObserverType.ATTACK_DEFEND);
		this.chargePoints = chargePoints;
		this.item = item;
		if (item.getImprovement() != null) {
			attackBurn = item.getImprovement().getBurnAttack();
			defendBurn = item.getImprovement().getBurnDefend();
		} else {
			attackBurn = 0;
			defendBurn = 0;
		}
	}

	/** 更新充能点 / Update charge points*/
	public int updateChargePoints(int addPoints) {
		int newChargePoints = chargePoints + addPoints;
		if (newChargePoints > LEVEL2) {
			newChargePoints = LEVEL2;
		} else if (newChargePoints < 0) {
			newChargePoints = 0;
		}
		if (item.isEquipped() && player != null) {
			player.getEquipment().setPersistentState(PersistentState.UPDATE_REQUIRED);
		}
		item.setPersistentState(PersistentState.UPDATE_REQUIRED);
		this.chargePoints = newChargePoints;
		return newChargePoints;
	}

	public void burn(int points) {
		updateChargePoints(-points);
		Player player = this.player;
		if (player != null) {
			PacketSendUtility.sendPacket(player, new SM_INVENTORY_UPDATE_ITEM(player, item));
		}
	}

	/** 受攻击 / attacked. */
	@Override
	public void attacked(Creature creature) {
		burn(defendBurn);
	}

	/** 攻击。 / Attack. */
	@Override
	public void attack(Creature creature) {
		burn(attackBurn);
	}
}
