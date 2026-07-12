package com.aionemu.gameserver.ai2.poll;

/**
 * AI 决策投票的回答接口，提供是否肯定及具体结果。
 * Answer interface for AI decision polls, exposing positivity and a concrete result.
 *
 * @author ATracer
 */
public interface AIAnswer {

	/**
	 * 判断该回答是否为肯定。
	 * Whether this answer is positive.
	 *
	 * {@code true} if positive。
	 */
	boolean isPositive();

	/**
	 * 返回回答的具体结果对象。
	 * Return the concrete result object of this answer.
	 *
	 * Result object
	 */
	Object getResult();
}
