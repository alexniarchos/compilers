import syntaxtree.*;
import visitor.GJNoArguDepthFirst;
import java.lang.System;
import java.util.Map.Entry;
import java.util.ArrayList;

public class fillSTVisitor extends GJNoArguDepthFirst<String>{

    public class State{
        public Main.ClassStruct classSt;
        public Main.funStruct funSt;
        public String varOf;
        public Integer argCount;
    }

    public State state = new State();
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
        String mainArg = n.f11.accept(this);
        
        Main.ClassStruct tempClass = new Main.ClassStruct(className,null);
        Main.funStruct tempFun = new Main.funStruct("void","main");
        tempFun.args.put("String[]",mainArg);
        tempClass.functions.put("main",tempFun);
        
        Main.symbolTable.put(className,tempClass);
        state.classSt = tempClass;
        state.funSt = tempFun;
        state.varOf = "function";
        n.f14.accept(this);
        return null;
    } 

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public String visit(VarDeclaration n) throws Exception {
        String type = n.f0.accept(this);
        String id = n.f1.accept(this);
        Main.ClassStruct classST = state.classSt;
        if(state.varOf.equals("function")){
            // add var to function
            Main.funStruct fun = state.funSt;
            // check for duplicates
            if(fun.vars.get(id) == null && fun.args.get(id) == null){
                // unique
                fun.vars.put(id,type);
            }
            else{
                throw new Exception("VarDeclaration: Variable " + id + " has already been declared");
            }
        }
        else if(state.varOf.equals("class")){
            // add var to class
            // check for duplicates
            if(classST.dataMembers.get(id) == null){
                // unique
                classST.dataMembers.put(id,type);
            }
            else{
                throw new Exception("VarDeclaration: Variable " + id + " has already been declared");
            }
        }
        else{
            throw new Exception("VarDeclaration: VarOf = " + state.varOf);
        }
        return null;
    }

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
    public String visit(FormalParameter n) throws Exception{
        String type = n.f0.accept(this);
        String id = n.f1.accept(this);

        Main.ClassStruct tempClass = state.classSt;
        Main.funStruct tempFun = state.funSt;

        if(tempFun.overridesFun != null){
            ArrayList<Entry<String, String>> arglist = new ArrayList<Entry<String,String>> (tempFun.args.entrySet()); 
            if(state.argCount < arglist.size() && !arglist.get(state.argCount).getValue().equals(type)){
                throw new Exception("FormalParameter: Function: "+tempFun.funName+ " found: "+type+" ,expected: "+arglist.get(state.argCount).getValue());
            }
            tempFun.args.put(id,type);
            state.argCount++;
        }
        else{
            if(tempFun.args.get(id) != null){
                throw new Exception("FormalParameter: Function: "+tempFun.funName+" argument: " + id + " already exists!");
            }
            tempFun.args.put(id,type);
            state.argCount++;
        }
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
        String type = n.f1.accept(this);
        String id = n.f2.accept(this);

        // get class
        Main.ClassStruct tempClass = state.classSt;
        if(tempClass.functions.get(id) != null){
            throw new Exception("MethodDeclaration: function: " + tempClass.functions.get(id).funName + " already exists!");
        }

        Main.funStruct tempFun = new Main.funStruct(type, id);

        // check that if function overrides, then it overrides correctly
        Main.ClassStruct parentClass = Main.symbolTable.get(tempClass.parentName);
        while(parentClass != null){
            // check if function exists in parentClass
            if(parentClass.functions.get(id) != null){
                tempFun.overridesFun = parentClass.functions.get(id);
                break;
            }
            parentClass = Main.symbolTable.get(parentClass.parentName);
        }

        
        tempClass.functions.put(id,tempFun);
        state.funSt = tempFun;

        // add parameters to function
        state.argCount = 0;
        n.f4.accept(this);
        if(tempFun.overridesFun != null){
            // check argCount 
            if(state.argCount != tempFun.overridesFun.args.size()){
                throw new Exception("MessageSend: function "+tempFun.funName+" argument count found "+state.argCount+" expected "+tempFun.overridesFun.args.size());
            }
        }
       
        // add variables
        n.f7.accept(this);

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
    public String visit(ClassDeclaration n) throws Exception {
        // get class name
        String id = n.f1.accept(this);
        // check duplicate class name
        if(Main.symbolTable.get(id)!=null){
            throw new Exception("ClassDeclaration: Class "+id+" has already been declared");
        }
        Main.ClassStruct tempClass = new Main.ClassStruct(id,null);
        Main.symbolTable.put(id,tempClass);
        state.classSt = tempClass;
        state.varOf = "class";

        n.f3.accept(this);
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

        // check duplicates
        if(Main.symbolTable.get(id)!=null){
            throw new Exception("ClassExtendsDeclaration: class: "+id+" already exists");
        }

        // parent must be declared before extend
        if(Main.symbolTable.get(extId)==null){
            throw new Exception("ClassExtendsDeclaration: parent class: "+extId+" hasn't been declared before");
        }

        Main.ClassStruct tempClass = new Main.ClassStruct(id, extId);
        Main.symbolTable.put(id, tempClass);
        state.classSt = tempClass;
        state.varOf = "class";
        n.f5.accept(this);
        state.varOf = "function";
        n.f6.accept(this);
        return null;
    }

    /**
    * f0 -> <IDENTIFIER>
    */
    public String visit(Identifier n) {
        return n.f0.toString();
    }

    /**
    * f0 -> "int"
    * f1 -> "["
    * f2 -> "]"
    */
    public String visit(ArrayType n) {
        return new String("int[]");
    }

    /**
    * f0 -> "boolean"
    */
    public String visit(BooleanType n) {
        return new String("boolean");
    }

    /**
     * f0 -> "int"
    */
    public String visit(IntegerType n) {
        return new String("int");
    }

}