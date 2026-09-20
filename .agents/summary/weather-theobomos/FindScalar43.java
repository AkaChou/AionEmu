import ghidra.app.script.GhidraScript;
import ghidra.program.model.lang.OperandType;
import ghidra.program.model.listing.Function;
import ghidra.program.model.listing.Instruction;
import ghidra.program.model.listing.InstructionIterator;
import ghidra.program.model.scalar.Scalar;

public class FindScalar43 extends GhidraScript {

    @Override
    protected void run() throws Exception {
        InstructionIterator instructions = currentProgram.getListing().getInstructions(true);
        while (instructions.hasNext() && !monitor.isCancelled()) {
            Instruction instruction = instructions.next();
            for (int i = 0; i < instruction.getNumOperands(); i++) {
                if (!instruction.getOperandType(i).contains(OperandType.SCALAR)) {
                    continue;
                }
                for (Object object : instruction.getOpObjects(i)) {
                    if (!(object instanceof Scalar)) {
                        continue;
                    }
                    long value = ((Scalar) object).getUnsignedValue();
                    if (value == 0x43L) {
                        Function function = getFunctionContaining(instruction.getAddress());
                        println("SCALAR43 " + instruction.getAddress() + " "
                            + (function == null ? "<no function>" : function.getName() + " @ " + function.getEntryPoint())
                            + " :: " + instruction);
                    }
                }
            }
        }
    }
}
