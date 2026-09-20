import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Data;
import ghidra.program.model.listing.DataIterator;
import ghidra.program.model.listing.Function;
import ghidra.program.model.symbol.Reference;

public class FindStringRefs extends GhidraScript {
    @Override
    protected void run() throws Exception {
        String[] args = getScriptArgs();
        if (args.length == 0) {
            println("usage: FindStringRefs.java <substr> [substr ...]");
            return;
        }
        DataIterator it = currentProgram.getListing().getDefinedData(true);
        int hits = 0;
        int scanned = 0;
        while (it.hasNext() && !monitor.isCancelled()) {
            Data d = it.next();
            scanned++;
            Object v = d.getValue();
            if (!(v instanceof String)) {
                continue;
            }
            String s = (String) v;
            String lower = s.toLowerCase();
            boolean matched = false;
            for (String a : args) {
                if (lower.contains(a.toLowerCase())) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                continue;
            }
            hits++;
            println("STRING " + d.getAddress() + " :: " + s);
            for (Reference r : getReferencesTo(d.getAddress())) {
                Function f = getFunctionContaining(r.getFromAddress());
                println("   REF " + r.getFromAddress() + " in "
                    + (f == null ? "<none>" : f.getName() + "@" + f.getEntryPoint()));
            }
        }
        println("SCANNED " + scanned + " HITS " + hits);
    }
}
