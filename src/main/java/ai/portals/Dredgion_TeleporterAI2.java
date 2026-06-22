package ai.portals;

import ai.ActionItemNpcAI2;
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

@AIName("dredgion_teleporter") // Название AI
public class Dredgion_TeleporterAI2 extends ActionItemNpcAI2 {

    private static final byte TELEPORT_HEADING = (byte) 25;
    private static final TeleportAnimation TELEPORT_ANIMATION = TeleportAnimation.BEAM_ANIMATION;

    // Координаты телепортации для Нях
    private static final float ELYOS_TELEPORT_X = 414f;
    private static final float ELYOS_TELEPORT_Y = 193f;
    private static final float ELYOS_TELEPORT_Z = 431f;

	// Координаты телепортации для Асмов
    private static final float ASMODIAN_TELEPORT_X = 399.3425f;
    private static final float ASMODIAN_TELEPORT_Y = 165.760f;
    private static final float ASMODIAN_TELEPORT_Z = 432.288f;

    @Override
    protected void handleUseItemFinish(Player player) {
        switch (getNpcId()) {
            case 730949: // Elyos teleport to Dredgion sites
                handleTeleport(player);
                break; // Важно!
            case 730950: //Asmo teleport to Dredgion sites
                handleTeleport(player);
                break;
        }
    }

    private void handleTeleport(Player player) {
        int teleportId = getTeleportId(player);

        if (teleportId != 0) {
            float teleportX, teleportY, teleportZ;
			
			// Телепорт для Нях
            if (player.getRace() == Race.ELYOS) {
                teleportX = ELYOS_TELEPORT_X;
                teleportY = ELYOS_TELEPORT_Y;
                teleportZ = ELYOS_TELEPORT_Z;
            } else { // Телепорт для Асмов
                teleportX = ASMODIAN_TELEPORT_X;
                teleportY = ASMODIAN_TELEPORT_Y;
                teleportZ = ASMODIAN_TELEPORT_Z;
            }

            goTo(player, teleportId, teleportX, teleportY, teleportZ); // Телепорт в Дерадикон
			
			
        } else {
			
            PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_Telepoter_Under_User);
			
        }
    }

    private int getTeleportId(Player player) {
        int level = player.getLevel();

        if (level >= 46 && level <= 50) {
            return 300110000; // Baranath Dredgion 46-50
        } else if (level >= 51 && level <= 54) {
            return 300210000; // Chantra Dredgion 51-54
        } else if (level >= 55 && level <= 64) {
            return 300440000; // Terath Dredgion 55-64
        } else if (level >= 65) {
            return 301650000; // Ashunatal Dredgion 65-75
        }
        return 0;
    }
	
	private static void goTo(final Player player, int worldId, float x, float y, float z) {
		
		WorldMap destinationMap = World.getInstance().getWorldMap(worldId);
		
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