package com.aionemu.gameserver.ai.instance.azoturanFortress;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;
import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.ai2.event.AIEventType;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Azoturan Fortress 副本 NPC AI：Lehpar Icaronix 的阈值变身（@AIName "betrayer_icaronix"），继承 AggressiveNpcAI2。
 * 原版归属：本 AI 只服务 214598（IDLF3CL_LehparIcaronixQ_45_Ah），其原版 AI pattern ND2_AhC_1
 * 在 75%（battle timer）或死亡时生成最终形态 214599（IDLF3CL_TestResultIcaronixQ_45_Ah）并自删；
 * 城堡 Boss 233877 的原版 pattern D2_FnA 为空，不得挂本 AI（否则 75% 自删不产生击杀事件）。
 * Azoturan Fortress instance NPC AI: the Lehpar Icaronix threshold transform (@AIName "betrayer_icaronix").
 * Retail ownership: this AI serves only 214598, whose retail pattern ND2_AhC_1 spawns the final form
 * 214599 at 75% HP (battle timer) or on death and despawns itself; the castle boss 233877 runs the
 * empty D2_FnA pattern and must not use this AI (its 75% self-despawn emits no kill event).
 * @author Encom
 */
@AIName("betrayer_icaronix")
public class Betrayer_IcaronixAI2 extends AggressiveNpcAI2
{
	private static final int FINAL_FORM_NPC_ID = 214599;
	private final AtomicBoolean finalFormSpawned = new AtomicBoolean();

	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkForSupport(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}

	@Override
	protected void handleDied() {
		spawnFinalFormOnce();
		super.handleDied();
	}

	private void checkForSupport(Creature creature) {
		for (VisibleObject object: getKnownList().getKnownObjectsSnapshot()) {
			if (object instanceof Npc && isInRange(object, 40)) {
				((Npc) object).getAi2().onCreatureEvent(AIEventType.CREATURE_AGGRO, creature);
			}
		}
	}

	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 75 && spawnFinalFormOnce()) {
			AI2Actions.deleteOwner(this);
		}
	}

	private boolean spawnFinalFormOnce() {
		if (!finalFormSpawned.compareAndSet(false, true)) {
			return false;
		}
		spawn(FINAL_FORM_NPC_ID, getOwner().getX(), getOwner().getY(), getOwner().getZ(),
			getOwner().getHeading());
		return true;
	}
}
