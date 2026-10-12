package #(packageConfig.servicePackage);

import #(serviceConfig.buildSuperClassImport());
import #(packageConfig.entityPackage).#(table.buildEntityClassName());

/**
#if(hasText(table.getComment()))
 * #(table.getComment())
#end
 * 表 #(table.getName()) 的数据访问层。
 * Data access layer for table #(table.getName()).
 */
public interface #(table.buildServiceClassName()) extends #(serviceConfig.buildSuperClassName())<#(table.buildEntityClassName())> {
}
