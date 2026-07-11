package com.aionemu.gameserver.ai.battlefieldUnion;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 战场同盟相关 NPC AI：Krotan Fortress Commander（@AIName "Krotan_Fortress_Commander"），继承 AggressiveNpcAI2。
 * Battlefield-union related NPC AI: Krotan Fortress Commander (@AIName "Krotan_Fortress_Commander"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("Krotan_Fortress_Commander")
public class Krotan_Fortress_CommanderAI2 extends AggressiveNpcAI2
{
	@Override
    protected void handleSpawned() {
		switch (getNpcId()) {
			// 克罗坦要塞指挥官【龙族】 / Krotan Fortress Commander [Balaur]
			case 884057: //Ereshkigal Icecrowned Krotan Commander.
				announceIcecrownedAppears();
			break;
			case 884058: //Ereshkigal Icecoated Krotan Commander.
				announceIcecoatedAppears();
			break;
			case 884059: //Ereshkigal Icebladed Krotan Commander.
				announceIcebladedAppears();
			break;
			case 884060: //Ereshkigal Icesteeped Krotan Commander.
				announceIcesteepedAppears();
			break;
			case 884061: //Ereshkigal Icedrenched Krotan Commander.
				announceIcedrenchedAppears();
			break;
			case 884062: //Ereshkigal Iceblooded Krotan Commander.
				announceIcebloodedAppears();
			break;
			// 克罗坦要塞指挥官【天族】 / Krotan Fortress Commander [Elyos]
			case 884094: //Krotan Fortress Sunbathed Commander
				announceSunbathedAppears();
			break;
			case 884118: //Krotan Fortress Sunsoaked Commander
				announceSunsoakedAppears();
			break;
			case 884142: //Krotan Fortress Suntouched Commander.
				announceSuntouchedAppears();
			break;
			case 884166: //Krotan Fortress Sunsteeped Commander.
				announceSunsteepedAppears();
			break;
			case 884190: //Krotan Fortress Sundrenched Commander.
				announceSundrenchedAppears();
			break;
			case 884214: //Krotan Fortress Sunblessed Commander.
				announceSunblessedAppears();
			break;
			// 克罗坦要塞指挥官【魔族】 / Krotan Fortress Commander [Asmodians]
			case 884106: //Krotan Fortress Shadeprotected Commander.
				announceShadeprotectedAppears();
			break;
			case 884130: //Krotan Fortress Shadesoaked Commander.
				announceShadesoakedAppears();
			break;
			case 884154: //Krotan Fortress Shadetouched Commander.
				announceShadetouchedAppears();
			break;
			case 884178: //Krotan Fortress Shadesteeped Commander.
				announceShadesteepedAppears();
			break;
			case 884202: //Krotan Fortress Shadedrenched Commander.
				announceShadedrenchedAppears();
			break;
			case 884226: //Krotan Fortress Shadeblessed Commander.
				announceShadeblessedAppears();
			break;
		}
		super.handleSpawned();
    }
	
   /**
	 * 天族 / Elyos
	 */
	private void announceSunbathedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞浴日指挥官已出现！ / The Krotan Fortress Sunbathed Commander has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_01_Spawn_Li, 0);
			}
		});
	}
	private void announceSunsoakedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞浸日指挥官已出现！ / The Krotan Fortress Sunsoaked Commander has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_02_Spawn_Li, 10000);
			}
		});
	}
	private void announceSuntouchedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞触日指挥官已出现！ / The Krotan Fortress Suntouched Commander has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_03_Spawn_Li, 20000);
			}
		});
	}
	private void announceSunsteepedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞深日指挥官已出现！ / The Krotan Fortress Sunsteeped Commander has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_04_Spawn_Li, 30000);
			}
		});
	}
	private void announceSundrenchedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞沐日指挥官已出现！ / The Krotan Fortress Sundrenched Commander has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_05_Spawn_Li, 40000);
			}
		});
	}
	private void announceSunblessedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞日福指挥官已出现！ / The Krotan Fortress Sunblessed Commander has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_06_Spawn_Li, 50000);
			}
		});
	}
	
   /**
	 * 魔族 / Asmodians
	 */
	private void announceShadeprotectedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞蔽护指挥官已出现！ / The Krotan Fortress Shadeprotected Commander has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_01_Spawn_Da, 0);
			}
		});
	}
	private void announceShadesoakedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞浸影指挥官已出现！ / The Krotan Fortress Shadesoaked Commander has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_02_Spawn_Da, 10000);
			}
		});
	}
	private void announceShadetouchedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞触影指挥官已出现！ / The Krotan Fortress Shadetouched Commander has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_03_Spawn_Da, 20000);
			}
		});
	}
	private void announceShadesteepedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞深影指挥官已出现！ / The Krotan Fortress Shadesteeped Commander has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_04_Spawn_Da, 30000);
			}
		});
	}
	private void announceShadedrenchedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞浸影指挥官已出现！ / The Krotan Fortress Shadedrenched Commander has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_05_Spawn_Da, 40000);
			}
		});
	}
	private void announceShadeblessedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞荫福指挥官已出现！ / The Krotan Fortress Shadeblessed Commander has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_06_Spawn_Da, 50000);
			}
		});
	}
	
   /**
	 * 龙族 / Balaur
	 */
	private void announceIcecrownedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 埃雷什基伽尔冰冠克罗坦指挥官已出现！ / The Ereshkigal Icecrowned Krotan Commander has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_01_Spawn_Dr, 0);
			}
		});
	}
	private void announceIcecoatedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 埃雷什基伽尔覆冰克罗坦指挥官已出现！ / The Ereshkigal Icecoated Krotan Commander has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_02_Spawn_Dr, 10000);
			}
		});
	}
	private void announceIcebladedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 埃雷什基伽尔冰刃克罗坦指挥官已出现！ / The Ereshkigal Icebladed Krotan Commander has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_03_Spawn_Dr, 20000);
			}
		});
	}
	private void announceIcesteepedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 埃雷什基伽尔深冰克罗坦指挥官已出现！ / The Ereshkigal Icesteeped Krotan Commander has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_04_Spawn_Dr, 30000);
			}
		});
	}
	private void announceIcedrenchedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 埃雷什基伽尔冰浸克罗坦指挥官已出现！ / The Ereshkigal Icedrenched Krotan Commander has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_05_Spawn_Dr, 40000);
			}
		});
	}
	private void announceIcebloodedAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 埃雷什基伽尔冰血克罗坦指挥官已出现！ / The Ereshkigal Iceblooded Krotan Commander has appeared!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_06_Spawn_Dr, 50000);
			}
		});
	}
	
	@Override
	protected void handleDied() {
		switch (getNpcId()) {
			// 克罗坦要塞指挥官【龙族】 / Krotan Fortress Commander [Balaur]
			case 884057: //Ereshkigal Icecrowned Krotan Commander.
				announceIcecrownedDied();
			break;
			case 884058: //Ereshkigal Icecoated Krotan Commander.
				announceIcecoatedDied();
			break;
			case 884059: //Ereshkigal Icebladed Krotan Commander.
				announceIcebladedDied();
			break;
			case 884060: //Ereshkigal Icesteeped Krotan Commander.
				announceIcesteepedDied();
			break;
			case 884061: //Ereshkigal Icedrenched Krotan Commander.
				announceIcedrenchedDied();
			break;
			case 884062: //Ereshkigal Iceblooded Krotan Commander.
				announceIcebloodedDied();
			break;
			// 克罗坦要塞指挥官【天族】 / Krotan Fortress Commander [Elyos]
			case 884094: //Krotan Fortress Sunbathed Commander
				announceSunbathedDied();
			break;
			case 884118: //Krotan Fortress Sunsoaked Commander
				announceSunsoakedDied();
			break;
			case 884142: //Krotan Fortress Suntouched Commander.
				announceSuntouchedDied();
			break;
			case 884166: //Krotan Fortress Sunsteeped Commander.
				announceSunsteepedDied();
			break;
			case 884190: //Krotan Fortress Sundrenched Commander.
				announceSundrenchedDied();
			break;
			case 884214: //Krotan Fortress Sunblessed Commander.
				announceSunblessedDied();
			break;
			// 克罗坦要塞指挥官【魔族】 / Krotan Fortress Commander [Asmodians]
			case 884106: //Krotan Fortress Shadeprotected Commander.
				announceShadeprotectedDied();
			break;
			case 884130: //Krotan Fortress Shadesoaked Commander.
				announceShadesoakedDied();
			break;
			case 884154: //Krotan Fortress Shadetouched Commander.
				announceShadetouchedDied();
			break;
			case 884178: //Krotan Fortress Shadesteeped Commander.
				announceShadesteepedDied();
			break;
			case 884202: //Krotan Fortress Shadedrenched Commander.
				announceShadedrenchedDied();
			break;
			case 884226: //Krotan Fortress Shadeblessed Commander.
				announceShadeblessedDied();
			break;
		}
		super.handleDied();
	}
	
   /**
	 * 天族 / Elyos
	 */
	private void announceSunbathedDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞浴日指挥官已被击杀！ / The Krotan Fortress Sunbathed Commander has been slain!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_01_Die_Li, 0);
			}
		});
	}
	private void announceSunsoakedDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞浸日指挥官已被击杀！ / The Krotan Fortress Sunsoaked Commander has been slain!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_02_Die_Li, 0);
			}
		});
	}
	private void announceSuntouchedDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞触日指挥官已被击杀！ / The Krotan Fortress Suntouched Commander has been slain!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_03_Die_Li, 0);
			}
		});
	}
	private void announceSunsteepedDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞深日指挥官已被击杀！ / The Krotan Fortress Sunsteeped Commander has been slain!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_04_Die_Li, 0);
			}
		});
	}
	private void announceSundrenchedDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞沐日指挥官已被击杀！ / The Krotan Fortress Sundrenched Commander has been slain!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_05_Die_Li, 0);
			}
		});
	}
	private void announceSunblessedDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞日福指挥官已被击杀！ / The Krotan Fortress Sunblessed Commander has been slain!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_06_Die_Li, 0);
			}
		});
	}
	
   /**
	 * 魔族 / Asmodians
	 */
	private void announceShadeprotectedDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞蔽护指挥官已被击杀！ / The Krotan Fortress Shadeprotected Commander has been slain!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_01_Die_Da, 0);
			}
		});
	}
	private void announceShadesoakedDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞浸影指挥官已被击杀！ / The Krotan Fortress Shadesoaked Commander has been slain!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_02_Die_Da, 0);
			}
		});
	}
	private void announceShadetouchedDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞触影指挥官已被击杀！ / The Krotan Fortress Shadetouched Commander has been slain!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_03_Die_Da, 0);
			}
		});
	}
	private void announceShadesteepedDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞深影指挥官已被击杀！ / The Krotan Fortress Shadesteeped Commander has been slain!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_04_Die_Da, 0);
			}
		});
	}
	private void announceShadedrenchedDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞浸影指挥官已被击杀！ / The Krotan Fortress Shadedrenched Commander has been slain!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_05_Die_Da, 0);
			}
		});
	}
	private void announceShadeblessedDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 克罗坦要塞荫福指挥官已被击杀！ / The Krotan Fortress Shadeblessed Commander has been slain!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_06_Die_Da, 0);
			}
		});
	}
	
   /**
	 * 龙族 / Balaur
	 */
	private void announceIcecrownedDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 埃雷什基伽尔冰冠克罗坦指挥官已被击杀！ / The Ereshkigal Icecrowned Krotan Commander has been slain!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_01_Die_Dr, 0);
			}
		});
	}
	private void announceIcecoatedDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 埃雷什基伽尔覆冰克罗坦指挥官已被击杀！ / The Ereshkigal Icecoated Krotan Commander has been slain!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_02_Die_Dr, 0);
			}
		});
	}
	private void announceIcebladedDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 埃雷什基伽尔冰刃克罗坦指挥官已被击杀！ / The Ereshkigal Icebladed Krotan Commander has been slain!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_03_Die_Dr, 0);
			}
		});
	}
	private void announceIcesteepedDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 埃雷什基伽尔深冰克罗坦指挥官已被击杀！ / The Ereshkigal Icesteeped Krotan Commander has been slain!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_04_Die_Dr, 0);
			}
		});
	}
	private void announceIcedrenchedDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 埃雷什基伽尔冰浸克罗坦指挥官已被击杀！ / The Ereshkigal Icedrenched Krotan Commander has been slain!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_05_Die_Dr, 0);
			}
		});
	}
	private void announceIcebloodedDied() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 埃雷什基伽尔冰血克罗坦指挥官已被击杀！ / The Ereshkigal Iceblooded Krotan Commander has been slain!
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1221_commander_06_Die_Dr, 0);
			}
		});
	}
}
