// 真资源 XSD 校验探针（分析工具，非生产代码）。与门测试用同一 JDK SchemaFactory 路径。
// XSD validation probe (analysis tool, not production code); same JDK SchemaFactory path the gate test uses.
//
// 用法 / Usage: java xsd_validate.java <schema.xsd> <doc.xml> [<doc2.xml> ...]
import java.io.File;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

public final class xsd_validate {
	public static void main(String[] args) throws Exception {
		Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
				.newSchema(new StreamSource(new File(args[0])));
		int failures = 0;
		for (int i = 1; i < args.length; i++) {
			try {
				Validator validator = schema.newValidator();
				validator.validate(new StreamSource(new File(args[i])));
				System.out.println("OK   " + args[i]);
			} catch (org.xml.sax.SAXException e) {
				failures++;
				System.out.println("FAIL " + args[i] + " :: " + e.getMessage());
			}
		}
		if (failures > 0) {
			System.exit(1);
		}
	}
}
