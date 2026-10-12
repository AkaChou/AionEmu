package #(packageConfig.serviceImplPackage);

import org.springframework.stereotype.Service;

import #(packageConfig.servicePackage).#(table.buildServiceClassName());
import #(packageConfig.entityPackage).#(table.buildEntityClassName());
import #(packageConfig.mapperPackage).#(table.buildMapperClassName());
import #(serviceImplConfig.buildSuperClassImport());

#if(hasText(table.getComment()))
/**
 * #(table.getComment())
 */
#end
@Service
public class #(table.buildServiceImplClassName())
	extends #(serviceImplConfig.buildSuperClassName())<#(table.buildMapperClassName()), #(table.buildEntityClassName())>
	implements #(table.buildServiceClassName()) {
}
