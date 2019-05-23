class Factorial{
    public static void main(String[] arg){
        int[] x;
        x = new int[10];
        x[0] = 1;
    }
}

class A {
    int var;
    public int a(){
        var = 2;
        return 0 ;
    }

    public int b(){
        return 0 ;
    }

    public int c(){
        return 0 ;
    }
}

class B extends A {
    public int a(){
        return 1;
    }
    
    public int d(){
        B x;
        A x2;
        x = new B();
        System.out.println(x.d());
        x2 = new A();
        System.out.println(x2.c());
        return 1;
    }
}