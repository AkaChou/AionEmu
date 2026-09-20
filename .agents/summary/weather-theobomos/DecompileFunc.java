import ghidra.app.decompiler.DecompInterface;
import ghidra.app.decompiler.DecompileResults;
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Function;

public class DecompileFunc extends GhidraScript {
    @Override
    protected void run() throws Exception {
        String[] args = getScriptArgs();
        DecompInterface decompiler = new DecompInterface();
        decompiler.openProgram(currentProgram);
        for (String arg : args) {
            Address address = toAddr(arg);
            Function function = getFunctionAt(address);
            if (function == null) {
                function = getFunctionContaining(address);
            }
            if (function == null) {
                println("NO FUNCTION @ " + address);
                continue;
            }
            DecompileResults results = decompiler.decompileFunction(function, 180, monitor);
            println("===== " + function.getName() + " @ " + function.getEntryPoint() + " =====");
            if (!results.decompileCompleted()) {
                println("DECOMPILE FAILED: " + results.getErrorMessage());
                continue;
            }
            println(results.getDecompiledFunction().getC());
        }
        decompiler.dispose();
    }
}
