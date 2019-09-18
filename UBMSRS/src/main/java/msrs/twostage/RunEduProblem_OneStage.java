package msrs.twostage;

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


/**
*
* Utility-based multi-stakeholder recommendations: the basic solution
* Note: this implementation learns Alpha only
* Note: this implementation did not deal with the issue of over-/under- expectations
* 
* Yong Zheng, Nastaran Ghane, Milad Sabouri. "Personalized Educational Learning with Multi-Stakeholder Optimizations", Adjunct Proceedings of the 27th ACM Conference on User Modeling, Adaptation and Personalization (ACM UMAP), Cyprus, June, 2019
* 
*/



public class RunEduProblem_OneStage {
	String path=null;
	Config conf;
	DecimalFormat df = new DecimalFormat("#.000");
	
	public RunEduProblem_OneStage(Config conf) {
		this.conf=conf;
		this.path=conf.getPath();
	}

    public void execute() throws Exception {

        String[] algorithms = {"eMOEA", "NSGAII","NSGAIII", "MSOPS", "SMPSO", "OMOPSO"}; 

		// maximal metrics from baseline approaches
        Double maxF1 = conf.getMaxF1(); // from UBRec
        Double maxNDCG = conf.getMaxNDCG(); // from UBRec  
        Double max_util_instructor = conf.getMaxUtil_Instructor(); // from Rankp
        Double max_util_student = conf.getMaxUtil_Student(); // from UBRec
        
        Solution globalBest = null;
        double globalMinLoss1=999, globalMinLoss2=999;
        String globalBestAlgorithm=null;
        
        Logs.info("------------------------------------------------------------------------------");
        
        for (String algorithm : algorithms) {
        	Logs.info("Running MSRS by using "+algorithm+" as the optimizer...");
            NondominatedPopulation rst = new Executor()
                    .withProblemClass(EduProblem.class, conf)
                    .withMaxEvaluations(conf.getMaxEval()) // maximal function evaluations
                    .withAlgorithm(algorithm)
                    .distributeOnAllCores()
                    .run();
            
            Solution localBest=null;
            double localMinLoss1 = 999, localMinLoss2 = 999;
            
            for (Solution sol : rst) {            	
            	double f1=Math.abs(Math.abs(sol.getObjective(3)));
            	double ndcg=Math.abs(sol.getObjective(4));
            	double util_student=Math.abs(sol.getObjective(0));
            	double util_instructor=Math.abs(sol.getObjective(1));
            	
            	double loss1 = ( (max_util_instructor - util_instructor)/max_util_instructor
            			+ (max_util_student - util_student)/max_util_student
            			+ ( (maxF1 - f1)/maxF1 + (maxNDCG - ndcg)/maxNDCG )/2 )/3;
            	double loss2 = ( (max_util_instructor - util_instructor)/max_util_instructor
            			+ ( (maxF1 - f1)/maxF1 + (maxNDCG - ndcg)/maxNDCG )/2 )/2;
            	
            	if(loss2<localMinLoss2) {
            		localMinLoss1 = loss1;
            		localMinLoss2 = loss2;
            		localBest = sol;
            	}
            	
            	if(localMinLoss2<globalMinLoss2) {
            		globalMinLoss1 = localMinLoss1;
            		globalMinLoss2 = localMinLoss2;
            		globalBest = sol;
            		globalBestAlgorithm = algorithm;
            	}            		
            }
            double f1=Math.abs(Math.abs(localBest.getObjective(3)));
        	double ndcg=Math.abs(localBest.getObjective(4));
        	double util_student=Math.abs(localBest.getObjective(0));
        	double util_instructor=Math.abs(localBest.getObjective(1));
        	

    		Logs.info(algorithm+": F1 = "+df.format(f1)
            		+", NDCG = "+df.format(ndcg)+", Utility_List_Student U(s, L) = "+df.format(util_student)
            		+ ", Utility_List_Instructor U(p, L) = "+df.format(util_instructor)
            		+", Loss1 = "+df.format(localMinLoss1)
            		+", Loss2 = "+df.format(localMinLoss2)
            		+", Alpha = "+df.format(EncodingUtils.getReal(localBest.getVariable(0))));
        	
        }
        
        Logs.info("------------------------------------------------------------------------------");
        
        Logs.info("The best model was learned by "+globalBestAlgorithm+". Setting: expectation.learn=off");
        double f1=Math.abs(Math.abs(globalBest.getObjective(3)));
    	double ndcg=Math.abs(globalBest.getObjective(4));
    	double util_student=Math.abs(globalBest.getObjective(0));
    	double util_instructor=Math.abs(globalBest.getObjective(1));
    	Logs.info(globalBestAlgorithm+": F1 = "+df.format(f1)
		+", NDCG = "+df.format(ndcg)+", Utility_List_Student U(s, L) = "+df.format(util_student)
		+ ", Utility_List_Instructor U(p, L) = "+df.format(util_instructor)
		+", Loss1 = "+df.format(globalMinLoss1)
		+", Loss2 = "+df.format(globalMinLoss2)
		+", Alpha = "+df.format(EncodingUtils.getReal(globalBest.getVariable(0))));

    }
    
}
