package com.aionemu.gameserver.ai.housing;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AI2Request;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.model.templates.housing.BuildingType;
import com.aionemu.gameserver.network.aion.serverpackets.SM_QUESTION_WINDOW;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * 房屋相关 NPC AI：House Gate（@AIName "housegate"），继承 NpcAI2。
 * Housing-related NPC AI: House Gate (@AIName "housegate"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("housegate")
public class HouseGateAI2 extends NpcAI2
{
	@Override
	/**
	 * 房屋门扉对话：仅房主或其组队成员可触发；确认弹窗后把玩家传送至房屋所在区域（个人房屋或退出点）。
	 * House gate dialog: only the house owner or group members may trigger it; on confirmation teleports the player to the house area (personal instance or exit point).
	 */
	protected void handleDialogStart(Player player) {
		final int creatorId = getCreatorId();
		if (!player.getObjectId().equals(creatorId)) {
			if (player.getCurrentGroup() == null || !player.getCurrentGroup().hasMember(creatorId))
				return;
		}
		House house = GameHousingServices.housingService().getPlayerStudio(creatorId);
		if (house == null) {
			int address = GameHousingServices.housingService().getPlayerAddress(creatorId);
			house = GameHousingServices.housingService().getHouseByAddress(address);
		}
		if (house == null)
			return;
		AI2Actions.addRequest(this, player, SM_QUESTION_WINDOW.STR_ASK_GROUP_GATE_DO_YOU_ACCEPT_MOVE, 0, 9,
		new AI2Request() {
			private boolean decided = false;
			@Override
			public void acceptRequest(Creature requester, Player responder) {
				if (decided)
					return;
				House house = GameHousingServices.housingService().getPlayerStudio(creatorId);
				if (house == null) {
					int address = GameHousingServices.housingService().getPlayerAddress(creatorId);
					house = GameHousingServices.housingService().getHouseByAddress(address);
				}
				int instanceOwnerId = responder.getPosition().getWorldMapInstance().getOwnerId();
				int exitMapId = 0;
				float x = 0, y = 0, z = 0;
				byte heading = 0;
				int instanceId = 0;
				if (instanceOwnerId > 0) {
					house = GameHousingServices.housingService().getPlayerStudio(instanceOwnerId);
					exitMapId = house.getAddress().getExitMapId();
					instanceId = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(exitMapId).getMainWorldMapInstance().getInstanceId();
					x = house.getAddress().getExitX();
					y = house.getAddress().getExitY();
					z = house.getAddress().getExitZ();
				} else {
					exitMapId = house.getAddress().getMapId();
					if (house.getBuilding().getType() == BuildingType.PERSONAL_INS) {
						WorldMapInstance instance = InstanceService.getPersonalInstance(exitMapId, creatorId);
						if (instance == null) {
							instance = InstanceService.getNextAvailableInstance(exitMapId, creatorId);
							InstanceService.registerPlayerWithInstance(instance, responder);
						}
						instanceId = instance.getInstanceId();
					} else {
						instanceId = house.getInstanceId();
					}
					x = house.getAddress().getX();
					y = house.getAddress().getY();
					z = house.getAddress().getZ();
					if (exitMapId == 710010000) {
						heading = 36;
					}
				}
				TeleportService2.teleportTo(responder, exitMapId, instanceId, x, y, z, heading, TeleportAnimation.BEAM_ANIMATION);
				decided = true;
			}
			
			@Override
			public void denyRequest(Creature requester, Player responder) {
			    decided = true;
			}
		});
	}
}
