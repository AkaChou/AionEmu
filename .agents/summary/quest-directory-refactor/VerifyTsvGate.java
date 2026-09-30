import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

public class VerifyTsvGate {
    public static void main(String[] args) throws Exception {
        Path retailDir = Path.of("src/main/resources/aion/data/static_data/quest/retail");
        Path dialogDir = Path.of("src/main/resources/aion/definitions/quest_dialog");
        Path manifestPath = retailDir.resolve("quest-retail-tsv-manifest.tsv");

        Set<String> onDisk = new TreeSet<>();
        try (var s = Files.list(retailDir)) {
            s.filter(p -> p.toString().endsWith(".tsv") && !p.getFileName().toString().equals("quest-retail-tsv-manifest.tsv"))
             .map(p -> p.getFileName().toString())
             .forEach(onDisk::add);
        }
        try (var s = Files.list(dialogDir)) {
            s.filter(p -> p.toString().endsWith(".tsv"))
             .map(p -> p.getFileName().toString())
             .forEach(onDisk::add);
        }

        Set<String> inManifest = new TreeSet<>();
        for (String line : Files.readAllLines(manifestPath)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] cols = line.split("\t");
            inManifest.add(cols[0]);
        }

        System.out.println("Disk TSVs: " + onDisk.size() + ", Manifest TSVs: " + inManifest.size());
        if (!onDisk.equals(inManifest)) {
            Set<String> diff1 = new TreeSet<>(onDisk);
            diff1.removeAll(inManifest);
            Set<String> diff2 = new TreeSet<>(inManifest);
            diff2.removeAll(onDisk);
            throw new IllegalStateException("TSV mismatch! Extra on disk: " + diff1 + ", Missing from disk: " + diff2);
        }
        System.out.println("SUCCESS: RetailTsvManifestGate verified! Zero discrepancy!");
    }
}
