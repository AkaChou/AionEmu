import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;
import org.junit.platform.launcher.listeners.TestExecutionSummary;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.io.PrintWriter;

public class RunCoreGates {
    public static void main(String[] args) {
        System.setProperty("aion.quest.retailDriver", "true");
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
            .selectors(
                selectClass("com.aionemu.gameserver.questEngine.ProductionCatalogWhitelistVerificationTest"),
                selectClass("com.aionemu.gameserver.questEngine.retail.RetailQuestDriverOverlayTest"),
                selectClass("com.aionemu.gameserver.questEngine.retail.RetailTsvManifestGateTest"),
                selectClass("com.aionemu.gameserver.questEngine.definition.QuestDefinitionCatalogManifestTest"),
                selectClass("com.aionemu.gameserver.dataholders.QuestRandomRewardsDataTest"),
                selectClass("com.aionemu.gameserver.model.templates.QuestTemplateRacePermittedTest")
            )
            .build();

        Launcher launcher = LauncherFactory.create();
        SummaryGeneratingListener listener = new SummaryGeneratingListener();
        launcher.registerTestExecutionListeners(listener);
        launcher.execute(request);

        TestExecutionSummary summary = listener.getSummary();
        summary.printTo(new PrintWriter(System.out));

        if (summary.getTotalFailureCount() > 0) {
            summary.printFailuresTo(new PrintWriter(System.err));
            System.exit(1);
        }
        System.out.println("ALL CORE GATES PASSED!");
    }
}
