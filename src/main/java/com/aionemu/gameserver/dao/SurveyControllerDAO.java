package com.aionemu.gameserver.dao;

import com.aionemu.commons.database.dao.DAO;
import com.aionemu.gameserver.model.templates.survey.SurveyItem;

import java.util.List;

/**
 * 问卷/调查控制器数据访问抽象层。
 * DAO for survey controller item persistence.
 *
 * @author KID
 */
public abstract class SurveyControllerDAO implements DAO {

	/**
	 * 返回实现唯一类名标识。
	 * Returns unique class name for all implementations.
	 *
	 * fully qualified class name
	 */
	@Override
	public final String getClassName() {
		return SurveyControllerDAO.class.getName();
	}

	/**
	 * 标记调查物品为已使用。
	 * Marks a survey item as used.
	 *
	 * @param id 调查物品 ID / survey item id
	 * @return 是否使用成功 / true if used
	 */
	public abstract boolean useItem(int id);

	/**
	 * 查询全部新增调查物品。
	 * Returns all new survey items.
	 *
	 * @return 调查物品列表 / survey item list
	 */
	public abstract List<SurveyItem> getAllNew();
}
