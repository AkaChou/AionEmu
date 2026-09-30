import java.nio.file.Path;
import com.aionemu.gameserver.questEngine.definition.*;
import com.aionemu.gameserver.questEngine.retail.*;

public class Test26930Transitions {
    public static void main(String[] args) throws Exception {
        System.setProperty("aion.quest.retailDriver", "true");
        QuestCatalog base = QuestDefinitionCatalogManifest.compile(
            Path.of("src/main/resources/aion/data/static_data/quest/definitions"));
        QuestCatalog overlay = RetailQuestDriver.overlay(base);
        QuestDefinition definition = overlay.findExecutable(26930).orElseThrow().definition();
        System.out.println("Transitions for 26930:");
        for (QuestTransition t : definition.transitions()) {
            System.out.println("  source=" + t.sourceNode() + " target=" + t.targetNode() + " event=" + t.event());
        }
    }
}
