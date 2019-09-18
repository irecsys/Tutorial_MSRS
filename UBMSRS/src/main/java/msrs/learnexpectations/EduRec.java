package msrs.learnexpectations;

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
import java.util.*;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.ml.distance.EuclideanDistance;
import java.util.AbstractMap.SimpleImmutableEntry;


/**
*
* Utility-based multi-stakeholder recommendations: the basic solution
* Note: this implementation learns both Alpha and student expectations
* Note: this implementation did not deal with the issue of over-/under- expectations
* 
* Yong Zheng, Nastaran Ghane, Milad Sabouri. "Personalized Educational Learning with Multi-Stakeholder Optimizations", Adjunct Proceedings of the 27th ACM Conference on User Modeling, Adaptation and Personalization (ACM UMAP), Cyprus, June, 2019
* 
*/



public class EduRec {
	
	String path="";
	Config conf;
	String fileRatings_instructor="ratings_instructor.csv";
	String fileRatings_candidates="ratings_student_candidates.csv";
	String fileRatings_test="ratings_student_test.csv";
		
	int numRecs; // top-N recommendations
	double alpha=1.0;
	// student and instructor expectations
    HashMap<String, ArrayList<Double>> exp_students = new HashMap<>();
    double[] exp_instructor;
    
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
	
	public EduRec(Config conf, double[] parameters)  {
		try
		{
			this.conf=conf;
			this.path=conf.getPath();
			this.numRecs=conf.getNumRec();
			
			this.alpha=parameters[0];
			// load student expectations from the parameters
			LoadExpectations_Student(parameters);
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
	
	protected void LoadExpectations_Student(double[] parameters) throws Exception {
		// we have UserID: 1000 to 1331, 332 users
		int student = 1000;
        for (int i = 1; i < parameters.length; ++i) { 
            this.exp_students.put(""+student,
                    new ArrayList<Double>(Arrays.asList(
                    		parameters[i],
                    		parameters[++i],
                    		parameters[++i])));
            student++;
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
				double score = this.alpha*itemUtility_student + (1 - this.alpha)*itemUtility_instructor;				
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