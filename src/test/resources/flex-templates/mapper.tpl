package #(packageConfig.mapperPackage);

import #(mapperConfig.buildSuperClassImport());
import #(packageConfig.entityPackage).#(table.buildEntityClassName());

/**
#if(hasText(table.getComment()))
 * #(table.getComment())
#end
 * 表 #(table.getName()) 的映射层。
 * Mapper for table #(table.getName()).
 */
public interface #(table.buildMapperClassName()) extends #(mapperConfig.buildSuperClassName())<#(table.buildEntityClassName())> {
}
