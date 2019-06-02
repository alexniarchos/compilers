import syntaxtree.*;
import visitor.*;
import java.io.*;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

class Main {
	public static LinkedHashMap<String,ClassStruct> symbolTable;
	public static LinkedHashMap<String,VTable> VTables;
	public static String mainClass;

	public static class funStruct{
		public String returnType;
		public String funName;
		public LinkedHashMap<String,String> args;
		public LinkedHashMap<String,String> vars;
		public funStruct overridesFun;

		public funStruct(String rt,String fn){
			// initialization
			returnType = rt;
			funName = fn;
			args = new LinkedHashMap<String,String>();
			vars = new LinkedHashMap<String,String>();
		}
	}

	public static class ClassStruct {
		public String className;
		public String parentName;
		public LinkedHashMap<String,String> dataMembers;
		public LinkedHashMap<String,funStruct> functions;

		public ClassStruct(String cn,String pn){
			// initialization
			className = cn;
			parentName = pn;
			dataMembers = new LinkedHashMap<String,String>();
			functions = new LinkedHashMap<String,funStruct>();
		}
	}

	public static class Offsets{
		public Integer varOffset;
		public Integer funOffset;

		public Offsets(Integer v,Integer f){
			varOffset = v;
			funOffset = f;
		}
	}

	public static void printOffsets(){
		LinkedHashMap<String,Offsets> offsets = new LinkedHashMap<String,Offsets>();
		Integer varOffset,funOffset;
		for(Map.Entry<String,ClassStruct> classEntry : symbolTable.entrySet()){
			varOffset=0;
			funOffset=0;
			if(classEntry.getValue().functions.get("main") != null){
				continue;
			}
			// check if this class has a parent (offsets will continue from there)
			if(classEntry.getValue().parentName != null){
				// inherit offsets
				Offsets offset = offsets.get(classEntry.getValue().parentName);
				if(offset!=null){
					varOffset = offset.varOffset;
					funOffset = offset.funOffset;	
				}
				else{
					// System.out.println("Parent class doesn't have offsets!");
				}
			}
			// calculate and print variable offsets
			for(Map.Entry<String,String> varEntry : classEntry.getValue().dataMembers.entrySet()){
				System.out.println(classEntry.getKey()+"."+varEntry.getKey()+" : "+varOffset);
				if(varEntry.getValue().equals("int")){
					varOffset+=4;
				}
				else if(varEntry.getValue().equals("boolean")){
					varOffset+=1;
				}
				else{
					varOffset+=8;
				}
			}
			// if there is no parent class no need to check for overrides
			if(classEntry.getValue().parentName == null){
				for(Map.Entry<String,funStruct> funEntry : classEntry.getValue().functions.entrySet()){
					System.out.println(classEntry.getKey()+"."+funEntry.getKey()+" : "+funOffset);
					funOffset += 8;
				}
			}
			else{
				for(Map.Entry<String,funStruct> funEntry : classEntry.getValue().functions.entrySet()){
					// overriding function
					if(funEntry.getValue().overridesFun!=null){
						continue;
					}  
					System.out.println(classEntry.getKey()+"."+funEntry.getKey()+" : "+funOffset);
					funOffset += 8;
				}
			}
			// update offsets
			offsets.put(classEntry.getKey(),new Offsets(varOffset,funOffset));
		}
    }
    
    public static void main (String [] args) throws Exception{
		if(args.length < 1){
			System.err.println("No input files");
			System.exit(1);
		}
		FileInputStream fis = null;
		for(int i=0;i<args.length;i++){
			try{
				System.out.println("\n---Checking file: "+args[i]+"\n");
				fis = new FileInputStream(args[i]);
				MiniJavaParser parser = new MiniJavaParser(fis);
				System.err.println("Program parsed successfully.");
				symbolTable = new LinkedHashMap<String,ClassStruct>();
				VTables = new LinkedHashMap<String,VTable>();
				fillSTVisitor fillST = new fillSTVisitor();
				Goal root = parser.Goal();
				try {
					root.accept(fillST);
				} catch (Exception ex) {
					System.out.println("fill Symbol Table Error: "+ex.getMessage());
				}
				printOffsets();
				FileWriter fw = new FileWriter("./out/"+Main.mainClass+".ll");
				System.out.println("Generating: ./out/"+Main.mainClass+".ll");
				Minijava_to_LLVM_visitor generateCodeVisitor = new Minijava_to_LLVM_visitor(fw);
				root.accept(generateCodeVisitor);
				fw.close();
			}
			catch(ParseException ex){
				System.out.println(ex.getMessage());
			}
			catch(FileNotFoundException ex){
				System.err.println(ex.getMessage());
			}
			catch (Exception ex) {
				System.out.println("Code Generator Visitor Error: "+ex.getMessage());
			}
			finally{
				try{
					if(fis != null) fis.close();
				}
				catch(IOException ex){
					System.err.println(ex.getMessage());
				}
			}
		}
    }
}
