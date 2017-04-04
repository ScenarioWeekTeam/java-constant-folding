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
		    Method optimized = optimiseMethod(mg);
		    
		    m = optimized;
		}
        
		this.optimized = cgen.getJavaClass();
	}
	
	private Method optimizeMethod(MethodGen mgen) {
	    int optimizations = 1;
	    InstructionList il = mgen.getInstructionList();
	    
	    while (optimizations > 0) {
	        optimizations = 0;
	        optimizations += simpleFolding(il);
	    }
	    
	    Method m = mgen.getMethod();
	    il.dispose();
	    return m;
	}

	private int simpleFolding(InstructionList il) {
	    InstructionFinder f = new InstructionFinder(il):
	    int counter = 0;
	    
	    for (Iterator iter = f.search("PushInstruction PushInstruction ArithmeticInstruction"); iter.hasNext();) {
	        InstructionHandle[] match = (InstructionHandle[]) iter.next();
	        PushInstruction left = match[0].getInstruction();
	        PushInstruction right = match[1].getInstruction();
	        ArithmeticInstruction op = match[2].getInstruction();
	        if (!(a instanceof ConstantPushInstruction))
	            continue;
	        if (!(b instanceof ConstantPushInstruction))
	            continue;
	        Number a = left.getValue();
	        Number b = right.getValue();
	        Instruction folded = foldOperation(a, b, op);
	        match[0].setInstruction(folded);
	        il.delete(match[1], match[2]);
	        counter++;
	    }
	    
	    return counter;
	}
	
	private Instruction foldOperation(Number a, Number b,  Instruction op) {
	
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
