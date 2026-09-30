import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;
import java.nio.file.Path;

public class VerifyStaticDataXsd {
    public static void main(String[] args) throws Exception {
        System.out.println("Validating static_data.xsd...");
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
            .newSchema(Path.of("src/main/resources/aion/data/static_data/static_data.xsd").toFile());
        System.out.println("SUCCESS: static_data.xsd compiles cleanly!");
    }
}
