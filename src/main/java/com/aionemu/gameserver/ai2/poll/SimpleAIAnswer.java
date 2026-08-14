package com.aionemu.gameserver.ai2.poll;

/**
 * 基于布尔值的简单 AI 投票回答实现。
 * Simple boolean-based implementation of an AI poll answer.
 *
 * @author ATracer
 */
public class SimpleAIAnswer implements AIAnswer {

	private final boolean answer;

	/**
	 * 使用给定布尔结果构造回答。
	 * Construct an answer with the given boolean result.
	 *
	 * @param answer 回答是否为肯定 / Whether the answer is positive
	 */
	SimpleAIAnswer(boolean answer) {
		this.answer = answer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isPositive() {
		return answer;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Object getResult() {
		return answer;
	}
}
