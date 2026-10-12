package #(packageConfig.mapperPackage);

import #(mapperConfig.buildSuperClassImport());
import #(packageConfig.entityPackage).#(table.buildEntityClassName());

#if(hasText(table.getComment()))
/**
 * #(table.getComment())
 */
#end
public interface #(table.buildMapperClassName()) extends #(mapperConfig.buildSuperClassName())<#(table.buildEntityClassName())> {
}
