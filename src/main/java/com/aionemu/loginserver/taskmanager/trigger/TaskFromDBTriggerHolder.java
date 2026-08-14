package com.aionemu.loginserver.taskmanager.trigger;

import com.aionemu.loginserver.taskmanager.trigger.implementations.AfterRestartTrigger;
import com.aionemu.loginserver.taskmanager.trigger.implementations.FixedInTimeTrigger;

/**
 * 数据库任务触发器类型枚举，映射名称到具体实现类。
 * Enum of DB task trigger types mapping names to concrete trigger classes.
 *
 * @author nrg
 */
public enum TaskFromDBTriggerHolder {

    FIXED_IN_TIME(FixedInTimeTrigger.class),
    AFTER_RESTART(AfterRestartTrigger.class);
    private Class<? extends TaskFromDBTrigger> triggerClass;

    private TaskFromDBTriggerHolder(Class<? extends TaskFromDBTrigger> triggerClass) {
        this.triggerClass = triggerClass;
    }

    /**
     * 获取对应的触发器实现类。
     * Returns the associated trigger implementation class.
     *
     * @return 触发器实现类 / trigger class
     */
    public Class<? extends TaskFromDBTrigger> getTriggerClass() {
        return triggerClass;
    }
}
