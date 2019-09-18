package msrs.baseline.ubrec;

import happy.coding.io.FileConfiger;
import happy.coding.io.FileIO;
import happy.coding.io.LineConfiger;
import happy.coding.io.Logs;
import msrs.demo.Config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.moeaframework.Executor;
import org.moeaframework.core.NondominatedPopulation;
import org.moeaframework.core.Solution;
import org.moeaframework.core.variable.EncodingUtils;

import com.google.common.collect.ArrayListMultimap;

public class RunUBRec {
	String path=null;
	DecimalFormat df = new DecimalFormat("#.000");
	protected Config conf;
	
	public RunUBRec(Config conf) {
		this.conf=conf;
		this.path=conf.getPath();
	}

    public void execute() throws Exception {

        String[] algorithms = {"OMOPSO"}; // you can select other optimizers too!
        
        Solution bestSolution = null;
        double bestNDCG = -1;
        
        for (String algorithm : algorithms) {
            NondominatedPopulation rst = new Executor()
                    .withProblemClass(UBRecProblem.class, conf)
                    .withMaxEvaluations(conf.getMaxEval()) // maximal function evaluations
                    .withAlgorithm(algorithm)
                    .distributeOnAllCores()
                    .run();

            
            for (Solution sol : rst) {            	
            	double ndcg=Math.abs(Math.abs(sol.getObjective(0)));
            	if(ndcg>bestNDCG) {
            		bestNDCG=ndcg;
            		bestSolution=sol;
            	}
            }
        }
        
        // output the learned student expectations into external files
        double[] array=EncodingUtils.getReal(bestSolution);
        BufferedWriter bw=FileIO.getWriter(path+"expectations_student_learned_by_UBRec.csv");
        bw.write("User, App, Data, Ease\n");
        bw.flush();
        
        int uid=999;
        for(int i=0;i<array.length;++i) {
        	bw.write((++uid)+", "+array[i]+", "+array[++i]+", "+array[++i]+"\n");
        	bw.flush();
        }
        bw.close();
        Logs.info("UBRec: Baseline UBRec has been executed.");
		Logs.info("UBRec: F1 = "+df.format(bestSolution.getAttribute("F1"))
		+", NDCG = "+df.format(bestNDCG)+", Utility_List_Student U(s, L) = "+df.format(bestSolution.getAttribute("UtilityStudent"))
		+ ", Utility_List_Instructor U(p, L) = "+df.format(bestSolution.getAttribute("UtilityInstructor"))
		+", Loss1 = "+df.format(bestSolution.getAttribute("Loss1"))
		+", Loss2 = "+df.format(bestSolution.getAttribute("Loss2")));
        Logs.info("UBRec: Student expectations have been learned and stored to "+path+"expectations_student_learned_by_UBRec.csv");
        	
    }
}
