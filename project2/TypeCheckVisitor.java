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
    public String visit(MainClass n) {
        String id = n.f1.accept(this);

        String arg = n.f11.accept(this);

        state.classSt = Main.symbolTable.get(id);
        state.funSt = state.classSt.functions.get("main");
        n.f14.accept(this);
        n.f15.accept(this);

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
        // check that type exists
        if(Main.symbolTable.get(type)==null && !type.equals("int") && !type.equals("boolean") && !type.equals("int[]")){
            throw new RuntimeException("Type: "+type+" doesn't exist");
        }
        return null;
    }

    /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
    public String visit(AssignmentStatement n) {
        String id = n.f0.accept(this);
        String exprType = n.f2.accept(this);
        if(state.funSt.vars.get(id)==null){
            throw new RuntimeException("Variable: "+id+" hasn't been declared");
        }
        // todo: if id type is subclass of expr type then allow it
        if(!state.funSt.vars.get(id).equals(exprType)){
            throw new RuntimeException("Types: "+state.funSt.vars.get(id)+" and "+exprType+" don't match");
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
    public String visit(ArrayAssignmentStatement n) {
        String id = n.f0.accept(this);
        if(state.funSt.vars.get(id)==null){
            throw new RuntimeException("Variable: "+id+" hasn't been declared");
        }
        if(!state.funSt.vars.get(id).equals("int[]")){
            throw new RuntimeException("Variable: "+id+" should be of type int[], found "+state.funSt.vars.get(id));
        }
        String exprType = n.f2.accept(this);
        if(!state.funSt.vars.get(id).equals("int")){
            throw new RuntimeException("Types: "+state.funSt.vars.get(id)+" and "+exprType+" don't match");
        }
        String exprType2 = n.f5.accept(this);
        if(!state.funSt.vars.get(id).equals("int")){
            throw new RuntimeException("Types: "+state.funSt.vars.get(id)+" and "+exprType2+" don't match");
        }
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
    public String visit(IfStatement n) {
        String exprType = n.f2.accept(this);
        if(!exprType.equals("boolean")){
            throw new RuntimeException("Expression: should be of type boolean, found: "+exprType);
        }
        n.f4.accept(this);
        
        n.f6.accept(this);
        return null;
    }

    /**
     * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
    public R visit(WhileStatement n) {
        R _ret=null;
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(this);
        n.f3.accept(this);
        n.f4.accept(this);
        return _ret;
    }

    /**
     * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public R visit(PrintStatement n) {
        R _ret=null;
        n.f0.accept(this);
        n.f1.accept(this);
        n.f2.accept(this);
        n.f3.accept(this);
        n.f4.accept(this);
        return _ret;
    }
}