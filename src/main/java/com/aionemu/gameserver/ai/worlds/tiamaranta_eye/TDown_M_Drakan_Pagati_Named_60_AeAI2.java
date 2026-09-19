package com.aionemu.gameserver.ai.worlds.tiamaranta_eye;

import java.util.concurrent.atomic.AtomicBoolean;

import com.aionemu.gameserver.ai.AggressiveNpcAI2;

import com.aionemu.gameserver.ai2.AI2Actions;
import com.aionemu.gameserver.ai2.AIName;
import com.aionemu.gameserver.model.gameobjects.Creature;

/**
 * Tiamaranta eye 区域 NPC AI：T Down M Drakan Pagati Named 60 Ae（@AIName "TDown_M_Drakan_Pagati_Named_60_Ae"），继承 AggressiveNpcAI2。
 * Tiamaranta eye zone NPC AI: T Down M Drakan Pagati Named 60 Ae (@AIName "TDown_M_Drakan_Pagati_Named_60_Ae"), extends AggressiveNpcAI2.
 *
 * @author Encom
 */
@AIName("TDown_M_Drakan_Pagati_Named_60_Ae")
public class TDown_M_Drakan_Pagati_Named_60_AeAI2 extends AggressiveNpcAI2
{
	/** 变身后的形态 / transformed form */
	private static final int TRANSFORMED_NPC_ID = 249099;

	private final AtomicBoolean transformed = new AtomicBoolean(false);

	@Override
	protected void handleAttack(Creature creature) {
		super.handleAttack(creature);
		checkPercentage(getLifeStats().getHpPercentage());
	}

	/**
	 * 死亡兜底：{@code handleAttack} 读取的是本次伤害结算前的 HP，一击/爆发致死会跳过阈值变身。
	 * Death fallback: {@code handleAttack} sees the HP before the hit, so a lethal blow skips the transform.
	 */
	@Override
	protected void handleDied() {
		spawnTransformedOnce();
		super.handleDied();
	}

	private void checkPercentage(int hpPercentage) {
		if (hpPercentage <= 50 && spawnTransformedOnce()) {
			AI2Actions.deleteOwner(this);
		}
	}

	private boolean spawnTransformedOnce() {
		if (!transformed.compareAndSet(false, true)) {
			return false;
		}
		spawn(TRANSFORMED_NPC_ID, getOwner().getX(), getOwner().getY(), getOwner().getZ(),
			getOwner().getHeading());
		return true;
	}
}
