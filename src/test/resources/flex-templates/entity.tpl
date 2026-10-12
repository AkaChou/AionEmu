package #(packageConfig.entityPackage);

#for(importClass : table.buildImports(false))
import #(importClass);
#end
import java.io.Serial;

import lombok.Data;

#if(hasText(table.getComment()))
/**
 * #(table.getComment())
 */
#end
@Data
#(table.buildTableAnnotation().trim())
public class #(table.buildEntityClassName()) implements Serializable {

	@Serial
	private static final long serialVersionUID = 1L;

#for(column : table.columns)
#if(hasText(column.comment))
	/** #(column.comment) */
#end
#set(annotations = column.buildAnnotations())
#if(hasText(annotations))
	#(annotations)
#end
	private #(column.propertySimpleType) #(column.property);

#end}
