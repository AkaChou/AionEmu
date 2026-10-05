// Finds instructions that reference a given scalar constant and prints the containing function.
// @category Analysis
import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.Instruction;
import ghidra.program.model.listing.InstructionIterator;
import ghidra.program.model.listing.Listing;
import ghidra.program.model.scalar.Scalar;

public class FindScalarXrefs extends GhidraScript {
    @Override
    protected void run() throws Exception {
        String[] args = getScriptArgs();
        if (args.length < 1) {
            throw new IllegalArgumentException("usage: FindScalarXrefs.java <scalar> [more scalars...]");
        }
        FunctionManager fm = currentProgram.getFunctionManager();
        Listing listing = currentProgram.getListing();
        for (String arg : args) {
            long target = Long.decode(arg);
            InstructionIterator it = listing.getInstructions(true);
            int found = 0;
            println("== scalar " + arg + " (0x" + Long.toHexString(target) + ") ==");
            while (it.hasNext()) {
                Instruction ins = it.next();
                for (int i = 0; i < ins.getNumOperands(); i++) {
                    for (Object o : ins.getOpObjects(i)) {
                        if (o instanceof Scalar s && s.getUnsignedValue() == target) {
                            Function f = fm.getFunctionContaining(ins.getAddress());
                            println("HIT " + ins.getAddress() + " "
                                    + (f != null ? f.getName() + " @" + f.getEntryPoint() : "<no function>")
                                    + " :: " + ins);
                            found++;
                        }
                    }
                }
            }
            println("TOTAL " + found + " for " + arg);
        }
    }
}
