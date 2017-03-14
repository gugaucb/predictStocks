package me.costa.gustavo.predictStocks.pocs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import weka.classifiers.Evaluation;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Debug.Random;
import weka.core.Instances;
import weka.core.Utils;

public class PoCWeka {
	public static void main(String[] args) {
		
	}

	/***
	 * Building a Neural Classifier
	 * 
	 * @param filepath
	 */
	public void simpleWekaTrain(String filepath) {
		try {
			// Reading training arff or csv file
			FileReader trainreader = new FileReader(filepath);
			Instances train = new Instances(trainreader);
			// train.setClassIndex(train.numAttributes() – 1);
			// Instance of NN
			MultilayerPerceptron mlp = new MultilayerPerceptron();
			// Setting Parameters
			mlp.setLearningRate(0.1);
			mlp.setMomentum(0.2);
			mlp.setTrainingTime(2000);
			mlp.setHiddenLayers("3?");
			mlp.buildClassifier(train);

			/*
			 * Another Way to set parameters, Where, L = Learning Rate M =
			 * Momentum N = Training Time or Epochs H = Hidden Layers etc.
			 */
			mlp.setOptions(Utils.splitOptions("-L 0.1 -M 0.2 -N 2000 -V 0 -S 0 -E 20 -H 3?"));

			/*
			 * Neural Classifier Training Validation For evaluation of training
			 * data,
			 */
			Evaluation eval = new Evaluation(train);
			eval.evaluateModel(mlp, train);
			System.out.println(eval.errorRate()); // Printing Training Mean root
													// squared Error
			System.out.println(eval.toSummaryString()); // Summary of Training

			// To apply K-Fold validation
			int kfolds = 10;
			eval.crossValidateModel(mlp, train, kfolds, new Random(1));

			// Evaluating/Predicting unlabelled data
			Instances datapredict = new Instances(new BufferedReader(new FileReader("<Predictdatapath>")));
			datapredict.setClassIndex(datapredict.numAttributes() - 1);
			Instances predicteddata = new Instances(datapredict);
			// Predict Part
			for (int i = 0; i < datapredict.numInstances(); i++) {
				double clsLabel = mlp.classifyInstance(datapredict.instance(i));
				predicteddata.instance(i).setClassValue(clsLabel);
			}
			// Storing again in arff
			BufferedWriter writer = new BufferedWriter(new FileWriter("<Output File Path>"));
			writer.write(predicteddata.toString());
			writer.newLine();
			writer.flush();
			writer.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
