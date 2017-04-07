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
	    System.out.println("Optimising: " + classFilePath);
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
		
		Method[] methods = cgen.getMethods();

		for (int i = 0; i < methods.length; i++) {
		    MethodGen mg = new MethodGen(methods[i], cgen.getClassName(), cpgen);
		    Method optimized = optimizeMethod(mg);
		    
		    cgen.replaceMethod(methods[i], optimized);
		    methods[i] = optimized;
		}
        
        this.gen.setMethods(methods);
        this.gen.setConstantPool(cpgen);
        this.gen.setMajor(50);
		this.optimized = gen.getJavaClass();
	}
	
	private Method optimizeMethod(MethodGen mgen) {
	    int optimizations = 1;
	    InstructionList il = mgen.getInstructionList();
	    
	    while (optimizations > 0) {
	        optimizations = 0;
	        optimizations += simpleFolding(mgen, il);
	        optimizations += propagateConstantVariables(mgen, il);
	        optimizations += foldNegations(mgen, il);
	        optimizations += foldConversions(mgen, il);
	    }
	    
	    mgen.setMaxStack();
	    mgen.setMaxLocals();
	    Method m = mgen.getMethod();
	    il.dispose();
	    return m;
	}
	
	private int foldComparisons(MethodGen m, InstructionList il) {
	    ConstantPoolGen cpgen = m.getConstantPool();
	    InstructionFinder f = new InstructionFinder(il);
	    int counter = 0;
	    
	    for (Iterator iter = f.search("PushInstruction PushInstruction (LCMP|DCMPG|DCMPL|FCMPG|FCMPL)"); iter.hasNext();) {
	        System.out.println("Match found");
	        InstructionHandle[] match = (InstructionHandle[]) iter.next();
	        PushInstruction left = (PushInstruction)match[0].getInstruction();
	        PushInstruction right = (PushInstruction)match[1].getInstruction();
	        Instruction op = match[2].getInstruction();
	        Number a;
	        Number b;
	        
	        a = getConstant(left, cpgen);
	        b = getConstant(right, cpgen);
	        
	        if (a == null || b == null) {
	            continue;
	        }
	        
	        System.out.println("New instruction being added");
	        Instruction folded = null;
	        
	        if (op instanceof LCMP) {
	            long alpha = a.longValue();
	            long beta = b.longValue();
	            if (alpha < beta) {
	                folded = new ICONST(-1);
	            }
	            else if (alpha == beta) {
	                folded = new ICONST(0);
	            }
	            else {
	                folded = new ICONST(1);
	            }
	        }
	        else if (op instanceof DCMPG) {
	            double alpha = a.doubleValue();
	            double beta = b.doubleValue();
	            if (alpha < beta) {
	                folded = new ICONST(-1);
	            }
	            else if (alpha == beta) {
	                folded = new ICONST(0);
	            }
	            else {
	                folded = new ICONST(1);
	            }
	        }
	        else if (op instanceof DCMPL) {
	        
	        }
	        else if (op instanceof FCMPG) {
	        
	        }
	        else if (op instanceof FCMPL) {
	        
	        }
	        
	        System.out.println(folded.toString());
	        match[0].setInstruction(folded);
	        try {
	            il.delete(match[1], match[2]);
	        }
	        catch (TargetLostException e) {
	            System.out.println("Target lost");
	            continue;
	        }
	        
	        counter++;
	    }
	}

    private int foldNegations(MethodGen m, InstructionList il) {
        ConstantPoolGen cpgen = m.getConstantPool();
	    InstructionFinder f = new InstructionFinder(il);
	    int counter = 0;
	    
	    for (Iterator iter = f.search("PushInstruction (DNEG|FNEG|INEG|LNEG)"); iter.hasNext();) {
	        System.out.println("Match found");
	        InstructionHandle[] match = (InstructionHandle[]) iter.next();
	        PushInstruction constant = (PushInstruction)match[0].getInstruction();
	        Instruction op = match[1].getInstruction();
	        Number v;
	        v = getConstant(constant, cpgen);
	        
	        if (v == null) {
	            continue;
	        }
	        
	        System.out.println("New instruction being added");
	        Instruction folded = null;
	        if (op instanceof LNEG) {
	            folded = new LDC2_W(cpgen.addLong(-(v.longValue())));
	        }
	        else if (op instanceof INEG) {
	            folded = new LDC(cpgen.addInteger(-(v.intValue())));
	        }
	        else if (op instanceof FNEG) {
	            folded = new LDC(cpgen.addFloat(-(v.floatValue())));
	        }
	        else if (op instanceof DNEG) {
	            folded = new LDC2_W(cpgen.addDouble(-(v.doubleValue())));
	        }
	        else {
	            continue;
	        }
	        System.out.println(folded.toString());
	        match[0].setInstruction(folded);
	        try {
	            il.delete(match[1]);
	        }
	        catch (TargetLostException e) {
	            System.out.println("Target lost");
	            continue;
	        }
	        
	        counter++;
	    }
	    return counter;
    }
    
    private int foldConversions(MethodGen m, InstructionList il) {
        ConstantPoolGen cpgen = m.getConstantPool();
	    InstructionFinder f = new InstructionFinder(il);
	    int counter = 0;
	    
	    for (Iterator iter = f.search("PushInstruction ConversionInstruction"); iter.hasNext();) {
	        System.out.println("Match found");
	        InstructionHandle[] match = (InstructionHandle[]) iter.next();
	        PushInstruction value = (PushInstruction)match[0].getInstruction();
	        ConversionInstruction op = (ConversionInstruction)match[1].getInstruction();
	        
	        Number v = getConstant(value, cpgen);
	        
	        if (v == null) {
	            continue;
	        }
	        
	        System.out.println("New instruction being added");
	        Instruction folded = null;
	        if (op instanceof D2I || op instanceof F2I || op instanceof L2I) {
	            folded = new LDC(cpgen.addInteger(v.intValue()));
	        }
	        else if (op instanceof D2L || op instanceof F2L || op instanceof I2L) {
	            folded = new LDC2_W(cpgen.addLong(v.longValue()));
	        }
	        else if (op instanceof I2F || op instanceof D2F || op instanceof L2F) {
	            folded = new LDC(cpgen.addFloat(v.floatValue()));
	        }
	        else if (op instanceof I2D || op instanceof F2D || op instanceof L2D) {
	            folded = new LDC2_W(cpgen.addDouble(v.doubleValue()));
	        }
	        else {
	            continue;
	        }
	        
	        System.out.println(folded.toString());
	        match[0].setInstruction(folded);
	        try {
	            il.delete(match[1]);
	        }
	        catch (TargetLostException e) {
	            System.out.println("Target lost");
	            continue;
	        }
	        
	        counter++;
	    }
	    return counter;
    }
    
	private int simpleFolding(MethodGen m, InstructionList il) {
	    ConstantPoolGen cpgen = m.getConstantPool();
	    InstructionFinder f = new InstructionFinder(il);
	    int counter = 0;
	    
	    for (Iterator iter = f.search("PushInstruction PushInstruction ArithmeticInstruction"); iter.hasNext();) {
	        System.out.println("Match found");
	        InstructionHandle[] match = (InstructionHandle[]) iter.next();
	        PushInstruction left = (PushInstruction)match[0].getInstruction();
	        PushInstruction right = (PushInstruction)match[1].getInstruction();
	        Instruction op = match[2].getInstruction();
	        Number a;
	        Number b;
	        
	        a = getConstant(left, cpgen);
	        b = getConstant(right, cpgen);
	        
	        if (a == null || b == null) {
	            continue;
	        }
	        
	        System.out.println("New instruction being added");
	        Instruction folded = foldOperation(m, a, b, op);
	        System.out.println(folded.toString());
	        match[0].setInstruction(folded);
	        try {
	            il.delete(match[1], match[2]);
	        }
	        catch (TargetLostException e) {
	            System.out.println("Target lost");
	            continue;
	        }
	        
	        counter++;
	    }
	    
	    return counter;
	}
	
	private int propagateConstantVariables(MethodGen m, InstructionList il) {
	    ConstantPoolGen cpgen = m.getConstantPool();
	    InstructionFinder f = new InstructionFinder(il);
	    int counter = 0;
	    
	    for (Iterator iter = f.search("PushInstruction StoreInstruction"); iter.hasNext();) {
	        System.out.println("Match found");
	        InstructionHandle[] match = (InstructionHandle[])iter.next();
	        PushInstruction v = (PushInstruction)match[0].getInstruction();
	        StoreInstruction s = (StoreInstruction)match[1].getInstruction();
	        int index;
	        
	        Number value = getConstant(v, cpgen);
	        
	        if (value == null) {
	            continue;
	        }
	        
	        /*if (v instanceof SIPUSH) {
	            v = new LDC(cpgen.addInteger((int)value.shortValue()));
	        }
	        else if (v instanceof BIPUSH) {
	            v = new LDC(cpgen.addInteger((int)value.byteValue()));
	        } */
	        
	        if ((s instanceof DSTORE) || (s instanceof FSTORE) || (s instanceof ISTORE) || (s instanceof LSTORE)) {
	            index = s.getIndex();
	        }
	        else {
	            continue;
	        }
	        
	        InstructionHandle i = match[1].getNext();
	        while (i != null ) {
	            Instruction instruction = i.getInstruction();
	            int skip = i.getPosition();
	            if (i.hasTargeters()) {
	                break;
	            }
	            
	            if (skip > i.getPosition()) {
	                while (i.getNext().getPosition() < skip) {
	                    i = i.getNext();
	                }
	            }
	            
	            if (instruction instanceof LoadInstruction) {
	                LoadInstruction load = (LoadInstruction)instruction;
	                if (load.getIndex() == index) {
	                    System.out.println("Adding new instruction");
	                    i.setInstruction((Instruction)v);
	                    System.out.println(v.toString());
	                    counter++;
	                }
	            }
	            else if (instruction instanceof StoreInstruction) {
	                StoreInstruction store = (StoreInstruction)instruction;
	                if (store.getIndex() == index) {
	                    break;
	                }
	            }
	            i = i.getNext();
	        }
	    }
	    
	    return counter;
	}
	
	private Number getConstant(PushInstruction input, ConstantPoolGen cpgen) {
	    Number value = null;
	    if (input instanceof LDC) {
	        LDC i = (LDC)input;
	        Object v = i.getValue(cpgen);
	        if (v instanceof Number) {
	           value = (Number) v;
	        }
	        else {
	           return null;
	        }
	    }
	    else if (input instanceof LDC2_W) {
	        LDC2_W i = (LDC2_W)input;
	        Object v = i.getValue(cpgen);
	        if (v instanceof Number) {
	            value = (Number) v;
	        }
	        else {
	            return null;
	        }
	    }
	    else if (input instanceof ConstantPushInstruction) {
	        ConstantPushInstruction i = (ConstantPushInstruction)input;
	        Object v = i.getValue();
	        if (v instanceof Number) {
	            value = (Number) v;
	        }
	        else {
	            return null;
	        }
	    }
	    else {
	        return null;
	    }
	    
	    return value;
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
