import com.aionemu.gameserver.dataholders.StaticData;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Unmarshaller;
import java.io.File;

public class VerifyStaticData {
    public static void main(String[] args) throws Exception {
        System.out.println("Testing JAXB load for static_data.xml...");
        File file = new File("src/main/resources/aion/data/static_data/static_data.xml");
        JAXBContext jc = JAXBContext.newInstance(StaticData.class);
        Unmarshaller u = jc.createUnmarshaller();
        StaticData data = (StaticData) u.unmarshal(file);
        System.out.println("StaticData loaded! QuestsData: " + (data.questData != null ? data.questData.size() : "null"));
        System.out.println("RandomRewards loaded: " + (data.questRandomRewardsData != null));
        System.out.println("ChallengeTasks: " + (data.challengeData != null ? data.challengeData.size() : "null"));
        System.out.println("SUCCESS: StaticData JAXB load verified!");
    }
}
