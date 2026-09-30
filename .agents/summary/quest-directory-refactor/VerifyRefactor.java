import com.aionemu.gameserver.questEngine.definition.ProductionQuestDefinitions;
import com.aionemu.gameserver.questEngine.definition.QuestCatalog;
import com.aionemu.gameserver.questEngine.retail.RetailQuestDriver;

public class VerifyRefactor {
    public static void main(String[] args) throws Exception {
        System.setProperty("aion.quest.retailDriver", "true");
        System.out.println("Starting verifyRefactor...");
        QuestCatalog production = ProductionQuestDefinitions.catalog();
        System.out.println("Production catalog loaded successfully! Total entries: " + production.entries().size());
        if (production.entries().size() != 6224) {
            throw new IllegalStateException("Expected 6224 entries, got " + production.entries().size());
        }
        System.out.println("RetailQuestDriver current stats: " + RetailQuestDriver.current().map(RetailQuestDriver::overlayStats).orElse("none"));
        System.out.println("SUCCESS: 6224/6224 loaded and verified!");
    }
}
