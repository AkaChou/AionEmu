import java.nio.file.Path;
import com.aionemu.gameserver.questEngine.definition.*;
import com.aionemu.gameserver.questEngine.retail.*;

public class Test26930 {
    public static void main(String[] args) throws Exception {
        System.setProperty("aion.quest.retailDriver", "true");
        QuestCatalog base = QuestDefinitionCatalogManifest.compile(
            Path.of("src/main/resources/aion/data/static_data/quest/definitions"));
        System.out.println("Base catalog size: " + base.entries().size());
        System.out.println("Base 26930 entry: " + base.findEntry(26930));

        QuestCatalog overlay = RetailQuestDriver.overlay(base);
        System.out.println("Overlay size: " + overlay.entries().size());
        System.out.println("Overlay 26930 entry: " + overlay.findEntry(26930));
        System.out.println("Overlay 26930 executable present: " + overlay.findExecutable(26930).isPresent());
    }
}
