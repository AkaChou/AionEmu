package com.aionemu.gameserver.network.aion;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import java.io.IOException;

import com.aionemu.commons.network.AConnection;
import com.aionemu.commons.network.ConnectionTransport;
import com.aionemu.commons.network.NettyConnectionFactory;
import com.aionemu.commons.services.ServiceContext;
import com.aionemu.gameserver.configs.network.NetworkConfig;
import com.aionemu.gameserver.network.sequrity.FloodManager;
import com.aionemu.gameserver.network.sequrity.FloodManager.Result;

/**
 * 创建 AionConnection 的 NettyConnectionFactory 实现，可选连接洪泛检测。
 * NettyConnectionFactory implementation that creates AionConnections with optional connection flood checks.
 *
 * @author -Nemesiss-
 */
@Slf4j
public class GameConnectionFactoryImpl implements NettyConnectionFactory {

	private static final String GAME_CONTEXT = "game";

	/** 连接洪泛管理器（可选） / optional connection flood manager */
	private FloodManager floodAcceptor;

	/**
	 * 按配置初始化短/长周期连接洪泛过滤器。
	 * Initializes short/long period connection flood filters when enabled.
	 */
	public GameConnectionFactoryImpl() {
		if (NetworkConfig.ENABLE_FLOOD_CONNECTIONS) {
			floodAcceptor = new FloodManager(NetworkConfig.Flood_Tick,
					new FloodManager.FloodFilter(NetworkConfig.Flood_SWARN, NetworkConfig.Flood_SReject,
							NetworkConfig.Flood_STick), // short period
					new FloodManager.FloodFilter(NetworkConfig.Flood_LWARN, NetworkConfig.Flood_LReject,
							NetworkConfig.Flood_LTick)); // long period
		}
	}

	/**
	 * 为传入传输创建 AionConnection；洪泛拒绝时关闭传输并返回 null。
	 * Creates an AionConnection for the inbound transport; closes and returns null on flood reject.
	 *
	 * @param transport 连接传输 / connection transport
	 * @return 新连接或 null / new connection or null
	 * @throws IOException 创建失败 / if creation fails
	 */
	@Override
	public AConnection create(ConnectionTransport transport) throws IOException {
		if (NetworkConfig.ENABLE_FLOOD_CONNECTIONS) {
			final Result isFlooding = floodAcceptor.isFlooding(transport.getIP(), true);
			switch (isFlooding) {
			case REJECTED: {
				log.warn(I18n.get("log.33eb4763c5c6", transport.getIP()));
				transport.close(true);
				return null;
			}
			case WARNED: {
				log.warn(I18n.get("log.7dbdf21e7192", transport.getIP()));
				break;
			}
			}
		}
		try (ServiceContext.Scope ignored = ServiceContext.use(GAME_CONTEXT)) {
			return new AionConnection(transport);
		}
	}
}
