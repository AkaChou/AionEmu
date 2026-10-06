// Lists code instructions that reference any address in [lo, hi), excluding data-to-data refs.
// usage: FindRefsInRange.java <loAddr> <hiAddr>
// @category Analysis
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.address.AddressSet;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.Instruction;
import ghidra.program.model.listing.InstructionIterator;
import ghidra.program.model.listing.Listing;
import ghidra.program.model.symbol.RefType;
import ghidra.program.model.symbol.Reference;
import ghidra.program.model.symbol.ReferenceManager;

public class FindRefsInRange extends GhidraScript {
    @Override
    protected void run() throws Exception {
        String[] args = getScriptArgs();
        FunctionManager fm = currentProgram.getFunctionManager();
        Address lo = currentProgram.getAddressFactory().getAddress(args[0]);
        Address hi = currentProgram.getAddressFactory().getAddress(args[1]);
        AddressSet set = new AddressSet(lo, hi);
        ReferenceManager rm = currentProgram.getReferenceManager();
        // iterate references FROM code: use getReferenceSourceIterator over memory is heavy; instead scan instructions
        Listing listing = currentProgram.getListing();
        InstructionIterator it = listing.getInstructions(true);
        int found = 0;
        while (it.hasNext()) {
            Instruction ins = it.next();
            Reference[] refs = ins.getReferencesFrom();
            for (Reference r : refs) {
                if (set.contains(r.getToAddress()) && r.getReferenceType().isData()) {
                    Function f = fm.getFunctionContaining(ins.getAddress());
                    println("REF " + r.getReferenceType() + " to " + r.getToAddress() + " from "
                            + ins.getAddress() + " "
                            + (f != null ? f.getName() + " @" + f.getEntryPoint() : "<no func>")
                            + " :: " + ins);
                    found++;
                }
            }
        }
        println("TOTAL " + found);
    }
}
