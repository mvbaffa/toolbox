package eu.amidst.cim2015.examples;


import eu.amidst.core.datastream.DataInstance;
import eu.amidst.core.datastream.DataStream;
import eu.amidst.core.io.DataStreamLoader;
import eu.amidst.core.io.DataStreamWriter;
import eu.amidst.core.learning.parametric.ParallelMaximumLikelihood;
import eu.amidst.core.models.BayesianNetwork;
import eu.amidst.core.models.DAG;
import eu.amidst.core.utils.BayesianNetworkSampler;
import eu.amidst.core.utils.OptionParser;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.variables.Variables;

import java.io.IOException;
import java.util.stream.IntStream;

/**
 * Created by ana@cs.aau.dk on 30/06/15.
 */
public final class batchSizeComparisonsML {

    static boolean parallel = true;
    static boolean sampleData = true;
    static int sampleSize = 1000000;
    static int[] batchSizes = {100, 200, 500, 1000, 2000, 5000, 10000};
    static int numDiscVars = 5000;
    static int numGaussVars = 5000;
    static int numStatesHiddenDiscVars = 10;
    static int numHiddenGaussVars = 2;
    static int numStates = 10;

    public static boolean isParallel() {
        return parallel;
    }

    public static void setParallel(boolean parallel) {
        batchSizeComparisonsML.parallel = parallel;
    }

    public static int getSampleSize() {
        return sampleSize;
    }

    public static void setSampleSize(int sampleSize) {
        batchSizeComparisonsML.sampleSize = sampleSize;
    }

    public static int[] getBatchSizes() {
        return batchSizes;
    }

    public static void setBatchSizes(int[] batchSizes) {
        batchSizeComparisonsML.batchSizes = batchSizes;
    }

    public static int getNumDiscVars() {
        return numDiscVars;
    }

    public static void setNumDiscVars(int numDiscVars) {
        batchSizeComparisonsML.numDiscVars = numDiscVars;
    }

    public static int getNumGaussVars() {
        return numGaussVars;
    }

    public static void setNumGaussVars(int numGaussVars) {
        batchSizeComparisonsML.numGaussVars = numGaussVars;
    }

    public static int getNumStatesHiddenDiscVars() {
        return numStatesHiddenDiscVars;
    }

    public static void setNumStatesHiddenDiscVars(int numStatesHiddenDiscVars) {
        batchSizeComparisonsML.numStatesHiddenDiscVars = numStatesHiddenDiscVars;
    }

    public static int getNumHiddenGaussVars() {
        return numHiddenGaussVars;
    }

    public static void setNumHiddenGaussVars(int numHiddenGaussVars) {
        batchSizeComparisonsML.numHiddenGaussVars = numHiddenGaussVars;
    }

    public static int getNumStates() {
        return numStates;
    }

    public static void setNumStates(int numStates) {
        batchSizeComparisonsML.numStates = numStates;
    }

    public static boolean isSampleData() {
        return sampleData;
    }

    public static void setSampleData(boolean sampleData) {
        batchSizeComparisonsML.sampleData = sampleData;
    }

    static DAG dag;

    public static void compareBatchSizes() throws IOException {

        createBayesianNetwork();
        if(isSampleData())
            sampleBayesianNetwork();

        DataStream<DataInstance> data = DataStreamLoader.openFromFile("datasets/sampleBatchSize.arff");


        ParallelMaximumLikelihood parameterLearningAlgorithm = new ParallelMaximumLikelihood();
        parameterLearningAlgorithm.setParallelMode(isParallel());
        parameterLearningAlgorithm.setDAG(dag);
        parameterLearningAlgorithm.setDataStream(data);


        System.out.println("Available number of processors: "+Runtime.getRuntime().availableProcessors());
        System.out.println("BatchSize\tAverageTime");
        //We discard the first five experiments and then record the following 10 repetitions
        for (int i = 0; i < batchSizes.length; i++) {
            long average = 0L;
            for (int j = 0; j <16; j++) {
                parameterLearningAlgorithm.setBatchSize(batchSizes[i]);
                long start = System.nanoTime();
                parameterLearningAlgorithm.runLearning();
                long duration = (System.nanoTime() - start) / 1_000_000;
                //System.out.println("Iteration ["+j+"] = "+duration + " msecs");
                if(j>4){
                    average+=duration;
                }
            }
            System.out.println(batchSizes[i]+"\t"+average/10.0 + " msecs");
        }
    }

    private static void createBayesianNetwork(){
        /* ********** */
        /* Create DAG */
        /* Create all variables */
        Variables variables = new Variables();

        IntStream.range(0, getNumDiscVars() -1)
                .forEach(i -> variables.newMultionomialVariable("DiscreteVar" + i, getNumStates()));

        IntStream.range(0, getNumGaussVars())
                .forEach(i -> variables.newGaussianVariable("GaussianVar" + i));

        Variable classVar = variables.newMultionomialVariable("ClassVar", getNumStates());

        if(getNumHiddenGaussVars() > 0)
            IntStream.rangeClosed(0, getNumHiddenGaussVars() - 1).forEach(i -> variables.newGaussianVariable("GaussianSPVar_" + i));
        //if(numStatesHiddenDiscVars > 0)
        Variable discreteHiddenVar = variables.newMultionomialVariable("DiscreteSPVar", getNumStatesHiddenDiscVars());

        dag = new DAG(variables);

        /* Link variables */
        dag.getParentSets().stream()
                .filter(parentSet -> !parentSet.getMainVar().equals(classVar) && !parentSet.getMainVar().getName().startsWith("GaussianSPVar_")
                        && !parentSet.getMainVar().getName().startsWith("DiscreteSPVar"))
                .forEach(w -> {
                    w.addParent(classVar);
                    w.addParent(discreteHiddenVar);
                    if (getNumHiddenGaussVars() > 0 && w.getMainVar().isNormal())
                        IntStream.rangeClosed(0, getNumHiddenGaussVars() - 1).forEach(i -> w.addParent(variables.getVariableByName("GaussianSPVar_" + i)));
                });

        /*Add classVar as parent of all super-parent variables*/
        if(getNumHiddenGaussVars() > 0)
            IntStream.rangeClosed(0, getNumHiddenGaussVars()-1).parallel()
                    .forEach(hv -> dag.getParentSet(variables.getVariableByName("GaussianSPVar_" + hv)).addParent(classVar));
        dag.getParentSet(variables.getVariableByName("DiscreteSPVar")).addParent(classVar);


        System.out.println(dag.toString());
    }
    private static void sampleBayesianNetwork()  throws IOException {


        BayesianNetwork bn = BayesianNetwork.newBayesianNetwork(dag);

        BayesianNetworkSampler sampler = new BayesianNetworkSampler(bn);
        sampler.setSeed(0);

        //The method sampleToDataStream returns a DataStream with ten DataInstance objects.
        DataStream<DataInstance> dataStream = sampler.sampleToDataStream(getSampleSize());

        //We finally save the sampled data set to an arff file.
        DataStreamWriter.writeDataToFile(dataStream, "datasets/sampleBatchSize.arff");
    }


    public static void main(String[] args) throws Exception {
        OptionParser.setArgsOptions(batchSizeComparisonsML.class, args);
        batchSizeComparisonsML.loadOptions();
        compareBatchSizes();
    }

    public static String classNameID(){
        return "eu.amidst.cim2015.examples.batchSizeComparisonsML";
    }

    public static String getOption(String optionName) {
        return OptionParser.parse(classNameID(), listOptions(), optionName);
    }

    public static int getIntOption(String optionName){
        return Integer.parseInt(getOption(optionName));
    }

    public static boolean getBooleanOption(String optionName){
        return getOption(optionName).equalsIgnoreCase("true") || getOption(optionName).equalsIgnoreCase("T");
    }

    public static String listOptions(){

        return  classNameID() +",\\"+
                "-sampleSize, 1000000, Sample size of the dataset\\" +
                "-numStates, 10, Num states of all disc. variables (including the class)\\"+
                "-GV, 5000, Num of gaussian variables\\"+
                "-DV, 5000, Num of discrete variables\\"+
                "-SPGV, 2, Num of gaussian super-parent variables\\"+
                "-SPDV, 10, Num of states for super-parent discrete variable\\"+
                "-sampleData, true, Sample arff data (if not read datasets/sampleBatchSize.arff by default)\\"+
                "-parallelMode, true, Run in parallel\\";
    }

    public static void loadOptions(){
        setNumGaussVars(getIntOption("-GV"));
        setNumDiscVars(getIntOption("-DV"));
        setNumHiddenGaussVars(getIntOption("-SPGV"));
        setNumStatesHiddenDiscVars(getIntOption("-SPDV"));
        setNumStates(getIntOption("-numStates"));
        setSampleSize(getIntOption("-sampleSize"));
        setSampleData(getBooleanOption("-sampleData"));
        setParallel(getBooleanOption("-parallelMode"));
    }

}