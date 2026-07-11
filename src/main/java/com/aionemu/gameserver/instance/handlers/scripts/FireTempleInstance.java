package com.aionemu.gameserver.instance.handlers.scripts;

import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.drop.DropItem;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.lifecycle.GameWorldServices;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.Set;

/**
 * 火焰神殿副本事件处理器。
 * Instance event handler for Fire Temple.
 *
 * @author Encom
 * @author MATTY (ADev.Team
 */

@InstanceID(320100000)
public class FireTempleInstance extends GeneralInstanceHandler
{
	/**
	 * 处理死亡事件。
	 * Handle a death event.
	 *
	 * npc
	 */
	@Override
    public void onDie(Npc npc) {
		Player player = npc.getAggroList().getMostPlayerDamage();
		switch (npc.getObjectTemplate().getTemplateId()) {
            case 212846: //Kromede The Corrupt.
			    spawnKromedeTreasureChest();
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Fire Temple>");
            break;
			case 214621: //Vile Judge Kromede.
				spawnKromedeTreasureChest();
				// 成功逃脱消息（注释掉的调试输出）。 / sendMsg("[SUCCES]: You have finished <Fire Temple>");
            break;
		}
	}
	
	private void spawnKromedeTreasureChest() {
		switch (Rnd.get(1, 3)) {
		    case 1:
			    announceKromedeOrnate();
				spawn(833523, 418.16385f, 95.81711f, 117.3052f, (byte) 50); //Kromede's Ornate Treasure Chest.
			break;
			case 2:
			    announceKromedeBrilliant();
				spawn(833524, 418.16385f, 95.81711f, 117.3052f, (byte) 50); //Kromede's Brilliant Treasure Chest.
			break;
			case 3:
			    announceKromedeDazzling();
				spawn(833525, 418.16385f, 95.81711f, 117.3052f, (byte) 50); //Kromede's Dazzling Treasure Chest.
			break;
		}
	}
	
	private void announceKromedeOrnate() {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 需要克罗梅德的华丽钥匙才能打开。 / I need a Kromede's Ornate Key to open it.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(false, 1111313, player.getObjectId(), 2));
				}
			}
		});
	}
	private void announceKromedeBrilliant() {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 需要克罗梅德的辉煌钥匙才能打开。 / I need a Kromede's Brilliant Key to open it.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(false, 1111314, player.getObjectId(), 2));
				}
			}
		});
	}
	private void announceKromedeDazzling() {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 需要克罗梅德的炫目钥匙才能打开。 / I need a Kromede's Dazzling Key to open it.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(false, 1111315, player.getObjectId(), 2));
				}
			}
		});
	}
	
	/**
	 * NPC 掉落表注册时处理。
	 * Handle NPC drop-table registration.
	 *
	 * npc
	 */
	@Override
    public void onDropRegistered(Npc npc) {
        Set<DropItem> dropItems = GameWorldServices.dropRegistrationService().getCurrentDropMap().get(npc.getObjectId());
		int npcId = npc.getNpcId();
		int index = dropItems.size() + 1;
        switch (npcId) {
            case 212846: //Kromede The Corrupt.
                for (Player player: instance.getPlayersInside()) {
                    if (player.isOnline()) {
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053994, 1)); //Kromede's Key Bundle.
                    }
                }
				break;
			case 214621: //Vile Judge Kromede.
                for (Player player: instance.getPlayersInside()) {
                    if (player.isOnline()) {
                        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053787, 1)); //烙印之石支援包。 / Stigma Support Bundle.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188053994, 1)); //Kromede's Key Bundle.
                    }
                }
				break;
			case 833523: //Kromede's Ornate Treasure Chest.
                for (Player player: instance.getPlayersInside()) {
                    if (player.isOnline()) {
                        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 170030000, 1)); //[Souvenir] Kromede's Mirror.
                    }
                }
				break;
			case 833524: //Kromede's Brilliant Treasure Chest.
                for (Player player: instance.getPlayersInside()) {
                    if (player.isOnline()) {
                        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 170030000, 1)); //[Souvenir] Kromede's Mirror.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188052826, 1)); //Judge's Fabled Weapon Chest
                    }
                }
				break;
			case 833525: //Kromede's Dazzling Treasure Chest.
                for (Player player: instance.getPlayersInside()) {
                    if (player.isOnline()) {
                        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 170030000, 1)); //[Souvenir] Kromede's Mirror.
						dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188052826, 1)); //Judge's Fabled Weapon Chest
                    }
                }
				break;
			case 212840: //Lava Gatneri.
                for (Player player: instance.getPlayersInside()) {
                    if (player.isOnline()) {
                        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188051411, 1)); //Gatneri's Fabled Weapon Chest.
                    }
                }
				break;
			case 212842: //Black Smoke Asparn.
                for (Player player: instance.getPlayersInside()) {
                    if (player.isOnline()) {
                        dropItems.add(GameWorldServices.dropRegistrationService().regDropItem(index++, player.getObjectId(), npcId, 188051412, 1)); //Asparn's Fabled Weapon Chest.
                    }
                }
				break;
        }
    }
	
	/**
	 * 副本创建时初始化逻辑。
	 * Initialize logic when the instance is created.
	 *
	 * @param instance 世界地图实例 / world-map instance
	 */
	@Override
    public void onInstanceCreate(WorldMapInstance instance) {
        super.onInstanceCreate(instance);
		
        // Boss 随机生成 / Random spawns of bosses
        if (Rnd.get(1, 100) > 25) { // Blue Crystal Molgat
            spawn(212839, 127.1218f, 176.1912f, 99.67548f, (byte) 15);
        } else { // elite mob spawns
            spawn(212790, 127.1218f, 176.1912f, 99.67548f, (byte) 15);
        }

        if (Rnd.get(1, 100) > 25) { // Black Smoke Asparn
            spawn(212842, 322.3193f, 431.2696f, 134.5296f, (byte) 80);
        } else { // elite mob spawns
            spawn(212799, 322.3193f, 431.2696f, 134.5296f, (byte) 80);
        }

        if (Rnd.get(1, 100) > 25) { // Lava Gatneri
            spawn(212840, 153.0038f, 299.7786f, 123.0186f, (byte) 30);
        } else { // elite mob spawns
            spawn(212794, 153.0038f, 299.7786f, 123.0186f, (byte) 30);
        }

        if (Rnd.get(1, 100) > 25) { // Tough Sipus
            spawn(212843, 296.6911f, 201.9092f, 119.3652f, (byte) 30);
        } else { // elite mob spawns
            spawn(212803, 296.6911f, 201.9092f, 119.3652f, (byte) 15);
        }

        if (Rnd.get(1, 100) > 25) { // Flame Branch Flavi
            spawn(212841, 350.9276f, 351.7389f, 146.8498f, (byte) 45);
        } else { // elite mob spawns
            spawn(212799, 350.9276f, 351.7389f, 146.8498f, (byte) 45);
        }

        if (Rnd.get(1, 100) > 25) { // Broken Wing Kutisen
            spawn(212845, 298.7095f, 89.42245f, 128.7143f, (byte) 15);
        } else { // elite mob spawns
            spawn(214094, 298.7095f, 89.42245f, 128.7143f, (byte) 15);
        }

        if (Rnd.get(1, 100) > 90) {// stronger kromede
            spawn(214621, 421.9935f, 93.18915f, 117.3053f, (byte) 46);
        } else { // normal kromede
            spawn(212846, 421.9935f, 93.18915f, 117.3053f, (byte) 46);
        }
    }
	
	private void deleteNpc(int npcId) {
		if (getNpc(npcId) != null) {
			getNpc(npcId).getController().onDelete();
		}
	}
	
	private void sendMsg(final String str) {
		instance.doOnAllPlayers(new Visitor<Player>() {
			/**
			 * 处理 visit。
			 * Handle visit.
			 *
			 * @param player 玩家 / player
			 */
			@Override
			public void visit(Player player) {
				PacketSendUtility.sendWhiteMessageOnCenter(player, str);
			}
		});
	}
}