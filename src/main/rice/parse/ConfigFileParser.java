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

public class ConfigFileParser {

    public static String readFile(String filepath) throws IOException {
        return Files.readString(Paths.get(filepath));
    }

    public static ConfigFile parse(String contents) throws InvalidConfigException {

        JSONObject obj;

        try {
            obj = new JSONObject(contents);
        } catch (JSONException e) {
            throw new InvalidConfigException("File does not contain a valid JSONObject");
        }

        String fname;

        try {
            fname = (String) obj.get("fname");
        } catch (JSONException e) {
            throw new InvalidConfigException("fname key does not exist");
        } catch (ClassCastException e) {
            throw new InvalidConfigException("fname value is not a String");
        }

        JSONArray types;

        try {
            types = (JSONArray) obj.get("types");
        } catch (JSONException e) {
            throw new InvalidConfigException("types key does not exist");
        } catch (ClassCastException e) {
            throw new InvalidConfigException("types value is not a JSONArray");
        }

        JSONArray exDomain;

        try {
            exDomain = (JSONArray) obj.get("exhaustive domain");
        } catch (JSONException e) {
            throw new InvalidConfigException("exhaustive domain key does not exist");
        } catch (ClassCastException e) {
            throw new InvalidConfigException("exhaustive domain value is not a JSONArray");
        }

        JSONArray ranDomain;

        try {
            ranDomain = (JSONArray) obj.get("random domain");
        } catch (JSONException e) {
            throw new InvalidConfigException("random domain key does not exist");
        } catch (ClassCastException e) {
            throw new InvalidConfigException("random domain value is not a JSONArray");
        }

        int numRand;

        try {
            numRand = (int) obj.get("num random");
        } catch (JSONException e) {
            throw new InvalidConfigException("num random key does not exist");
        } catch (ClassCastException e) {
            throw new InvalidConfigException("num random is not an integer");
        }

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

        if (exDomain.length() != typeParams.size()) {
            throw new InvalidConfigException("types and domain parameter structure do not match");
        }

        for (int i = 0; i < exDomain.length(); i++) {
            String exDom;

            try {
                exDom = (String) exDomain.get(i);
            } catch (ClassCastException e) {
                throw new InvalidConfigException("exhaustive domain's value is not a JSONArray of Strings");
            }

            parseDomain(exDom, typeParams.get(i), "exhaustive");
        }

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

    private static APyNode<?> parseType(String type) throws InvalidConfigException {
        type = type.strip();
        int parInd = type.indexOf("(");

        if (parInd == -1) {
            return parseSimpleType(type);

        } else if (type.substring(0, parInd).strip().equals("str")) {
            String stringVal;
            Set<Character> charVal = new HashSet<>();

            try {
                stringVal = (String) type.substring(parInd+1);
            } catch (ClassCastException e) {
                throw new InvalidConfigException("Expected str val is not a String");
            }

            stringVal = stringVal.strip();

            if (stringVal.substring(1,stringVal.length()-1).isEmpty()) {
                throw new InvalidConfigException("character domain is empty");
            }

            for (char c : stringVal.toCharArray()) {
                charVal.add(c);
            }

            return new PyStringNode(charVal);

        } else if (type.substring(0, parInd).strip().equals("dict")) {
            int colonInd = type.indexOf(":");

            return new PyDictNode<>(parseType(type.substring(parInd+1, colonInd)),
                    parseType(type.substring(colonInd+1)));

        } else {
            return parseIterableType(type);
        }

    }

    private static APyNode<?> parseSimpleType(String type) throws InvalidConfigException {
        type = type.strip();
        if (type.equals("int")) {
            return new PyIntNode();

        } else if (type.equals("float")) {
            return new PyFloatNode();

        } else if (type.equals("bool")) {
            return new PyBoolNode();

        } else {
            throw new InvalidConfigException("Expected simple type invalid");
        }
    }

    private static APyNode<?> parseIterableType(String type) throws InvalidConfigException {
        type = type.strip();

        if (type.startsWith("list")) {
            return new PyListNode<>(parseType(type.substring(type.indexOf("(")+1)));

        } else if (type.startsWith("tuple")) {
            return new PyTupleNode<>(parseType(type.substring(type.indexOf("(")+1)));

        } else if (type.startsWith("set")) {
            return new PySetNode<>(parseType(type.substring(type.indexOf("(")+1)));

        } else {
            throw new InvalidConfigException("Expected iterable type invalid");
        }
    }

    private static void parseDomain(String domain, APyNode<?> node, String domainType) throws InvalidConfigException {
        domain = domain.strip();
        int parInd = domain.indexOf("(");
        int colonInd = domain.indexOf(":");

        if (parInd == -1) {
            if (domainType.equals("exhaustive")) {
                node.setExDomain(parseSimpleIterableDomain(domain,node));
            } else if (domainType.equals("random")) {
                node.setRanDomain(parseSimpleIterableDomain(domain,node));
            }

        } else if (colonInd == -1) {
            if (domain.substring(0,parInd).isEmpty() || domain.substring(parInd+1).isEmpty()) {
                throw new InvalidConfigException("invalid domain construction, unexpected (");
            }
            if (domainType.equals("exhaustive")) {
                node.setExDomain(parseSimpleIterableDomain(domain.substring(0,parInd),node));
            } else if (domainType.equals("random")) {
                node.setRanDomain(parseSimpleIterableDomain(domain.substring(0,parInd),node));
            }
            parseDomain(domain.substring(parInd+1), node.getLeftChild(), domainType);
        } else {
            if (domain.substring(0,parInd).isEmpty() || domain.substring(parInd+1,colonInd).isEmpty() || domain.substring(colonInd+1).isEmpty()) {
                throw new InvalidConfigException("invalid domain construction, unexpected : or (");
            }
            if (domainType.equals("exhaustive")) {
                node.setExDomain(parseSimpleIterableDomain(domain.substring(0,parInd),node));
            } else if (domainType.equals("random")) {
                node.setRanDomain(parseSimpleIterableDomain(domain.substring(0,parInd),node));
            }
            parseDomain(domain.substring(parInd+1,colonInd), node.getLeftChild(), domainType);
            parseDomain(domain.substring(colonInd+1), node.getRightChild(), domainType);
        }
    }

    private static List<? extends Number> parseSimpleIterableDomain(String domain, APyNode<?> node) throws InvalidConfigException {
        domain = domain.strip();
        int tilInd = domain.indexOf("~");

        if (tilInd == -1) {
            return parseArray(domain, node);
        } else if (node instanceof PyIntNode || node instanceof PyBoolNode || node instanceof PyStringNode ||
                node instanceof PySetNode || node instanceof PyListNode || node instanceof PyTupleNode ||
                node instanceof PyDictNode){

            int start;
            int end;

            start = parseIntVal(domain.substring(0,domain.indexOf("~")), node);
            end = parseIntVal(domain.substring(domain.indexOf("~")+1), node);

            if (start <= end) {
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
        } else if (node instanceof PyFloatNode) {
            Double start;
            Double end;

            start = parseDoubleVal(domain.substring(0,domain.indexOf("~")));
            end = parseDoubleVal(domain.substring(domain.indexOf("~")+1));

            if (start < end) {
                List<? extends Number> domainParams;
                List<Double> tempList = new ArrayList<>();
                for (Double i = start; i <= end; i++) {
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
                tempList.add(parseIntVal(s.strip(),node));
            }
            domainParam = new ArrayList<>(tempList);
            return domainParam;

        } else if (node instanceof PyFloatNode) {
            List<Double> tempList = new ArrayList<>();
            for (String s : domainArray.split(",")) {
                tempList.add(parseDoubleVal(s.strip()));
            }
            domainParam = new ArrayList<>(tempList);
            return domainParam;

        } else {
            throw new InvalidConfigException("invalid array for domain");
        }

    }

    private static int parseIntVal(String domainVal, APyNode<?> node) throws InvalidConfigException {
        domainVal = domainVal.strip();

        if (node instanceof PyIntNode) {
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

    private static double parseDoubleVal(String domainVal) throws InvalidConfigException {
        domainVal = domainVal.strip();

        try {
            return parseDouble(domainVal);
        } catch (NumberFormatException e) {
            throw new InvalidConfigException("invalid float domain value");
        }
    }

}

// TODO: implement the ConfigFileParser class here
