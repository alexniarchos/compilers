class Main {

    public static void main(String[] args){
        A a;
        a = (new A ());
    }

}


class A {
  public A foo(){
    return new A();
  }
}