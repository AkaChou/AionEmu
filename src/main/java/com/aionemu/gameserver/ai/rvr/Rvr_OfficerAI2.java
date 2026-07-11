package com.aionemu.gameserver.ai.rvr;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.HTMLService;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * RvR 相关 NPC AI：Rvr Officer（@AIName "rvr_officer"），继承 AggressiveNpcAI2。
 * RvR-related NPC AI: Rvr Officer (@AIName "rvr_officer"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("rvr_officer")
public class Rvr_OfficerAI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		startLifeTask();
	}
	
	@Override
	protected void handleDied() {
        switch (getNpcId()) {
			// 攻击者 魔族。 / Attacker Asmodians.
			case 857733: //Officer Tarkan.
				sendRvrGuide();
				announceGeneralMiltarRescued();
			break;
			case 857734: //Officer Shagad.
			    sendRvrGuide();
				announceGeneralKuparoRescued();
			break;
			case 857735: //Officer Argan.
			    sendRvrGuide();
				announceGeneralLanstriRescued();
			break;
			// 攻击者 天族。 / Attacker Elyos.
			case 857740: //Officer Nars.
			    sendRvrGuide();
				announceGeneralMagkenRescued();
			break;
			case 857741: //Officer Fasig.
			    sendRvrGuide();
				announceGeneralHarkRescued();
			break;
			case 857742: //Officer Gadevir.
			    sendRvrGuide();
				announceGeneralTombolkRescued();
			break;
		}
		super.handleDied();
	}
	
	private void sendRvrGuide() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (MathUtil.isIn3dRange(player, getOwner(), 15)) {
					HTMLService.sendGuideHtml(player, "Rvr_Guide");
				}
			}
		});
	}
	
   /**
	 * 进攻方魔族 / Attacker Asmodians
	 */
	private void announceGeneralMiltarRescued() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 魔族袭击者成功消灭了米尔塔将军。 / The Asmodian Raiders have successfully eliminated General Miltar.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_RVR_LF3_BOSS_HEAL_NOTICE_01);
			}
		});
	}
	private void announceGeneralKuparoRescued() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 魔族袭击者成功消灭了库皮亚罗将军。 / The Asmodian Raiders have successfully eliminated General Kupiaro.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_RVR_LF3_BOSS_HEAL_NOTICE_02);
			}
		});
	}
	private void announceGeneralLanstriRescued() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 魔族袭击者成功消灭了兰斯崔将军。 / The Asmodian Raiders have successfully eliminated General Lanstri.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_RVR_LF3_BOSS_HEAL_NOTICE_03);
			}
		});
	}
	
   /**
	 * 进攻方天族 / Attacker Elyos
	 */
	private void announceGeneralMagkenRescued() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 魔族守护者成功救出马格肯将军。 / The Asmodian Protectors have successfully rescued General Magken.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_RVR_DF3_BOSS_HEAL_NOTICE_01);
			}
		});
	}
	private void announceGeneralHarkRescued() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 魔族守护者成功救出哈克将军。 / The Asmodian Protectors have successfully rescued General Hark.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_RVR_DF3_BOSS_HEAL_NOTICE_02);
			}
		});
	}
	private void announceGeneralTombolkRescued() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 魔族守护者成功救出通博尔克将军。 / The Asmodian Protectors have successfully rescued General Tombolk.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_RVR_DF3_BOSS_HEAL_NOTICE_03);
			}
		});
	}
	
	private void startLifeTask() {
		GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
			@Override
			public void run() {
				AI2Actions.deleteOwner(Rvr_OfficerAI2.this);
			}
		}, 3540000);
	}
}
