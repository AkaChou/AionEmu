import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.nio.file.Path;

public class VerifyChallengeXsd {
    public static void main(String[] args) throws Exception {
        System.out.println("Validating challenge_tasks.xml against challenge_tasks.xsd...");
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
            .newSchema(Path.of("src/main/resources/aion/data/static_data/quest/legacy/challenge_tasks.xsd").toFile())
            .newValidator()
            .validate(new StreamSource(Path.of("src/main/resources/aion/data/static_data/quest/legacy/challenge_tasks.xml").toFile()));
        System.out.println("SUCCESS: challenge_tasks.xml schema validation passed!");
    }
}
