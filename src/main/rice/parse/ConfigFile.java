package main.rice.parse;
import main.rice.node.APyNode;
import java.util.*;

/**
 * A representation of the results of parsing a Config file.
 */
public class ConfigFile {

    /**
     * The name of the function under test.
     */
    private String funcName;
    /**
     * A List of APyNodes that are involved in the function in the Config file. These
     * will be used to generate TestCases for the function under test.
     */
    private List<APyNode<?>> nodes;
    /**
     * The number of random test cases to be generated.
     */
    private int numRand;

    /**
     * Constructor for a ConfigFile object; initializes all fields.
     *
     * @param funcName  name of function under test
     * @param nodes     APyNodes used to generate TestCases
     * @param numRand   number of random test cases to generate
     */
    public ConfigFile(String funcName, List<APyNode<?>> nodes, int numRand) {
        this.funcName = funcName;
        this.nodes = nodes;
        this.numRand = numRand;
    }

    /**
     * Returns the name of the function under test.
     *
     * @return name of the function under test
     */
    public String getFuncName() {
        return this.funcName;
    }

    /**
     * Returns the List of PyNodes that will be used to generate TestCases for the function under test.
     *
     * @return List of PyNodes that will be used to generate TestCases
     */
    public List<APyNode<?>> getNodes() {
        return this.nodes;
    }

    /**
     * Returns the number of random test cases to be generated.
     *
     * @return number of random test cases to be generated
     */
    public int getNumRand() {
        return this.numRand;
    }
}
