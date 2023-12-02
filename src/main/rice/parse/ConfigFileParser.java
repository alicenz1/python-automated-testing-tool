package main.rice.parse;

import java.io.*;
import java.nio.file.Files;

import main.rice.node.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.Math.floor;

/**
 * Parser for the config file containing data on the function under test.
 */
public class ConfigFileParser {

    /**
     * Reads and returns the contents of the file located at the input filepath;
     * throws an IOException if the file does not exist or cannot be read.
     *
     * @param filepath      string representing file path to desired config file
     * @return contents of the file at filepath in a single String
     * @throws IOException  file does not exist or cannot be read
     */
    public static String readFile(String filepath) throws IOException {
        return Files.readString(Paths.get(filepath));
    }

    /**
     * Parses the config file and returns a ConfigFile containing data such as function name,
     * APyNodes involved, as well as the number of random test cases desired.
     * @param contents      contents of the config file to be parsed
     * @return ConfigFile   containing data such as function name, APyNodes involved, as well as
     *                      the number of random test cases desired.
     * @throws InvalidConfigException   part of the config file is missing or malformed
     */
    public static ConfigFile parse(String contents) throws InvalidConfigException {

        //extracts JSON object from file contents
        JSONObject obj;

        try {
            obj = new JSONObject(contents);
        } catch (JSONException e) {
            throw new InvalidConfigException("File does not contain a valid JSONObject");
        }

        //extracts value of fname from JSONObject
        String fname;

        try {
            fname = (String) obj.get("fname");
        } catch (JSONException e) {
            throw new InvalidConfigException("fname key does not exist");
        } catch (ClassCastException e) {
            throw new InvalidConfigException("fname value is not a String");
        }

        //extracts value of types from JSONObject
        JSONArray types;

        try {
            types = (JSONArray) obj.get("types");
        } catch (JSONException e) {
            throw new InvalidConfigException("types key does not exist");
        } catch (ClassCastException e) {
            throw new InvalidConfigException("types value is not a JSONArray");
        }

        //extracts value of exhaustive domain from JSONObject
        JSONArray exDomain;

        try {
            exDomain = (JSONArray) obj.get("exhaustive domain");
        } catch (JSONException e) {
            throw new InvalidConfigException("exhaustive domain key does not exist");
        } catch (ClassCastException e) {
            throw new InvalidConfigException("exhaustive domain value is not a JSONArray");
        }

        //extracts value of random domain from JSONObject
        JSONArray ranDomain;

        try {
            ranDomain = (JSONArray) obj.get("random domain");
        } catch (JSONException e) {
            throw new InvalidConfigException("random domain key does not exist");
        } catch (ClassCastException e) {
            throw new InvalidConfigException("random domain value is not a JSONArray");
        }

        //extracts value of numRand from JSONObject
        int numRand;

        try {
            numRand = (int) obj.get("num random");
        } catch (JSONException e) {
            throw new InvalidConfigException("num random key does not exist");
        } catch (ClassCastException e) {
            throw new InvalidConfigException("num random is not an integer");
        }

        //handles attempted negative value of numRand, cannot have a negative amount of tests
        if (numRand < 0) {
            throw new InvalidConfigException("num random is a negative integer");
        }

        //parsing types
        List<APyNode<?>> typeParams = new ArrayList<>();

        for (int i = 0; i < types.length(); i++ ) {
            String type;

            try {
                type = (String) types.get(i);
            } catch (ClassCastException e) {
                throw new InvalidConfigException("types value is not a JSONArray of Strings");
            }

            typeParams.add(parseType(type));
        }

        //parsing domains
        List<List<? extends Number>> exDomParams = new ArrayList<>();
        List<List<? extends Number>> ranDomParams = new ArrayList<>();

        //checks that each of the parameter lists are the same size
        if (exDomain.length() != typeParams.size() || ranDomain.length() != typeParams.size()) {
            throw new InvalidConfigException("types and domain parameter structure do not match");
        }

        //parses and adds exhaustive domain into associated APyNode
        for (int i = 0; i < exDomain.length(); i++) {
            String exDom;

            try {
                exDom = (String) exDomain.get(i);
            } catch (ClassCastException e) {
                throw new InvalidConfigException("exhaustive domain's value is not a JSONArray of Strings");
            }

            parseDomain(exDom, typeParams.get(i), "exhaustive");
        }

        //parses and adds random domain into associated APyNode
        for (int i = 0; i < ranDomain.length(); i++) {
            String ranDom;

            try {
                ranDom = (String) ranDomain.get(i);
            } catch (ClassCastException e) {
                throw new InvalidConfigException("random domain's value is not a JSONArray of Strings");
            }

            parseDomain(ranDom, typeParams.get(i), "random");
        }
        return new ConfigFile(fname,typeParams,numRand);
    }

    /**
     * A helper function to parse types. Returns an APyNode object of the associated type.
     *
     * @param type  a string of a python type
     * @return      an APyNode object of the type indicated in the input
     * @throws InvalidConfigException   part of the type is missing or malformed
     */
    private static APyNode<?> parseType(String type) throws InvalidConfigException {
        type = type.strip();
        int parInd = type.indexOf("(");

        //no parenthesis found, must be simpletype
        if (parInd == -1) {
            return parseSimpleType(type);

        }
        //string type, handles chardomain
        else if (type.substring(0, parInd).strip().equals("str")) {
            String stringVal;
            Set<Character> charVal = new HashSet<>();

            //parses chardomain
            try {
                stringVal = (String) type.substring(parInd+1);
            } catch (ClassCastException e) {
                throw new InvalidConfigException("Expected str val is not a String");
            }

            stringVal = stringVal.strip();

            //handles empty chardomain
            if (stringVal.isEmpty()) {
                throw new InvalidConfigException("character domain is empty");
            }

            //convert to a set of characters
            for (char c : stringVal.toCharArray()) {
                charVal.add(c);
            }
            return new PyStringNode(charVal);

        }
        //dictionary type
        else if (type.substring(0, parInd).strip().equals("dict")) {
            int colonInd = type.indexOf(":");

            return new PyDictNode<>(parseType(type.substring(parInd+1, colonInd)),
                    parseType(type.substring(colonInd+1)));

        } else {
            return parseIterableType(type);
        }

    }

    /**
     * A helper function to parse simple types. Returns an APyNode object of the associated type.
     *
     * @param type      a string of a simple python type
     * @return          an APyNode object of the type indicated in the input
     * @throws InvalidConfigException      part of the type is missing or malformed
     */
    private static APyNode<?> parseSimpleType(String type) throws InvalidConfigException {
        type = type.strip();

        //parsing int type
        if (type.equals("int")) {
            return new PyIntNode();
        }
        //parsing float type
        else if (type.equals("float")) {
            return new PyFloatNode();
        }
        //parsing bool type
        else if (type.equals("bool")) {
            return new PyBoolNode();
        }
        //unexpected contents of type
        else {
            throw new InvalidConfigException("Expected simple type invalid");
        }
    }

    /**
     * A helper function to parse iterable types. Returns an APyNode object of the associated type.
     *
     * @param type      a string of an iterable python type
     * @return          an APyNode object of the type indicated in the input
     * @throws InvalidConfigException      part of the type is missing or malformed
     */
    private static APyNode<?> parseIterableType(String type) throws InvalidConfigException {
        type = type.strip();

        //parsing list type
        if (type.startsWith("list")) {
            return new PyListNode<>(parseType(type.substring(type.indexOf("(")+1)));
        }
        //parsing tuple type
        else if (type.startsWith("tuple")) {
            return new PyTupleNode<>(parseType(type.substring(type.indexOf("(")+1)));
        }
        //parsing set type
        else if (type.startsWith("set")) {
            return new PySetNode<>(parseType(type.substring(type.indexOf("(")+1)));
        }
        //unexpected contents of type
        else {
            throw new InvalidConfigException("Expected iterable type invalid");
        }
    }

    /**
     * A helper function to parse domains. Does not return anything but populates domain fields of
     * associated nodes.
     *
     * @param domain        a string of domain for node object
     * @param node          APyNode object associated with parsed domain
     * @param domainType    string indicating whether domain is for exhaustive or random
     * @throws InvalidConfigException   part of the domain is missing or malformed
     */
    private static void parseDomain(String domain, APyNode<?> node, String domainType) throws InvalidConfigException {
        domain = domain.strip();
        int parInd = domain.indexOf("(");
        int colonInd = domain.indexOf(":");

        //parses domain of simple type
        if (parInd == -1) {
            if (domainType.equals("exhaustive")) {
                node.setExDomain(parseSimpleIterableDomain(domain,node));
            } else if (domainType.equals("random")) {
                node.setRanDomain(parseSimpleIterableDomain(domain,node));
            }
        }
        //parses domain of iterable type
        else if (colonInd == -1) {

            //checks for extra or missing parenthesis
            if (domain.substring(0,parInd).isEmpty() || domain.substring(parInd+1).isEmpty()) {
                throw new InvalidConfigException("invalid domain construction, unexpected (");
            }

            //set exhaustive or random domain
            if (domainType.equals("exhaustive")) {
                node.setExDomain(parseSimpleIterableDomain(domain.substring(0,parInd),node));
            } else if (domainType.equals("random")) {
                node.setRanDomain(parseSimpleIterableDomain(domain.substring(0,parInd),node));
            }

            //parses domain of inner type
            parseDomain(domain.substring(parInd+1), node.getLeftChild(), domainType);
        }
        //parses domain of a dictionary
        else {
            //checks for extra or missing parenthesis and colons
            if (domain.substring(0,parInd).isEmpty() || domain.substring(parInd+1,colonInd).isEmpty() || domain.substring(colonInd+1).isEmpty()) {
                throw new InvalidConfigException("invalid domain construction, unexpected : or (");
            }
            //set exhaustive or random domain
            if (domainType.equals("exhaustive")) {
                node.setExDomain(parseSimpleIterableDomain(domain.substring(0,parInd),node));
            } else if (domainType.equals("random")) {
                node.setRanDomain(parseSimpleIterableDomain(domain.substring(0,parInd),node));
            }
            //parses domain of key and value types
            parseDomain(domain.substring(parInd+1,colonInd), node.getLeftChild(), domainType);
            parseDomain(domain.substring(colonInd+1), node.getRightChild(), domainType);
        }
    }

    /**
     * A helper function to parse simple and iterative domains. Returns a List of Numbers
     * representing the domain of the APyNode object.
     *
     * @param domain        a string of domain for node object
     * @param node          APyNode object associated with parsed domain
     * @throws InvalidConfigException   part of the domain is missing or malformed
     */
    private static List<? extends Number> parseSimpleIterableDomain(String domain, APyNode<?> node) throws InvalidConfigException {
        domain = domain.strip();
        int tilInd = domain.indexOf("~");

        //for explicit domains
        if (tilInd == -1) {
            return parseArray(domain, node);
        }

        //for range domains
        int start;
        int end;

        start = parseIntVal(domain.substring(0,domain.indexOf("~")), node);
        end = parseIntVal(domain.substring(domain.indexOf("~")+1), node);

        //constructs domain from given range for integer ranges
        if (node instanceof PyIntNode || node instanceof PyBoolNode || node instanceof PyStringNode ||
                node instanceof PySetNode || node instanceof PyListNode || node instanceof PyTupleNode ||
                node instanceof PyDictNode){
            //checks if lower bound is less than upper bound
            if (start <= end) {
                //tempList because you can't add to extends Lists
                List<? extends Number> domainParams;
                List<Integer> tempList = new ArrayList<>();
                for (int i = start; i <= end; i++) {
                    tempList.add(i);
                }
                domainParams = new ArrayList<>(tempList);
                return domainParams;
            } else {
                throw new InvalidConfigException("invalid integer range, lower bound is greater than upper bound");
            }
        }
        //constructs domain from given range for double ranges
        else if (node instanceof PyFloatNode) {
            //checks if lower bound is less than upper bound
            if (start < end) {
                //tempList because you can't add to extends Lists
                List<? extends Number> domainParams;
                List<Double> tempList = new ArrayList<>();
                for (Double i = start*1.0; i <= end; i++) {
                    tempList.add(floor(i));
                }
                domainParams = new ArrayList<>(tempList);
                return domainParams;

            } else {
                throw new InvalidConfigException("invalid float range, lower bound is greater than upper bound");
            }
        } else {
            throw new InvalidConfigException("invalid domain range construction");
        }
    }

    /**
     * A helper function that parses domain arrays. Returns a List of Numbers
     * representing the domain of the APyNode object.
     *
     * @param domainArray
     * @param node
     * @return
     * @throws InvalidConfigException   part of the domain array is missing or malformed
     */
    private static List<? extends Number> parseArray(String domainArray, APyNode<?> node) throws InvalidConfigException {
        domainArray = domainArray.strip();
        domainArray = domainArray.substring(1,domainArray.length()-1);
        domainArray = domainArray.strip();

        if (domainArray.isEmpty()) {
            throw new InvalidConfigException("domain array is empty");
        }

        List<? extends Number> domainParam;

        if (node instanceof PyBoolNode || node instanceof PyIntNode || node instanceof PyStringNode ||
                node instanceof PyListNode ) {
            List<Integer> tempList = new ArrayList<>();
            for (String s : domainArray.split(",")) {
                Integer elem = parseIntVal(s.strip(),node);
                if (!tempList.contains(elem)) {
                    tempList.add(elem);
                }
            }
            domainParam = new ArrayList<>(tempList);
            return domainParam;

        } else if (node instanceof PyFloatNode) {
            List<Double> tempList = new ArrayList<>();
            for (String s : domainArray.split(",")) {
                Double elem = parseDoubleVal(s.strip());
                if (!tempList.contains(elem)) {
                    tempList.add(elem);
                }
            }
            domainParam = new ArrayList<>(tempList);
            return domainParam;

        } else {
            throw new InvalidConfigException("invalid array for domain");
        }
    }

    /**
     *
     * @param domainVal
     * @param node
     * @return
     * @throws InvalidConfigException   part of the integer is missing or malformed
     */
    private static int parseIntVal(String domainVal, APyNode<?> node) throws InvalidConfigException {
        domainVal = domainVal.strip();

        if (node instanceof PyIntNode || node instanceof PyFloatNode) {
            try {
                return parseInt(domainVal);
            } catch (NumberFormatException e) {
                throw new InvalidConfigException("expected integer domain value");
            }

        } else if (node instanceof PyBoolNode) {
            if (parseInt(domainVal) == 0 || parseInt(domainVal) == 1) {
                try {
                    return parseInt(domainVal);
                } catch (NumberFormatException e) {
                    throw new InvalidConfigException("expected integer domain value");
                }
            } else {
                throw new InvalidConfigException("invalid domain for PyBoolNode, not 0 or 1");
            }
        }
        else if (node instanceof PyStringNode || node instanceof PySetNode || node instanceof PyListNode
        || node instanceof PyTupleNode || node instanceof PyDictNode) {
            try {
                if (parseInt(domainVal) < 0) {
                    throw new InvalidConfigException("invalid negative domain for IterablePyNode");
                }
                return parseInt(domainVal);
            } catch (NumberFormatException e) {
                throw new InvalidConfigException("expected integer domain value");
            }
        } else {
            throw new InvalidConfigException("invalid non-number attempted for domain");
        }
    }

    /**
     *
     * @param domainVal
     * @return
     * @throws InvalidConfigException   part of the double is missing or malformed
     */
    private static double parseDoubleVal(String domainVal) throws InvalidConfigException {
        domainVal = domainVal.strip();

        try {
            return parseDouble(domainVal);
        } catch (NumberFormatException e) {
            throw new InvalidConfigException("invalid float domain value");
        }
    }
}
