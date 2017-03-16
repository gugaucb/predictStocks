package me.costa.gustavo.predictStocks.pocs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import weka.classifiers.Evaluation;
import weka.classifiers.functions.MultilayerPerceptron;
import weka.core.Debug.Random;
import weka.core.Instances;
import weka.core.SelectedTag;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.StringToWordVector;

public class PoCWeka {
	public static void main(String[] args) {
		simpleWekaTrain(PoCWeka.class.getClassLoader().getResource("trainDataSetTweet.arff").getPath());
	}

	/***
	 * Building a Neural Classifier
	 * 
	 * @param filepath
	 */
	public static void simpleWekaTrain(String filepath) {
		try {
			// Reading training arff or csv file
			FileReader trainreader = new FileReader(filepath);
			Instances train = new Instances(trainreader);
			 train.setClassIndex(train.numAttributes() - 1);
			// MultilayerPerceptron cls = (MultilayerPerceptron) weka.core.SerializationHelper.read("multilayerPerceptron"+System.currentTimeMillis()+".model");
			 
			 // Instance of NN
			MultilayerPerceptron mlp = new MultilayerPerceptron();
			// Setting Parameters
			mlp.setLearningRate(0.1);
			mlp.setMomentum(0.2);
			mlp.setTrainingTime(2000);
			mlp.setHiddenLayers("6");
			
			
			StringToWordVector filter = new StringToWordVector();

			// Initialize filter and tell it about the input format.
            filter.setInputFormat(train);
            filter.setMinTermFreq(10);
            filter.setOutputWordCounts(true);
            filter.setWordsToKeep(1000000);
            filter.setNormalizeDocLength(new SelectedTag(StringToWordVector.FILTER_NORMALIZE_ALL, StringToWordVector.TAGS_FILTER));
			// Generate word counts from the training data.
			Instances filteredData = Filter.useFilter(train, filter);
			
			filteredData.setClassIndex(filteredData.numAttributes()-1);
			System.out.println(" iniciando buildClassifier");
			mlp.buildClassifier(filteredData);
			System.out.println(" fim buildClassifier");
			/*
			 * Another Way to set parameters, Where, L = Learning Rate M =
			 * Momentum N = Training Time or Epochs H = Hidden Layers etc.
			 */
			mlp.setOptions(Utils.splitOptions("-L 0.1 -M 0.2 -N 2000 -V 0 -S 0 -E 20 -H 6"));
			//salvar model
			weka.core.SerializationHelper.write("multilayerPerceptron"+System.currentTimeMillis()+".model", mlp);
			/*
			 * Neural Classifier Training Validation For evaluation of training
			 * data,
			 */
			System.out.println(" iniciando Evaluation");
			Evaluation eval = new Evaluation(filteredData);
			eval.evaluateModel(mlp, filteredData);
			System.out.println(eval.errorRate()); // Printing Training Mean root
													// squared Error
			System.out.println(eval.toSummaryString()); // Summary of Training
			System.out.println(" fim Evaluation");
			// To apply K-Fold validation
			int kfolds = 10;
			System.out.println(" iniciando crossValidateModel");
			eval.crossValidateModel(mlp, filteredData, kfolds, new Random(1));
			System.out.println(" fim crossValidateModel");
			weka.core.SerializationHelper.write("multilayerPerceptron"+System.currentTimeMillis()+".model", mlp);
			
			// Evaluating/Predicting unlabelled data
			Instances test = new Instances(new BufferedReader(new FileReader(PoCWeka.class.getResource("testDataSetTweet.arff").getPath())));
			
			
			StringToWordVector filterTest = new StringToWordVector();
			 // Initialize filter and tell it about the input format.
			test.setClassIndex(test.numAttributes()-1);
			filterTest.setInputFormat(test);
			filterTest.setMinTermFreq(10);
			filterTest.setOutputWordCounts(true);
			filterTest.setWordsToKeep(1000000);
			filterTest.setNormalizeDocLength(new SelectedTag(StringToWordVector.FILTER_NORMALIZE_ALL, StringToWordVector.TAGS_FILTER));
			// Generate word counts from the training data.
			Instances datapredict = Filter.useFilter(test, filterTest);
			
			

			
			
			
			System.out.println(" iniciando predicteddata");
			datapredict.setClassIndex(datapredict.numAttributes() - 1);
			Instances predicteddata = new Instances(datapredict);
			// Predict Part
			for (int i = 0; i < datapredict.numInstances(); i++) {
				double clsLabel = mlp.classifyInstance(datapredict.instance(i));
				predicteddata.instance(i).setClassValue(clsLabel);
			}
			// Storing again in arff
			BufferedWriter writer = new BufferedWriter(new FileWriter("text.arff"));
			writer.write(predicteddata.toString());
			writer.newLine();
			writer.flush();
			writer.close();

		
			
			System.out.println(" fim predicteddata");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
