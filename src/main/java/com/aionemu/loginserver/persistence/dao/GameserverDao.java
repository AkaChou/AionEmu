package com.aionemu.loginserver.persistence.dao;

import java.util.Map;

import com.aionemu.loginserver.GameServerInfo;
import com.aionemu.loginserver.persistence.entity.GameserverPo;
import com.mybatisflex.core.service.IService;

/**
 * 游戏服注册信息（ls_gameservers）数据访问层。
 */
public interface GameserverDao extends IService<GameserverPo> {

	Map<Byte, GameServerInfo> getAllGameServers();
}
