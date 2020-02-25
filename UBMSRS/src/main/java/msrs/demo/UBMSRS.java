package msrs.demo;

import java.util.Arrays;
import java.util.List;

import org.apache.log4j.PropertyConfigurator;

import happy.coding.io.FileConfiger;
import happy.coding.io.FileIO;
import happy.coding.io.LineConfiger;
import happy.coding.io.Logs;
import msrs.baseline.Rankp;
import msrs.baseline.ubrec.RunUBRec;
import msrs.twostage.RunEduProblem_TwoStage;
import msrs.onestage.RunEduProblem_OneStage;


public class UBMSRS {
	
	protected List<String> configFiles=null;
	protected String path;
	protected Config conf;

	
	public static void main(String[] args) throws Exception {
		try {
			new UBMSRS().execute(args);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
	protected void execute(String[] args) throws Exception {

		cmdLine(args);
		
		FileConfiger cf = new FileConfiger(configFiles.get(0));
		path=cf.getPath("data.path").trim();
		Logs.info("Local path of your data: "+this.path);
		
		String filename_expectation = cf.getString("expectation.filename").trim();
		int topN=cf.getInt("topN");
		int max=cf.getInt("maxeval");
		
		double maxf1=cf.getDouble("maxf1");
		double maxndcg=cf.getDouble("maxndcg");
		double maxutil_p=cf.getDouble("maxutil_instructor");
		double maxutil_s=cf.getDouble("maxutil_student");
		
		conf=new Config(path, filename_expectation, topN, max, maxf1, maxndcg, maxutil_p, maxutil_s);
		
		// Run baseline
		String runbaseline=cf.getString("runbaseline").toLowerCase().trim();
		if(runbaseline.equals("on")) {		
			Logs.info("The system is running baseline approaches...");
			RunBaseline(conf);
		}		
		
		// Run multi-stakeholder recommendations
		String learn=cf.getString("expectation.learn").toLowerCase().trim();		
		if(learn.equals("on")) {
			// learn user expectations and Alpha
			RunEduProblem_OneStage moo_learn=new RunEduProblem_OneStage(conf);
			moo_learn.execute();
		}else if(learn.equals("off")){
			// load user expectations and learn Alpha only
			RunEduProblem_TwoStage moo_learn=new RunEduProblem_TwoStage(conf);
			moo_learn.execute();
		}else {
			Logs.error("expectation.learn: incorrect setting");
		}
	}

	
	protected void cmdLine(String[] args) throws Exception {

		if (args == null || args.length < 1) {
			if (configFiles == null)
				configFiles = Arrays.asList("setting.conf");
			return;
		}

		LineConfiger paramOptions = new LineConfiger(args);
		configFiles = paramOptions.contains("-c") ? paramOptions.getOptions("-c") : Arrays.asList("setting.conf");
	}
	
	protected void RunBaseline(Config conf) throws Exception{
		RunUBRec ubrec = new RunUBRec(conf);
		ubrec.execute();	
		
		Rankp rankp = new Rankp(conf);
		rankp.run();
	}
	
}
