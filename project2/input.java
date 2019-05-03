class Main{
    public static void main(String[] args){
		System.out.println(1);
		// int a = new A().foo();
    }
}

class A{
	int i;
	boolean flag;
	int j;
	public int foo() { return 0;}
	public int fa() { return 0;}
}

class B extends A{
	A type;
	int k;
	public int foo() {return 0;}
	public int bla() {return 0;}
}