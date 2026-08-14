package com.aionemu.gameserver.ai.instance.drakenseerLair;

import com.aionemu.gameserver.lifecycle.GameFeatureServices;

import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.AIState;
import com.aionemu.gameserver.ai2.AbstractAI;
import com.aionemu.gameserver.ai2.NpcAI2;
import com.aionemu.gameserver.model.EmotionType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.network.aion.serverpackets.SM_EMOTION;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.NpcShoutsService;
import com.aionemu.gameserver.utils.PacketSendUtility;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Drakenseer Lair 副本 NPC AI：IDF6 Dragon Artifact Boost A（@AIName "IDF6_Dragon_Artifact_Boost_A"），继承 NpcAI2。
 * Drakenseer Lair instance NPC AI: IDF6 Dragon Artifact Boost A (@AIName "IDF6_Dragon_Artifact_Boost_A"), extends NpcAI2.
 *
 * @author Encom
 */
@AIName("IDF6_Dragon_Artifact_Boost_A")
public class IDF6_Dragon_Artifact_Boost_AAI2 extends NpcAI2
{
	private Npc IDF6DragonGate;
	private boolean canThink = true;
	private int artifactBoostPhase = 0;
	private AtomicBoolean isAggred = new AtomicBoolean(false);
	
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		if (isAggred.compareAndSet(false, true)) {
		} if (IDF6DragonGate == null) {
		    IDF6DragonGate = (Npc)spawn(703159, 238.9179f, 292.35995f, 318.56708f, (byte) 111);
		}
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	private void checkPercentage(int hpPercentage) {
		if (hpPercentage == 99 && artifactBoostPhase < 1) {
			artifactBoostPhase = 1;
			startIDF6DragonRaidA1();
		} if (hpPercentage == 80 && artifactBoostPhase < 2) {
			artifactBoostPhase = 2;
			startIDF6DragonRaidA1();
		} if (hpPercentage == 60 && artifactBoostPhase < 3) {
			artifactBoostPhase = 3;
			startIDF6DragonRaidA1();
		} if (hpPercentage == 40 && artifactBoostPhase < 4) {
			artifactBoostPhase = 4;
			startIDF6DragonRaidA1();
		} if (hpPercentage == 8 && artifactBoostPhase < 5) {
			artifactBoostPhase = 5;
			startIDF6DragonRaidA1();
		}
	}
	
   /**
	 * 发起龙族突袭 A 阶段。
	 * Dragon Raid A
	 */
	private void startIDF6DragonRaidA1() {
		// 增援已抵达，保护护盾导管。 / Reinforcements have arrived to defend the Shielding Conduits.
		PacketSendUtility.npcSendPacketTime(getOwner(), SM_SYSTEM_MESSAGE.STR_MSG_IDF6_Dragon_Attack_Tower, 0);
		//准备战斗！敌人接近！ / Prepare for combat! Enemies approaching!
		GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1402785, 10000);
		//再坚持一下就能活下来。 / Hold a little longer and you will survive.
		GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1402833, 20000);
		//只剩少数敌人！ / Only a few enemies left!
		GameFeatureServices.npcShoutsService().sendMsg(getOwner(), 1402834, 30000);
		dragonRaid((Npc)spawn(220432, 243.8898f, 288.99213f, 318.18826f, (byte) 115), 272.1099f, 271.96097f, 318.85516f, false);
		dragonRaid((Npc)spawn(220433, 243.8898f, 288.99213f, 318.18826f, (byte) 115), 272.1099f, 271.96097f, 318.85516f, false);
		dragonRaid((Npc)spawn(220432, 243.8898f, 288.99213f, 318.18826f, (byte) 115), 272.1099f, 271.96097f, 318.85516f, false);
		dragonRaid((Npc)spawn(220434, 243.8898f, 288.99213f, 318.18826f, (byte) 115), 272.1099f, 271.96097f, 318.85516f, false);
		dragonRaid((Npc)spawn(220435, 243.8898f, 288.99213f, 318.18826f, (byte) 115), 272.1099f, 271.96097f, 318.85516f, false);
		dragonRaid((Npc)spawn(220438, 243.8898f, 288.99213f, 318.18826f, (byte) 115), 272.1099f, 271.96097f, 318.85516f, false);
	}
	
	private void dragonRaid(final Npc npc, float x, float y, float z, boolean despawn) {
		((AbstractAI) npc.getAi2()).setStateIfNot(AIState.WALKING);
		npc.setState(1);
		npc.getMoveController().moveToPoint(x, y, z);
		PacketSendUtility.broadcastPacket(npc, new SM_EMOTION(npc, EmotionType.START_EMOTE2, 0, npc.getObjectId()));
	}
	
	@Override
	public boolean canThink() {
		return canThink;
	}
	
	@Override
	protected void handleBackHome() {
		canThink = true;
		isAggred.set(false);
		super.handleBackHome();
	}
}
