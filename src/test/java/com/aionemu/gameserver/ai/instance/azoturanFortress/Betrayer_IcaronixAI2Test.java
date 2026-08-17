package com.aionemu.gameserver.ai.instance.azoturanFortress;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.controllers.attack.AggroList;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

/**
 * 锁定伊卡罗尼斯最终形态的幂等生成合同。
 * Locks the idempotent spawn contract for Icaronix's final form.
 */
class Betrayer_IcaronixAI2Test {

	@Test
	void spawnsFinalFormOnceWhenThresholdAndDeathFallbackBothRequestIt() throws Exception {
		RecordingAI ai = new RecordingAI();

		invokeCheckPercentage(ai, 75);
		assertEquals(List.of(214599), ai.spawnedNpcIds, "the threshold request must spawn the final form");
		ai.handleDied();

		assertEquals(List.of(214599), ai.spawnedNpcIds);
	}

	@Test
	void spawnsFinalFormWhenEntryFormDiesBeforeThreshold() {
		RecordingAI ai = new RecordingAI();

		ai.handleDied();

		assertEquals(List.of(214599), ai.spawnedNpcIds);
	}

	private static void invokeCheckPercentage(Betrayer_IcaronixAI2 ai, int hpPercentage) throws Exception {
		Method method = Betrayer_IcaronixAI2.class.getDeclaredMethod("checkPercentage", int.class);
		method.setAccessible(true);
		method.invoke(ai, hpPercentage);
	}

	private static final class RecordingAI extends Betrayer_IcaronixAI2 {
		private final List<Integer> spawnedNpcIds = new ArrayList<>();
		private final Npc owner = owner();

		@Override
		public Npc getOwner() {
			return owner;
		}

		@Override
		public boolean isMayShout() {
			return false;
		}

		@Override
		protected VisibleObject spawn(int npcId, float x, float y, float z, byte heading) {
			spawnedNpcIds.add(npcId);
			return null;
		}
	}

	private static Npc owner() {
		try {
			RecordingNpc owner = new ObjenesisStd().newInstance(RecordingNpc.class);
			Field aggroList = Creature.class.getDeclaredField("aggroList");
			aggroList.setAccessible(true);
			aggroList.set(owner, new AggroList(owner));
			return owner;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		}
	}

	private static final class RecordingNpc extends Npc {
		private RecordingNpc() {
			super(0, null, null, null);
		}

		@Override
		public NpcController getController() {
			return new RecordingNpcController();
		}

		@Override
		public void setTarget(VisibleObject target) {
		}

		@Override
		public float getX() {
			return 10;
		}

		@Override
		public float getY() {
			return 20;
		}

		@Override
		public float getZ() {
			return 30;
		}

		@Override
		public byte getHeading() {
			return 4;
		}
	}

	private static final class RecordingNpcController extends NpcController {
		@Override
		public void onDelete() {
		}
	}
}
