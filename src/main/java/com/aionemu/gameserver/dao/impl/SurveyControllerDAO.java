package com.aionemu.gameserver.dao.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.jdbc.core.simple.JdbcClient;

import com.aionemu.boot.i18n.I18n;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.templates.survey.SurveyItem;

import lombok.extern.slf4j.Slf4j;

/**
 * 问卷控制器 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of SurveyControllerDAO using JdbcClient.
 * Updated for MySQL 8 - Fixed connection leaks.
 * @author KID
 */
@Slf4j
@Repository
public class SurveyControllerDAO extends com.aionemu.gameserver.dao.SurveyControllerDAO {

	/** Mark survey as used SQL / Mark survey as used SQL */
	private static final String UPDATE_QUERY = "UPDATE `surveys` SET `used`=?, used_time=NOW() WHERE `unique_id`=?";
	/** 查询问卷 SQL / Select surveys SQL */
	private static final String SELECT_QUERY = "SELECT * FROM `surveys` WHERE `used`=?";

	private final JdbcClient jdbcClient;

	public SurveyControllerDAO() {
		this(DatabaseFactory.getJdbcClient());
	}

	@Autowired

	public SurveyControllerDAO(JdbcClient jdbcClient) {
		this.jdbcClient = Objects.requireNonNull(jdbcClient, "jdbcClient must not be null");
	}

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 * @param database 数据库名 / database name
	 * @param majorVersion 主版本 / major version
	 * @param minorVersion 次版本 / minor version
	 * @return 是否支持 / whether supported
	 */
	@Override
	public boolean supports(String database, int majorVersion, int minorVersion) {
		return DAOUtils.supports(database, majorVersion, minorVersion);
	}

	/**
	 * 加载全部未使用的新问卷项。
	 * Loads all unused new survey items.
	 * @return 问卷项列表 / survey item list
	 */
	@Override
	public List<SurveyItem> getAllNew() {
		List<SurveyItem> list = new ArrayList<>();
		try {
			jdbcClient.sql(SELECT_QUERY)
				.param(0)
				.query((rs, _) -> {
					SurveyItem item = new SurveyItem();
					item.uniqueId = rs.getInt("unique_id");
					item.ownerId = rs.getInt("owner_id");
					item.itemId = rs.getInt("item_id");
					item.count = rs.getLong("item_count");
					item.html = rs.getString("html_text");
					item.radio = rs.getString("html_radio");
					return item;
				})
				.list()
				.forEach(list::add);
		} catch (Exception e) {
			log.warn(I18n.get("log.05d62bb9dc4d"), e);
		}
		return list;
	}

	/**
	 * 将问卷标记为已使用。
	 * Marks a survey item as used.
	 * @param id 唯一 ID / unique id
	 * @return 是否更新成功 / whether the update succeeded
	 */
	@Override
	public boolean useItem(int id) {
		try {
			int affected = jdbcClient.sql(UPDATE_QUERY)
				.param(1)
				.param(id)
				.update();
			return affected > 0;
		} catch (Exception e) {
			log.error(I18n.get("log.4f720321ff23"), e);
			return false;
		}
	}
}
