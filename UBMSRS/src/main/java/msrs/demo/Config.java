package msrs.demo;

public class Config {
	
	protected String path;
	protected String fileExpectations_student;
	protected int N;
	protected int maxEval;
	
	protected double maxf1;
	protected double maxndcg;
	protected double maxutil_instructor;
	protected double maxutil_student;
	
	public Config() {
		
	}
	
	public Config(String folder, String filename, int n, int maxEval,
			double maxf1, double maxndcg, double maxutil_instructor, double maxutil_student) {
		this.path=folder;
		this.fileExpectations_student=filename;
		this.N=n;		
		this.maxEval=maxEval;
		this.maxf1=maxf1;
		this.maxndcg=maxndcg;
		this.maxutil_instructor=maxutil_instructor;
		this.maxutil_student=maxutil_student;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public String getExpectationFilename() {
		return this.fileExpectations_student;
	}
	
	public int getNumRec() {
		return this.N;
	}
	
	public int getMaxEval() {
		return this.maxEval;
	}
	
	public double getMaxF1() {
		return this.maxf1;
	}
	
	public double getMaxNDCG() {
		return this.maxndcg;
	}
	
	public double getMaxUtil_Instructor() {
		return this.maxutil_instructor;
	}
	
	public double getMaxUtil_Student() {
		return this.maxutil_student;
	}

}
