package com.aionemu.gameserver.ai.instance.fissureOfOblivion.shadowOfOblivion;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * Fissure Of Oblivion 副本 NPC AI：Shadow Of Oblivion 66（@AIName "Shadow_Of_Oblivion_66"），继承 AggressiveNpcAI2。
 * Fissure Of Oblivion instance NPC AI: Shadow Of Oblivion 66 (@AIName "Shadow_Of_Oblivion_66"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("Shadow_Of_Oblivion_66")
public class Shadow_Of_Oblivion_66AI2 extends AggressiveNpcAI2
{
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 70) {
			ShadowOfOblivionType();
			announceShadowOfOblivion();
			AI2Actions.deleteOwner(this);
		}
	}
	
	private void announceShadowOfOblivion() {
		getPosition().getWorldMapInstance().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// 遗忘之影正在变身。 / Shadow of Oblivion is transforming.
					PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1403699));
				}
			}
		});
	}
	
	private void ShadowOfOblivionType() {
		GameEngineServices.skillEngine().getSkill(getOwner(), 18277, 60, getOwner()).useNoAnimationSkill(); //Oblivion.
		switch (Rnd.get(1, 4)) {
			case 1:
				spawn(244491, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
			break;
			case 2:
				spawn(244492, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
			break;
			case 3:
				spawn(244493, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
			break;
			case 4:
				spawn(244494, getOwner().getX(), getOwner().getY(), getOwner().getZ(), (byte) getOwner().getHeading());
			break;
		}
	}
}
