// Lists callers (and other references) of the function at a given address.
// @category Analysis
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.symbol.Reference;
import ghidra.program.model.symbol.ReferenceManager;

public class FindCallers extends GhidraScript {
    @Override
    protected void run() throws Exception {
        String[] args = getScriptArgs();
        if (args.length < 1) {
            throw new IllegalArgumentException("usage: FindCallers.java <address> [address...]");
        }
        FunctionManager fm = currentProgram.getFunctionManager();
        ReferenceManager rm = currentProgram.getReferenceManager();
        for (String arg : args) {
            Address addr = currentProgram.getAddressFactory().getAddress(arg);
            println("== callers of " + arg + " ==");
            int found = 0;
            for (Reference ref : rm.getReferencesTo(addr)) {
                if (!ref.getReferenceType().isCall() && !ref.getReferenceType().isJump()) {
                    continue;
                }
                Function f = fm.getFunctionContaining(ref.getFromAddress());
                println("CALL " + ref.getFromAddress() + " "
                        + (f != null ? f.getName() + " @" + f.getEntryPoint() : "<no function>")
                        + " :: " + ref.getReferenceType());
                found++;
            }
            println("TOTAL " + found + " callers of " + arg);
        }
    }
}
