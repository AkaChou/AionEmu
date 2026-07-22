package com.aionemu.gameserver.dao.impl;


import com.aionemu.boot.i18n.I18n;
import lombok.extern.slf4j.Slf4j;
import com.aionemu.commons.database.DatabaseFactory;
import com.aionemu.gameserver.model.templates.survey.SurveyItem;
import java.util.ArrayList;
import java.util.List;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 问卷控制器 DAO 的 MySQL 8 实现。
 * MySQL 8 implementation of SurveyControllerDAO.
 *
 * Updated for MySQL 8 - Fixed connection leaks.
 *
 * @author KID
 */
@Slf4j
public class SurveyControllerDAO extends com.aionemu.gameserver.dao.SurveyControllerDAO {


	/** Mark survey as used SQL / Mark survey as used SQL */
	private static final String UPDATE_QUERY = "UPDATE `surveys` SET `used`=?, used_time=NOW() WHERE `unique_id`=?";
	/** 查询问卷 SQL / Select surveys SQL */
	private static final String SELECT_QUERY = "SELECT * FROM `surveys` WHERE `used`=?";

	/**
	 * 是否支持当前数据库。
	 * Whether the current database is supported.
	 *
	 * @param arg0 数据库名 / database name
	 * major version
	 * minor version
	 * whether supported
	 */
	@Override
	public boolean supports(String arg0, int arg1, int arg2) {
		return DAOUtils.supports(arg0, arg1, arg2);
	}

	/**
	 * 加载全部未使用的新问卷项。
	 * Loads all unused new survey items.
	 *
	 * @return 问卷项列表 / survey item list
	 */
	@Override
	public List<SurveyItem> getAllNew() {
		List<SurveyItem> list = new ArrayList<>();

		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(SELECT_QUERY)) {

			stmt.setInt(1, 0);

			try (ResultSet rset = stmt.executeQuery()) {
				while (rset.next()) {
					SurveyItem item = new SurveyItem();
					item.uniqueId = rset.getInt("unique_id");
					item.ownerId = rset.getInt("owner_id");
					item.itemId = rset.getInt("item_id");
					item.count = rset.getLong("item_count");
					item.html = rset.getString("html_text");
					item.radio = rset.getString("html_radio");
					list.add(item);
				}
			}
		} catch (Exception e) {
			log.warn(I18n.get("log.05d62bb9dc4d", e), e);
		}
		return list;
	}

	/**
	 * 将问卷标记为已使用。
	 * Marks a survey item as used.
	 *
	 * @param id 唯一 ID / unique id
	 * @return 是否更新成功 / whether the update succeeded
	 */
	@Override
	public boolean useItem(int id) {
		try (Connection con = DatabaseFactory.getConnection();
			 PreparedStatement stmt = con.prepareStatement(UPDATE_QUERY)) {

			stmt.setInt(1, 1);
			stmt.setInt(2, id);
			return stmt.executeUpdate() > 0;
		} catch (Exception e) {
			log.error(I18n.get("log.4f720321ff23", e), e);
			return false;
		}
	}
}
