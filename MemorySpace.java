
public class MemorySpace {
	public static int[] memory=new int[4000];
	public static int getMemValue(int addr)
	{
		return memory[addr];
	}
	public static void setMemValue(int addr,int value)
	{
		memory[addr]=value;
	}
	public static void displayMem()
	{
		System.out.println("----------CONTENTS OF MEMORY----------------");
		for(int i=0;i<=100;i++)
		{	
			if(i>0&&i%6==0)
				System.out.println();		
			System.out.print("["+i+"]"+"-->"+memory[i]+"\t|\t");
		}
	}
}
