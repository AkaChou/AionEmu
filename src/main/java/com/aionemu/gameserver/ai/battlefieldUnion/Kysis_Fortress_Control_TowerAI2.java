package com.aionemu.gameserver.ai.battlefieldUnion;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.Race;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 战场同盟相关 NPC AI：Kysis Fortress Control Tower（@AIName "Kysis_Fortress_Control_Tower"），继承 NpcAI2。
 * Battlefield-union related NPC AI: Kysis Fortress Control Tower (@AIName "Kysis_Fortress_Control_Tower"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("Kysis_Fortress_Control_Tower")
public class Kysis_Fortress_Control_TowerAI2 extends NpcAI2
{
	private Race MsgRace;
	private Race KysisRace;
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	
	@Override
    protected void handleSpawned() {
		switch (getNpcId()) {
			case 884071: //Balaur Kysis Fortress 1st Control Tower.
				announce1stBalaurAppears();
			break;
			case 884074: //Balaur Kysis Fortress 2nd Control Tower.
				announce2ndBalaurAppears();
			break;
			case 884077: //Balaur Kysis Fortress 3rd Control Tower.
				announce3rdBalaurAppears();
			break;
			case 884080: //Balaur Kysis Fortress 4th Control Tower.
				announce4thBalaurAppears();
			break;
		}
		super.handleSpawned();
    }
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
			switch (getNpcId()) {
				case 884069: //Elyos Kysis Fortress 1st Control Tower.
				case 884070: //Asmodians Kysis Fortress 1st Control Tower.
				case 884071: //Balaur Kysis Fortress 1st Control Tower.
				    announceTalked01();
				break;
				case 884072: //Elyos Kysis Fortress 2nd Control Tower.
				case 884073: //Asmodians Kysis Fortress 2nd Control Tower.
				case 884074: //Balaur Kysis Fortress 2nd Control Tower.
				    announceTalked02();
				break;
				case 884075: //Elyos Kysis Fortress 3rd Control Tower.
				case 884076: //Asmodians Kysis Fortress 3rd Control Tower.
				case 884077: //Balaur Kysis Fortress 3rd Control Tower.
				    announceTalked03();
				break;
				case 884078: //Elyos Kysis Fortress 4th Control Tower.
				case 884079: //Asmodians Kysis Fortress 4th Control Tower.
				case 884080: //Balaur Kysis Fortress 4th Control Tower.
				    announceTalked04();
				break;
			}
		}
	}
	
	@Override
	protected void handleDied() {
		switch (getNpcId()) {
			case 884069: //Elyos Kysis Fortress 1st Control Tower.
			case 884070: //Asmodians Kysis Fortress 1st Control Tower.
			case 884071: //Balaur Kysis Fortress 1st Control Tower.
				spawn1stAppears();
				announce1stAppears();
				announceOccupied1st();
			break;
			case 884072: //Elyos Kysis Fortress 2nd Control Tower.
			case 884073: //Asmodians Kysis Fortress 2nd Control Tower.
			case 884074: //Balaur Kysis Fortress 2nd Control Tower.
				spawn2ndAppears();
				announce2ndAppears();
				announceOccupied2nd();
			break;
			case 884075: //Elyos Kysis Fortress 3rd Control Tower.
			case 884076: //Asmodians Kysis Fortress 3rd Control Tower.
			case 884077: //Balaur Kysis Fortress 3rd Control Tower.
				spawn3rdAppears();
				announce3rdAppears();
				announceOccupied3rd();
			break;
			case 884078: //Elyos Kysis Fortress 4th Control Tower.
			case 884079: //Asmodians Kysis Fortress 4th Control Tower.
			case 884080: //Balaur Kysis Fortress 4th Control Tower.
				spawn4thAppears();
				announce4thAppears();
				announceOccupied4th();
			break;
		}
		super.handleDied();
	}
	
	private void announce1stAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 魔族第 1 基西斯控制塔已出现。 / The Asmodians's 1st Kysis Control Tower has appeared.
				// 天族第 1 基西斯控制塔已出现。 / The Elyos's 1st Kysis Control Tower has appeared.
				final int msgAppears01 = MsgRace == Race.ASMODIANS ? 1403939 : 1403935;
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msgAppears01));
			}
		});
	}
	private void announce2ndAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 魔族第 2 基西斯控制塔已出现。 / The Asmodians's 2nd Kysis Control Tower has appeared.
				// 天族第 2 基西斯控制塔已出现。 / The Elyos's 2nd Kysis Control Tower has appeared.
				final int msgAppears02 = MsgRace == Race.ASMODIANS ? 1403940 : 1403936;
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msgAppears02));
			}
		});
	}
	private void announce3rdAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 魔族第 3 基西斯控制塔已出现。 / The Asmodians's 3rd Kysis Control Tower has appeared.
				// 天族第 3 基西斯控制塔已出现。 / The Elyos's 3rd Kysis Control Tower has appeared.
				final int msgAppears03 = MsgRace == Race.ASMODIANS ? 1403941 : 1403937;
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msgAppears03));
			}
		});
	}
	private void announce4thAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 魔族第 4 基西斯控制塔已出现。 / The Asmodians's 4th Kysis Control Tower has appeared.
				// 天族第 4 基西斯控制塔已出现。 / The Elyos's 4th Kysis Control Tower has appeared.
				final int msgAppears04 = MsgRace == Race.ASMODIANS ? 1403942 : 1403938;
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msgAppears04));
			}
		});
	}
	
	private void announceOccupied1st() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 天族占领了第 1 基西斯控制塔。 / The Elyos have occupied the 1st Kysis Control Tower.
				// 魔族占领了第 1 基西斯控制塔。 / The Asmodians have occupied the 1st Kysis Control Tower.
				final int msgOccupied01 = MsgRace == Race.ASMODIANS ? 1403916 : 1403912;
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msgOccupied01));
			}
		});
	}
	private void announceOccupied2nd() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 天族占领了第 2 基西斯控制塔。 / The Elyos have occupied the 2nd Kysis Control Tower.
				// 魔族占领了第 2 基西斯控制塔。 / The Asmodians have occupied the 2nd Kysis Control Tower.
				final int msgOccupied02 = MsgRace == Race.ASMODIANS ? 1403917 : 1403913;
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msgOccupied02));
			}
		});
	}
	private void announceOccupied3rd() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 天族占领了第 3 基西斯控制塔。 / The Elyos have occupied the 3rd Kysis Control Tower.
				// 魔族占领了第 3 基西斯控制塔。 / The Asmodians have occupied the 3rd Kysis Control Tower.
				final int msgOccupied03 = MsgRace == Race.ASMODIANS ? 1403918 : 1403914;
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msgOccupied03));
			}
		});
	}
	private void announceOccupied4th() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 天族占领了第 4 基西斯控制塔。 / The Elyos have occupied the 4th Kysis Control Tower.
				// 魔族占领了第 4 基西斯控制塔。 / The Asmodians have occupied the 4th Kysis Control Tower.
				final int msgOccupied04 = MsgRace == Race.ASMODIANS ? 1403919 : 1403915;
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msgOccupied04));
			}
		});
	}
	
	private void announceTalked01() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 魔族正试图摧毁第 1 基西斯控制塔。 / The Asmodians are trying to destroy the 1st Kysis Control Tower.
				// 天族正试图摧毁第 1 基西斯控制塔。 / The Elyos are trying to destroy the 1st Kysis Control Tower.
				final int msgTalked01 = MsgRace == Race.ASMODIANS ? 1403923 : 1403927;
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msgTalked01));
			}
		});
	}
	private void announceTalked02() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 魔族正试图摧毁第 2 基西斯控制塔。 / The Asmodians are trying to destroy the 2nd Kysis Control Tower.
				// 天族正试图摧毁第 2 基西斯控制塔。 / The Elyos are trying to destroy the 2nd Kysis Control Tower.
				final int msgTalked02 = MsgRace == Race.ASMODIANS ? 1403924 : 1403928;
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msgTalked02));
			}
		});
	}
	private void announceTalked03() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 魔族正试图摧毁第 3 基西斯控制塔。 / The Asmodians are trying to destroy the 3rd Kysis Control Tower.
				// 天族正试图摧毁第 3 基西斯控制塔。 / The Elyos are trying to destroy the 3rd Kysis Control Tower.
				final int msgTalked03 = MsgRace == Race.ASMODIANS ? 1403925 : 1403929;
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msgTalked03));
			}
		});
	}
	private void announceTalked04() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 魔族正试图摧毁第 4 基西斯控制塔。 / The Asmodians are trying to destroy the 4th Kysis Control Tower.
				// 天族正试图摧毁第 4 基西斯控制塔。 / The Elyos are trying to destroy the 4th Kysis Control Tower.
				final int msgTalked04 = MsgRace == Race.ASMODIANS ? 1403926 : 1403930;
				PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(msgTalked04));
			}
		});
	}
	
	private void spawn1stAppears() {
		final int kysisControlTower01 = KysisRace == Race.ASMODIANS ? 884070 : 884069;
		spawn(kysisControlTower01, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
	}
	private void spawn2ndAppears() {
		final int kysisControlTower02 = KysisRace == Race.ASMODIANS ? 884073 : 884072;
		spawn(kysisControlTower02, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
	}
	private void spawn3rdAppears() {
		final int kysisControlTower03 = KysisRace == Race.ASMODIANS ? 884076 : 884075;
		spawn(kysisControlTower03, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
	}
	private void spawn4thAppears() {
		final int kysisControlTower04 = KysisRace == Race.ASMODIANS ? 884079 : 884078;
		spawn(kysisControlTower04, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
	}
	
	private void announce1stBalaurAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 龙族第 1 基西斯控制塔已出现。 / The Balaur's 1st Kysis Control Tower has appeared.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1231_Dkisas_Position_Spawn_01_Dr, 0);
				// 龙族占领了第 1 基西斯控制塔。 / The Balaur have occupied the 1st Kysis Control Tower.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1231_Dkisas_Position_100th_01_Dr, 10000);
				
			}
		});
	}
	private void announce2ndBalaurAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 龙族第 2 基西斯控制塔已出现。 / The Balaur's 2nd Kysis Control Tower has appeared.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1231_Dkisas_Position_Spawn_02_Dr, 20000);
				// 龙族占领了第 2 基西斯控制塔。 / The Balaur have occupied the 2nd Kysis Control Tower.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1231_Dkisas_Position_100th_02_Dr, 30000);
			}
		});
	}
	private void announce3rdBalaurAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 龙族第 3 基西斯控制塔已出现。 / The Balaur's 3rd Kysis Control Tower has appeared.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1231_Dkisas_Position_Spawn_03_Dr, 40000);
				// 龙族占领了第 3 基西斯控制塔。 / The Balaur have occupied the 3rd Kysis Control Tower.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1231_Dkisas_Position_100th_03_Dr, 50000);
			}
		});
	}
	private void announce4thBalaurAppears() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 龙族第 4 基西斯控制塔已出现。 / The Balaur's 4th Kysis Control Tower has appeared.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1231_Dkisas_Position_Spawn_04_Dr, 60000);
				// 龙族占领了第 4 基西斯控制塔。 / The Balaur have occupied the 4th Kysis Control Tower.
				PacketSendUtility.playerSendPacketTime(player, SM_SYSTEM_MESSAGE.STR_MSG_1231_Dkisas_Position_100th_04_Dr, 70000);
			}
		});
	}
}
