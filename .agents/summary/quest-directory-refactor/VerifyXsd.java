import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.nio.file.Path;

public class VerifyXsd {
    public static void main(String[] args) throws Exception {
        System.out.println("Validating quest_data.xml against quest_data.xsd...");
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
            .newSchema(Path.of("src/main/resources/aion/data/static_data/quest/legacy/quest_data.xsd").toFile())
            .newValidator()
            .validate(new StreamSource(Path.of("src/main/resources/aion/data/static_data/quest/legacy/quest_data.xml").toFile()));
        System.out.println("SUCCESS: quest_data.xml schema validation passed!");
    }
}
