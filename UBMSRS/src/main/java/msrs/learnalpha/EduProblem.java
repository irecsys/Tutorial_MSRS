package msrs.learnalpha;

import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;

import happy.coding.io.FileIO;
import msrs.demo.Config;

import java.io.BufferedReader;

public class EduProblem extends AbstractProblem {
	
	String path=null;
	Config conf;

	public EduProblem(Config conf) {
		
		// super(a, b);
		// a = # of parameters to be learned
		// b = # of objectives 

		// in this example, we only learn the Alpha
		
		super(1,5);
		this.conf=conf;
		this.path=conf.getPath();
	}
	
	@Override
	public void evaluate(Solution solution){
		try {
			EduRec rec=new EduRec(conf, EncodingUtils.getReal(solution));
			rec.run();
			solution.setObjective(0, (-1.0)*rec.getUtility_students()); // maximize
			solution.setObjective(1, (-1.0)*rec.getUtility_instructors()); // maximize
			solution.setObjective(2, rec.getUtility_diffs()); //minimize
			solution.setObjective(3, (-1.0)*rec.getF1()); // maximize
			solution.setObjective(4, (-1.0)*rec.getNDCG()); // maximize
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public Solution newSolution() {
		Solution solution =new Solution(1,5);
		solution.setVariable(0, EncodingUtils.newReal(0.0,1.0)); // scale of the Alpha is 0 to 1		
		return solution;
	}
}