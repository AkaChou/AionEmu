package com.aionemu.gameserver.commands.admin;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.gameserver.controllers.HouseController;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.gameobjects.player.PlayerHouseOwnerFlags;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.house.HouseStatus;
import com.aionemu.gameserver.model.templates.housing.BuildingType;
import com.aionemu.gameserver.model.templates.housing.HouseAddress;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_ACQUIRE;
import com.aionemu.gameserver.network.aion.serverpackets.SM_HOUSE_OWNER_INFO;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.chathandlers.AdminCommand;

import java.sql.Timestamp;

/**
 * 管理员房屋命令：传送至房屋、为目标玩家获取或收回房屋。
 * Admin house command: teleport to a house, or acquire/revoke ownership for the selected player.
 */
public class HouseCommand extends AdminCommand
{
	public HouseCommand() {
		super("house");
	}

	/**
	 * 处理 tp/acquire/revoke 子命令。
	 * Handle tp/acquire/revoke subcommands.
	 *
	 * @param admin 执行命令的管理员 / Admin executing the command
	 * @param params 子命令与房屋 ID / Subcommand and house id
	 */
	@Override
	public void execute(Player admin, String... params) {
		if (params.length == 0) {
			PacketSendUtility.sendMessage(admin, "Syntax: //house <tp | acquire | revoke>");
			return;
		} if (params[0].equals("acquire")) {
			if (params.length == 1) {
				PacketSendUtility.sendMessage(admin, "Syntax: //house acquire <HOUSE_Id>");
				return;
			}
			ChangeHouseOwner(admin, params[1].toUpperCase(), true);
		} else if (params[0].equals("revoke")) {
			if (params.length == 1) {
				PacketSendUtility.sendMessage(admin, "Syntax: //house revoke <HOUSE_Id>");
				return;
			}
			ChangeHouseOwner(admin, params[1].toUpperCase(), false);
		} else if (params[0].equals("tp")) {
			if (params.length == 1) {
				PacketSendUtility.sendMessage(admin, "Syntax: //house tp <HOUSE_Id>");
				return;
			}
			House house = GameHousingServices.housingService().getHouseByName(params[1].toUpperCase());
			if (house == null) {
				PacketSendUtility.sendMessage(admin, "No such house!");
				return;
			}
			HouseAddress address = house.getAddress();
			TeleportService2.teleportTo(admin, address.getMapId(), address.getX(), address.getY(), address.getZ());
		}
	}

	private void ChangeHouseOwner(Player admin, String houseName, boolean acquire) {
		Player target = null;
		VisibleObject creature = admin.getTarget();
		if (admin.getTarget() instanceof Player) {
			target = (Player) creature;
		} if (target == null) {
			PacketSendUtility.sendMessage(admin, "You should select a target first!");
			return;
		} if (acquire) {
			if (target.getHouses().size() == 2) {
				PacketSendUtility.sendMessage(admin, "Player can not own more than 2 houses!");
				return;
			}
			House house = GameHousingServices.housingService().getHouseByName(houseName);
			if (house == null) {
				PacketSendUtility.sendMessage(admin, "No such house!");
				return;
			} if (target.getHouses().size() == 1) {
				House current = target.getHouses().get(0);
				current.revokeOwner();
				if (current.getBuilding().getType() == BuildingType.PERSONAL_INS) {
					target.getHouses().remove(current);
					PacketSendUtility.sendMessage(admin, "Deleted studio.");
				} else {
					current.setStatus(HouseStatus.ACTIVE);
					current.setFeePaid(true);
					current.setNextPay(null);
					current.save();
					PacketSendUtility.sendMessage(admin, current.getName() + " status is now " + current.getStatus().toString());
				}
			}
			house.setAcquiredTime(new Timestamp(System.currentTimeMillis()));
			house.setOwnerId(target.getCommonData().getPlayerObjId());
			house.setStatus(HouseStatus.ACTIVE);
			house.setFeePaid(true);
			house.setNextPay(null);
            house.reloadHouseRegistry();
			house.save();
			target.getHouses().add(house);
			target.setHouseRegistry(house.getRegistry());
			target.setBuildingOwnerState(PlayerHouseOwnerFlags.HOUSE_OWNER.getId());
			PacketSendUtility.sendMessage(admin, "House " + house.getName() + " acquired");
			PacketSendUtility.sendPacket(target, new SM_HOUSE_OWNER_INFO(target, house));
			PacketSendUtility.sendPacket(target, new SM_HOUSE_ACQUIRE(target.getObjectId(), house.getAddress().getId(), true));
		} else {
			if (target.getHouses().size() == 0) {
				PacketSendUtility.sendMessage(admin, "Nothing to revoke!");
				return;
			}
			House revokedHouse = null;
			for (House house : target.getHouses()) {
				if (house.getName().equals(houseName)) {
					revokedHouse = house;
					house.revokeOwner();
				} else if (house.getStatus() != HouseStatus.ACTIVE) {
					house.setStatus(HouseStatus.ACTIVE);
					house.setSellStarted(null);
					house.save();
				}
			} if (revokedHouse == null) {
				PacketSendUtility.sendMessage(admin, "Target doesn't own this house!");
				return;
			}
			target.getHouses().remove(revokedHouse);
			House oldHouse = null;
			if (target.getHouses().size() != 0) {
				oldHouse = target.getHouses().get(0);
			} else {
				target.setBuildingOwnerState(PlayerHouseOwnerFlags.BUY_STUDIO_ALLOWED.getId());
			}
			target.setHouseRegistry(oldHouse == null ? null : oldHouse.getRegistry());
			PacketSendUtility.sendMessage(admin, "House " + revokedHouse.getName() + " revoked");
			PacketSendUtility.sendPacket(target, new SM_HOUSE_OWNER_INFO(target, oldHouse));
			PacketSendUtility.sendPacket(target, new SM_HOUSE_ACQUIRE(target.getObjectId(), revokedHouse.getAddress().getId(), false));
			((HouseController) revokedHouse.getController()).updateAppearance();
		}
	}

	/**
	 * 参数错误时显示命令语法。
	 * Show command syntax on invalid arguments.
	 *
	 * @param player 接收提示的玩家 / Player receiving the hint
	 */
	@Override
	public void onFail(Player player, String message) {
		PacketSendUtility.sendMessage(player, "syntax //house <tp | list | acquire | revoke>");
	}
}
