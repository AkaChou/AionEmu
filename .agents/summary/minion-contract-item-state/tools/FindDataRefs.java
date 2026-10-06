// Lists data (and code) references TO a given address, printing the referencing instruction or data.
// @category Analysis
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.Instruction;
import ghidra.program.model.symbol.Reference;

public class FindDataRefs extends GhidraScript {
    @Override
    protected void run() throws Exception {
        String[] args = getScriptArgs();
        FunctionManager fm = currentProgram.getFunctionManager();
        for (String arg : args) {
            Address addr = currentProgram.getAddressFactory().getAddress(Long.toHexString(Long.decode(arg)));
            println("== refs to " + addr + " ==");
            Reference[] refs = getReferencesTo(addr);
            int found = 0;
            for (Reference r : refs) {
                Address from = r.getFromAddress();
                Function f = fm.getFunctionContaining(from);
                String ctx;
                Instruction ins = getInstructionAt(from);
                if (ins != null) {
                    ctx = "INS " + ins;
                } else {
                    ctx = "DATA";
                }
                println("REF " + r.getReferenceType() + " from " + from + " "
                        + (f != null ? f.getName() + " @" + f.getEntryPoint() : "<no func>") + " :: " + ctx);
                found++;
            }
            println("TOTAL " + found + " refs to " + arg);
        }
    }
}
