import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class RunAllFamilyGates {
    public static void main(String[] args) throws Exception {
        System.setProperty("aion.quest.retailDriver", "true");

        String[] testClasses = {
            "com.aionemu.gameserver.questEngine.definition.RetailMetadataEquivalenceGateTest",
            "com.aionemu.gameserver.questEngine.definition.RetailOwnershipGateTest",
            "com.aionemu.gameserver.questEngine.retail.RetailSimpleHuntEquivalenceGateTest",
            "com.aionemu.gameserver.questEngine.retail.RetailSimpleTalkGateTest",
            "com.aionemu.gameserver.questEngine.retail.RetailDataDrivenGateTest",
            "com.aionemu.gameserver.questEngine.retail.RetailCombineTaskGateTest",
            "com.aionemu.gameserver.questEngine.retail.RetailSimpleCollectItemGateTest",
            "com.aionemu.gameserver.questEngine.retail.RetailSimpleItemPlayGateTest",
            "com.aionemu.gameserver.questEngine.retail.RetailSimpleSerialHuntGateTest",
            "com.aionemu.gameserver.questEngine.retail.RetailSimpleUseItemGateTest"
        };

        int totalRun = 0;
        int failed = 0;

        for (String className : testClasses) {
            System.out.println("Running " + className + "...");
            Class<?> clazz = Class.forName(className);

            List<Method> beforeAllMethods = new ArrayList<>();
            List<Method> afterAllMethods = new ArrayList<>();
            List<Method> beforeMethods = new ArrayList<>();
            List<Method> afterMethods = new ArrayList<>();
            List<Method> testMethods = new ArrayList<>();

            for (Method m : clazz.getDeclaredMethods()) {
                for (java.lang.annotation.Annotation a : m.getAnnotations()) {
                    String aname = a.annotationType().getSimpleName();
                    if ("BeforeAll".equals(aname)) beforeAllMethods.add(m);
                    if ("AfterAll".equals(aname)) afterAllMethods.add(m);
                    if ("BeforeEach".equals(aname)) beforeMethods.add(m);
                    if ("AfterEach".equals(aname)) afterMethods.add(m);
                    if ("Test".equals(aname)) testMethods.add(m);
                }
            }

            for (Method ba : beforeAllMethods) {
                ba.setAccessible(true);
                ba.invoke(null);
            }

            for (Method testMethod : testMethods) {
                totalRun++;
                var ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                Object instance = ctor.newInstance();

                for (Field field : clazz.getDeclaredFields()) {
                    if (field.getType().equals(Path.class) && !Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) {
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

            for (Method aa : afterAllMethods) {
                aa.setAccessible(true);
                aa.invoke(null);
            }
        }

        System.out.println("\n-------------------------------------------");
        System.out.println("Total tests run: " + totalRun + ", Failures: " + failed);
        if (failed > 0) {
            System.exit(1);
        }
        System.out.println(">>> 100% ALL FAMILY GATES GREEN! <<<");
    }
}
