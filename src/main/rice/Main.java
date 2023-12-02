package main.rice;

import main.rice.basegen.BaseSetGenerator;
import main.rice.concisegen.ConciseSetGenerator;
import main.rice.parse.ConfigFile;
import main.rice.parse.ConfigFileParser;
import main.rice.parse.InvalidConfigException;
import main.rice.test.TestCase;
import main.rice.test.TestResults;
import main.rice.test.Tester;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Main class to execute FEAT tool.
 */
public class Main {
    /**
     * The Main function for the FEAT tool.
     *
     * @param args
     * @throws IOException
     * @throws InvalidConfigException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws IOException, InvalidConfigException, InterruptedException {
        System.out.print("This is a concise set of test cases that should be used to evaluate the function under test.");
        System.out.print(generateTests(args));
    }

    /**
     * A helper function to generate a concise set of tests.
     *
     * @param args
     * @return
     * @throws IOException
     * @throws InvalidConfigException
     * @throws InterruptedException
     */
    public static Set<TestCase> generateTests(String[] args) throws IOException, InvalidConfigException, InterruptedException {
        String contents = ConfigFileParser.readFile(args[0]);
        ConfigFile configFile = ConfigFileParser.parse(contents);

        BaseSetGenerator baseSet = new BaseSetGenerator(configFile.getNodes(),configFile.getNumRand());
        Tester tester = new Tester(configFile.getFuncName(), args[1], args[2], baseSet.genBaseSet());
        tester.computeExpectedResults();
        TestResults results = tester.runTests();
        return ConciseSetGenerator.setCover(results);
    }
}

// TODO: implement the Main class here