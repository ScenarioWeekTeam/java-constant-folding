package comp207p.main;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.Code;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ClassGen;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.*;
import org.apache.bcel.util.InstructionFinder;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.TargetLostException;



public class ConstantFolder
{
	ClassParser parser = null;
	ClassGen gen = null;

	JavaClass original = null;
	JavaClass optimized = null;

	public ConstantFolder(String classFilePath)
	{
		try{
			this.parser = new ClassParser(classFilePath);
			this.original = this.parser.parse();
			this.gen = new ClassGen(this.original);
		} catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public void optimize()
	{
		ClassGen cgen = new ClassGen(original);
		ConstantPoolGen cpgen = cgen.getConstantPool();
		
		Method[] methods = gen.getMethods();

		for (Method m : methods) {
		    MethodGen mg = new MethodGen(m, cgen.getClassName(), cpgen);
		    Method optimized = optimizeMethod(mg);
		    
		    m = optimized;
		}
        
		this.optimized = cgen.getJavaClass();
	}
	
	private Method optimizeMethod(MethodGen mgen) {
	    int optimizations = 1;
	    InstructionList il = mgen.getInstructionList();
	    
	    while (optimizations > 0) {
	        optimizations = 0;
	        optimizations += simpleFolding(mgen, il);
	    }
	    
	    Method m = mgen.getMethod();
	    il.dispose();
	    return m;
	}

	private int simpleFolding(MethodGen m, InstructionList il) {
	    InstructionFinder f = new InstructionFinder(il);
	    int counter = 0;
	    
	    for (Iterator iter = f.search("PushInstruction PushInstruction ArithmeticInstruction"); iter.hasNext();) {
	        InstructionHandle[] match = (InstructionHandle[]) iter.next();
	        PushInstruction left = (PushInstruction)match[0].getInstruction();
	        PushInstruction right = (PushInstruction)match[1].getInstruction();
	        Instruction op = match[2].getInstruction();
	        if (!(left instanceof ConstantPushInstruction))
	            continue;
	        if (!(right instanceof ConstantPushInstruction))
	            continue;
	        ConstantPushInstruction l = (ConstantPushInstruction)left;
	        ConstantPushInstruction r = (ConstantPushInstruction)right;
	        Number a = l.getValue();
	        Number b = r.getValue();
	        Instruction folded = foldOperation(m, a, b, op);
	        match[0].setInstruction(folded);
	        try {
	            il.delete(match[1], match[2]);
	        }
	        catch (TargetLostException e) {
	        
	        }
	        
	        counter++;
	    }
	    
	    return counter;
	}
	
	private Instruction foldOperation(MethodGen m, Number a, Number b,  Instruction op) {
	    ConstantPoolGen cpgen = m.getConstantPool();
	    Instruction folded = null;
	    
	    if (op instanceof DADD) {
	        folded = new LDC2_W(cpgen.addDouble(a.doubleValue() + b.doubleValue()));
	    }
	    else if (op instanceof DDIV) {
	        folded = new LDC2_W(cpgen.addDouble(a.doubleValue() / b.doubleValue()));
	    }
	    else if (op instanceof DMUL) {
	        folded = new LDC2_W(cpgen.addDouble(a.doubleValue() * b.doubleValue()));
	    }
	    else if (op instanceof DREM) {
	        folded = new LDC2_W(cpgen.addDouble(a.doubleValue() % b.doubleValue()));
	    }
	    else if (op instanceof DSUB) {
	        folded = new LDC2_W(cpgen.addDouble(a.doubleValue() - b.doubleValue()));
	    }
	    else if (op instanceof FADD) {
	        folded = new LDC(cpgen.addFloat(a.floatValue() + b.floatValue()));
	    }
	    else if (op instanceof FDIV) {
	        folded = new LDC(cpgen.addFloat(a.floatValue() / b.floatValue()));
	    }
	    else if (op instanceof FMUL) {
	        folded = new LDC(cpgen.addFloat(a.floatValue() * b.floatValue()));
	    }
	    else if (op instanceof FREM) {
	        folded = new LDC(cpgen.addFloat(a.floatValue() % b.floatValue()));
	    }
	    else if (op instanceof FSUB) {
	        folded = new LDC(cpgen.addFloat(a.floatValue() - b.floatValue()));
	    }
	    else if (op instanceof IADD) {
	        folded = new LDC(cpgen.addInteger(a.intValue() + b.intValue()));
	    }
	    else if (op instanceof IDIV) {
	        folded = new LDC(cpgen.addInteger(a.intValue() / b.intValue()));
	    }
	    else if (op instanceof IMUL) {
	        folded = new LDC(cpgen.addInteger(a.intValue() * b.intValue()));
	    }
	    else if (op instanceof IREM) {
	        folded = new LDC(cpgen.addInteger(a.intValue() % b.intValue()));
	    }
	    else if (op instanceof ISUB) {
	        folded = new LDC(cpgen.addInteger(a.intValue() - b.intValue()));
	    }
	    else if (op instanceof LADD) {
	        folded = new LDC2_W(cpgen.addLong(a.longValue() + b.longValue()));
	    }
	    else if (op instanceof LDIV) {
	        folded = new LDC2_W(cpgen.addLong(a.longValue() / b.longValue()));
	    }
	    else if (op instanceof LMUL) {
	        folded = new LDC2_W(cpgen.addLong(a.longValue() * b.longValue()));
	    }
	    else if (op instanceof LREM) {
	        folded = new LDC2_W(cpgen.addLong(a.longValue() % b.longValue()));
	    }
	    else if (op instanceof LSUB) {
	        folded = new LDC2_W(cpgen.addLong(a.longValue() - b.longValue()));
	    }
	    
	    return folded;
	}
	
	public void write(String optimisedFilePath)
	{
		this.optimize();

		try {
			FileOutputStream out = new FileOutputStream(new File(optimisedFilePath));
			this.optimized.dump(out);
		} catch (FileNotFoundException e) {
			// Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// Auto-generated catch block
			e.printStackTrace();
		}
	}
}
