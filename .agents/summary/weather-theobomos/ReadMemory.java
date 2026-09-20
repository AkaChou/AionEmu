import ghidra.app.script.GhidraScript;
import ghidra.program.model.address.Address;

public class ReadMemory extends GhidraScript {
    @Override
    protected void run() throws Exception {
        for (String arg : getScriptArgs()) {
            Address a = toAddr(arg);
            try {
                int i = getInt(a);
                long l = getLong(a);
                println("ADDR " + arg + " int=" + i + " long=" + l);
            } catch (Exception e) {
                println("ADDR " + arg + " FAILED: " + e.getMessage());
            }
        }
    }
}
