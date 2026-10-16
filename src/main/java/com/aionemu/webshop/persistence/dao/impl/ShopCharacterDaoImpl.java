package com.aionemu.webshop.persistence.dao.impl;

import java.util.List;

import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.webshop.model.ShopCharacter;
import com.aionemu.webshop.persistence.dao.ShopCharacterDao;
import com.aionemu.webshop.persistence.mapper.ShopCharacterMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * 角色只读数据访问实现。
 * Read-only character data access implementation.
 */
@Slf4j
@Service
@Lazy
public class ShopCharacterDaoImpl implements ShopCharacterDao {

	private final ShopCharacterMapper mapper;

	public ShopCharacterDaoImpl(ShopCharacterMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public List<ShopCharacter> listByAccount(String accountName) {
		try {
			return mapper.findByAccountName(accountName);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.a72b4d9e6c08", accountName), e);
			return List.of();
		}
	}

	@Override
	public boolean isOwnedBy(String accountName, int playerId) {
		try {
			return mapper.findOwnedPlayerId(accountName, playerId) != null;
		} catch (DataAccessException e) {
			log.error(I18n.get("log.b83c5e0f7d19", playerId), e);
			return false;
		}
	}

	@Override
	public String findName(String accountName, int playerId) {
		try {
			return mapper.findName(accountName, playerId);
		} catch (DataAccessException e) {
			log.error(I18n.get("log.c94d6f107e2a", playerId), e);
			return null;
		}
	}
}
