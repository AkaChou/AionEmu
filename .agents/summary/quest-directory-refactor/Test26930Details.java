import java.nio.file.Path;
import com.aionemu.gameserver.questEngine.definition.*;
import com.aionemu.gameserver.questEngine.retail.*;

public class Test26930Details {
    public static void main(String[] args) throws Exception {
        System.setProperty("aion.quest.retailDriver", "true");
        QuestCatalog base = QuestDefinitionCatalogManifest.compile(
            Path.of("src/main/resources/aion/data/static_data/quest/definitions"));
        QuestCatalog overlay = RetailQuestDriver.overlay(base);
        QuestDefinition definition = overlay.findExecutable(26930).orElseThrow().definition();
        for (QuestTransition t : definition.transitions()) {
            if ("started".equals(t.sourceNode()) && "reward".equals(t.targetNode())) {
                System.out.println("Transition: " + t);
                System.out.println("  conditions: " + t.conditions());
                System.out.println("  actions: " + t.actions());
                System.out.println("  afterCommit: " + t.afterCommit());
            }
        }
    }
}
