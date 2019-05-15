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
	public static String code;

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

	public static void fillVTables(){
		// generate VTable from symbolTable
		LinkedHashMap<String,Offsets> offsets = new LinkedHashMap<String,Offsets>();
		Integer varOffset,funOffset;
		for(Map.Entry<String,ClassStruct> classEntry : Main.symbolTable.entrySet()){
			// create new VTable
			VTable tempVtable = new VTable(classEntry.getKey(),classEntry.getValue().parentName);
			VTables.put(classEntry.getKey(),tempVtable);

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
				// System.out.println(classEntry.getKey()+"."+varEntry.getKey()+" : "+varOffset);
				tempVtable.dataMembers.put(varEntry.getKey(),varOffset);
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
					// System.out.println(classEntry.getKey()+"."+funEntry.getKey()+" : "+funOffset);
					tempVtable.functions.put(funEntry.getKey(),funOffset);
					funOffset += 8;
				}
			}
			else{
				for(Map.Entry<String,funStruct> funEntry : classEntry.getValue().functions.entrySet()){
					// overriding function
					if(funEntry.getValue().overridesFun!=null){
						tempVtable.functions.put(funEntry.getKey(),funOffset);
						continue;
					}  
					// System.out.println(classEntry.getKey()+"."+funEntry.getKey()+" : "+funOffset);
					funOffset += 8;
				}
			}
			// update offsets
			offsets.put(classEntry.getKey(),new Offsets(varOffset,funOffset));
		}
	}

	public static String llvmType(String type){
		if(type.equals("int")){
			return "i32";
		}
		else if(type.equals("boolean")){
			return "i1";
		}
		else if(type.equals("int[]")){
			return "i32*";
		}
		else{
			return "i8*";
		}
	}

	public static void printVTable(){
		// generate llvm vtable code
		for(Map.Entry<String,ClassStruct> classEntry : Main.symbolTable.entrySet()){
			// get current class Vtable
			VTable tempVtable = Main.VTables.get(classEntry.getKey());
			code += "@." + classEntry.getKey() + "_vtable = global ["+tempVtable.functions.size()+" x i8*] [";
			int count = 0;
			for(Map.Entry<String,Integer> funEntry : tempVtable.functions.entrySet()){
				count++;
				funStruct tempFun = Main.symbolTable.get(classEntry.getKey()).functions.get(funEntry.getKey());
				code += "i8* bitcast (" + llvmType(tempFun.returnType)+" (i8*";
				String argTypes = "";
				for(Map.Entry<String,String> argEntry : tempFun.args.entrySet()){
					argTypes += ","+llvmType(argEntry.getValue());
				}
				code += argTypes+")* @"+classEntry.getKey()+"."+tempFun.funName+" to i8*)";
				if(count!=tempVtable.functions.size()){
					// not last function, add ','
					code += ", ";
				}
			}
			code += "]\n";
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
				Minijava_to_LLVM_visitor generateCodeVisitor = new Minijava_to_LLVM_visitor();
				Goal root = parser.Goal();
				root.accept(fillST);
				printOffsets();
				code = "";
				fillVTables();
				printVTable();
				code += "declare i8* @calloc(i32, i32)\n"+
				"declare i32 @printf(i8*, ...)\n"+
				"declare void @exit(i32)\n\n"+
				"@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n"+
				"@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n"+
				"define void @print_int(i32 %i) {\n"+
				"    %_str = bitcast [4 x i8]* @_cint to i8*\n"+
				"    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n"+
				"    ret void\n"+
				"}\n\n"+
				"define void @throw_oob() {\n"+
				"    %_str = bitcast [15 x i8]* @_cOOB to i8*\n"+
				"    call i32 (i8*, ...) @printf(i8* %_str)\n"+
				"    call void @exit(i32 1)\n"+
				"    ret void\n"+
				"}\n\n";
				root.accept(generateCodeVisitor);
				System.out.println("Generated Code:\n"+code);
			}
			catch(ParseException ex){
				System.out.println(ex.getMessage());
			}
			catch(FileNotFoundException ex){
				System.err.println(ex.getMessage());
			}
			// catch (Exception ex) {
			// 	System.out.println("Code Generator Visitor Error: "+ex.getMessage());
			// }
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
