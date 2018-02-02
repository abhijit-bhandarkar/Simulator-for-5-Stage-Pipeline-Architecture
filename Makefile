run:compile jar
	java -jar APEXSim.jar $(filename)
jar:compile 
	jar -cfe APEXSim.jar Main Main.class Instruction.class MemorySpace.class
compile:Main.java Instruction.java MemorySpace.java 
	javac Main.java Instruction.java MemorySpace.java 
clean:
	rm -f *.class *.jar *.class
