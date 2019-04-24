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


}