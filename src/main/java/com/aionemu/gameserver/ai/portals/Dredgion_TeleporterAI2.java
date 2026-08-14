package com.aionemu.gameserver.ai.portals;

import com.aionemu.gameserver.ai.ActionItemNpcAI2;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.services.teleport.TeleportService2;
import com.aionemu.gameserver.model.TeleportAnimation;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;


import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMap;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.WorldMapType;
import com.aionemu.gameserver.services.instance.InstanceService;


/*
 * Author: MATTY
 */

@AIName("dredgion_teleporter") // AI 名称。 / AI name.

/**
 * 传送门/传送点 AI：Dredgion Teleporter（@AIName "dredgion_teleporter"），继承 ActionItemNpcAI2。
 * Portal/teleporter AI: Dredgion Teleporter (@AIName "dredgion_teleporter"), extends ActionItemNpcAI2.
 */
public class Dredgion_TeleporterAI2 extends ActionItemNpcAI2 {

    private static final byte TELEPORT_HEADING = (byte) 25;
    private static final TeleportAnimation TELEPORT_ANIMATION = TeleportAnimation.BEAM_ANIMATION;

    // 天族传送坐标。 / Elyos teleport coordinates.
    private static final float ELYOS_TELEPORT_X = 414f;
    private static final float ELYOS_TELEPORT_Y = 193f;
    private static final float ELYOS_TELEPORT_Z = 431f;

	// 魔族传送坐标。 / Asmodian teleport coordinates.
    private static final float ASMODIAN_TELEPORT_X = 399.3425f;
    private static final float ASMODIAN_TELEPORT_Y = 165.760f;
    private static final float ASMODIAN_TELEPORT_Z = 432.288f;

    @Override
    protected void handleUseItemFinish(Player player) {
        switch (getNpcId()) {
            case 730949: // 天族传送到德雷德吉翁据点。 / Elyos teleport to Dredgion sites.
                handleTeleport(player);
                break; // 重要！ / Important!
            case 730950: // 魔族传送到德雷德吉翁据点。 / Asmodian teleport to Dredgion sites.
                handleTeleport(player);
                break;
        }
    }

    private void handleTeleport(Player player) {
        int teleportId = getTeleportId(player);

        if (teleportId != 0) {
            float teleportX, teleportY, teleportZ;
			
			// 天族传送。 / Elyos teleport.
            if (player.getRace() == Race.ELYOS) {
                teleportX = ELYOS_TELEPORT_X;
                teleportY = ELYOS_TELEPORT_Y;
                teleportZ = ELYOS_TELEPORT_Z;
            } else { // 魔族传送。 / Asmodian teleport.
                teleportX = ASMODIAN_TELEPORT_X;
                teleportY = ASMODIAN_TELEPORT_Y;
                teleportZ = ASMODIAN_TELEPORT_Z;
            }

            goTo(player, teleportId, teleportX, teleportY, teleportZ); // 传送到德雷德吉翁。 / Teleport to Dredgion.
			
			
        } else {
			
            PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Telepoter_Under_User);
			
        }
    }

    private int getTeleportId(Player player) {
        int level = player.getLevel();

        if (level >= 46 && level <= 50) {
            return 300110000; // Baranath 德雷德吉翁（46-50 级）。 / Baranath Dredgion 46-50.
        } else if (level >= 51 && level <= 54) {
            return 300210000; // Chantra 德雷德吉翁（51-54 级）。 / Chantra Dredgion 51-54.
        } else if (level >= 55 && level <= 64) {
            return 300440000; // Terath 德雷德吉翁（55-64 级）。 / Terath Dredgion 55-64.
        } else if (level >= 65) {
            return 301650000; // Ashunatal 德雷德吉翁（65-75 级）。 / Ashunatal Dredgion 65-75.
        }
        return 0;
    }
	
	private static void goTo(final Player player, int worldId, float x, float y, float z) {
		
		WorldMap destinationMap = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().getWorldMap(worldId);
		
		if (destinationMap.isInstanceType()) {
			
			TeleportService2.teleportTo(player, worldId, getInstanceId(worldId, player), x, y, z);
			
		} else {
			
			TeleportService2.teleportTo(player, worldId, x, y, z);
			
		}
		
	}
	
	private static int getInstanceId(int worldId, Player player) {
		
		if (player.getWorldId() == worldId) {
			
			WorldMapInstance registeredInstance = InstanceService.getRegisteredInstance(worldId, player.getObjectId());
			
			if (registeredInstance != null) {
				
				return registeredInstance.getInstanceId();
				
			}
			
		}
		
		WorldMapInstance newInstance = InstanceService.getNextAvailableInstance(worldId);
		InstanceService.registerPlayerWithInstance(newInstance, player);
		return newInstance.getInstanceId();
	}
	
}
