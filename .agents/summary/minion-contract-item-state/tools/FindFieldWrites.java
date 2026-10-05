// Finds instructions whose destination operand is a memory reference [reg + disp].
// @category Analysis
import ghidra.app.script.GhidraScript;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.listing.Instruction;
import ghidra.program.model.listing.InstructionIterator;
import ghidra.program.model.listing.Listing;
import ghidra.program.model.lang.Register;
import ghidra.program.model.scalar.Scalar;

public class FindFieldWrites extends GhidraScript {
    @Override
    protected void run() throws Exception {
        String[] args = getScriptArgs();
        if (args.length < 1) {
            throw new IllegalArgumentException("usage: FindFieldWrites.java <disp> [value]");
        }
        long disp = Long.decode(args[0]);
        Long onlyValue = args.length > 1 ? Long.decode(args[1]) : null;
        FunctionManager fm = currentProgram.getFunctionManager();
        Listing listing = currentProgram.getListing();
        InstructionIterator it = listing.getInstructions(true);
        int found = 0;
        while (it.hasNext()) {
            Instruction ins = it.next();
            if (ins.getNumOperands() < 1) {
                continue;
            }
            Object[] op0 = ins.getOpObjects(0);
            Register reg = null;
            Scalar scalar = null;
            for (Object o : op0) {
                if (o instanceof Register r) {
                    reg = r;
                } else if (o instanceof Scalar s) {
                    scalar = s;
                }
            }
            if (reg == null || scalar == null || scalar.getUnsignedValue() != disp) {
                continue;
            }
            String mnemonic = ins.getMnemonicString();
            if (mnemonic.startsWith("CMP") || mnemonic.startsWith("TEST")) {
                continue; // reads, not writes
            }
            if (onlyValue != null) {
                boolean match = false;
                for (int i = 1; i < ins.getNumOperands(); i++) {
                    for (Object o : ins.getOpObjects(i)) {
                        if (o instanceof Scalar s2 && s2.getUnsignedValue() == onlyValue) {
                            match = true;
                        }
                    }
                }
                if (!match) {
                    continue;
                }
            }
            Function f = fm.getFunctionContaining(ins.getAddress());
            println("WRITE " + ins.getAddress() + " "
                    + (f != null ? f.getName() + " @" + f.getEntryPoint() : "<no function>")
                    + " :: " + ins);
            found++;
        }
        println("TOTAL " + found + " writes to [reg+0x" + Long.toHexString(disp) + "]"
                + (onlyValue != null ? " value 0x" + Long.toHexString(onlyValue) : ""));
    }
}
