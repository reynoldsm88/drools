package org.drools.learner.builder;

import java.util.ArrayList;

import org.drools.learner.DecisionTree;
import org.drools.learner.Domain;
import org.drools.learner.Instance;
import org.drools.learner.InstanceList;
import org.drools.learner.Stats;
import org.drools.learner.eval.ClassDistribution;
import org.drools.learner.tools.LoggerFactory;
import org.drools.learner.tools.SimpleLogger;
import org.drools.learner.tools.Util;

public class BoostedTester extends Tester{
	
	private static SimpleLogger flog = LoggerFactory.getUniqueFileLogger(BoostedTester.class, SimpleLogger.DEFAULT_LEVEL);
	private static SimpleLogger slog = LoggerFactory.getSysOutLogger(BoostedTester.class, SimpleLogger.DEFAULT_LEVEL);
	
	private ArrayList<DecisionTree> trees;
	private ArrayList<Double> accuracy;
	private Domain targetDomain;
	
	public BoostedTester(ArrayList<DecisionTree> forest, ArrayList<Double> _accuracy) {
		trees = forest;
		accuracy = _accuracy;
		targetDomain = forest.get(0).getTargetDomain();
	}
	
	public Stats test(InstanceList data) {

		Stats evaluation = new Stats(data.getSchema().getObjectClass()) ; //represent.getObjClass());
		
		int i = 0;
		for (Instance instance : data.getInstances()) {
			Object forest_decision = this.voteOn(instance);
			Integer result = evaluate(targetDomain, instance, forest_decision);
			
			//flog.debug(Util.ntimes("#\n", 1)+i+ " <START> TEST: instant="+ instance + " = target "+ result);			
			if (i%1000 ==0 && slog.stat() != null)
					slog.stat().stat("."); 

			evaluation.change(result, 1);
			i ++;
		}	
		return evaluation;
		
		//printStats(evaluation, executionSignature);
	}
	
	public Object voteOn(Instance i) {
		ClassDistribution classification = new ClassDistribution(targetDomain);

		for (int j = 0; j< trees.size() ; j ++) {
			Object vote = trees.get(j).vote(i);
			if (vote != null) {
				classification.change(vote, accuracy.get(j));
				//classification.change(Util.sum(), accuracy.get(j));
			} else {
				// TODO add an unknown value
				//classification.change(-1, 1);
				if (flog.error() !=null)
					flog.error().log(Util.ntimes("\n", 10)+"Unknown situation at tree: " + j + " for fact "+ i);
				System.exit(0);
			}
			if (slog.debug() != null)
				slog.debug().log("Vote "+accuracy.get(j)+" for "+vote + "\n");
		}
		classification.evaluateMajority();
		Object winner = classification.get_winner_class();
		if (slog.debug() != null)
			slog.debug().log("Winner = "+winner + "\n");
		
		double ratio = 0.0;
		if (classification.get_num_ideas() == 1) {
			//100 %
			ratio = 1.0d;
			return winner;
		} else {
			double num_votes = classification.getVoteFor(winner);
			ratio = (num_votes/(double) trees.size());
			// TODO if the ratio is smaller than some number => reject
		}
		return winner;
		
	}
	
	public void printStats(final Stats evaluation, String executionSignature) {
		super.printStats(evaluation, executionSignature);
	}

}
