package com.aionemu.gameserver.model.stats.calc.functions;

import com.aionemu.gameserver.model.stats.calc.Stat2;
import com.aionemu.gameserver.model.stats.calc.StatOwner;
import com.aionemu.gameserver.model.stats.container.StatEnum;
import com.aionemu.gameserver.utils.stats.CalculationType;

/**
 * 属性函数 Proxy 模型。
 * Stat Function Proxy model.
 *
 * @author ATracer
 */
public class StatFunctionProxy implements IStatFunction, Comparable<IStatFunction> {

	private final StatOwner owner;
	private final IStatFunction proxiedFunction;
	private final StatEnum stat;

	public StatFunctionProxy(StatOwner owner, IStatFunction statFunction) {
		this.owner = owner;
		this.proxiedFunction = statFunction;
		this.stat = statFunction.getName();
	}

	public StatFunctionProxy(StatOwner owner, IStatFunction statFunction, StatEnum statEnum) {
		this.owner = owner;
		this.proxiedFunction = statFunction;
		this.stat = statEnum;
	}

	/** 返回 proxied function / Returns the proxied function */
	public IStatFunction getProxiedFunction() {
		return proxiedFunction;
	}

	/** 返回哈希码。 / Returns hash code. */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((owner == null) ? 0 : owner.hashCode());
		return result;
	}

	/** 是否相等。 / Equality check. */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		StatFunctionProxy other = (StatFunctionProxy) obj;
		if (owner == null) {
			if (other.owner != null) {
				return false;
			}
		} else if (!owner.equals(other.owner)) {
			return false;
		}
		return true;
	}

	/** 比较。 / Compares to another instance. */
	@Override
	public int compareTo(IStatFunction o) {
		return proxiedFunction.compareTo(o);
	}

	/** 返回所有者 / Returns the owner*/
	@Override
	public StatOwner getOwner() {
		return owner;
	}

	/** 获取名称。 / Returns the name. */
	@Override
	public StatEnum getName() {
		return stat;
	}

	/** 是否加成。 / Whether Bonus. */
	@Override
	public boolean isBonus() {
		return proxiedFunction.isBonus();
	}

	/** 返回 priority / Returns the priority */
	@Override
	public int getPriority() {
		return proxiedFunction.getPriority();
	}

	/** 获取值。 / Returns the value. */
	@Override
	public int getValue() {
		return proxiedFunction.getValue();
	}

	/** 校验。 / Validate. */
	@Override
	public boolean validate(Stat2 stat, IStatFunction statFunction) {
		return proxiedFunction.validate(stat, statFunction);
	}

	/** 应用。 / Apply. */
	@Override
	public void apply(Stat2 stat) {
		proxiedFunction.apply(stat);
	}

	/** 应用。 / Apply. */
	@Override
	public void apply(Stat2 stat, CalculationType... calculationTypes) {
		proxiedFunction.apply(stat, calculationTypes);
	}

	/**
	 * @return Whether conditions / Whether conditions
	 */
	@Override
	public boolean hasConditions() {
		return proxiedFunction.hasConditions();
	}

	/** 返回字符串表示。 / Returns string representation. */
	@Override
	public String toString() {
		return "Proxy [name=" + proxiedFunction.getName() + ", bonus=" + isBonus() + ", value=" + getValue()
				+ ", priority=" + getPriority() + ", owner=" + owner + "]";
	}
}
