package me.costa.gustavo.predictStocks.pocs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

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

			Instances train = carregarDataTrain(filepath);
			 MultilayerPerceptron mlp;
			Instances filteredData = filtrarInstancesTrain(train);
			try{ 
				mlp = carregarModelo();
			}catch(Exception e){
				mlp = criarModelo();
				buildClassifier(filteredData, mlp);
				salvarModelo(mlp);			
			}


			
			

			/* *//** Another Way to set parameters, Where, L = Learning Rate M =
			 * Momentum N = Training Time or Epochs H = Hidden Layers etc.
			 *//*
			mlp.setOptions(Utils.splitOptions("-L 0.1 -M 0.2 -N 2000 -V 0 -S 0 -E 20 -H 6"));*/

			
			/* * Neural Classifier Training Validation For evaluation of training
			 * data,*/
			 
			/*Evaluation eval = executarEvaluation(mlp, filteredData);
			salvarModelo(mlp);
			// To apply K-Fold validation
			executarCrossValidateModel(mlp, filteredData, eval);
			salvarModelo(mlp);*/
			// Evaluating/Predicting unlabelled data
			Instances test = carregarDataTest();
			
			
			Instances datapredict = filtrarTestInstances(test);
			
			

			
			
			
			classificarDataTest(mlp, datapredict);

			salvarModelo(mlp);
			
			System.out.println(" fim predicteddata");
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	private static Instances buildClassifier(Instances train, MultilayerPerceptron mlp) throws Exception {
		Instances filteredData = filtrarInstancesTrain(train);
		mlp.buildClassifier(filteredData);
		System.out.println(" fim buildClassifier");
		return filteredData;
	}

	private static Instances filtrarInstancesTrain(Instances train) throws Exception {
		Instances filteredData = filtrarInstances(train);
		System.out.println(" iniciando buildClassifier");
		return filteredData;
	}

	private static void salvarModelo(MultilayerPerceptron mlp) throws Exception {
		weka.core.SerializationHelper.write("multilayerPerceptron"+System.currentTimeMillis()+".model", mlp);
	}

	private static void classificarDataTest(MultilayerPerceptron mlp, Instances datapredict)
			throws Exception, IOException {
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
	}

	private static Instances filtrarTestInstances(Instances test) throws Exception {
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
		return datapredict;
	}

	private static Instances carregarDataTest() throws IOException, FileNotFoundException {
		Instances test = new Instances(new BufferedReader(new FileReader(PoCWeka.class.getClassLoader().getResource("testDataSetTweet.arff").getPath())));
		return test;
	}

	private static void executarCrossValidateModel(MultilayerPerceptron mlp, Instances filteredData, Evaluation eval)
			throws Exception {
		int kfolds = 10;
		System.out.println(" iniciando crossValidateModel");
		eval.crossValidateModel(mlp, filteredData, kfolds, new Random(1));
		System.out.println(" fim crossValidateModel");
	}

	private static Evaluation executarEvaluation(MultilayerPerceptron mlp, Instances filteredData) throws Exception {
		System.out.println(" iniciando Evaluation");
		Evaluation eval = new Evaluation(filteredData);
		eval.evaluateModel(mlp, filteredData);
		System.out.println(eval.errorRate()); // Printing Training Mean root
												// squared Error
		System.out.println(eval.toSummaryString()); // Summary of Training
		System.out.println(" fim Evaluation");
		return eval;
	}

	private static Instances carregarDataTrain(String filepath) throws FileNotFoundException, IOException {
		FileReader trainreader = new FileReader(filepath);
		Instances train = new Instances(trainreader);
		 train.setClassIndex(train.numAttributes() - 1);
		return train;
	}

	private static Instances filtrarInstances(Instances train) throws Exception {
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
		return filteredData;
	}

	private static MultilayerPerceptron carregarModelo() throws Exception {
		return (MultilayerPerceptron) weka.core.SerializationHelper.read(PoCWeka.class.getClassLoader().getResource("multilayerPerceptron1489711727528.model").getPath());
	}

	private static MultilayerPerceptron criarModelo() {
		// Instance of NN
		MultilayerPerceptron mlp = new MultilayerPerceptron();
		// Setting Parameters
		mlp.setLearningRate(0.1);
		mlp.setMomentum(0.2);
		mlp.setTrainingTime(2000);
		mlp.setHiddenLayers("6");
		return mlp;
	}
}
