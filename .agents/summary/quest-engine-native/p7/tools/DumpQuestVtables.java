/* Ghidra post-script: vftables whose namespace mentions Quest, with slot function names. */
import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;
import ghidra.program.model.listing.Program;
import ghidra.program.model.mem.Memory;
import ghidra.program.model.symbol.Symbol;
import ghidra.program.model.symbol.SymbolIterator;
import ghidra.program.model.symbol.SymbolTable;

public class DumpQuestVtables extends GhidraScript {

    private String slotName(long v) {
        if (v <= 0x100000L) {
            return "";
        }
        Symbol s = getSymbolAt(toAddr(v));
        if (s != null) {
            return s.getName(true);
        }
        var f = getFunctionAt(toAddr(v));
        if (f != null) {
            return f.getName(true);
        }
        return "";
    }

    @Override
    public void run() throws Exception {
        Program p = currentProgram;
        Memory mem = p.getMemory();
        SymbolTable st = p.getSymbolTable();
        SymbolIterator it = st.getAllSymbols(true);
        java.util.List<Symbol> vfts = new java.util.ArrayList<Symbol>();
        while (it.hasNext()) {
            Symbol s = it.next();
            String n = s.getName();
            if (!n.equals("vftable")) {
                continue;
            }
            String ns = s.getParentNamespace().getName();
            String full = s.getName(true);
            if (full.contains("Quest") || ns.contains("Quest")) {
                println("VFTABLE " + full + " @ " + s.getAddress());
                vfts.add(s);
            }
        }
        println("matched " + vfts.size() + " quest vftables");
        for (Symbol s : vfts) {
            println("== slots of " + s.getName(true) + " @ " + s.getAddress() + " ==");
            Address base = s.getAddress();
            for (int i = 0; i < 130; i++) {
                Address slot = base.add(i * 8L);
                if (!mem.contains(slot)) {
                    break;
                }
                byte[] b = new byte[8];
                mem.getBytes(slot, b);
                long v = 0;
                for (int k = 7; k >= 0; k--) {
                    v = (v << 8) | (b[k] & 0xffL);
                }
                println(String.format("  slot %3d (+%#x) = %#x %s", i, i * 8L, v, slotName(v)));
            }
        }
        println("DONE");
    }
}
