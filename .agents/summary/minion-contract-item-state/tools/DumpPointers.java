// Dumps qwords at the given address range, resolving any that point into functions.
// usage: DumpPointers.java <startAddr> <countQwords> [strideBytes]
// @category Analysis
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.FunctionManager;
import ghidra.program.model.mem.Memory;

public class DumpPointers extends GhidraScript {
    @Override
    protected void run() throws Exception {
        String[] args = getScriptArgs();
        FunctionManager fm = currentProgram.getFunctionManager();
        Memory mem = currentProgram.getMemory();
        Address start = currentProgram.getAddressFactory().getAddress(args[0]);
        int count = Integer.decode(args[1]);
        int stride = args.length > 2 ? Integer.decode(args[2]) : 8;
        Address a = start;
        for (int i = 0; i < count; i++) {
            Address slotAddr = a;
            try {
                long v = stride == 8 ? mem.getLong(a) : mem.getInt(a) & 0xffffffffL;
                Address target = currentProgram.getAddressFactory().getDefaultAddressSpace().getAddress(v);
                Function f = fm.getFunctionAt(target);
                String name;
                if (f != null) {
                    name = f.getName() + " @" + f.getEntryPoint();
                } else {
                    Function fc = fm.getFunctionContaining(target);
                    name = (fc != null ? "mid-" + fc.getName() : "data/unk");
                }
                println(slotAddr + ": 0x" + Long.toHexString(v) + " -> " + name);
            } catch (Exception e) {
                println(slotAddr + ": <unreadable>");
            }
            a = a.add(stride);
        }
    }
}
