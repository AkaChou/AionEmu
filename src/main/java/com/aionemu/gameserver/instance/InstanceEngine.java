package com.aionemu.gameserver.instance;

import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.springframework.beans.factory.ObjectProvider;

import com.aionemu.commons.scripting.classlistener.AggregatedClassListener;
import com.aionemu.commons.scripting.classlistener.OnClassLoadUnloadListener;
import com.aionemu.commons.scripting.classlistener.ScheduledTaskClassListener;
import com.aionemu.commons.scripting.CompiledScriptLoader;
import com.aionemu.gameserver.GameServerError;
import com.aionemu.gameserver.instance.handlers.GeneralInstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceHandler;
import com.aionemu.gameserver.instance.handlers.InstanceID;
import com.aionemu.gameserver.model.GameEngine;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.world.World;
import com.aionemu.gameserver.world.WorldMapInstance;
import com.aionemu.gameserver.world.knownlist.Visitor;

/**
 * 副本引擎：加载脚本化副本处理器，并按地图 ID 创建对应处理器实例。
 * Instance engine: loads scripted instance handlers and creates handler instances by map id.
 */
@Slf4j
public class InstanceEngine implements GameEngine {

	/** Spring 实例提供者 / Spring instance provider */
	private static volatile ObjectProvider<InstanceEngine> instanceProvider;

	/**
	 * 无专用脚本时使用的默认（空操作）处理器。
	 * Default (no-op) handler used when no script is registered.
	 */
	public static final InstanceHandler DUMMY_INSTANCE_HANDLER = new GeneralInstanceHandler();

	/**
	 * 地图 ID → 处理器类 的注册表。
	 * Registry of map id → handler class.
	 */
	private Map<Integer, Class<? extends InstanceHandler>> handlers = new HashMap<Integer, Class<? extends InstanceHandler>>();

	/**
	 * 加载副本脚本处理器。
	 * Load scripted instance handlers.
	 *
	 * @param progressLatch 进度闩；完成后倒数 / progress latch; counted down when finished
	 */
	@Override
	public void load(CountDownLatch progressLatch) {
		log.info(I18n.get("log.05ae1f3c90a6"));
		AggregatedClassListener acl = new AggregatedClassListener();
		acl.addClassListener(new OnClassLoadUnloadListener());
		acl.addClassListener(new ScheduledTaskClassListener());
		acl.addClassListener(new InstanceHandlerClassListener());
		try {
			acl.postLoad(CompiledScriptLoader.load("com.aionemu.gameserver.instance.handlers.scripts"));
			log.info(I18n.get("log.5dc4258161be", handlers.size()));
		} catch (Exception e) {
			throw new GameServerError("Can't initialize instance handlers.", e);
		} finally {
			if (progressLatch != null) {
				progressLatch.countDown();
			}
		}
	}

	/**
	 * 关闭引擎并清空处理器注册表。
	 * Shut down the engine and clear the handler registry.
	 */
	@Override
	public void shutdown() {
		log.info(I18n.get("log.4bd55e25e398"));
		handlers.clear();
		log.info(I18n.get("log.a55399a5f217"));
	}

	/**
	 * 按世界地图 ID 创建新的副本处理器；未注册时返回默认处理器。
	 * Create a new instance handler for the given world-map id; returns the dummy handler if none is registered.
	 *
	 * world-map id
	 *
	 * @param worldId
	 * @return 处理器实例 / handler instance
	 */
	public InstanceHandler getNewInstanceHandler(int worldId) {
		Class<? extends InstanceHandler> instanceClass = handlers.get(worldId);
		InstanceHandler instanceHandler = null;
		if (instanceClass != null) {
			try {
				instanceHandler = instanceClass.getDeclaredConstructor().newInstance();
			} catch (ReflectiveOperationException ex) {
				log.warn(I18n.get("log.e64071d6fa05", worldId, ex), ex);
			}
		}
		if (instanceHandler == null) {
			instanceHandler = DUMMY_INSTANCE_HANDLER;
		}
		return instanceHandler;
	}

	/**
	 * 将带 {@link InstanceID} 注解的处理器类注册到注册表。
	 * Register a handler class annotated with {@link InstanceID} into the registry.
	 *
	 * handler class
	 */
	final void addInstanceHandlerClass(Class<? extends InstanceHandler> handler) {
		InstanceID idAnnotation = handler.getAnnotation(InstanceID.class);
		if (idAnnotation != null) {
			for (int worldId : idAnnotation.value()) {
				handlers.put(worldId, handler);
			}
		}
	}

	/**
	 * 通知副本已创建，并调用其处理器的 {@code onInstanceCreate}。
	 * Notify that an instance has been created and invoke its handler's {@code onInstanceCreate}.
	 *
	 * @param instance 新建的世界地图实例 / newly created world-map instance
	 */
	public void onInstanceCreate(final WorldMapInstance instance) {
		com.aionemu.gameserver.lifecycle.GameWorldBootstrapServices.world().doOnAllPlayers(new Visitor<Player>() {
			@Override
			public void visit(Player player) {
				if (player.isOnline()) {
					// PacketSendUtility.sendPacket(player, new SM_SYSTEM_MESSAGE(1400360,
					// clt.getMaxMemberDark(), instance.getName()));
				}
			}
		});
		instance.getInstanceHandler().onInstanceCreate(instance);
	}

	/**
	 * 返回副本引擎单例。
	 * Return the instance-engine singleton.
	 *
	 * engine instance
	 */
	public static final InstanceEngine getInstance() {
		ObjectProvider<InstanceEngine> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.instance);
		}
		return SingletonHolder.instance;
	}

	/**
	 * 设置 Spring 实例提供者。
	 * Set the Spring instance provider.
	 *
	 * Spring provider
	 */
	public static void setInstanceProvider(ObjectProvider<InstanceEngine> provider) {
		instanceProvider = provider;
	}

	/**
	 * 静态单例持有者。
	 * Static singleton holder.
	 */
	@SuppressWarnings("synthetic-access")
	private static class SingletonHolder {
		/** 默认引擎实例 / default engine instance */
		protected static final InstanceEngine instance = new InstanceEngine();
	}
}
