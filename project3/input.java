class Factorial{
    public static void main(String[] arg){
        int[] x;
        int a;
        int b;
        int c;
        
        a = 1;
        b = 2;
        c = a < b;
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
        B x2;
        int a;
        int b;
        int c;
        int y;
        int z;
        x = new B();
        System.out.println(x.d(a,x2.d(y,z),c));
        // x2 = new A();
        // System.out.println(x2.c());
        return 1;
    }
}