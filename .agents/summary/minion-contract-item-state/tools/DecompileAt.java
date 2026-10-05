// Decompiles the function at (or containing) each given address and prints the C source.
// @category Analysis
import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompileOptions;
import ghidra.app.decompiler.DecompileResults;
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionManager;

public class DecompileAt extends GhidraScript {
    @Override
    protected void run() throws Exception {
        String[] args = getScriptArgs();
        if (args.length < 1) {
            throw new IllegalArgumentException("usage: DecompileAt.java <address> [address...]");
        }
        FunctionManager fm = currentProgram.getFunctionManager();
        DecompInterface decompiler = new DecompInterface();
        DecompileOptions options = new DecompileOptions();
        decompiler.setOptions(options);
        if (!decompiler.openProgram(currentProgram)) {
            throw new IllegalStateException("cannot open program in decompiler");
        }
        decompiler.setSimplificationStyle("decompile");
        for (String arg : args) {
            Address addr = currentProgram.getAddressFactory().getAddress(arg);
            Function f = fm.getFunctionAt(addr);
            if (f == null) {
                f = fm.getFunctionContaining(addr);
            }
            if (f == null) {
                println("== " + arg + " : <no function> ==");
                continue;
            }
            println("== " + arg + " : " + f.getName() + " @" + f.getEntryPoint() + " ==");
            DecompileResults res = decompiler.decompileFunction(f, 600, monitor);
            if (res != null && res.decompileCompleted()) {
                println(res.getDecompiledFunction().getC());
            } else {
                println("// decompile failed: " + (res == null ? "null" : res.getErrorMessage()));
            }
        }
    }
}
