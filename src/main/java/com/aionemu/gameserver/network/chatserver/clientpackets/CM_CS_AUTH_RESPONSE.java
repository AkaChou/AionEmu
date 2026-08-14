package com.aionemu.gameserver.network.chatserver.clientpackets;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.gameserver.lifecycle.GameThreadPoolServices;

import lombok.extern.slf4j.Slf4j;

import com.aionemu.gameserver.network.GameServerAuthFailure;
import com.aionemu.gameserver.network.chatserver.ChatServerConnection.State;
import com.aionemu.gameserver.network.chatserver.CsClientPacket;
import com.aionemu.gameserver.network.chatserver.serverpackets.SM_CS_AUTH;
import com.aionemu.gameserver.services.ChatService;

/**
 * 聊天服对游戏服认证请求的应答包。
 * Chat-server response to a game-server authentication request.
 */
@Slf4j
public class CM_CS_AUTH_RESPONSE extends CsClientPacket {
	/**
	 * 认证结果码：0 成功，1 失败，2 已注册。
	 * Auth result code: 0 success, 1 failure, 2 already registered.
	 */
	private int response;

	/**
	 * 聊天服对外 IP（4 字节）。
	 * Chat server public IP (4 bytes).
	 */
	private byte[] ip;

	/**
	 * 聊天服对外端口。
	 * Chat server public port.
	 */
	private int port;

	/**
	 * 使用指定操作码构造应答包。
	 * Constructs the response packet with the given opcode.
	 *
	 * @param opcode 数据包操作码 / packet opcode
	 */
	public CM_CS_AUTH_RESPONSE(int opcode) {
		super(opcode);
	}

	/**
	 * 读取认证结果、IP 与端口。
	 * Reads auth result, IP, and port.
	 */
	@Override
	protected void readImpl() {
		response = readC();
		ip = readB(4);
		port = readH();
	}

	/**
	 * 按结果码更新连接状态、登记聊天服地址或重试认证。
	 * Updates connection state, registers chat address, or retries auth based on result code.
	 */
	@Override
	protected void runImpl() {
		switch (response) {
		case 0: // Authed
			log.info(I18n.get("log.7e2105e38d8a", ip[0] & 0xFF, ip[1] & 0xFF, ip[2] & 0xFF,
					ip[3] & 0xFF, port));
			getConnection().setState(State.AUTHED);
			ChatService.setIp(ip);
			ChatService.setPort(port);
			break;
		case 1: // Not Authed
			log.error(I18n.get("log.26672f710434"));
			GameServerAuthFailure.notAuthenticated("ChatServer");
			break;
		case 2: // Already Registered
			log.info(I18n.get("log.2812bb8416bb"));
			GameThreadPoolServices.threadPoolManager().schedule(new Runnable() {
				@Override
				public void run() {
					CM_CS_AUTH_RESPONSE.this.getConnection().sendPacket(new SM_CS_AUTH());
				}
			}, 10000);
			break;
		}
	}
}
