import syntaxtree.*;
import visitor.GJNoArguDepthFirst;
import java.lang.System;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;

public class Minijava_to_LLVM_visitor extends GJNoArguDepthFirst<exprInfo>{

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
        public String tempStr;
        public String exprType;
        public String varOrigin;
        public ArrayList<String> args; 
        public ArrayList<Integer> argsCount;
    }

    public State state = new State();

    public Minijava_to_LLVM_visitor(){
        fillVTables();
        printVTables();

        state.args = new ArrayList<String>();
        state.argsCount = new ArrayList<Integer>();

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
    public exprInfo visit(MainClass n) throws Exception {
        exprInfo className = n.f1.accept(this);
        state.classSt = Main.symbolTable.get(className.value);
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
    public exprInfo visit(ClassDeclaration n) throws Exception{
        // get class name
        exprInfo id = n.f1.accept(this);
        state.classSt = Main.symbolTable.get(id.value);

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
    public exprInfo visit(ClassExtendsDeclaration n) throws Exception{
        exprInfo id = n.f1.accept(this);
        exprInfo extId = n.f3.accept(this);

        state.classSt = Main.symbolTable.get(id.value);

        state.varOf = "function";
        n.f6.accept(this);
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public exprInfo visit(VarDeclaration n) throws Exception{
        exprInfo type = n.f0.accept(this);
        exprInfo id = n.f1.accept(this);
        
        emit("\t%"+id.value+" = alloca " + llvmType(type.value)+"\n");
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
    public exprInfo visit(MethodDeclaration n) throws Exception{
        this.registerCount=0;
        exprInfo type = n.f1.accept(this);
        exprInfo id = n.f2.accept(this);
        state.funSt = state.classSt.functions.get(id.value);
        emit("define "+llvmType(type.value)+" @"+state.classSt.className+"."+id.value+"(i8* %this");
        state.tempStr = "";
        n.f4.accept(this);
        emit("){\n");
        emit(state.tempStr);
        n.f7.accept(this);
        n.f8.accept(this);
        exprInfo exprString= n.f10.accept(this);
        emit("\tret "+llvmType(type.value)+" "+ exprString.value +"\n}\n\n");
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public exprInfo visit(FormalParameter n) throws Exception{
        exprInfo type = n.f0.accept(this);
        exprInfo id = n.f1.accept(this);

        emit(", "+llvmType(type.value)+" %."+id.value);
        state.tempStr += "\t%"+id.value+" = alloca "+llvmType(type.value)+"\n\tstore "+llvmType(type.value)+" %."+id.value+", "+llvmType(type.value)+"* %"+id.value+"\n";
        return null;
    }

    /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    public exprInfo visit(AssignmentStatement n) throws Exception{
        exprInfo id = n.f0.accept(this);
        exprInfo expr = n.f2.accept(this);
        String idType = findTypeOf(id.value);

        // local variable
        if(state.funSt.args.get(id.value)!=null || state.funSt.vars.get(id.value)!=null){
            emit("\tstore "+expr.type+" "+expr.value+", "+llvmType(idType)+"* %"+id.value+"\n");
        }
        else{
            // class field
            VTable vtable = Main.VTables.get(state.classSt.className);
            int temp = this.registerCount++; 
            int temp2 = this.registerCount++; 
            emit("\t%_"+ temp + " = getelementptr i8, i8* %this, i32 "+(vtable.dataMembers.get(id.value) + 8) +"\n");
            emit("\t%_"+ temp2 + " = bitcast i8* %_" + temp + " to " + llvmType(idType) + "*\n");
			emit("\tstore " + expr.type+" "+expr.value + ", " + llvmType(idType) + "* %_" + temp2+"\n");
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
    public exprInfo visit(ArrayAssignmentStatement n) throws Exception{
        exprInfo id = n.f0.accept(this);
        exprInfo expr = n.f2.accept(this);
        exprInfo expr2 = n.f5.accept(this);

        String idType = findTypeOf(id.value);

        String type = llvmType(idType);
        int temp,temp2,temp3;
        // load identifier
        if(state.varOrigin.equals("local") || state.varOrigin.equals("argument")){
            // local variable
            temp3 = this.registerCount++;
            emit("\t%_"+temp3+" = load i32*, i32** %"+id.value+"\n");
        }
        else{
            // class field
            VTable vtable = Main.VTables.get(state.classSt.className);
            temp = this.registerCount++;
            temp2 = this.registerCount++;
            temp3 = this.registerCount++;

            emit("\t%_"+ temp + " = getelementptr i8, i8* %this, i32 "+(vtable.dataMembers.get(id.value) + 8) +"\n");
            emit("\t%_"+ temp2 + " = bitcast i8* %_" + temp + " to i32**\n");
            emit("\t%_"+ temp3 + " = load i32*, i32** "+temp2+"\n");
        }

        temp = this.registerCount++;
        temp2 = this.registerCount++;

        emit("\t%_"+temp+" = load i32, i32* %_"+temp3+"\n");
        emit("\t%_"+temp2+" = icmp ult "+expr.type+" "+expr.value+", %_"+temp+"\n");

        int label = this.oobCount++;
        int label2 = this.oobCount++;
        int label3 = this.oobCount++;

        emit("\tbr i1 %_"+temp2+", label %oob"+label+", label %oob"+label2+"\n");
        emit("\noob"+ label + ":\n");
        temp = this.registerCount++;
        temp2 = this.registerCount++;
        emit("\t%_"+temp+" = add "+expr.type+" "+expr.value+", 1\n");
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
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
    public exprInfo visit(IfStatement n) throws Exception{
        exprInfo expr = n.f2.accept(this);
        int label1 = this.ifCount++;
        int label2 = this.ifCount++;
        int label3 = this.ifCount++;

        emit("\tbr "+expr.type+" "+expr.value+", label %if"+label1+", label %if"+label2+"\n");
        emit("if"+label1+":\n");

        n.f4.accept(this);
        emit("\tbr label %if"+label3+"\n");
        emit("if"+label2+":\n");
        n.f6.accept(this);
        emit("\tbr label %if"+label3+"\n");
        emit("if"+label3+":\n");
        return null;
    }

    /**
     * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    public exprInfo visit(WhileStatement n) throws Exception{
        int label1 = this.loopCount++;
        int label2 = this.loopCount++;
        int label3 = this.loopCount++;

        emit("\tbr label %loopstart"+label1+"\n");
        emit("loopstart"+label1+":\n");
        exprInfo expr = n.f2.accept(this);
        emit("\tbr "+expr.type+" "+expr.value+", label %next"+label2+", label %end"+label3+"\n");
        emit("next"+label2+":\n");
        n.f4.accept(this);
        emit("\tbr label %loopstart"+label1+"\n");
        emit("end"+label3+":\n");
        return null;
    }

    /**
     * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public exprInfo visit(PrintStatement n) throws Exception{
        exprInfo expr = n.f2.accept(this);
        emit("\tcall void (i32) @print_int(" + expr.type+" "+expr.value +")\n");
        return null;
    }

    /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
    public exprInfo visit(AndExpression n) throws Exception {
        exprInfo t1 = n.f0.accept(this);

        int label1 = this.andclauseCount++;
        int label2 = this.andclauseCount++;
        int label3 = this.andclauseCount++;
        int label4 = this.andclauseCount++;
        emit("\tbr label %andclause"+label1+"\n");
        emit("andclause"+label1+":\n");
        emit("\tbr "+t1.type+" "+t1.value+", label %andclause"+label2+", label %andclause"+label4+"\n");
        emit("andclause"+label2+":\n");
        exprInfo t2 = n.f2.accept(this);
        emit("\tbr label %andclause"+label3+"\n");
        emit("andclause"+label3+":\n");
        emit("\tbr label %andclause"+label4+"\n");
        emit("andclause"+label4+":\n");
        int temp = this.registerCount++;
        emit("\t%_"+temp+" = phi i1 [ 0, %andclause" + label1 + " ], [ " + t2.value +", %andclause" + label3 + " ]\n");
        return new exprInfo("i1","%_"+temp);
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    public exprInfo visit(CompareExpression n) throws Exception {
        exprInfo t1 = n.f0.accept(this);
        exprInfo t2 = n.f2.accept(this);
        int temp = this.registerCount++;
        emit("\t%_"+temp+" = icmp slt "+t1.type+" "+t1.value+", "+t2.value+"\n");
        return new exprInfo("i1","%_"+temp);
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    public exprInfo visit(PlusExpression n) throws Exception {
        exprInfo t1 = n.f0.accept(this);
        exprInfo t2 = n.f2.accept(this);
        int temp = this.registerCount++;
        emit("\t%_"+temp+" = add "+t1.type+" "+t1.value+", "+t2.value+"\n");
        return new exprInfo("i32","%_"+temp);
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    public exprInfo visit(MinusExpression n) throws Exception {
        exprInfo t1 = n.f0.accept(this);
        exprInfo t2 = n.f2.accept(this);
        int temp = this.registerCount++;
        emit("\t%_"+temp+" = sub "+t1.type+" "+t1.value+", "+t2.value+"\n");
        return new exprInfo("i32","%_"+temp);
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    public exprInfo visit(TimesExpression n) throws Exception {
        exprInfo t1 = n.f0.accept(this);
        exprInfo t2 = n.f2.accept(this);
        int temp = this.registerCount++;
        emit("\t%_"+temp+" = mul "+t1.type+" "+t1.value+", "+t2.value+"\n");
        return new exprInfo("i32","%_"+temp);
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public exprInfo visit(ArrayLookup n) throws Exception {
        exprInfo t1 = n.f0.accept(this);
        exprInfo t2 = n.f2.accept(this);
        int temp1 = this.registerCount++;
        int temp2 = this.registerCount++;
        int temp3 = this.registerCount++;
        int temp4 = this.registerCount++;
        int temp5 = this.registerCount++;

        int label1 = this.oobCount++;
        int label2 = this.oobCount++;
        int label3 = this.oobCount++;

        emit("\t%_"+temp1+" = load i32, i32* "+t1.value+"\n");
        emit("\t%_"+temp2+" = icmp ult i32 "+t2.value+", "+temp1+"\n");
        emit("\tbr i1 "+temp2+", label %"+label1+", label %"+label2+"\n");
        emit(label1+":\n");
        emit("\t%_"+temp3+" = add i32 "+t2.value+", 1\n");
        emit("\t%_"+temp4+" = getelementptr i32, i32* "+t1.value+", i32 "+temp3+"\n");
        emit("\t%_"+temp5+" = load i32, i32* %_"+temp4+"\n");
        emit("\tbr label %"+label3+"\n");
        emit(label2+":\n");
        emit("\tcall void @throw_oob\n");
        emit("\tbr label %"+label3+"\n");
        emit(label3+":\n");

        return new exprInfo("i32","%_"+temp5);
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    public exprInfo visit(ArrayLength n) throws Exception {
        exprInfo t1 = n.f0.accept(this);
        int temp = this.registerCount++;
        emit("%_"+temp+" = load i32, i32* "+t1.value+"\n");
        return new exprInfo("i32","%_"+temp);
    }

    /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    public exprInfo visit(MessageSend n) throws Exception {
        exprInfo primaryExpr = n.f0.accept(this);
        exprInfo funId = n.f2.accept(this);

        int temp1 = this.registerCount++;
        int temp2 = this.registerCount++;
        int temp3 = this.registerCount++;
        int temp4 = this.registerCount++;
        int temp5 = this.registerCount++;
        int temp6 = this.registerCount++;

        int offset = Main.VTables.get(primaryExpr.originalType).functions.get(funId.value);
        emit("\t%_"+temp1+" = bitcast "+primaryExpr.value+" to i8***\n");
        emit("\t%_"+temp2+" = load i8**, i8*** %_"+temp1+"\n");
        emit("\t%_"+temp3+" = getelementptr i8*, i8** %_"+temp2+", i32 "+offset/8+"\n");
        emit("\t%_"+temp4+" = load i8*, i8** %_"+temp3+" to i8***\n");

        // get return type
        Main.ClassStruct tempClass = Main.symbolTable.get(primaryExpr.originalType);
        Main.funStruct tempFun = tempClass.functions.get(funId.value);
        if(tempFun == null){
            // check parent classes
            tempClass = Main.symbolTable.get(tempClass.parentName); 
            while(tempClass!=null){
                tempFun = tempClass.functions.get(funId.value);
                if(tempFun!=null){
                    break;
                }
                tempClass = Main.symbolTable.get(tempClass.parentName);
            }
            if(tempFun == null){
                // couldnt find function
                throw new Exception("MessageSend: function "+funId+" hasn't been declared");
            }
        }
        if(tempFun == null){
            // couldnt find function
            throw new Exception("MessageSend: function "+funId+" hasn't been declared");
        }

        emit("\t%_"+temp5+" = bitcast i8* %_"+temp4+" to "+llvmType(tempFun.returnType)+" (i8*");
        
        ArrayList<Entry<String, String>> argTypelist = new ArrayList<Entry<String,String>> (tempFun.args.entrySet()); 
        for(int i=0; i < argTypelist.size(); i++){
            emit(", "+llvmType(argTypelist.get(i).getValue()));
        }
        emit(")*\n");

        exprInfo arglist = n.f4.accept(this);

        emit("\t%_"+temp6+" = call "+llvmType(tempFun.returnType)+" %_"+temp5+"( "+ arglist.arglist + " )\n");

        return new exprInfo(llvmType(tempFun.returnType),"%_"+temp6);
    }

    /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public exprInfo visit(ExpressionList n) throws Exception {
        exprInfo expr = n.f0.accept(this);
        state.argsCount.add(0);
        n.f1.accept(this);
        exprInfo args = new exprInfo();
        // System.out.println(state.args);
        // System.out.println(state.argsCount);
        args.arglist = expr.type + " " + expr.value;
        String temp = "";
        for(int i=0; i<state.argsCount.get(state.argsCount.size()-1); i++){
            temp = ", " + state.args.get(state.args.size()-1) + temp;
            state.args.remove(state.args.size()-1);
        }
        state.argsCount.remove(state.argsCount.size()-1);
        args.arglist += temp;
        return args;
    }

    /**
    * f0 -> ","
    * f1 -> Expression()
    */
    public exprInfo visit(ExpressionTerm n) throws Exception {
        exprInfo expr = n.f1.accept(this);
        state.args.add(expr.type+" "+expr.value);
        int temp = state.argsCount.get(state.argsCount.size()-1);
        state.argsCount.set(state.argsCount.size()-1,temp+1);
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
    public exprInfo visit(PrimaryExpression n) throws Exception {
        exprInfo expr = n.f0.accept(this);
        // int
        if(n.f0.which == 0){
            return new exprInfo("i32",expr.value,"int");
        }
        // boolean
        else if(n.f0.which == 1 || n.f0.which == 2){
            return new exprInfo("i8",expr.value,"boolean");
        }
        // id
        else if(n.f0.which == 3){
            String type = findTypeOf(expr.value);
            String originalType = type;
            type = llvmType(type);
            // local variable
            if(state.varOrigin.equals("local") || state.varOrigin.equals("argument")){
                int tempReg = this.registerCount++;
                emit("\t%_"+tempReg+" = load "+type+", "+type+"* %"+expr.value+"\n");
                return new exprInfo(type,"%_"+tempReg,originalType);
            }
            else{
                // class field
                VTable vtable = Main.VTables.get(state.classSt.className);
                int temp = this.registerCount++;
                int temp2 = this.registerCount++;
                emit("\t%_"+ temp + " = getelementptr i8, i8* %this, i32 "+(vtable.dataMembers.get(expr.value) + 8) +"\n");
                emit("\t%_"+ temp2 + " = bitcast i8* %_" + temp + " to " + type + "*\n");
                emit("\t%_"+temp+" = load "+type+", "+type+"* %_"+temp2+"\n");
                return new exprInfo(type,"%_"+temp,originalType);
            }
        }
        return null;
    }

    /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public exprInfo visit(ArrayAllocationExpression n) throws Exception {
        exprInfo expr = n.f3.accept(this);

        int temp1 = this.registerCount++;
        int temp2 = this.registerCount++;
        int temp3 = this.registerCount++;
        int temp4 = this.registerCount++;

        int label1 = this.arr_allocCount++;
        int label2 = this.arr_allocCount++;

        emit("\t%_"+temp1+" = icmp slt "+expr.type +" "+ expr.value+ ", 0\n");
        emit("\tbr i1 %_"+temp1+", label %"+label1+", label %"+label2+"\n");
        emit(label1+":\n");
        emit("\tcall void @throw_oob()\n");
        emit("\tbr label %"+label2+"\n");
        emit(label2+":\n");
        emit("\t%_"+temp2+" = add "+expr.type +" "+ expr.value+", 1\n");
        emit("\t%_"+temp3+" = call i8* @calloc(i32 4, i32 %_"+temp2+")\n");
        emit("\t%_"+temp4+" = bitcast i8* %_"+temp3+" to i32*\n");
        emit("\tstore "+expr.type +" "+ expr.value+", i32* %_"+temp4+"\n");

        return new exprInfo("i32*","%_"+temp4,"int[]");
    }

    /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public exprInfo visit(AllocationExpression n) throws Exception {
        exprInfo expr = n.f1.accept(this);

        int temp1 = this.registerCount++;
        int temp2 = this.registerCount++;
        int temp3 = this.registerCount++;

        ArrayList<Entry<String, Integer>> fields = new ArrayList<Entry<String,Integer>> (Main.VTables.get(expr.value).dataMembers.entrySet()); 
        int fieldsOffset = fields.get(fields.size()-1).getValue();
        emit("\t%_"+temp1+" = call i8* @calloc(i32 1, i32 "+ Integer.toString(fieldsOffset+8)+")\n");
        emit("\t%_" + temp2 + " = bitcast i8* %_" + temp1 + " to i8***\n");
        int numOfFunctions = Main.VTables.get(expr.value).functions.size();
        emit("\t%_" + temp3 + " = getelementptr [" + numOfFunctions + " x i8*], ["+ numOfFunctions + " x i8*]* @." + expr.value + "_vtable, i32 0, i32 0\n");
		emit("\tstore i8** %_" + temp3 + ", i8*** " + temp2+"\n");
        return new exprInfo("i8*",expr.value,expr.value);
    }

    /**
    * f0 -> "!"
    * f1 -> Clause()
    */
    public exprInfo visit(NotExpression n) throws Exception {
        exprInfo expr = n.f1.accept(this);
        int temp = this.registerCount++;
        emit("\t%_"+temp+" = xor i1 1, "+expr.value+"\n");
        return new exprInfo("i1","%_"+temp,expr.value);
    }

    /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    public exprInfo visit(BracketExpression n) throws Exception {
        return n.f1.accept(this);
    }



    /**
    * f0 -> <INTEGER_LITERAL>
    */
    public exprInfo visit(IntegerLiteral n) throws Exception{
        return new exprInfo("i32",n.f0.toString());
    }

    /**
     * f0 -> "true"
    */
    public exprInfo visit(TrueLiteral n) throws Exception{
        return new exprInfo("i1","1");
    }

    /**
     * f0 -> "false"
    */
    public exprInfo visit(FalseLiteral n) throws Exception{
        return new exprInfo("i1","0");
    }

    /**
     * f0 -> <IDENTIFIER>
    */
    public exprInfo visit(Identifier n) throws Exception{
        return new exprInfo(null,n.f0.toString());
    }

    /**
     * f0 -> "this"
    */
    public exprInfo visit(ThisExpression n) throws Exception{
        return new exprInfo("i8*","%this");
    }

     /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    public exprInfo visit(ArrayType n) throws Exception{
        return new exprInfo(null,"int[]");
    }

    /**
    * f0 -> "boolean"
    */
    public exprInfo visit(BooleanType n) throws Exception{
        return new exprInfo(null,"boolean");
    }

    /**
     * f0 -> "int"
    */
    public exprInfo visit(IntegerType n) throws Exception{
        return new exprInfo(null,"int");
    }
}