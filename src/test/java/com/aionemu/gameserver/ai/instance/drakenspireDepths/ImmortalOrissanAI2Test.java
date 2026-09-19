package com.aionemu.gameserver.ai.instance.drakenspireDepths;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;

import com.aionemu.gameserver.controllers.NpcController;
import com.aionemu.gameserver.controllers.attack.AggroList;
import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.Npc;
import com.aionemu.gameserver.model.gameobjects.VisibleObject;

/**
 * 锁定不灭之奥里萨（237230）向虚脱的奥里萨（237231）变身的阈值/死亡兜底合同。
 * Locks the threshold plus death-fallback contract of the Immortal Orissan (237230) transformation.
 *
 * <p>任务 15300/25300 的击杀步骤只认 237231：阈值变身一旦被一击/爆发致死跳过，任务就永久停在
 * “消灭盘龙巢穴的奥里萨”。因此死亡事件必须补生成，且阈值与死亡两条路径只能生成一次。</p>
 */
class ImmortalOrissanAI2Test {

	@Test
	void spawnsExhaustedOrissanOnceWhenThresholdAndDeathFallbackBothRequestIt() throws Exception {
		RecordingAI ai = new RecordingAI();

		invokeCheckPercentage(ai, 80);
		assertEquals(List.of(237231), ai.spawnedNpcIds, "the 80% threshold must spawn the quest kill target");
		ai.handleDied();

		assertEquals(List.of(237231), ai.spawnedNpcIds, "the death fallback must not duplicate the quest kill target");
	}

	@Test
	void spawnsExhaustedOrissanWhenImmortalOrissanDiesBeforeThreshold() {
		RecordingAI ai = new RecordingAI();

		ai.handleDied();

		assertEquals(List.of(237231), ai.spawnedNpcIds, "a lethal blow must still create the quest kill target");
	}

	private static void invokeCheckPercentage(immortalOrissanAI2 ai, int hpPercentage) throws Exception {
		Method method = immortalOrissanAI2.class.getDeclaredMethod("checkPercentage", int.class);
		method.setAccessible(true);
		method.invoke(ai, hpPercentage);
	}

	private static final class RecordingAI extends immortalOrissanAI2 {
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
