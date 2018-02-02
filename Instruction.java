
public class Instruction {
	private String opcode;
	private String src1,src2,des;
	private String src1Val,src2Val;
	private String literal,result;
	private String memAddr;
	private String text;
	public String getText() {
		return text;
	}
	public void setText(String text) {
		this.text = text;
	}
	public String getMemAddr() {
		return memAddr;
	}
	public void setMemAddr(String memAddr) {
		this.memAddr = memAddr;
	}
	private int addr;
	public int getAddr() {
		return addr;
	}
	public void setAddr(int addr) {
		this.addr = addr;
	}
	public Instruction() {
		// TODO Auto-generated constructor stub
		opcode=null;
		src1=null;
		src2=null;
		des=null;
		literal=null;
		src1Val=null;
		src2Val=null;
		result=null;
//		opcode="";
//		src1="";
//		src2="";
//		des="";
//		literal="";
//		src1Val="";
//		src2Val="";
//		result="";
	}
	public String getResult() {
		return result;
	}
	public void setResult(String result) {
		this.result = result;
	}
	public String getSrc1Val() {
		return src1Val;
	}
	public void setSrc1Val(String src1Val) {
		this.src1Val = src1Val;
	}
	public String getSrc2Val() {
		return src2Val;
	}
	public void setSrc2Val(String src2Val) {
		this.src2Val = src2Val;
	}
	public String getLiteral() {
		return literal;
	}
	public void setLiteral(String literal) {
		this.literal = literal;
	}
	public String getOpcode() {
		return opcode;
	}
	public void setOpcode(String opcode) {
		this.opcode = opcode;
	}
	public String getSrc1() {
		return src1;
	}
	public void setSrc1(String src1) {
		this.src1 = src1;
	}
	public String getSrc2() {
		return src2;
	}
	public void setSrc2(String src2) {
		this.src2 = src2;
	}
	public String getDes() {
		return des;
	}
	public void setDes(String des) {
		this.des = des;
	}
	public boolean hasSourceRegisters()
	{
		if(src1==null&&src2==null)
			return true;
		else
			return false;
	}
	public String[] getAllSourceRegisters()
	{
		String[] sources = new String[2];
		sources[0]=src1;
		sources[1]=src2;
		return sources;
	}
	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return text;
	}
}
