all: c

compile:
	java -jar ./jtb132di.jar -te minijava.jj
	java -jar ./javacc5.jar minijava-jtb.jj
	javac Main.java
c:
	javac Main.java

r:
	java Main ./testcases/minijava-examples-new/BinaryTree.java ./testcases/minijava-examples-new/BubbleSort-error.java ./testcases/minijava-examples-new/BubbleSort.java ./testcases/minijava-examples-new/Factorial-error.java ./testcases/minijava-examples-new/Factorial.java ./testcases/minijava-examples-new/LinearSearch-error.java ./testcases/minijava-examples-new/LinearSearch.java ./testcases/minijava-examples-new/LinkedList-error.java ./testcases/minijava-examples-new/LinkedList.java ./testcases/minijava-examples-new/MoreThan4-error.java ./testcases/minijava-examples-new/MoreThan4.java ./testcases/minijava-examples-new/QuickSort-error.java ./testcases/minijava-examples-new/QuickSort.java ./testcases/minijava-examples-new/TreeVisitor-error.java ./testcases/minijava-examples-new/TreeVisitor.java

r2: 
	java Main ./testcases/minijava-error-extra/BadAssign.java ./testcases/minijava-error-extra/BadAssign2.java ./testcases/minijava-error-extra/Classes-error.java ./testcases/minijava-error-extra/DoubleDeclaration1.java ./testcases/minijava-error-extra/DoubleDeclaration4.java ./testcases/minijava-error-extra/DoubleDeclaration6.java ./testcases/minijava-error-extra/NoMatchingMethod.java ./testcases/minijava-error-extra/NoMethod.java ./testcases/minijava-error-extra/Overload1.java ./testcases/minijava-error-extra/UseArgs.java ./testcases/minijava-error-extra/test18.java ./testcases/minijava-error-extra/test21.java ./testcases/minijava-error-extra/test35.java ./testcases/minijava-error-extra/test52.java ./testcases/minijava-error-extra/test68.java

clean:
	rm -f *.class *~
