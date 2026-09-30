import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.nio.file.Path;

public class VerifyStaticDataXmlAgainstXsd {
    public static void main(String[] args) throws Exception {
        System.out.println("Validating static_data.xml against static_data.xsd...");
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
            .newSchema(Path.of("src/main/resources/aion/data/static_data/static_data.xsd").toFile())
            .newValidator()
            .validate(new StreamSource(Path.of("src/main/resources/aion/data/static_data/static_data.xml").toFile()));
        System.out.println("SUCCESS: static_data.xml validates against static_data.xsd!");
    }
}
