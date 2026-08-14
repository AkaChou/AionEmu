package com.aionemu.gameserver.ai;

import com.aionemu.gameserver.lifecycle.GameEngineServices;

import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import com.aionemu.commons.network.util.ThreadPoolManager;
import com.aionemu.commons.utils.Rnd;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.dataholders.DataManager;
import com.aionemu.gameserver.model.ai.Percentage;
import com.aionemu.gameserver.model.ai.SummonGroup;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.templates.spawns.SpawnTemplate;
import com.aionemu.gameserver.network.aion.serverpackets.SM_SYSTEM_MESSAGE;
import com.aionemu.gameserver.services.abyss.AbyssPointsService;
import com.aionemu.gameserver.skillengine.SkillEngine;
import com.aionemu.gameserver.spawnengine.SpawnEngine;
import com.aionemu.gameserver.utils.MathUtil;
import com.aionemu.gameserver.utils.PacketSendUtility;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.knownlist.Visitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 召唤师型 NPC AI：在战斗中按阶段召唤随从。
 * Summoner-style NPC AI that spawns adds during combat by phase.
 *
 * @author Encom
 */
@AIName("summoner")
public class SummonerAI2 extends AggressiveNpcAI2
{
	private final List<Integer> spawnedNpc = new ArrayList<Integer>();
	private List<Percentage> percentage = Collections.emptyList();
	private int spawnedPercent = 0;
	
	/**
	 * 处理受到攻击事件。
	 * Handle being attacked.
	 *
	 * @param creature 攻击者 / attacker
	 */
	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}
	
	/**
	 * 处理消失事件。
	 * Handle despawn.
	 */
	@Override
	protected void handleDespawned() {
		super.handleDespawned();
		synchronized(spawnedNpc) {
			removeHelpersSpawn();
			spawnedNpc.clear();
		}
		percentage.clear();
	}
	
	/**
	 * 处理归位完成事件。
	 * Handle back-home.
	 */
	@Override
	protected void handleBackHome() {
		super.handleBackHome();
		synchronized(spawnedNpc) {
			removeHelpersSpawn();
			spawnedNpc.clear();
		}
		spawnedPercent = 0;
	}
	
	/**
	 * 处理生成完成事件。
	 * Handle post-spawn.
	 */
	@Override
	protected void handleSpawned() {
		super.handleSpawned();
		switch (getNpcId()) {
			case 215240:
			case 215241:
			    anuhartBravery();
		    break;
		} switch (getNpcId()) {
			case 235975:
			    bellowingRoar();
		    break;
		} switch (getNpcId()) {
			case 237111:
			case 237112:
			case 237113:
			case 237114:
			case 237246:
			case 237247:
			case 237250:
			//5.0
			case 220425:
			    elementalLordship();
		    break;
		}
		percentage = DataManager.AI_DATA.getAiTemplate().get(getNpcId()).getSummons().getPercentage();
	}
	
	private void anuhartBravery() {
	    GameEngineServices.skillEngine().getSkill(getOwner(), 18168, 1, getOwner()).useNoAnimationSkill(); //Anuhart's Bravery.
	}
	private void bellowingRoar() {
	    GameEngineServices.skillEngine().getSkill(getOwner(), 22659, 1, getOwner()).useNoAnimationSkill(); //Bellowing Roar.
	}
	private void elementalLordship() {
	    GameEngineServices.skillEngine().getSkill(getOwner(), 22744, 1, getOwner()).useNoAnimationSkill(); //Elemental Lordship.
	}
	
	/**
	 * 处理死亡事件。
	 * Handle death.
	 */
	@Override
	protected void handleDied() {
		super.handleDied();
		switch (getNpcId()) {
		    // 塔马特与主塔马特。 / Tarmat & Prime Tarmat.
			case 234610:
			    addGpPlayer();
				announceTarmatDie();
			break;
			case 219998:
			case 220001:
			case 236727:
			case 236728:
			case 236732:
				announceTarmatDie();
			break;
		}
		removeHelpersSpawn();
		spawnedNpc.clear();
		percentage.clear();
	}
	
	private void addGpPlayer() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (MathUtil.isIn3dRange(player, getOwner(), 15)) {
					AbyssPointsService.addGp(player, 500);
				}
			}
		});
	}
	private void announceTarmatDie() {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				// 恶魔部队的塔马特贝塔已被摧毁。 / The Devil Unit's Tarmat Beta has been destroyed.
				PacketSendUtility.sendPacket(player, SM_SYSTEM_MESSAGE.STR_MSG_WORLDRAID_MESSAGE_DIE_02);
			}
		});
	}
	
	private void removeHelpersSpawn() {
		for (Integer object : spawnedNpc) {
			VisibleObject npc = com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().findVisibleObject(object);
			if (npc != null && npc.isSpawned()) {
				npc.getController().onDelete();
			}
		}
	}
	
	protected void addHelpersSpawn(int objId) {	
		synchronized(spawnedNpc) {
			spawnedNpc.add(objId);
		}
	}
	
	private void checkPercentage(int hpPercentage) {
		for (Percentage percent : percentage) {
			if (spawnedPercent != 0 && spawnedPercent <= percent.getPercent()) {
				continue;
			} if (hpPercentage <= percent.getPercent()) {
				int skill = percent.getSkillId();
				if (skill != 0) {
					AI2Actions.useSkill(this, skill);
				} if (percent.isIndividual()) {
					handleIndividualSpawnedSummons(percent);
				} else if (percent.getSummons() != null) {
					handleBeforeSpawn(percent);
					for (SummonGroup summonGroup : percent.getSummons()) {
						final SummonGroup sg = summonGroup;
						GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
							@Override
							public void run() {
								spawnHelpers(sg);
							}
						}, summonGroup.getSchedule());
					}
				}
				spawnedPercent = percent.getPercent();
			}
		}
	}
	
	protected void spawnHelpers(SummonGroup summonGroup) {
		if (!isAlreadyDead() && checkBeforeSpawn()) {
			int count = 0;
			if (summonGroup.getCount() != 0) {
				count = summonGroup.getCount();
			} else {
				count = Rnd.get(summonGroup.getMinCount(), summonGroup.getMaxCount());
			} for (int i = 0; i < count; i++) {
				SpawnTemplate summon = null;
				if (summonGroup.getDistance() != 0) {
					summon = rndSpawnInRange(summonGroup.getNpcId(), summonGroup.getDistance());
				} else {
					summon = SpawnEngine.addNewSingleTimeSpawn(getPosition().getMapId(), summonGroup.getNpcId(), summonGroup.getX(), summonGroup.getY(), summonGroup.getZ(), summonGroup.getH());
				}
				VisibleObject npc = SpawnEngine.spawnObject(summon, getPosition().getInstanceId());
				addHelpersSpawn(npc.getObjectId());
			}
			handleSpawnFinished(summonGroup);
		}
	}
	
	protected SpawnTemplate rndSpawnInRange(int npcId, float distance) {
		float direction = Rnd.get(0, 199) / 100f;
		float x = (float) (Math.cos(Math.PI * direction) * distance);
		float y = (float) (Math.sin(Math.PI * direction) * distance);
		return SpawnEngine.addNewSingleTimeSpawn(getPosition().getMapId(), npcId, getPosition().getX() + x, getPosition().getY() + y, getPosition().getZ(), getPosition().getHeading());
	}
	
	protected boolean checkBeforeSpawn() {
		return true;
	}
	
	/**
	 * 处理生成前事件。
	 * Handle before-spawn.
	 *
	 * @param percent 血量百分比 / HP percent
	 */
	protected void handleBeforeSpawn(Percentage percent) {
	}
	
	/**
	 * 处理召唤物生成完成。
	 * Handle spawn-finished for summons.
	 *
	 * @param summonGroup 召唤组配置 / summon group config
	 */
	protected void handleSpawnFinished(SummonGroup summonGroup) {
	}
	
	/**
	 * 按血量百分比分批生成召唤物。
	 * Spawn individual summons by HP percent.
	 *
	 * @param percent 血量百分比 / HP percent
	 */
	protected void handleIndividualSpawnedSummons(Percentage percent) {
	}
}
