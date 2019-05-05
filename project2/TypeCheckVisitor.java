import syntaxtree.*;
import visitor.GJNoArguDepthFirst;
import java.lang.System;
import java.util.Map.Entry;
import java.util.ArrayList;
import java.util.Map;

public class TypeCheckVisitor extends GJNoArguDepthFirst<String>{

    public class State{
        public Main.ClassStruct classSt;
        public Main.funStruct funSt;
        public Main.funStruct messageSendFun;
        public String varOf;
        public Integer argCount;
    }

    public State state = new State();

    public String findTypeOf(String id){
        String type;
        // variables declared inside function
        type = state.funSt.vars.get(id);
        if(type!=null){
            return type;
        }
        // variables declared as function arguments
        type = state.funSt.args.get(id);
        if(type!=null){
            return type;
        }
        // variables declared as class datamembers
        type = state.classSt.dataMembers.get(id);
        if(type!=null){
            return type;
        }
        // variables declared as parentclass datamembers
        Main.ClassStruct tempClass = Main.symbolTable.get(state.classSt.parentName);
        while(tempClass!=null){
            type = tempClass.dataMembers.get(id);
            if(type!=null){
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
    public String visit(MainClass n) throws Exception{
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
    public String visit(VarDeclaration n) throws Exception{
        String type = n.f0.accept(this);
        String id = n.f1.accept(this);
        // check that type exists
        if(Main.symbolTable.get(type)==null && !type.equals("int") && !type.equals("boolean") && !type.equals("int[]")){
            throw new Exception("VarDeclaration: Type: "+type+" doesn't exist");
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

        // check that type exists
        if(Main.symbolTable.get(type)==null && !type.equals("int") && !type.equals("boolean") && !type.equals("int[]")){
            throw new Exception("FormalParameter: Type: "+type+" doesn't exist");
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
        state.funSt = state.classSt.functions.get(id);

        n.f4.accept(this);
        n.f7.accept(this);
        n.f8.accept(this);
        String exprType = n.f10.accept(this);

        if(!type.equals(exprType)){
            Main.ClassStruct tempClass = Main.symbolTable.get(state.classSt.parentName);
            while(tempClass!=null){
                if(tempClass.equals(exprType)){
                    return type;
                }
                tempClass = Main.symbolTable.get(tempClass.parentName);
            }
        }
        else{
            return type;
        }
        throw new Exception("MethodDeclaration: Function: "+state.funSt.funName+" return type found: "+exprType+" expected: "+type);
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

        state.classSt = Main.symbolTable.get(id);
        state.varOf = "class";
        n.f5.accept(this);
        state.varOf = "function";
        n.f6.accept(this);
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
        String exprType = n.f2.accept(this);
        String idType = findTypeOf(id);
        if(idType == null){
            throw new Exception("AssignmentStatement: Variable: "+id+" hasn't been declared");
        }
        if(!idType.equals(exprType)){
            Main.ClassStruct tempClass = Main.symbolTable.get(exprType); 
            if(tempClass == null){
                throw new Exception("AssignmentStatement: Types: id = "+id+", type = "+idType+" and "+exprType+" don't match");
            }
            tempClass = Main.symbolTable.get(tempClass.parentName);
            while(tempClass!=null){
                if(idType.equals(tempClass.className)){
                    return null;
                }
                tempClass = Main.symbolTable.get(tempClass.parentName);
            }
            throw new Exception("AssignmentStatement: Types: id = "+id+", type = "+idType+" and "+exprType+" don't match");
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

        String t = findTypeOf(id);

        if(t!=null){
            if(t.equals("int[]")){
                if(expr.equals("int")){
                    if(expr2.equals("int")){
                        // yes
                        return null;
                    }
                    else{
                        throw new Exception("ArrayAssignmentStatement: Types: t1 = "+t+" and expr2 = "+expr2+" don't match");
                    }
                }
                else{
                    throw new Exception("ArrayAssignmentStatement: Types: t1 = "+t+" and expr = "+expr+" don't match");
                }
            }
            else{
                throw new Exception("ArrayAssignmentStatement: Variable: "+id+" should be of type int[], found "+t);
            }
        }
        else{
            throw new Exception("ArrayAssignmentStatement: Variable: "+id+" hasn't been declared");
        }
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
    public String visit(IfStatement n) throws Exception{
        String exprType = n.f2.accept(this);
        if(!exprType.equals("boolean")){
            throw new Exception("IfStatement: should be of type boolean, found: "+exprType);
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
    public String visit(WhileStatement n) throws Exception{
        String exprType = n.f2.accept(this);
        if(!exprType.equals("boolean")){
            throw new Exception("WhileStatement: should be of type boolean, found: "+exprType);
        }
        n.f4.accept(this);
        return null;
    }

    /**
     * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
    public String visit(PrintStatement n) throws Exception{
        String exprType = n.f2.accept(this);
        if(!exprType.equals("int") && !exprType.equals("boolean")){
            throw new Exception("PrintStatement: should be of type int or boolean, found: "+exprType);
        }
        return null;
    }

    /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
    public String visit(AndExpression n) throws Exception {
        String t1 = n.f0.accept(this);
        String t2 = n.f2.accept(this);
        if(t1.equals("boolean") && t2.equals("boolean")){
            return new String("boolean");
        }
        throw new Exception("AndExpression: t1 = "+t1+", t2 = "+t2+" should be both boolean");
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
    public String visit(CompareExpression n) throws Exception {
        String t1 = n.f0.accept(this);
        String t2 = n.f2.accept(this);
        if(t1.equals("int") && t2.equals("int")){
            return new String("boolean");
        }
        throw new Exception("CompareExpression: t1 = "+t1+", t2 = "+t2+" should be both int");
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
    public String visit(PlusExpression n) throws Exception {
        String t1 = n.f0.accept(this);
        String t2 = n.f2.accept(this);
        if(t1.equals("int") && t2.equals("int")){
            return new String("int");
        }
        throw new Exception("PlusExpression: t1 = "+t1+", t2 = "+t2+" should be both int");
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
    public String visit(MinusExpression n) throws Exception {
        String t1 = n.f0.accept(this);
        String t2 = n.f2.accept(this);
        if(t1.equals("int") && t2.equals("int")){
            return new String("int");
        }
        throw new Exception("MinusExpression: t1 = "+t1+", t2 = "+t2+" should be both int");
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
    public String visit(TimesExpression n) throws Exception {
        String t1 = n.f0.accept(this);
        String t2 = n.f2.accept(this);
        if(t1.equals("int") && t2.equals("int")){
            return new String("int");
        }
        throw new Exception("TimesExpression: t1 = "+t1+", t2 = "+t2+" should be both int");
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
    public String visit(ArrayLookup n) throws Exception {
        String t1 = n.f0.accept(this);
        String t2 = n.f2.accept(this);
        if(t1.equals("int[]") && t2.equals("int")){
            return new String("int");
        }
        throw new Exception("ArrayLookup: t1 = "+t1+", t2 = "+t2+" should be int[], int");
    }

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
    public String visit(ArrayLength n) throws Exception {
        String t1 = n.f0.accept(this);
        if(t1.equals("int[]")){
            return new String("int");
        }
        throw new Exception("ArrayLength: t1 = "+t1+" should be int[]");
    }

    /**
    * f0 -> "!"
    * f1 -> Clause()
    */
    public String visit(NotExpression n) throws Exception {
        String t1 = n.f1.accept(this);
        if(t1.equals("boolean")){
            return new String("boolean");
        }
        throw new Exception("NotExpression: t1 = "+t1+" should be boolean");
    }

    /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
    public String visit(BracketExpression n) throws Exception {
        return n.f1.accept(this);
    }
    

    /**
     * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
    public String visit(MessageSend n) throws Exception {
        String primaryExpr = n.f0.accept(this);
        // check if primary expression identifier is a known variable or type
        String type = findTypeOf(primaryExpr);
        if(type == null && !primaryExpr.equals("this") && Main.symbolTable.get(primaryExpr)==null){
            throw new Exception("MessageSend: Variable: "+primaryExpr+" hasn't been declared");
        }
        Main.ClassStruct tempClass;
        if(type!=null){
            tempClass = Main.symbolTable.get(type);
        }
        else if(primaryExpr.equals("this")){
            tempClass = state.classSt;
        }
        else if(Main.symbolTable.get(primaryExpr)!=null){
            tempClass = Main.symbolTable.get(primaryExpr);
        }
        else{
            throw new Exception("MessageSend: this is not possible");
        }
        String funId = n.f2.accept(this);
        // check that function exists in this class of parent classes
        Main.funStruct tempFun = tempClass.functions.get(funId);
        if(tempFun == null){
            // check parent classes
            tempClass = Main.symbolTable.get(tempClass.parentName); 
            while(tempClass!=null){
                tempFun = tempClass.functions.get(funId);
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
        Main.funStruct oldfun = state.messageSendFun;
        state.messageSendFun = tempFun;
        state.argCount=0;
        n.f4.accept(this);
        if(state.argCount != state.messageSendFun.args.size()){
            throw new Exception("MessageSend: function "+funId+" argument count found "+state.argCount+" expected "+state.messageSendFun.args.size());
        }
        state.messageSendFun = oldfun;
        return tempFun.returnType;
    }

    /**
     * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
    public String visit(ExpressionList n) throws Exception {
        String expr = n.f0.accept(this);
        Main.funStruct tempFun = state.messageSendFun;
        ArrayList<Entry<String, String>> arglist = new ArrayList<Entry<String,String>> (tempFun.args.entrySet()); 
        if(state.argCount < arglist.size() && !arglist.get(state.argCount).getValue().equals(expr)){
            // check if it is subtype
            Main.ClassStruct tempClass = Main.symbolTable.get(expr);
            tempClass = Main.symbolTable.get(tempClass.parentName);
            while(tempClass!=null){
                if(arglist.get(state.argCount).getValue().equals(tempClass.className)){
                    state.argCount++;
                    n.f1.accept(this);
                    return null;
                }
                tempClass = Main.symbolTable.get(tempClass.parentName);
            }
            throw new Exception("ExpressionList: Class: "+state.classSt.className+" Function: "+tempFun.funName+ " found: "+expr+" ,expected: "+arglist.get(state.argCount).getValue());
        }
        state.argCount++;
        n.f1.accept(this);
        return null;
    }

    /**
    * f0 -> ","
    * f1 -> Expression()
    */
    public String visit(ExpressionTerm n) throws Exception {
        String expr = n.f1.accept(this);
        Main.funStruct tempFun = state.messageSendFun;
        ArrayList<Entry<String, String>> arglist = new ArrayList<Entry<String,String>> (tempFun.args.entrySet()); 
        if(state.argCount < arglist.size() && !arglist.get(state.argCount).getValue().equals(expr)){
            // check if it is subtype
            Main.ClassStruct tempClass = Main.symbolTable.get(expr);
            tempClass = Main.symbolTable.get(tempClass.parentName);
            while(tempClass!=null){
                if(arglist.get(state.argCount).getValue().equals(tempClass.className)){
                    state.argCount++;
                    n.f1.accept(this);
                    return null;
                }
                tempClass = Main.symbolTable.get(tempClass.parentName);
            }
            throw new Exception("ExpressionTerm: Class: "+state.classSt.className+" Function: "+tempFun.funName+ " found: "+expr+" ,expected: "+arglist.get(state.argCount).getValue());
        }
        state.argCount++;
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
        String id = n.f0.accept(this);
        if(id.equals("this")){
            return state.classSt.className;
        }
        else if(!id.equals("int") && !id.equals("boolean") && !id.equals("int[]")){
            if(Main.symbolTable.get(id)!=null){
                return id;
            }
            String type = findTypeOf(id);
            if(type!=null){
                return type;
            }
            throw new Exception("PrimaryExpression: Variable: "+id+" hasn't been declared");
        }
        return n.f0.accept(this);
    }

    /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
    public String visit(ArrayAllocationExpression n) throws Exception {
        String expr = n.f3.accept(this);
        if(expr.equals("int")){
            return new String("int[]");
        }
        throw new Exception("ArrayAllocationExpression: expr = "+expr+" should be int");
    }

    /**
     * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
    public String visit(AllocationExpression n) throws Exception {
        String id = n.f1.accept(this);
        if(Main.symbolTable.get(id)==null){
            throw new Exception("AllocationExpression: class: "+id+" doesn't exist");
        }
        return id;
    }

    /**
    * f0 -> <INTEGER_LITERAL>
    */
    public String visit(IntegerLiteral n) throws Exception{
        return new String("int");
     }
  
     /**
      * f0 -> "true"
      */
     public String visit(TrueLiteral n) throws Exception{
        return new String("boolean");
     }
  
     /**
      * f0 -> "false"
      */
     public String visit(FalseLiteral n) throws Exception{
        return new String("boolean");
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