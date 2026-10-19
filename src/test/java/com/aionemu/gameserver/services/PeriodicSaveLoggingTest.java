package com.aionemu.gameserver.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.qos.logback.classic.Level;
import com.aionemu.gameserver.lifecycle.WorldBootstrapServiceBindings;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.model.team.legion.Legion;
import com.aionemu.gameserver.world.World;
import com.aionemu.testutil.LogCapture;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objenesis.ObjenesisStd;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * 验证周期空保存只输出 DEBUG，有处理对象时保留 INFO 摘要。
 * Verifies empty periodic saves log at DEBUG while saves processing players retain INFO summaries.
 */
class PeriodicSaveLoggingTest {

	@Test
	void emptyWorldAndLegionsProduceDebugOnly() throws Exception {
		assertSaveLevels(List.of(), 0);
	}

	@Test
	void playersProduceCompletionInfo() throws Exception {
		assertSaveLevels(List.of(new ObjenesisStd().newInstance(OnlinePlayer.class)), 2);
	}

	private void assertSaveLevels(List<Player> players, int infos) throws Exception {
		ObjenesisStd objenesis = new ObjenesisStd();
		EmptyWorld world = objenesis.newInstance(EmptyWorld.class);
		world.players = players;
		DefaultListableBeanFactory beans = new DefaultListableBeanFactory();
		beans.registerSingleton("world", world);
		WorldBootstrapServiceBindings bindings = new WorldBootstrapServiceBindings(null, null, null, null,
			beans.getBeanProvider(World.class));
		try (LogCapture logs = new LogCapture(PeriodicSaveService.class)) {
			NoopSaveService service = objenesis.newInstance(NoopSaveService.class);
			var field = PeriodicSaveService.class.getDeclaredField("legionService");
			field.setAccessible(true);
			field.set(service, objenesis.newInstance(EmptyLegions.class));
			for (String type : new String[] { "LegionWhUpdateTask", "PlayerGeneralUpdateTask", "PlayerItemsUpdateTask" }) {
				var constructor = Class.forName(PeriodicSaveService.class.getName() + "$" + type)
					.getDeclaredConstructor(PeriodicSaveService.class);
				constructor.setAccessible(true);
				((Runnable) constructor.newInstance(service)).run();
			}
			assertEquals(infos, logs.events().stream().filter(event -> event.getLevel() == Level.INFO).count());
			assertEquals(4, logs.events().size());
			assertTrue(logs.events().stream().allMatch(event -> event.getLevel() == Level.DEBUG
				|| event.getLevel() == Level.INFO));
		} finally {
			bindings.destroy();
		}
	}

	/** 无玩家或仅有测试玩家的世界。 / World containing only the supplied test players. */
	private static final class EmptyWorld extends World {
		private List<Player> players;

		@Override
		public Iterator<Player> getPlayersIterator() {
			return players.iterator();
		}
	}

	/** 空军团缓存，构造由 Objenesis 跳过。 / Empty legion cache, with construction bypassed by Objenesis. */
	private static final class EmptyLegions extends LegionService {
		private EmptyLegions() {
			super(null, null, null, null, null, null);
		}

		@Override
		public Iterator<Legion> getCachedLegionIterator() {
			return List.<Legion>of().iterator();
		}
	}

	/** 不写数据库的保存替身。 / Save stub that does not access a database. */
	private static final class NoopSaveService extends PeriodicSaveService {
		private NoopSaveService() {
			super(null, null, null, null, null, null, null, null);
		}

		@Override
		public void savePlayerGeneral(Player player) {
		}

		@Override
		public void savePlayerItems(Player player) {
		}
	}

	/** 在线玩家替身。 / Online-player stub. */
	private static final class OnlinePlayer extends Player {
		private OnlinePlayer() {
			super(null, null, null, null);
		}

		@Override
		public boolean isOnline() {
			return true;
		}
	}
}
