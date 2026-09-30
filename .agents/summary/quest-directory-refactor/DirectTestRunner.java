import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DirectTestRunner {
    public static void main(String[] args) throws Exception {
        System.setProperty("aion.quest.retailDriver", "true");

        String[] testClasses = {
            "com.aionemu.gameserver.questEngine.ProductionCatalogWhitelistVerificationTest",
            "com.aionemu.gameserver.questEngine.retail.RetailQuestDriverOverlayTest",
            "com.aionemu.gameserver.questEngine.retail.RetailTsvManifestGateTest",
            "com.aionemu.gameserver.questEngine.definition.QuestDefinitionCatalogManifestTest",
            "com.aionemu.gameserver.dataholders.QuestRandomRewardsDataTest",
            "com.aionemu.gameserver.model.templates.QuestTemplateRacePermittedTest"
        };

        int totalRun = 0;
        int failed = 0;

        for (String className : testClasses) {
            System.out.println("Running " + className + "...");
            Class<?> clazz = Class.forName(className);

            List<Method> beforeMethods = new ArrayList<>();
            List<Method> afterMethods = new ArrayList<>();
            List<Method> testMethods = new ArrayList<>();

            for (Method m : clazz.getDeclaredMethods()) {
                for (java.lang.annotation.Annotation a : m.getAnnotations()) {
                    String aname = a.annotationType().getSimpleName();
                    if ("BeforeEach".equals(aname)) beforeMethods.add(m);
                    if ("AfterEach".equals(aname)) afterMethods.add(m);
                    if ("Test".equals(aname)) testMethods.add(m);
                }
            }

            for (Method testMethod : testMethods) {
                totalRun++;
                var ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                Object instance = ctor.newInstance();

                for (Field field : clazz.getDeclaredFields()) {
                    
                    if (field.getType().equals(Path.class) && !java.lang.reflect.Modifier.isStatic(field.getModifiers()) && !java.lang.reflect.Modifier.isFinal(field.getModifiers())) {

                        field.setAccessible(true);
                        field.set(instance, Files.createTempDirectory("test-dir"));
                    }
                }

                try {
                    System.setProperty("aion.quest.retailDriver", "true");
                    for (Method b : beforeMethods) {
                        b.setAccessible(true);
                        b.invoke(instance);
                    }
                    testMethod.setAccessible(true);
                    testMethod.invoke(instance);
                    for (Method af : afterMethods) {
                        af.setAccessible(true);
                        af.invoke(instance);
                    }
                    System.out.println("  PASS: " + testMethod.getName());
                } catch (Exception e) {
                    failed++;
                    System.err.println("  FAIL: " + testMethod.getName());
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    cause.printStackTrace();
                } finally {
                    System.setProperty("aion.quest.retailDriver", "true");
                }
            }
        }

        System.out.println("\n-------------------------------------------");
        System.out.println("Total tests run: " + totalRun + ", Failures: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
        System.out.println(">>> 100% ALL GATES GREEN! <<<");
    }
}
