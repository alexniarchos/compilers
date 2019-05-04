all: c

compile:
	java -jar ./jtb132di.jar -te minijava.jj
	java -jar ./javacc5.jar minijava-jtb.jj
	javac Main.java
c:
	javac Main.java
	java Main input.java

r:
	java Main input.java

clean:
	rm -f *.class *~
