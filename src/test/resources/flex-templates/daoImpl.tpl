package #(packageConfig.serviceImplPackage);

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import #(packageConfig.servicePackage).#(table.buildServiceClassName());
import #(packageConfig.entityPackage).#(table.buildEntityClassName());
import #(packageConfig.mapperPackage).#(table.buildMapperClassName());
import #(serviceImplConfig.buildSuperClassImport());

/**
#if(hasText(table.getComment()))
 * #(table.getComment())
#end
 * 表 #(table.getName()) 的数据访问实现。
 * Data access implementation for table #(table.getName()).
 */
@Service
@Lazy
public class #(table.buildServiceImplClassName())
	extends #(serviceImplConfig.buildSuperClassName())<#(table.buildMapperClassName()), #(table.buildEntityClassName())>
	implements #(table.buildServiceClassName()) {
}
