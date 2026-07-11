package com.aionemu.commons.database.dao;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * DAO 禁用注解
 * DAO Disable Annotation
 *
 * 此注解用于标记不应被加载的 DAO 实现类。
 * This annotation is used to mark DAO implementations that should not be loaded.
 * 被此注解标记的 DAO 类将在加载过程中被 DAOLoader 忽略。
 * DAO classes marked with this annotation will be ignored by the DAOLoader during loading process.
 *
 * @author SoulKeeper
 * @author Saelya
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DisabledDAO {
}
