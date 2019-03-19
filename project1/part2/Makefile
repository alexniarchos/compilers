all: compile

compile:
	../jflex-1.7.0/bin/jflex scanner.flex
	java -jar ../java-cup-11b.jar -interface -parser Parser parser.cup
	javac -cp ../java-cup-11b-runtime.jar *.java

execute:
	java -cp ../java-cup-11b-runtime.jar:. Main

clean:
	rm -f *.class *~
