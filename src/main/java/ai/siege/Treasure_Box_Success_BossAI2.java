package ai.siege;

import ai.AggressiveNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.utils.ThreadPoolManager;
import com.aionemu.gameserver.world.knownlist.Visitor;
import com.aionemu.gameserver.services.drop.DropRegistrationService;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AIName("treasure_box_success_boss")
public class Treasure_Box_Success_BossAI2 extends AggressiveNpcAI2 {

    private static final Logger log = LoggerFactory.getLogger(Treasure_Box_Success_BossAI2.class);

    private static final int TREASURE_CHEST_ID = 701481; // ID chest
    private static final int NUMBER_OF_CHESTS = 6; // Number of chests
    private static final Set<Integer> SPAWN_CHEST_IDS;

    static {
        SPAWN_CHEST_IDS = new HashSet<>();
        SPAWN_CHEST_IDS.add(263001);
        SPAWN_CHEST_IDS.add(263006);
        SPAWN_CHEST_IDS.add(263011);
        SPAWN_CHEST_IDS.add(263301);
        SPAWN_CHEST_IDS.add(263306);
        SPAWN_CHEST_IDS.add(263311);
        SPAWN_CHEST_IDS.add(264501);
        SPAWN_CHEST_IDS.add(264506);
        SPAWN_CHEST_IDS.add(264511);
    }

    @Override
    protected void handleDied() {
        if (SPAWN_CHEST_IDS.contains(getNpcId())) {
            treasureChest();
            ThreadPoolManager.getInstance().schedule(this::spawnTreasureChestTask, 10000);
        }
        super.handleDied();
    }

    private void spawnTreasureChestTask() {
        spawnTreasureChest(TREASURE_CHEST_ID);
    }

    private void treasureChest() {
        getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
            @Override
            public void visit(Player player) {
                PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_IDAbRe_Core_NmdC_BoxSpawn);
            }
        });
    }

    private void spawnTreasureChest(int npcId) {
        for (int i = 0; i < NUMBER_OF_CHESTS; i++) {
            rndSpawnInRange(npcId);
        }
    }

    private Npc rndSpawnInRange(int npcId) {
        float distance = Rnd.get(1,4);
        float direction = Rnd.get(0, 199) / 100f;
        float x1 = (float) (Math.cos(Math.PI * direction) * distance);
        float y1 = (float) (Math.sin(Math.PI * direction) * distance);

        float x = getPosition().getX() + x1;
        float y = getPosition().getY() + y1;
        float z = getPosition().getZ();
        byte heading = (byte) 0;

        try {
            Npc chest = (Npc) spawn(TREASURE_CHEST_ID, x, y, z, heading);
            onDropRegistered(chest);
            return chest;
        } catch (Exception e) {
            log.error("Error spawning treasure chest", e);
            return null;
        }
    }

	public void onDropRegistered(Npc npc) {
		int npcId = TREASURE_CHEST_ID;

		DropRegistrationService.getInstance().getCurrentDropMap().remove(npc.getObjectId());
		Set<DropItem> dropItems = new HashSet<>();
		DropRegistrationService.getInstance().getCurrentDropMap().put(npc.getObjectId(), dropItems);

		switch (npcId) {
			case TREASURE_CHEST_ID:

				List<DropChance> DropList = new ArrayList<>();
				DropList.add(new DropChance(186000053, 1, 15));   // Ancient Crown (15%)
				DropList.add(new DropChance(186000057, 1, 25));   // Ancient Goblet (25%)
				DropList.add(new DropChance(186000065, 1, 15));   // Ancient Icon (15%)
				DropList.add(new DropChance(186000061, 1, 15));   // Ancient Seal (15%)
				DropList.add(new DropChance(152014016, 1, 15));    // Firm Balaur Horn (15%)
				DropList.add(new DropChance(152014017, 1, 15));    // Firm Balaur Scale 157%)
				DropList.add(new DropChance(152014018, 1, 15));    // Firm Balaur Skin (15%)
				DropList.add(new DropChance(186000052, 1, 15));   // Greater Ancient Crown (15%)
				DropList.add(new DropChance(186000056, 1, 15));   // Greater Ancient Goblet (15%)
				DropList.add(new DropChance(186000064, 1, 15));    // Greater Ancient Icon (15%)
				DropList.add(new DropChance(186000060, 1, 15));   // Greater Ancient Seal (15%)
				DropList.add(new DropChance(186000199, 1, 15));   // Legion Coin (15%)
				DropList.add(new DropChance(186000054, 1, 15));    // Lesser Ancient Crown (15%)
				DropList.add(new DropChance(186000058, 1, 15));   // Lesser Ancient Goblet (15%)
				DropList.add(new DropChance(186000066, 1, 15));    // Lesser Ancient Icon (15%)
				DropList.add(new DropChance(186000062, 1, 15));   // Lesser Ancient Seal (15%)
				DropList.add(new DropChance(186000051, 1, 15));   // Major Ancient Crown (15%)
				DropList.add(new DropChance(186000055, 1, 15));   // Major Ancient Goblet (15%)
				DropList.add(new DropChance(186000063, 1, 15));    // Major Ancient Icon (15%)
				DropList.add(new DropChance(186000059, 1, 15));   // Major Ancient Seal (15%)
				DropList.add(new DropChance(186000469, 1, 15));   // Petra Medal (15%)
				DropList.add(new DropChance(152013034, 1, 15));    // Urge of Pleasure (15%)
				DropList.add(new DropChance(152013031, 1, 15));    // Urge of Terror (15%)
				DropList.add(new DropChance(152013028, 1, 15));    // Urge of Wrath (15%)

				for (DropChance drop : DropList) {
					if (Rnd.chance(drop.chance)) {
						dropItems.add(DropRegistrationService.getInstance().regDropItem(1, 0, npcId, drop.itemId, drop.count));
					}
				}
				break;
		}
	}
	
	private class DropChance {
		public int itemId;
		public int count;
		public int chance;

		public DropChance(int itemId, int count, int chance) {
			this.itemId = itemId;
			this.count = count;
			this.chance = chance;
		}
	}
	
	
}