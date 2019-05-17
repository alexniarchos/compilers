all: c

compile:
	java -jar ./jtb132di.jar -te minijava.jj
	java -jar ./javacc5.jar minijava-jtb.jj
	javac Main.java

c: 
	javac Main.java

cr:
	javac Main.java
	java Main ./testcases/TreeVisitor.java

r:
	java Main ./testcases/TreeVisitor.java

clean:
	rm -f *.class *~
