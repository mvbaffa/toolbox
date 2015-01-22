package eu.amidst.core.distribution;

import eu.amidst.core.variables.Assignment;
import eu.amidst.core.variables.Variable;
import eu.amidst.core.utils.MultinomialIndex;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * <h2>This class implements a conditional distribution of a normal variable given a set of multinomial parents.</h2>
 *
 * @author Antonio Fernández
 * @version 1.0
 * @since 2014-11-4
 */
public class Normal_MultinomialParents extends ConditionalDistribution implements Serializable {


    private static final long serialVersionUID = -68936476817641844L;

    /**
     * An array of normal distribution, one for each assignment of the multinomial parents
     */
    private Normal[] distribution;


    /**
     * The class constructor.
     * @param var1 The variable of the distribution.
     * @param parents1 The set of parent variables.
     */
    public Normal_MultinomialParents(Variable var1, List<Variable> parents1) {
        this.var = var1;
        this.parents = parents1;

        //Initialize the distribution uniformly for each configuration of the parents.
        int size = MultinomialIndex.getNumberOfPossibleAssignments(parents);

        this.distribution = new Normal[size];
        for (int i = 0; i < size; i++) {
            this.distribution[i] = new Normal(var);
        }

        //Make them unmodifiable
        this.parents = Collections.unmodifiableList(this.parents);

    }

    public Normal getNormal(int position) {
        return distribution[position];
    }




    @Override
    public int getNumberOfFreeParameters() {
        int n=0;
        for(Normal dist:this.getDistribution()){
            n+=dist.getNumberOfFreeParameters();
        }
        return n;
    }

    /**
     * Gets the corresponding univariate normal distribution after conditioning the distribution to a multinomial
     * parent assignment.
     * @param parentsAssignment An <code>Assignment</code> for the parents.
     * @return A <code>Normal</code> object with the univariate distribution.
     */
     public Normal getNormal(Assignment parentsAssignment) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.parents, parentsAssignment);
        return this.getNormal(position);
    }

    /**
     * Sets a <code>Normal</code> distribution in a given position in the array of distributions.
     * @param position The position in which the distribution is set.
     * @param normalDistribution The <code>Normal</code> distribution to be set.
     */
    public void setNormal(int position, Normal normalDistribution) {
        this.distribution[position] = normalDistribution;
    }

    /**
     * Sets a <code>Multinomial</code> distribution in a position in the array of distributions determined by a given
     * parents assignment.
     * @param parentsAssignment An <code>Assignment</code> for the parents.
     * @param normalDistribution The <code>Normal</code> distribution to be set.
     */
    public void setNormal(Assignment parentsAssignment, Normal normalDistribution) {
        int position = MultinomialIndex.getIndexFromVariableAssignment(this.parents, parentsAssignment);
        this.setNormal(position, normalDistribution);
    }

    /**
     * Computes the logarithm of the evaluated density function in a point after conditioning the distribution to a
     * given parent <code>Assignment</code>.
     * @param assignment An <code>Assignment</code> for the parents.
     * @return A <code>double</code> with the logarithm of the corresponding density value.
     */
    @Override
    public double getLogConditionalProbability(Assignment assignment) {

        double value = assignment.getValue(this.var);
        return this.getNormal(assignment).getLogProbability(value);
    }

    @Override
    public UnivariateDistribution getUnivariateDistribution(Assignment assignment) {
        return this.getNormal(assignment);
    }

    public String label(){
        if (this.getConditioningVariables().size()==0) {
            return "Normal";
        }
        else {
            return "Normal|Multinomial";
        }
    }

    @Override
    public void randomInitialization(Random random) {
        for (int i = 0; i < this.distribution.length; i++) {
            this.distribution[i].randomInitialization(random);
        }
    }

    public Normal[] getDistribution() {
        return distribution;
    }

    public int getNumberOfParentAssignments(){
        return getDistribution().length;
    }

    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("");
        for(int i=0;i<getNumberOfParentAssignments();i++){
            str.append(this.getNormal(i).toString() +"\n");
        }
        return str.toString();
    }

    @Override
    public boolean equalDist(ConditionalDistribution dist, double threshold) {
        if (dist.getClass().getName().equals("eu.amidst.core.distribution.Normal_MultinomialParents"))
            return this.equalDist((Normal_MultinomialParents)dist,threshold);
        return false;
    }

    public boolean equalDist(Normal_MultinomialParents dist, double threshold) {
        boolean equals = true;
        for (int i = 0; i < this.getDistribution().length; i++) {
            equals = equals && this.getNormal(i).equalDist(dist.getNormal(i),threshold);
        }
        return equals;
    }


}
