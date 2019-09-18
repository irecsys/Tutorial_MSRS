package msrs.baseline;


import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.TreeMultimap;
import com.google.common.collect.Ordering;
import com.google.common.collect.Multimap;
import com.google.common.primitives.Doubles;

import happy.coding.io.FileConfiger;
import happy.coding.io.FileIO;
import happy.coding.io.Logs;
import happy.coding.math.Maths;
import happy.coding.math.Sims;
import happy.coding.math.Stats;
import msrs.demo.Config;
import happy.coding.math.Measures;
import happy.coding.io.Lists;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.text.DecimalFormat;
import java.util.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import org.moeaframework.core.variable.EncodingUtils;

import java.util.AbstractMap.SimpleImmutableEntry;

/**
 *
 * Rank the items and produce top-N recommendations by considering the utility of the items from the perspective of instructors ONLY.
 *
 * Yong Zheng, Nastaran Ghane, Milad Sabouri. "Personalized Educational Learning with Multi-Stakeholder Optimizations", Adjunct Proceedings of the 27th ACM Conference on User Modeling, Adaptation and Personalization (ACM UMAP), Cyprus, June, 2019
 */


public class Rankp {
	
	String path="";
	Config conf;
	String fileRatings_instructor="ratings_instructor.csv";
	String fileRatings_candidates="ratings_student_candidates.csv";
	String fileRatings_test="ratings_student_test.csv";
	String fileExpectations_student;
		
	int numRecs; // top-N recommendations, N = 5 by default
	// student and instructor expectations
    HashMap<String, ArrayList<Double>> exp_students = new HashMap<>();
    double[] exp_instructor;
	DecimalFormat df = new DecimalFormat("#.000");
    
    // ratings
    HashMap<String, ArrayList<Double>> ratings_instructor = new HashMap<>();
    HashMap<String, ArrayList<Double>> ratings_candidates= new HashMap<>();
    
    HashMap<String, Double> utilities_instructor;
    EuclideanDistance dist=new EuclideanDistance();
    HashMultimap<String, String> truth;
    HashMultimap<String, String> candidates;

    // metrics or objectives
    double utility_topN_students=0;
    double utility_topN_instructors=0;
    double utility_topN_diff=0;
    double f1_topN=0;
    double ndcg_topN=0;
	
	public Rankp(Config conf)  {
		try
		{
			this.conf=conf;
			this.path=conf.getPath();
			this.numRecs=conf.getNumRec();
			this.fileExpectations_student=conf.getExpectationFilename();

			// load student expectations from external file
			LoadExpectations_Student(fileExpectations_student);
			// set instructor expectations
			exp_instructor = new double[] {4.0, 4.0};	
			
			// load ratings by instructors
			LoadRatings_Instructor();
			// load ratings with candidates
			LoadRatings_Candidates();
			
			
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void run() throws Exception {
		
		// calculate utility of the items in view of instructors
		calUtility_Instructor();
		// collect Truth from the test set for the purpose of evaluations
		collectTruth();		
		// run recommendations and calculate metrics
		recommend();
		
		// output information
		double[] loss=this.getLoss();
		Logs.info("Rankp: Baseline Rankp has been executed.");
		Logs.info("Rankp: F1 = "+df.format(this.f1_topN)
        		+", NDCG = "+df.format(this.ndcg_topN)+", Utility_List_Student U(s, L) = "+df.format(this.utility_topN_students)
        		+ ", Utility_List_Instructor U(p, L) = "+df.format(this.utility_topN_instructors)
        		+", Loss1 = "+df.format(loss[0])
        		+", Loss2 = "+df.format(loss[1]));
	}
	
	protected double[] getLoss() {
		// maximal metrics from baseline approaches
        Double maxF1 = conf.getMaxF1(); // from UBRec
        Double maxNDCG = conf.getMaxNDCG(); // from UBRec  
        Double max_util_instructor = conf.getMaxUtil_Instructor(); // from Rankp
        Double max_util_student = conf.getMaxUtil_Student(); // from UBRec
        
        double loss1 = ( (max_util_instructor - this.utility_topN_instructors)/max_util_instructor
    			+ (max_util_student - this.utility_topN_students)/max_util_student
    			+ ( (maxF1 - this.f1_topN)/maxF1 + (maxNDCG - this.ndcg_topN)/maxNDCG )/2 )/3;
        double loss2 = ( (max_util_instructor - this.utility_topN_instructors)/max_util_instructor    			
    			+ ( (maxF1 - this.f1_topN)/maxF1 + (maxNDCG - this.ndcg_topN)/maxNDCG )/2 )/2;
        
        double[] loss = new double[] {loss1, loss2};
        return loss;
	}
	
	public double getUtility_students(){
		return this.utility_topN_students;
	}
	
	public double getUtility_instructors() {
		return this.utility_topN_instructors;
	}
	
	public double getUtility_diffs() {
		return this.utility_topN_diff;
	}
	
	public double getF1() {
		return this.f1_topN;
	}
	
	public double getNDCG() {
		return this.ndcg_topN;
	}
	
	protected void LoadRatings_Instructor() throws Exception {
        BufferedReader br = FileIO.getReader(this.path + fileRatings_instructor);
        String line = br.readLine();

        while ((line = br.readLine()) != null) {
            String[] strs = line.split(",");
            String item = strs[0].trim();
            this.ratings_instructor.put(item,
                    new ArrayList<Double>(Arrays.asList(
                            new Double(strs[1]),
                            new Double(strs[2]))
                    )
            );
        }
        br.close();
    }
	
	protected void LoadRatings_Candidates() throws Exception {
        BufferedReader br = FileIO.getReader(this.path + fileRatings_candidates);
        String line = br.readLine();
        candidates=HashMultimap.create();
        
        while ((line = br.readLine()) != null) {
            String[] strs = line.split(",");
            String student = strs[0].trim();
            String item = strs[1].trim();
            candidates.put(student, item);
            this.ratings_candidates.put(student+","+item,
                    new ArrayList<Double>(Arrays.asList(
                            new Double(strs[3]),
                            new Double(strs[4]),
                            new Double(strs[5]))                   		
                    )
            );
        }
        br.close();
    }
	
	protected void LoadExpectations_Student(String fileExpectations_student) throws Exception {
		BufferedReader br=FileIO.getReader(path+fileExpectations_student);
		String line=br.readLine();
		while((line=br.readLine())!=null) {
			String[] strs=line.split(",");
			this.exp_students.put(strs[0].trim(),
                    new ArrayList<Double>(Arrays.asList(
                    		Double.parseDouble(strs[1].trim()),
                    		Double.parseDouble(strs[2].trim()),
                    		Double.parseDouble(strs[3].trim()))));
		}
    }

	protected void calUtility_Instructor() throws Exception{
		utilities_instructor=new HashMap<>();
		for(String item:ratings_instructor.keySet()) {
			double[] rates=Doubles.toArray(ratings_instructor.get(item));
			double distance = dist.compute(this.exp_instructor, rates);
			// convert distance to dissimilarity: a normalization process
			// note that utility in view of instructor is the dissimilarity
			double dissim = minMaxNorm(distance, 0, Math.sqrt(50), 0, 1);
			utilities_instructor.put(item, dissim);
		}
	}
	
	protected double minMaxNorm(double x, double oldmin, double oldmax, double newmin, double newmax){
    	return newmin+(newmax-newmin)*(x-oldmin)/(oldmax-oldmin);
    }
	
	protected void collectTruth() throws Exception
	{
		truth=HashMultimap.create();
        BufferedReader br = FileIO.getReader(path + fileRatings_test);
        String line=br.readLine();
        while((line=br.readLine())!=null){
            String[] strs=line.split(",");
            String user=strs[0].trim();
            String item=strs[1].trim();
            double rate=new Double(strs[2].trim());
            if(rate>3) // this is an relevant item
            {
            	truth.put(user, item);
            }
        }
        br.close();		
	}
	
	private void recommend() throws Exception {
		
		HashMap<String, HashMap<String, Double>> rankingScore_student_item=new HashMap<>();		
		
		int count=0;
		for(String student:candidates.keySet()) {
			
			double[] exp_student = Doubles.toArray(exp_students.get(student));
			List<Map.Entry<String, Double>> itemScores = new ArrayList<>();
			HashMap<String, Double> utilities_student=new HashMap<String, Double>();
			
			// for each candidate item to be recommended
			// calculate ranking score of the items for each student
			for(String candidateItem:candidates.get(student)) {
				
				String key=student+","+candidateItem;
				double[] rate_student = Doubles.toArray(this.ratings_candidates.get(key));
				double distance = dist.compute(exp_student, rate_student);
				// convert distance to normalized similarity
				double itemUtility_student = 1 - this.minMaxNorm(distance, 0, Math.sqrt(75), 0, 1);
				double itemUtility_instructor = this.utilities_instructor.get(candidateItem);
				// rank by the item utility in view of instructor only
				double score = itemUtility_instructor;				
				itemScores.add(new SimpleImmutableEntry<String, Double>(candidateItem, score));
				utilities_student.put(candidateItem, itemUtility_student);
			}
			
			// produce top-N recommendations
			double utility_list_student=0;
        	double utility_list_instructor=0;
        	double utility_list_diff=0;
        	
			Lists.sortList(itemScores, true);
			List<String> rankedItems = new ArrayList<>();
        	List<Map.Entry<String, Double>> recomd = (numRecs <= 0 || itemScores.size() <= numRecs) ? itemScores
					: itemScores.subList(0, numRecs);
        	for (Map.Entry<String, Double> kv : recomd) {
				String item = kv.getKey();
				rankedItems.add(item);
				utility_list_student += utilities_student.get(item);
				utility_list_instructor += utilities_instructor.get(item);
        	}
        	
        	// calculate metrics
        	Set<String> relevantItems = truth.get(student);
        	List<String> correctItems = new ArrayList<>();
            correctItems.addAll(relevantItems);
            
            
            if(correctItems.size()>0 && rankedItems.size()>0) {
            	++count;
	        	double precision = Measures.PrecAt(rankedItems, correctItems, numRecs);
	        	double recall = Measures.RecallAt(rankedItems, correctItems, numRecs);
	        	double f1 = 0;
	        	if((precision+recall)!=0)
	        		f1 = 2*precision*recall/(precision+recall);
	        	double ndcg = Measures.nDCG(rankedItems, correctItems);
	        	utility_list_student/=rankedItems.size();
	        	utility_list_instructor/=rankedItems.size();
	        	utility_list_diff = Math.abs(utility_list_student - utility_list_instructor);
	        	
	        	utility_topN_students+=utility_list_student;
	        	utility_topN_instructors+=utility_list_instructor;
	        	utility_topN_diff+=utility_list_diff;
	        	f1_topN+=f1;
	        	ndcg_topN+=ndcg;    
            }
        	
		}		
		// calculate the final metrics over all students
        utility_topN_students/=count;
        utility_topN_instructors/=count;
        utility_topN_diff/=count;
        f1_topN/=count;
        ndcg_topN/=count;
    }
}