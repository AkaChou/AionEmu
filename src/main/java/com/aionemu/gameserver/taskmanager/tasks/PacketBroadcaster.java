package com.aionemu.gameserver.taskmanager.tasks;

import com.aionemu.gameserver.model.gameobjects.Creature;
import com.aionemu.gameserver.model.gameobjects.player.Player;
import com.aionemu.gameserver.taskmanager.AbstractFIFOPeriodicTaskManager;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 数据包广播任务：按掩码批量发送生物状态/特效等更新包。
 * Packet broadcast task: sends creature stat/effect update packets batched by mask.
 *
 * @author lord_rex, MrPoke
 */
public final class PacketBroadcaster extends AbstractFIFOPeriodicTaskManager<Creature> {

	/**
	 * Spring 可选实例提供者。
	 * Optional Spring instance provider.
	 */
	private static volatile ObjectProvider<PacketBroadcaster> instanceProvider;

	/**
	 * 静态单例持有者。
	 * Static singleton holder.
	 */
	private static final class SingletonHolder {

		/**
		 * 默认单例实例。
		 * Default singleton instance.
		 */
		private static final PacketBroadcaster INSTANCE = new PacketBroadcaster();
	}

	/**
	 * 获取单例：优先 Spring 提供者，否则静态 holder。
	 * Get the singleton: prefer Spring provider, otherwise the static holder.
	 *
	 * @return 广播器实例 / Broadcaster instance
	 */
	public static PacketBroadcaster getInstance() {
		ObjectProvider<PacketBroadcaster> provider = instanceProvider;
		if (provider != null) {
			return provider.getIfAvailable(() -> SingletonHolder.INSTANCE);
		}
		return SingletonHolder.INSTANCE;
	}

	/**
	 * 注入 Spring 实例提供者。
	 * Inject the Spring instance provider.
	 *
	 * Provider
	 */
	public static void setInstanceProvider(ObjectProvider<PacketBroadcaster> provider) {
		instanceProvider = provider;
	}

	/**
	 * 以 200ms 周期构造数据包广播器。
	 * Construct the packet broadcaster with a 200ms period.
	 */
	public PacketBroadcaster() {
		super(200);
	}

	/**
	 * 广播模式：每位掩码对应一种待发送更新。
	 * Broadcast mode: each mask bit corresponds to one pending update.
	 */
	public static enum BroadcastMode {

		/**
		 * 更新基础属性包。
		 * Update base stats packet.
		 */
		UPDATE_STATS {

			@Override
			public void sendPacket(Creature creature) {
				creature.getGameStats().updateStatInfo();
			}
		},

		/**
		 * 更新移动速度包。
		 * Update movement speed packet.
		 */
		UPDATE_SPEED {

			@Override
			public void sendPacket(Creature creature) {
				creature.getGameStats().updateSpeedInfo();
			}
		},

		/**
		 * 更新玩家 HP 包。
		 * Update player HP packet.
		 */
		UPDATE_PLAYER_HP_STAT {

			@Override
			public void sendPacket(Creature creature) {
				((Player) creature).getLifeStats().sendHpPacketUpdateImpl();
			}
		},

		/**
		 * 更新玩家 MP 包。
		 * Update player MP packet.
		 */
		UPDATE_PLAYER_MP_STAT {

			@Override
			public void sendPacket(Creature creature) {
				((Player) creature).getLifeStats().sendMpPacketUpdateImpl();
			}
		},

		/**
		 * 更新玩家效果图标。
		 * Update player effect icons.
		 */
		UPDATE_PLAYER_EFFECT_ICONS {

			@Override
			public void sendPacket(Creature creature) {
				creature.getEffectController().updatePlayerEffectIconsImpl();
			}
		},

		/**
		 * 更新玩家飞行时间包。
		 * Update player fly-time packet.
		 */
		UPDATE_PLAYER_FLY_TIME {

			@Override
			public void sendPacket(Creature creature) {
				((Player) creature).getLifeStats().sendFpPacketUpdateImpl();
			}
		},

		/**
		 * 广播效果到可见范围。
		 * Broadcast effects to the visible range.
		 */
		BROAD_CAST_EFFECTS {

			@Override
			public void sendPacket(Creature creature) {
				creature.getEffectController().broadCastEffectsImp();
			}
		};

		/**
		 * 该模式对应的位掩码。
		 * Bit mask for this mode.
		 */
		private final byte MASK;

		/**
		 * 按枚举序数生成位掩码。
		 * Build the bit mask from the enum ordinal.
		 */
		private BroadcastMode() {
			MASK = (byte) (1 << ordinal());
		}

		/**
		 * 返回该模式的掩码位。
		 * Return this mode's mask bit.
		 *
		 * Mask
		 */
		public byte mask() {
			return MASK;
		}

		/**
		 * 实际发送对应更新包。
		 * Actually send the corresponding update packet.
		 *
		 * Target creature
		 */
		protected abstract void sendPacket(Creature creature);

		/**
		 * 若掩码包含本模式则发送并清除该位。
		 * If the mask includes this mode, send and clear the bit.
		 *
		 * Target creature
		 * @param mask     当前广播掩码 / Current broadcast mask
		 */
		protected final void trySendPacket(final Creature creature, byte mask) {
			if ((mask & mask()) == mask()) {
				sendPacket(creature);
				creature.removePacketBroadcastMask(this);
			}
		}
	}

	/**
	 * 全部广播模式缓存。
	 * Cached array of all broadcast modes.
	 */
	private static final BroadcastMode[] VALUES = BroadcastMode.values();

	/**
	 * 按生物当前掩码逐模式发送更新包直至掩码清零。
	 * Send updates for each mode bit on the creature until the mask is cleared.
	 *
	 * Target creature
	 */
	@Override
	protected void callTask(Creature creature) {
		for (byte mask; (mask = creature.getPacketBroadcastMask()) != 0;) {
			for (BroadcastMode mode : VALUES) {
				mode.trySendPacket(creature, mask);
			}
		}
	}

	/**
	 * 耗时统计方法名。
	 * Method name for runtime stats.
	 *
	 * Method name
	 */
	@Override
	protected String getCalledMethodName() {
		return "packetBroadcast()";
	}
}
