import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionIterator;

public class ListNamedFunctions extends GhidraScript {

    @Override
    protected void run() throws Exception {
        int named = 0;
        int total = 0;
        FunctionIterator functions = currentProgram.getFunctionManager().getFunctions(true);
        while (functions.hasNext() && !monitor.isCancelled()) {
            Function function = functions.next();
            total++;
            String name = function.getName();
            if (!name.startsWith("FUN_") && !name.startsWith("thunk_FUN_")) {
                named++;
                println("NAMED " + function.getEntryPoint() + " :: " + name);
            }
        }
        println("TOTAL " + total + " NAMED " + named);
    }
}
