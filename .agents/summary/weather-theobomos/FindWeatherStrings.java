import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Data;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.DataIterator;
import ghidra.program.model.symbol.Reference;

public class FindWeatherStrings extends GhidraScript {

    @Override
    protected void run() throws Exception {
        DataIterator data = currentProgram.getListing().getDefinedData(true);
        while (data.hasNext() && !monitor.isCancelled()) {
            Data item = data.next();
            Object value = item.getValue();
            if (!(value instanceof String)) {
                continue;
            }
            String text = (String) value;
            if (!text.toLowerCase().contains("weather")) {
                continue;
            }
            println("STRING " + item.getAddress() + " :: " + text);
            for (Reference reference : getReferencesTo(item.getAddress())) {
                Function function = getFunctionContaining(reference.getFromAddress());
                println("  REF " + reference.getFromAddress() + " -> "
                    + (function == null ? "<no function>" : function.getName() + " @ " + function.getEntryPoint()));
            }
        }
    }
}
