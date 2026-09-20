import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionIterator;

public class FindWeatherSymbols extends GhidraScript {

    @Override
    protected void run() throws Exception {
        FunctionIterator functions = currentProgram.getFunctionManager().getFunctions(true);
        while (functions.hasNext() && !monitor.isCancelled()) {
            Function function = functions.next();
            if (function.getName().toLowerCase().contains("weather")) {
                println("FUNCTION " + function.getEntryPoint() + " :: " + function.getName());
            }
        }
    }
}
