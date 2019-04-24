import syntaxtree.*;
import visitor.GJNoArguDepthFirst;
import java.lang.System;

public class fillSTVisitor extends GJNoArguDepthFirst<String>{

    public class State{
        public String className;
        public String funName;
        public String varOf;
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
    public String visit(MainClass n) {
        String className = n.f1.accept(this);
        String mainArg = n.f11.accept(this);
        
        Main.ClassStruct tempClass = new Main.ClassStruct(className,null);
        Main.funStruct tempFun = new Main.funStruct("void","main");
        tempFun.args.put("String[]",mainArg);
        tempClass.functions.put("main",tempFun);
        
        Main.symbolTable.put(className,tempClass);
        state.className = className;
        state.funName = "main";
        state.varOf = "function";
        n.f14.accept(this);
        return null;
    } 

    /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
    public String visit(VarDeclaration n) {
        String type = n.f0.accept(this);
        String id = n.f1.accept(this);
        Main.ClassStruct classST = Main.symbolTable.get(state.className);
        if(state.varOf.equals("function")){
            // add var to function
            Main.funStruct fun = classST.functions.get(state.funName);
            // check for duplicates
            if(fun.vars.get(id) == null){
                // unique
                fun.vars.put(id,type);
            }
        }
        else if(state.varOf.equals("class")){
            // add var to class
            // check for duplicates
            if(classST.dataMembers.get(id) == null){
                // unique
                classST.dataMembers.put(id,type);
            }
        }

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
    public String visit(ClassDeclaration n) {
        // get class name
        String id = n.f1.accept(this);

        Main.ClassStruct tempClass = new Main.ClassStruct(id,null);
        Main.symbolTable.put(id,tempClass);
        state.className = id;
        state.varOf = "class";

        n.f3.accept(this);
        state.varOf = "function";
        n.f4.accept(this);
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
    public String visit(MethodDeclaration n) {
        String type = n.f1.accept(this);
        String id = n.f2.accept(this);
        state.funName = id;

        n.f4.accept(this);
        n.f5.accept(this);
        n.f6.accept(this);
        n.f7.accept(this);
        n.f8.accept(this);
        n.f9.accept(this);
        n.f10.accept(this);
        n.f11.accept(this);
        n.f12.accept(this);
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