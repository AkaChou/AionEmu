package com.aionemu.gameserver.ai.instance.drakenspireDepths;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

@AIName("immortal_orissan_quest")
// 237230

/**
 * Drakenspire Depths 副本 NPC AI：不灭之奥里萨（@AIName "immortal_orissan_quest"），继承 AggressiveNpcAI2。
 * Drakenspire Depths instance NPC AI: immortal Orissan (@AIName "immortal_orissan_quest"), extends AggressiveNpcAI2.
 *
 * <p>不灭之奥里萨（237230）在 80% 生命以下变成虚脱的奥里萨（237231），而任务 15300/25300 的击杀步骤只认 237231，
 * 所以这次变身必须保证发生。{@code handleAttack} 读到的是本次伤害结算前的 HP（见 CreatureController#onAttack 的调用顺序），
 * 一击或爆发致死会直接跳过阈值检查，因此死亡事件也必须补生成。
 * Immortal Orissan (237230) turns into Exhausted Orissan (237231) below 80% HP, and quests 15300/25300 only accept a
 * kill of 237231, so the transformation must always happen. {@code handleAttack} sees the HP before the incoming hit is
 * applied (see the call order in CreatureController#onAttack), so a lethal blow skips the threshold check - the death
 * event therefore has to spawn the replacement as well.</p>
 *
 * @author Falke_34
 */
public class immortalOrissanAI2 extends AggressiveNpcAI2 {

	/** 虚脱的奥里萨（任务击杀目标）/ Exhausted Orissan (quest kill target) */
	private static final int EXHAUSTED_ORISSAN_NPC_ID = 237231;

	private final AtomicBoolean exhaustedOrissanSpawned = new AtomicBoolean(false);

	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}

	/**
	 * 死亡兜底：一击/爆发致死时阈值检查不会触发，仍需生成任务击杀目标。
	 * Death fallback: a lethal blow skips the threshold check, so the quest kill target is spawned here.
	 */
	@Override
	protected void handleDied() {
		spawnExhaustedOrissanOnce();
		super.handleDied();
	}

	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 80 && spawnExhaustedOrissanOnce()) {
			AI2Actions.deleteOwner(this);
		}
	}

	private boolean spawnExhaustedOrissanOnce() {
		if (!exhaustedOrissanSpawned.compareAndSet(false, true)) {
			return false;
		}
		spawn(EXHAUSTED_ORISSAN_NPC_ID, getOwner().getX(), getOwner().getY(), getOwner().getZ(),
			getOwner().getHeading());
		return true;
	}
}
