import syntaxtree.*;
import visitor.GJNoArguDepthFirst;
import java.lang.System;
import java.util.LinkedHashMap;
import java.util.Map;

public class Minijava_to_LLVM_visitor extends GJNoArguDepthFirst<String>{

    private int registerCount;
    private int loopCount;
    private int ifCount;
    private int oobCount;
    private int arr_allocCount;
    private int andclauseCount;

    public class State{
        public Main.ClassStruct classSt;
        public Main.funStruct funSt;
        public Main.funStruct messageSendFun;
        public String varOf;
        public Integer argCount;
        public String tempStr;
        public String exprType;
        public String varOrigin;
    }

    public State state = new State();

    public Minijava_to_LLVM_visitor(){
        fillVTables();
        printVTables();

        emit("\ndeclare i8* @calloc(i32, i32)\n"+
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
        "}\n\n");
    }

    public static void fillVTables(){
		// generate VTable from symbolTable
		LinkedHashMap<String,Main.Offsets> offsets = new LinkedHashMap<String,Main.Offsets>();
		Integer varOffset,funOffset;
		for(Map.Entry<String,Main.ClassStruct> classEntry : Main.symbolTable.entrySet()){
			// create new VTable
			VTable tempVtable = new VTable(classEntry.getKey(),classEntry.getValue().parentName);
			Main.VTables.put(classEntry.getKey(),tempVtable);

			varOffset=0;
			funOffset=0;
			if(classEntry.getValue().functions.get("main") != null){
				continue;
			}
			// check if this class has a parent (offsets will continue from there)
			if(classEntry.getValue().parentName != null){
				// inherit offsets
				Main.Offsets offset = offsets.get(classEntry.getValue().parentName);
				if(offset!=null){
					varOffset = offset.varOffset;
					funOffset = offset.funOffset;	
				}
				else{
					// System.out.println("Parent class doesn't have offsets!");
				}
            }
            // if there is parentClass inherit Vtable
			if(classEntry.getValue().parentName != null){
                tempVtable.functions.putAll(Main.VTables.get(classEntry.getValue().parentName).functions);
                tempVtable.dataMembers.putAll(Main.VTables.get(classEntry.getValue().parentName).dataMembers);
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
			for(Map.Entry<String,Main.funStruct> funEntry : classEntry.getValue().functions.entrySet()){
				tempVtable.functions.put(funEntry.getKey(),funOffset);
				funOffset += 8;
			}
			// update offsets
			offsets.put(classEntry.getKey(),new Main.Offsets(varOffset,funOffset));
		}
    }
    
    public void printVTables(){
		// generate llvm vtable code
		for(Map.Entry<String,Main.ClassStruct> classEntry : Main.symbolTable.entrySet()){
			// get current class Vtable
			VTable tempVtable = Main.VTables.get(classEntry.getKey());
			emit("@." + classEntry.getKey() + "_vtable = global ["+tempVtable.functions.size()+" x i8*] [");
			int count = 0;
			for(Map.Entry<String,Integer> funEntry : tempVtable.functions.entrySet()){
				count++;
				// search for function in parent classes too
				Main.ClassStruct tempClass = classEntry.getValue();
				while(tempClass.functions.get(funEntry.getKey()) == null){
					tempClass = Main.symbolTable.get(tempClass.parentName);
				}
				Main.funStruct tempFun = tempClass.functions.get(funEntry.getKey());
				emit("i8* bitcast (" + llvmType(tempFun.returnType)+" (i8*");
				String argTypes = "";
				for(Map.Entry<String,String> argEntry : tempFun.args.entrySet()){
					argTypes += ","+llvmType(argEntry.getValue());
				}
				emit(argTypes+")* @"+tempClass.className+"."+tempFun.funName+" to i8*)");
				if(count!=tempVtable.functions.size()){
					// not last function, add ','
					emit(", ");
				}
			}
			emit("]\n");
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

    public void emit(String str){
        System.out.print(str);
    }

    public String findTypeOf(String id){
        String type;
        // variables declared inside function
        type = state.funSt.vars.get(id);
        if(type!=null){
            state.varOrigin = "local";
            return type;
        }
        // variables declared as function arguments
        type = state.funSt.args.get(id);
        if(type!=null){
            state.varOrigin = "argument";
            return type;
        }
        // variables declared as class datamembers
        type = state.classSt.dataMembers.get(id);
        if(type!=null){
            state.varOrigin = "class field";
            return type;
        }
        // variables declared as parentclass datamembers
        Main.ClassStruct tempClass = Main.symbolTable.get(state.classSt.parentName);
        while(tempClass!=null){
            type = tempClass.dataMembers.get(id);
            if(type!=null){
                state.varOrigin = "class field";
                return type;
            }
            tempClass = Main.symbolTable.get(tempClass.parentName);
        }
        return null;
    }


    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
    public String visit(MainClass n) throws Exception {
        String className = n.f1.accept(this);
        String argName = n.f11.accept(this);
        state.classSt = Main.symbolTable.get(className);
        state.funSt = state.classSt.functions.get("main");
        emit("define i32 @main() {\n");
        n.f14.accept(this);
        n.f15.accept(this);
        emit("\tret i32 0\n}\n\n");
        return null;
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> ( VarDeclaration() )*
    * f4 -> ( MethodDeclaration() )*
    * f5 -> "}"
    */
    public String visit(ClassDeclaration n) throws Exception{
        // get class name
        String id = n.f1.accept(this);
        state.classSt = Main.symbolTable.get(id);

        state.varOf = "function";
        n.f4.accept(this);
        return null;
    }

    /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
    */
    public String visit(ClassExtendsDeclaration n) throws Exception{
        String id = n.f1.accept(this);
        String extId = n.f3.accept(this);

        state.classSt = Main.symbolTable.get(id);

        state.varOf = "function";
        n.f6.accept(this);
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public String visit(VarDeclaration n) throws Exception{
        String type = n.f0.accept(this);
        String id = n.f1.accept(this);
        
        emit("\t%"+id+" = alloca " + llvmType(type)+"\n");
        return null;
    }

    /**
    * f0 -> "public"
    * f1 -> Type()
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( FormalParameterList() )?
    * f5 -> ")"
    * f6 -> "{"
    * f7 -> ( VarDeclaration() )*
    * f8 -> ( Statement() )*
    * f9 -> "return"
    * f10 -> Expression()
    * f11 -> ";"
    * f12 -> "}"
    */
    public String visit(MethodDeclaration n) throws Exception{
        this.registerCount=0;
        String type = n.f1.accept(this);
        String id = n.f2.accept(this);
        state.funSt = state.classSt.functions.get(id);
        emit("define "+llvmType(type)+" @"+state.classSt.className+"."+id+"(i8* %this");
        state.tempStr = "";
        n.f4.accept(this);
        emit("){\n");
        emit(state.tempStr);
        n.f7.accept(this);
        n.f8.accept(this);
        String exprString= n.f10.accept(this);
        emit("\tret "+llvmType(type)+" "+ exprString +"\n}\n\n");
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public String visit(FormalParameter n) throws Exception{
        String type = n.f0.accept(this);
        String id = n.f1.accept(this);

        emit(", "+llvmType(type)+" %."+id);
        state.tempStr += "\t%"+id+" = alloca "+llvmType(type)+"\n\tstore "+llvmType(type)+" %."+id+", "+llvmType(type)+"* %"+id+"\n";
        return null;
    }

    /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    public String visit(AssignmentStatement n) throws Exception{
        String id = n.f0.accept(this);
        String expr = n.f2.accept(this);
        String idType = findTypeOf(id);

        // local variable
        if(state.funSt.args.get(id)!=null || state.funSt.vars.get(id)!=null){
            emit("\tstore "+expr+", "+llvmType(idType)+"* %"+id+"\n");
        }
        else{
            // class field
            VTable vtable = Main.VTables.get(state.classSt.className);
            int temp = this.registerCount++; 
            int temp2 = this.registerCount++; 
            emit("\t%_"+ temp + " = getelementptr i8, i8* %this, i32 "+(vtable.dataMembers.get(id) + 8) +"\n");
            emit("\t%_"+ temp2 + " = bitcast i8* %_" + temp + " to " + llvmType(idType) + "*\n");
			emit("\tstore " + state.exprType + " " + expr + ", " + llvmType(idType) + "* %_" + temp2+"\n");
        }

        return null;
    }

    /**
     * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
    public String visit(ArrayAssignmentStatement n) throws Exception{
        String id = n.f0.accept(this);
        String expr = n.f2.accept(this);
        String expr2 = n.f5.accept(this);

        String idType = findTypeOf(id);

        String type = llvmType(idType);
        state.exprType = type;
        int temp,temp2,temp3;
        // load identifier
        if(state.varOrigin.equals("local") || state.varOrigin.equals("argument")){
            // local variable
            temp3 = this.registerCount++;
            emit("\t%_"+temp3+" = load i32*, i32** %"+id+"\n");
        }
        else{
            // class field
            VTable vtable = Main.VTables.get(state.classSt.className);
            temp = this.registerCount++;
            temp2 = this.registerCount++;
            temp3 = this.registerCount++;

            emit("\t%_"+ temp + " = getelementptr i8, i8* %this, i32 "+(vtable.dataMembers.get(id) + 8) +"\n");
            emit("\t%_"+ temp2 + " = bitcast i8* %_" + temp + " to i32**\n");
            emit("\t%_"+ temp3 + " = load i32*, i32** "+temp2+"\n");
        }

        temp = this.registerCount++;
        temp2 = this.registerCount++;

        emit("\t%_"+temp+" = load i32, i32* %_"+temp3+"\n");
        emit("\t%_"+temp2+" = icmp ult "+expr+", %_"+temp+"\n");

        int label = this.oobCount++;
        int label2 = this.oobCount++;
        int label3 = this.oobCount++;

        emit("\tbr i1 %_"+temp2+", label %oob"+label+", label %oob"+label2+"\n");
        emit("\noob"+ label + ":\n");
        temp = this.registerCount++;
        temp2 = this.registerCount++;
        emit("\t%_"+temp+" = add "+state.exprType+" "+expr+", 1\n");
        emit("\t%_"+temp2+" = getelementptr i32, i32* %_"+temp3+", i32 %_"+temp+"\n");
        emit("\tstore "+expr2+", i32* %_"+temp2+"\n");

        label = this.oobCount++;

        emit("\tbr label %oob"+label3+"\n");

        emit("oob"+label2 + ":\n");
		emit("\t" + "call void @throw_oob()\n");
		emit("\t" + "br label %oob" + label3+"\n");

		emit("oob"+label3 + ":\n"); /* continue other Statements or return */
        return null;
    }





    /**
    * f0 -> IntegerLiteral()
    *       | TrueLiteral()
    *       | FalseLiteral()
    *       | Identifier()
    *       | ThisExpression()
    *       | ArrayAllocationExpression()
    *       | AllocationExpression()
    *       | BracketExpression()
    */
    public String visit(PrimaryExpression n) throws Exception {
        String expr = n.f0.accept(this);
        // int
        if(n.f0.which == 0){
            state.exprType = "i32";
            return expr;
        }
        // boolean
        else if(n.f0.which == 1 || n.f0.which == 2){
            state.exprType = "i8";
            return expr;
        }
        // id
        else if(n.f0.which == 3){
            String type = findTypeOf(expr);
            type = llvmType(type);
            state.exprType = type;
            // local variable
            if(state.varOrigin.equals("local") || state.varOrigin.equals("argument")){
                int tempReg = this.registerCount++;
                emit("\t%_"+tempReg+" = load "+type+", "+type+"* %"+expr+"\n");
                return "%_"+tempReg;
            }
            else{
                // class field
                VTable vtable = Main.VTables.get(state.classSt.className);
                int temp = this.registerCount++;
                int temp2 = this.registerCount++;
                emit("\t%_"+ temp + " = getelementptr i8, i8* %this, i32 "+(vtable.dataMembers.get(expr) + 8) +"\n");
                emit("\t%_"+ temp2 + " = bitcast i8* %_" + temp + " to " + type + "*\n");
                emit("\t%_"+temp+" = load "+type+", "+type+"* %_"+temp2+"\n");
                return "%_"+temp;
            }
            
        }
        return null;
    }

    /**
    * f0 -> <INTEGER_LITERAL>
    */
    public String visit(IntegerLiteral n) throws Exception{
        return n.f0.toString();
    }

    /**
     * f0 -> "true"
    */
    public String visit(TrueLiteral n) throws Exception{
        return new String("1");
    }

    /**
     * f0 -> "false"
    */
    public String visit(FalseLiteral n) throws Exception{
        return new String("0");
    }

    /**
     * f0 -> <IDENTIFIER>
    */
    public String visit(Identifier n) throws Exception{
        return n.f0.toString();
    }

    /**
     * f0 -> "this"
    */
    public String visit(ThisExpression n) throws Exception{
        return new String("this");
    }

     /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    public String visit(ArrayType n) throws Exception{
        return new String("int[]");
    }

    /**
    * f0 -> "boolean"
    */
    public String visit(BooleanType n) throws Exception{
        return new String("boolean");
    }

    /**
     * f0 -> "int"
    */
    public String visit(IntegerType n) throws Exception{
        return new String("int");
    }
}