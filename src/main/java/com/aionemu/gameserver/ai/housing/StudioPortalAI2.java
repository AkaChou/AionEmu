package com.aionemu.gameserver.ai.housing;

import com.aionemu.gameserver.lifecycle.GameHousingServices;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.house.House;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.instance.InstanceService;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMapInstance;

/**
 * 房屋相关 NPC AI：Studio Portal（@AIName "studioportal"），继承 ActionItemNpcAI2。
 * Housing-related NPC AI: Studio Portal (@AIName "studioportal"), extends ActionItemNpcAI2.
 *
 * @author Encom
 */
@AIName("studioportal")
public class StudioPortalAI2 extends ActionItemNpcAI2
{
	@Override
	public boolean onDialogSelect(Player player, int dialogId, int questId, int extendedRewardIndex) {
		return true;
	}
	
	@Override
	/**
	 * 使用工作室传送门：将玩家传送进工作室，或从工作室内部返回其出口区域。
	 * Uses the studio portal: teleports the player into the studio, or back to its exit area when already inside.
	 */
	protected void handleUseItemFinish(Player player) {
		int ownerId = player.getPosition().getWorldMapInstance().getOwnerId();
		House studio = GameHousingServices.housingService().getPlayerStudio(player.getObjectId());
		if (studio == null && ownerId == 0) {
			PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_HOUSING_ENTER_NEED_HOUSE);
			return;
		}
		int exitMapId = 0;
		float x = 0, y = 0, z = 0;
		byte heading = 0;
		int instanceId = 0;
		if (ownerId > 0) {
			studio = GameHousingServices.housingService().getPlayerStudio(ownerId);
			if (studio == null) {
				return;
			}
			exitMapId = studio.getAddress().getExitMapId();
			instanceId = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(exitMapId).getMainWorldMapInstance().getInstanceId();
			x = studio.getAddress().getExitX();
			y = studio.getAddress().getExitY();
			z = studio.getAddress().getExitZ();
		} else if (studio == null) {
			return;
		} else {
			exitMapId = studio.getAddress().getMapId();
			WorldMapInstance instance = InstanceService.getPersonalInstance(exitMapId, player.getObjectId());
			if (instance == null) {
				instance = InstanceService.getNextAvailableInstance(exitMapId, player.getObjectId());
				InstanceService.registerPlayerWithInstance(instance, player);
			}
			instanceId = instance.getInstanceId();
			x = studio.getAddress().getX();
			y = studio.getAddress().getY();
			z = studio.getAddress().getZ();
			if (exitMapId == 710010000) {
				heading = 36;
			}
		}
		TeleportService2.teleportTo(player, exitMapId, instanceId, x, y, z, heading, TeleportAnimation.BEAM_ANIMATION);
	}
}
