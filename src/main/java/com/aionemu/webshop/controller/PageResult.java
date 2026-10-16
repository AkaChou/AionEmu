package com.aionemu.webshop.controller;

import java.util.List;

/**
 * 分页响应体：列表接口统一返回该结构，前端据此渲染分页条。
 * Paged response body: every list endpoint returns this shape so the front end can render one pager.
 *
 * @param items    当前页条目 / items on the current page
 * @param total    命中总数 / total number of matches
 * @param page     当前页码，从 0 开始 / current zero-based page index
 * @param pageSize 每页条数 / page size
 * @param <T>      条目类型 / item type
 */
public record PageResult<T>(List<T> items, long total, int page, int pageSize) {

	/**
	 * 归一页码与每页条数，避免越界或过大的取值打到数据库。
	 * Normalises the page index and page size so out-of-range or oversized values never reach the database.
	 *
	 * @param page     请求页码 / requested page index
	 * @param pageSize 请求每页条数 / requested page size
	 * @return 页码 / normalised page index
	 */
	public static int normalisePage(int page) {
		return Math.max(0, page);
	}

	/**
	 * 归一每页条数：限制在 1..200，默认 24。
	 * Normalises the page size into 1..200, defaulting to 24.
	 *
	 * @param pageSize 请求每页条数 / requested page size
	 * @return 归一后的每页条数 / normalised page size
	 */
	public static int normalisePageSize(int pageSize) {
		if (pageSize <= 0) {
			return 24;
		}
		return Math.min(pageSize, 200);
	}
}
