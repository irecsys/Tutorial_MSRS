package msrs.baseline.ubrec;

import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;
import org.moeaframework.problem.AbstractProblem;

import happy.coding.io.FileIO;
import msrs.demo.Config;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;


/**
*
* We learn user expectations by maximizing NDCG
* 
* Yong Zheng. "Utility-Based Multi-Criteria Recommender Systems", Proceedings of the 34th ACM SIGAPP Symposium on Applied Computing (ACM SAC), Limassol, Cyprus, April, 2019
* 
*/



public class UBRecProblem extends AbstractProblem {
	
	Config conf;

	public UBRecProblem(Config conf) {
		
		// super(a, b);
		// a = # of parameters to be learned
		// b = # of objectives, we use NDCG as the single objective
		
		// we have UserID: 1000 to 1331, 332 users
		// student expectation is a 3-dimension vector
		// a = 332*3 = 996
		
		super(996,1);
		this.conf=conf;
	}
	
	@Override
	public void evaluate(Solution solution){
		try {
			UBRec rec=new UBRec(conf, EncodingUtils.getReal(solution));
			rec.run();
			solution.setObjective(0, (-1.0)*rec.getNDCG()); // maximize
			solution.setAttribute("F1", rec.getF1());
			solution.setAttribute("UtilityStudent", rec.getUtility_students());
			solution.setAttribute("UtilityInstructor", rec.getUtility_instructors());
			double[] loss=rec.getLoss();
			solution.setAttribute("Loss1", loss[0]);
			solution.setAttribute("Loss2", loss[1]);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public Solution newSolution() {
		Solution solution =new Solution(996,1);
		for(int i=0;i<996;++i)
			solution.setVariable(i, EncodingUtils.newReal(1.0, 5.0)); // rating scale is 1 to 5	
		
		// set additional attributes if necessary
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("F1", 0.0);
		attributes.put("UtilityStudent", 0.0);
		attributes.put("UtilityInstructor", 0.0);
		attributes.put("Loss", 0.0);
		solution.addAttributes(attributes);
		return solution;
	}
}