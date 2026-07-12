package com.aionemu.gameserver.model.templates.item.actions;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlType;

import com.aionemu.gameserver.model.gameobjects.Item;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerCommonData;
import com.aionemu.gameserver.model.gameobjects.player.PlayerSweep;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SHUGO_SWEEP;
import com.aionemu.gameserver.utils.PacketSendUtility;

/**
 * 术古清扫动作模板（静态数据/XML）。
 * XML template.
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "ShugoSweepAction")
public class ShugoSweepAction extends AbstractItemAction {
	@XmlAttribute(name = "type") // 1 reset ; 2 gold dice
	protected int type;

	@XmlAttribute(name = "count")
	protected int count;

	/**
	 * @return 是否允许执行。 / Whether act
	  */
	@Override
	public boolean canAct(Player player, Item parentItem, Item targetItem) {
		if (type == 1) {
			if (getCommonData(player).getResetBoard() != 0) {
				player.sendMessage("You have already one Reset Board");
				return false;
			}
		}
		return true;
	}

	/** 执行 / act. */
	@Override
	public void act(Player player, Item parentItem, Item targetItem) {
		if (player.getInventory().decreaseByObjectId(parentItem.getObjectId(), 1)) {
			if (type == 1) {
				getCommonData(player).setResetBoard(addConfiguredCount(getCommonData(player).getResetBoard()));
				player.sendMessage("You have received " + getConfiguredCount() + " Reset Board");
			}
			if (type == 2) {
				getCommonData(player).setGoldenDice(addConfiguredCount(getCommonData(player).getGoldenDice()));
				player.sendMessage("You have received " + getConfiguredCount() + " Golden Dice");
			}
			PacketSendUtility.sendPacket(player,
					new SM_SHUGO_SWEEP(getPlayerSweep(player).getBoardId(), getPlayerSweep(player).getStep(),
							getPlayerSweep(player).getFreeDice(), getCommonData(player).getGoldenDice(),
							getCommonData(player).getResetBoard(), 0));
		}
	}

	int addConfiguredCount(int currentCount) {
		return currentCount + getConfiguredCount();
	}

	private int getConfiguredCount() {
		return count > 0 ? count : 1;
	}

	/** 获取公共数据。 / Returns the common data. */
	public PlayerCommonData getCommonData(Player player) {
		return player.getCommonData();
	}

	/** 获取玩家清扫。 / Returns the player sweep. */
	public PlayerSweep getPlayerSweep(Player player) {
		return player.getPlayerShugoSweep();
	}
}
