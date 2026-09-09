package com.aionemu.gameserver.ai2.poll;

import lombok.AllArgsConstructor;
import lombok.AccessLevel;

/**
 * 基于布尔值的简单 AI 投票回答实现。
 * Simple boolean-based implementation of an AI poll answer.
 *
 * @author ATracer
 */
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class SimpleAIAnswer implements AIAnswer {

	private final boolean answer;

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
