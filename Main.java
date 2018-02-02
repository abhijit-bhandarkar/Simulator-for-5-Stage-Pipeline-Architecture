import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Main {
	public static HashMap<Integer, Instruction> addrToInsMap;              //address-to-instruction map
	private static String fileName;
	public static int curIns = 4000;
	public static HashMap<String, String> registerFile;
	public static HashMap<String, Instruction> stToIns;						//stages-to-instruction map
	public static boolean stop = false;
	public static int cycle = 1;
	public static final String FETCH = "FETCH";
	public static final String DRF = "DECODE";
	public static final String ALU1 = "ALU1";
	public static final String ALU2 = "ALU2";
	public static final String BFU = "BRANCHFU";
	public static final String DELAY = "DELAY";
	public static final String MEM = "MEMORY";
	public static final String WB = "WRITEBACK";
	public static boolean stalled = false;									//stall-flag
	public static boolean branchFlag = false;
	public static String[] waitingFor = null; //used for forwarding.First element in this array is register we are waiting for and second is the value forwarded by the instruction
	public static boolean zeroFlag = false;
	public static String x="";
	
	public static void displayStages()
	{
		System.out.println("------INSTRUCTIONS CURRENTLY PRESENT IN PIPELINE STAGES-------");
		for(Map.Entry<String, Instruction> entry:stToIns.entrySet())
		{
			System.out.println(entry.getKey()+":"+entry.getValue());
		}
	}

	public static void main(String[] args) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		fileName = args[0];
		System.out.println("Press 1 to initialize\nPress 2 to simulate\nPress 3 to display\nPress 4 to exit");
		Scanner sc=new Scanner(System.in);
		while(true){
		switch(sc.nextInt())
		{
		case 1:
			initialize();			 
			break;
		case 2:
			System.out.println("Enter number of cycles to simulate");
			int c=sc.nextInt();
			simulate(c);
			break;
		case 3:
			display();
			break;
		case 4:
			System.exit(0);
		}
		}
//		initialize();
//		System.out.println("ADDR\tOPCODE\tDES\tSRC1\tSRC2\tLTRL");
//		for (Map.Entry<Integer, Instruction> entry : addrToInsMap.entrySet()) {
//			Instruction current = entry.getValue();
//			System.out.println(entry.getKey() + "\t" + current.getOpcode() + "\t" + current.getDes() + "\t"
//					+ current.getSrc1() + "\t" + current.getSrc2() + "\t" + current.getLiteral());
//		}
//		simulate(400);
//		display();
	}
	
	public static void display()
	{
		displayRFile();
		MemorySpace.displayMem();
		displayStages();
	}

	public static void initialize() throws IOException {
		File f = new File(fileName);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String tmp;
		addrToInsMap = new HashMap<Integer, Instruction>();
		registerFile = new HashMap<String, String>();
		waitingFor = new String[2];
		initializeStageToIns();          //initializes the Hashmap stToIns which maps the current instruction in all stages.
		initializeRFile();				//initializes the Register File with values 0 for all registers
		initializeMem();				//initializes the Memory with 0
		int insAddr = 4000;
		while ((tmp = br.readLine()) != null) {								//logic for reading the file, creating the new Instruction object and adding the object to the 
			String[] ins = tmp.split(" ");									//address-to-instruction hashmap. The file is read line by line and every line 
			for (int i = 0; i < ins.length; i++) {							//and every line is split with respect to whitespace.
				ins[i] = ins[i].replaceAll(",", "");						//The source and destination registers are determined on the basis of the opcode of the instruction
			}
			Instruction curIns = new Instruction();
			curIns.setText(tmp);
			curIns.setOpcode(ins[0]);
			if (curIns.getOpcode().equals("MOVC")) {						//
				curIns.setDes(ins[1]);
				if (ins[2].equals("#"))
					curIns.setLiteral(ins[2] + ins[3]);
				else
					curIns.setLiteral(ins[2]);
			} else if (curIns.getOpcode().equals("STORE")) {
				curIns.setSrc1(ins[1]);
				curIns.setSrc2(ins[2]);
				if (ins[3].equals("#"))
					curIns.setLiteral(ins[3] + ins[4]);
				else
					curIns.setLiteral(ins[3]);
			} else if (curIns.getOpcode().equals("LOAD")) {
				curIns.setDes(ins[1]);
				curIns.setSrc1(ins[2]);
				if (ins[3].equals("#"))
					curIns.setLiteral(ins[3] + ins[4]);
				else
					curIns.setLiteral(ins[3]);
			} else if (curIns.getOpcode().equals("BZ") || curIns.getOpcode().equals("BNZ")) {
				if (ins[1].equals("#"))
					curIns.setLiteral(ins[1] + ins[2]);
				else
					curIns.setLiteral(ins[1]);
			} else if (curIns.getOpcode().equals("JUMP")) {
				curIns.setSrc1(ins[1]);
				if (ins[2].equals("#"))
					curIns.setLiteral(ins[2] + ins[3]);
				else
					curIns.setLiteral(ins[2]);
			} else if (curIns.getOpcode().equals("BAL")) {
				curIns.setSrc1(ins[1]);
				if (ins[2].equals("#"))
					curIns.setLiteral(ins[2] + ins[3]);
				else
					curIns.setLiteral(ins[2]);
			} else if (curIns.getOpcode().equals("ADD") || curIns.getOpcode().equals("SUB")
					|| curIns.getOpcode().equals("MUL") || curIns.getOpcode().equals("AND")
					|| curIns.getOpcode().equals("OR") || curIns.getOpcode().equals("EX-OR")) {
				curIns.setDes(ins[1]);
				curIns.setSrc1(ins[2]);
				curIns.setSrc2(ins[3]);
			}
			curIns.setAddr(insAddr);
			addrToInsMap.put(insAddr, curIns);													
			insAddr=insAddr+4;
		}
	}

	public static void simulate(int cycles) {						//simulates the execution of the program
		while (!stop && cycle != cycles) {
		//	System.out.println("---------CYCLE " + cycle + "---------");
			writeBack();
			memory();
			delay();
			branchfu();
			alu2();
			alu1();
			decode();
			fetch();
			cycle++;
			// Thread.sleep(1000);
		}
		System.out.println("SIMULATION COMPLETE "+(cycle-1)+" CYCLES SIMULATED");
	}

	public static void initializeMem() {								//initial the memory
		for (int i : MemorySpace.memory) {
			i = 0;
		}
	}

	public static void initializeRFile() {								//initial the register file
		registerFile.put("R0", "0");
		registerFile.put("R1", "0");
		registerFile.put("R2", "0");
		registerFile.put("R3", "0");
		registerFile.put("R4", "0");
		registerFile.put("R5", "0");
		registerFile.put("R6", "0");
		registerFile.put("R7", "0");
		registerFile.put("R8", "0");
		registerFile.put("R9", "0");
		registerFile.put("R10", "0");
		registerFile.put("R11", "0");
		registerFile.put("R12", "0");
		registerFile.put("R13", "0");
		registerFile.put("R14", "0");
		registerFile.put("R15", "0");
		registerFile.put("X", "0");
		waitingFor[0] = "";
		waitingFor[1] = "";
	}

	public static void fetch() {						//Representation of FETCH stage in the pipeline. New instruction fetched from the address-to-instruction Hashmap 
		if (stToIns.get(FETCH) == null && !branchFlag) {
			stToIns.put(FETCH, addrToInsMap.get(curIns));
			curIns=curIns+4;
		//	System.out.println("F:" + stToIns.get(FETCH));
			branchFlag = false;
		} else if (branchFlag) {
			branchFlag = false;
		//	System.out.println("F:" + stToIns.get(FETCH));
		} else {
		//	System.out.println("F:" + stToIns.get(FETCH));

		}
		// System.out.println("F:" + stToIns.get(FETCH));
	}

	public static void decode() { 				//Representation of DRF stage in the pipeline. Values of the source registers are read from the register file

		if (!stalled) {
			Instruction[] nextIns = new Instruction[3];
			nextIns[0] = stToIns.get(ALU1);					//retrieve the next 3 instructions 
			nextIns[1] = stToIns.get(ALU2);
			nextIns[2] = stToIns.get(MEM);
			stToIns.put(DRF, stToIns.get(FETCH));					//get the instruction in fetch and set fetch to null
			stToIns.put(FETCH, null);
			//System.out.println("DRF:" + stToIns.get(DRF));
			if (stToIns.get(DRF) != null) {
				switch (stToIns.get(DRF).getOpcode()) {				//determine the action to be taken based on the opcode of the instruction
				case "JUMP":
					stToIns.get(DRF).setSrc1Val(x);
					break;
				case "BAL":
					stToIns.get(DRF).setSrc1Val(registerFile.get(stToIns.get(DRF).getSrc1()));
					for (int i = 0; i < nextIns.length; i++) {								//logic for detecting dependencies and introduce a stall
						if (nextIns[i] != null && nextIns[i].getDes() != null
								&& nextIns[i].getDes().equals(stToIns.get(DRF).getSrc1())) {
							 //System.out.println("Stalled for " +
							// stToIns.get(DRF).getSrc1());
							waitingFor[0] = stToIns.get(DRF).getSrc1();        //set the 1st element of waitingFor to the register over which the dependency exists
							stalled = true;				//set the stall flag
						}
						if (stalled) {
							waitingFor[1] = "";				//initialize the value to empty string
						}
					}
					break;
				case "MOVC":
					break;
				case "HALT":
					break;
				case "LOAD":
					stToIns.get(DRF).setSrc1Val(registerFile.get(stToIns.get(DRF).getSrc1()));
					for (int i = 0; i < nextIns.length; i++) {		//logic for detecting dependencies and introduce a stall
						if (nextIns[i] != null && nextIns[i].getDes() != null
								&& nextIns[i].getDes().equals(stToIns.get(DRF).getSrc1())) {
							// System.out.println("Stalled for " +
							// stToIns.get(DRF).getSrc1());
							waitingFor[0] = stToIns.get(DRF).getSrc1();
							stalled = true;
						}
						if (stalled) {
							waitingFor[1] = "";
						}
					}
					break;
				case "STORE":
					stToIns.get(DRF).setSrc2Val(registerFile.get(stToIns.get(DRF).getSrc2()));
					for (int i = 0; i < nextIns.length; i++) {   //logic for detecting dependencies and introduce a stall
						if (nextIns[i] != null && nextIns[i].getDes() != null
								&& nextIns[i].getDes().equals(stToIns.get(DRF).getSrc2())) {
							// System.out.println("Stalled for " +
						//	 stToIns.get(DRF).getSrc2());
							waitingFor[0] = stToIns.get(DRF).getSrc2();
							stalled = true;
						}
						if (stalled) {
							waitingFor[1] = "";
						}
					}
					break;
				case "BNZ":
					stalled=true;							//Introduce a compulsory stall for the next instruction to reach the ALU2 stage and set the zero flag
					break;
				case "BZ":
					stalled=true;
					break;
				default:
				//	System.out.println("Reading:"+stToIns.get(DRF).getSrc1()+"--->"+registerFile.get(stToIns.get(DRF).getSrc1()));
					stToIns.get(DRF).setSrc1Val(registerFile.get(stToIns.get(DRF).getSrc1()));
				//	System.out.println("Reading:"+stToIns.get(DRF).getSrc2()+"--->"+registerFile.get(stToIns.get(DRF).getSrc2()));
					stToIns.get(DRF).setSrc2Val(registerFile.get(stToIns.get(DRF).getSrc2()));   //read values from the register file
					for (int i = 0; i < nextIns.length; i++) {	//logic for detecting dependencies and introduce a stall
						if (nextIns[i] != null && nextIns[i].getDes() != null
								&& nextIns[i].getDes().equals(stToIns.get(DRF).getSrc1())) {
							// System.out.println("Stalled for " +
						//	 stToIns.get(DRF).getSrc1());
							waitingFor[0] = stToIns.get(DRF).getSrc1();
							stalled = true;
						
						}
						if (nextIns[i] != null && nextIns[i].getDes() != null
								&& nextIns[i].getDes().equals(stToIns.get(DRF).getSrc2())) {
							// System.out.println("Stalled for " +
						//	 stToIns.get(DRF).getSrc2());
							waitingFor[0] = stToIns.get(DRF).getSrc2();
							stalled = true;
							
						}
						if (stalled) {
							waitingFor[1] = "";
						}
					}
//					if (!stalled) {
//						System.out.println("Reading:"+stToIns.get(DRF).getSrc1()+"--->"+registerFile.get(stToIns.get(DRF).getSrc1()));
//						stToIns.get(DRF).setSrc1Val(registerFile.get(stToIns.get(DRF).getSrc1()));
//						System.out.println("Reading:"+stToIns.get(DRF).getSrc2()+"--->"+registerFile.get(stToIns.get(DRF).getSrc2()));
//						stToIns.get(DRF).setSrc2Val(registerFile.get(stToIns.get(DRF).getSrc2()));
//					}
				}
			}
		} else {
			if(stToIns.get(DRF)!=null&&(stToIns.get(DRF).getOpcode().equals("BNZ")||stToIns.get(DRF).getOpcode().equals("BZ")))
			{
				stalled=false;					//stop the compulsory stall			
			}
			//boolean b = waitingFor[1] != "";
	//		System.out.println("DRF:" + stToIns.get(DRF));
			if (waitingFor[1] != "") { 			//check whether any instruction has forwarded the value
				if (waitingFor[0].equals(stToIns.get(DRF).getSrc1())){
				//	System.out.println("Reading:"+stToIns.get(DRF).getSrc1()+"--->"+waitingFor[1]);
					stToIns.get(DRF).setSrc1Val(waitingFor[1]);
				}
				if (waitingFor[0].equals(stToIns.get(DRF).getSrc2())){
				//	System.out.println("Reading:"+stToIns.get(DRF).getSrc1()+"--->"+waitingFor[1]);
					stToIns.get(DRF).setSrc2Val(waitingFor[1]);
				}
				waitingFor[0] = "";    //reset the waitingFor values and stall flag
				waitingFor[1] = "";
				stalled = false;
			}
		}
	}

	public static void branchfu() {                        //Representation of first stage of the branch function unit stage in the pipeline
		if(!stalled){
			
		if (stToIns.get(DRF) != null && (stToIns.get(DRF).getOpcode().equals("BNZ")||stToIns.get(DRF).getOpcode().equals("BZ")||stToIns.get(DRF).getOpcode().equals("JUMP")||stToIns.get(DRF).getOpcode().equals("BAL"))) 
		{
			stToIns.put(BFU, stToIns.get(DRF));
			stToIns.put(DRF, null);
		//	System.out.println("BFU:" + stToIns.get(BFU));
			switch(stToIns.get(BFU).getOpcode())
			{
			case "BNZ":
				if (!zeroFlag) 
				{
					stToIns.put(DRF, null);
					stToIns.put(FETCH, null);
					int value = Integer.parseInt(stToIns.get(BFU).getLiteral().replaceAll("#", ""));
					curIns = stToIns.get(BFU).getAddr() + value;
				//	System.out.println("Value:" + value + "\t" + "CurIns:" + curIns);
					branchFlag = true;
				}
				else {
				//	System.out.println("BFU:" + stToIns.get(BFU));
				}
				break;
			case "BZ":
				if (zeroFlag) 
				{
					stToIns.put(DRF, null);
					stToIns.put(FETCH, null);
					int value = Integer.parseInt(stToIns.get(BFU).getLiteral().replaceAll("#", ""));
					curIns = stToIns.get(BFU).getAddr() + value;
				//	System.out.println("Value:" + value + "\t" + "CurIns:" + curIns);
					branchFlag = true;
				}
				else {
				//	System.out.println("BFU:" + stToIns.get(BFU));
				}
				break;
			case "JUMP":
				stToIns.put(DRF, null);
				stToIns.put(FETCH, null);
				int value1=Integer.parseInt(stToIns.get(BFU).getLiteral().replaceAll("#", ""));
				int value2=Integer.parseInt(x);
				curIns=value1+value2;
				//System.out.println("Value1:" + value1 + "\t"+"Value2:" + value2 + "CurIns:" + curIns);
				//branchFlag=true;
				break;
			case "BAL":
				stToIns.put(DRF, null);
				stToIns.put(FETCH, null);
				x=stToIns.get(BFU).getAddr()+4+"";
				int v1=Integer.parseInt(stToIns.get(BFU).getLiteral().replaceAll("#", ""));
				int v2=Integer.parseInt(stToIns.get(BFU).getSrc1Val());
				curIns=v1+v2;
			//	System.out.println("Value1:" + v1 + "\t"+"Value2:" + v2 + "CurIns:" + curIns);
				break;
			}
			
//			if (!zeroFlag) 
//			{
//				stToIns.put(DRF, null);
//				stToIns.put(FETCH, null);
//				int value = Integer.parseInt(stToIns.get(BFU).getLiteral().replaceAll("#", ""));
//				curIns = stToIns.get(BFU).getAddr() + value / 4;
//				System.out.println("Value:" + value + "\t" + "CurIns:" + curIns);
//				branchFlag = true;
//			}
		} else {
		//	System.out.println("BFU:" + stToIns.get(BFU));
		}
		}
		else{
			stToIns.put(BFU, null);
		//	System.out.println("BFU:" + stToIns.get(BFU));
		}
	}

	public static void delay() { 								//Representation of delay after the branch FU in the pipeline
		stToIns.put(DELAY, stToIns.get(BFU));
		stToIns.put(BFU, null);
	//	System.out.println("DELAY:" + stToIns.get(DELAY));
	}

	public static void alu1() {					//Representation of first stage of the ALU function unit stage in the pipeline
		if (stalled) {
			stToIns.put(ALU1, null);
		//	System.out.println("ALU1:" + stToIns.get(ALU1));
			return;
		}
		stToIns.put(ALU1, stToIns.get(DRF));
		stToIns.put(DRF, null);
	//	System.out.println("ALU1:" + stToIns.get(ALU1));
	}

	public static void alu2() {								//Representation of second stage of the ALU function unit stage in the pipeline. Result of the instructions are produced here
		stToIns.put(ALU2, stToIns.get(ALU1));
		stToIns.put(ALU1, null);
	//	System.out.println("ALU2:" + stToIns.get(ALU2));
		if (stToIns.get(ALU2) != null) {
			String result = "";
			switch (stToIns.get(ALU2).getOpcode()) {
			case "HALT": {
				result = "";
				break;
			}
			case "ADD": {
				int value1 = Integer.parseInt(stToIns.get(ALU2).getSrc1Val().replaceAll("#", ""));
				int value2 = Integer.parseInt(stToIns.get(ALU2).getSrc2Val().replaceAll("#", ""));
				result = value1 + value2 + "";
				if (value1 + value2 == 0)
					zeroFlag = true;
				else
					zeroFlag = false;
				break;
			}
			case "SUB": {
				int value1 = Integer.parseInt(stToIns.get(ALU2).getSrc1Val().replaceAll("#", ""));
				int value2 = Integer.parseInt(stToIns.get(ALU2).getSrc2Val().replaceAll("#", ""));
				result = value1 - value2 + "";
				if (value1 - value2 == 0)
					zeroFlag = true;
				else
					zeroFlag = false;
				break;
			}
			case "MUL": {
				int value1 = Integer.parseInt(stToIns.get(ALU2).getSrc1Val().replaceAll("#", ""));
				int value2 = Integer.parseInt(stToIns.get(ALU2).getSrc2Val().replaceAll("#", ""));
				result = value1 * value2 + "";
				if (value1 * value2 == 0)
					zeroFlag = true;
				else
					zeroFlag = false;
				break;
			}
			case "AND": {
				int value1 = Integer.parseInt(stToIns.get(ALU2).getSrc1Val().replaceAll("#", ""));
				int value2 = Integer.parseInt(stToIns.get(ALU2).getSrc2Val().replaceAll("#", ""));
				result = (value1 & value2) + "";
				break;
			}
			case "OR": {
				int value1 = Integer.parseInt(stToIns.get(ALU2).getSrc1Val().replaceAll("#", ""));
				int value2 = Integer.parseInt(stToIns.get(ALU2).getSrc2Val().replaceAll("#", ""));
				result = (value1 | value2) + "";
				break;
			}
			case "EX-OR": {
				int value1 = Integer.parseInt(stToIns.get(ALU2).getSrc1Val().replaceAll("#", ""));
				int value2 = Integer.parseInt(stToIns.get(ALU2).getSrc2Val().replaceAll("#", ""));
				result = (value1 ^ value2) + "";
				break;
			}
			case "MOVC": {
				int value = Integer.parseInt(stToIns.get(ALU2).getLiteral().replaceAll("#", ""));
				result = value + "";
				break;
			}
			case "LOAD": {
				int value1 = Integer.parseInt(stToIns.get(ALU2).getSrc1Val().replaceAll("#", ""));
				int value2 = Integer.parseInt(stToIns.get(ALU2).getLiteral().replaceAll("#", ""));
				result = value1 + value2 + "";
				break;
			}
			case "STORE": {
				if(stToIns.get(MEM)!=null&&stToIns.get(MEM).getDes().equals(stToIns.get(ALU2).getSrc1()))
					stToIns.get(ALU2).setSrc1Val(stToIns.get(MEM).getResult());
				else
				stToIns.get(ALU2).setSrc1Val(registerFile.get(stToIns.get(ALU2).getSrc1()));
				int value1 = Integer.parseInt(stToIns.get(ALU2).getSrc2Val().replaceAll("#", ""));
				int value2 = Integer.parseInt(stToIns.get(ALU2).getLiteral().replaceAll("#", ""));
				result = value1 + value2 + "";
				break;
			}
			}
			if(stToIns.get(ALU2).getOpcode().equals("LOAD")||stToIns.get(ALU2).getOpcode().equals("STORE"))
				stToIns.get(ALU2).setMemAddr(result);
			else
				stToIns.get(ALU2).setResult(result);
		//	System.out.println("Result:"+result+" ZeroFlag:"+zeroFlag);
			if (!stToIns.get(ALU2).getOpcode().equals("HALT") && stToIns.get(ALU2).getDes() != null
					&& stToIns.get(ALU2).getDes().equals(waitingFor[0])) {
				waitingFor[1] = result;
		//		System.out.println("Forwarded" + result);
			}
		}
	}

	public static void memory() {							//Representation of the MEM stage in the pipeline
//		if (stToIns.get(ALU2)!=null&&!stToIns.get(ALU2).getOpcode().equals("HALT") && stToIns.get(ALU2).getDes() != null
//				&& stToIns.get(ALU2).getDes().equals(waitingFor[0])) {
//			waitingFor[1] = stToIns.get(ALU2).getResult();
//			System.out.println("ALU2:Forwarded" + stToIns.get(ALU2).getResult());
//		}
		if (stToIns.get(ALU2) != null) {
			stToIns.put(MEM, stToIns.get(ALU2));
			stToIns.put(ALU2, null);
		//	System.out.println("MEM:" + stToIns.get(MEM));
			if (stToIns.get(MEM) != null) {
				switch (stToIns.get(MEM).getOpcode()) {
				case "LOAD":
		//			System.out.println("Loading value from Mem["+Integer.parseInt(stToIns.get(MEM).getMemAddr())+"]("+MemorySpace.getMemValue(Integer.parseInt(stToIns.get(MEM).getMemAddr()))+")");
					int res = MemorySpace.getMemValue(Integer.parseInt(stToIns.get(MEM).getMemAddr()));
					stToIns.get(MEM).setResult(res + "");
					break;
				case "STORE":
		//			System.out.println("Storing value "+Integer.parseInt(stToIns.get(MEM).getSrc1Val())+" to Address "+Integer.parseInt(stToIns.get(MEM).getMemAddr()));
					MemorySpace.setMemValue(Integer.parseInt(stToIns.get(MEM).getMemAddr()),
							Integer.parseInt(stToIns.get(MEM).getSrc1Val()));
					break;

				}
				if (!stToIns.get(MEM).getOpcode().equals("HALT") && stToIns.get(MEM).getDes() != null
						&& stToIns.get(MEM).getDes().equals(waitingFor[0])) {
					waitingFor[1] = stToIns.get(MEM).getResult();
			//		System.out.println("MEM:Forwarded" + stToIns.get(MEM).getResult());
				}
			}
		} else if (stToIns.get(DELAY) != null) {
			stToIns.put(MEM, stToIns.get(DELAY));
			stToIns.put(DELAY, null);
		//	System.out.println("MEM:" + stToIns.get(MEM));
		} else {
		//	System.out.println("MEM:" + stToIns.get(MEM));
		}
	}

	public static void writeBack() {										//write the result produced by the instruction to it's destination register. Represents the WB stage in the pipeline
//		if (stToIns.get(MEM)!=null&&!stToIns.get(MEM).getOpcode().equals("HALT") && stToIns.get(MEM).getDes() != null
//				&& stToIns.get(MEM).getDes().equals(waitingFor[0])) {
//			waitingFor[1] = stToIns.get(MEM).getResult();
//			System.out.println("MEM:Forwarded" + stToIns.get(MEM).getResult());
//		}
		stToIns.put(WB, stToIns.get(MEM));
		stToIns.put(MEM, null);
	//	System.out.println("WB:" + stToIns.get(WB));
		if (stToIns.get(WB) != null) {
			switch (stToIns.get(WB).getOpcode()) {
			case "MOVC": {
				registerFile.put(stToIns.get(WB).getDes(), stToIns.get(WB).getResult());
				// System.out.println("Writing value " +
				// stToIns.get(WB).getResult() + " to " +
				// stToIns.get(WB).getDes());
				break;
			}
			case "HALT": {
				stop = true;
				break;
			}
			case "BAL":
				registerFile.put("X", x);
				break;
			case "STORE": {
				break;
			}
			case "BNZ":
				break;
			case "BZ":
				break;
			case "JUMP":
				break;
			default: {
				registerFile.put(stToIns.get(WB).getDes(), stToIns.get(WB).getResult());
		//		System.out.println("Writing value " + stToIns.get(WB).getResult() + " to " + stToIns.get(WB).getDes());
			}

			}
		}
	}

	public static void initializeStageToIns() {
		stToIns = new HashMap<String, Instruction>();
		stToIns.put(FETCH, null);
		stToIns.put(DRF, null);
		stToIns.put(ALU1, null);
		stToIns.put(ALU2, null);
		stToIns.put(BFU, null);
		stToIns.put(DELAY, null);
		stToIns.put(MEM, null);
		stToIns.put(WB, null);
	}

	public static void displayRFile() {
		System.out.println("------CONTENTS OF THE REGISTER FILE------------");
		System.out.println("REG\tVALUE");
		for (Map.Entry<String, String> entry : registerFile.entrySet()) {
			System.out.println(entry.getKey() + "\t" + entry.getValue());
		}
	}
//	public static void displayMem()
//	{
//		for(int i=0;i<100;i++)
//		{
//			System.out.print(MemorySpace.getMemValue(i));
//		}
//	}

}
