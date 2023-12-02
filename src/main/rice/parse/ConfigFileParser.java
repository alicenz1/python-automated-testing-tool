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

        for (int i = 0; i < exDomain.length(); i++) {
            String exDom;

            try {
                exDom = (String) exDomain.get(i);
            } catch (ClassCastException e) {
                throw new InvalidConfigException("exhaustive domain's value is not a JSONArray of Strings");
            }
        }

        for (int i = 0; i < ranDomain.length(); i++) {
            String ranDom;

            try {
                ranDom = (String) ranDomain.get(i);
            } catch (ClassCastException e) {
                throw new InvalidConfigException("random domain's value is not a JSONArray of Strings");
            }
        }




        return null;
    }

    private static APyNode<?> parseType(String type) throws InvalidConfigException {
        type = type.strip();
        int parInd = type.indexOf("(");

        if (parInd == -1) {
            return parseSimpleType(type);

        } else if (type.substring(0, parInd).equals("str")) {
            String stringVal;
            Set<Character> charVal = new HashSet<>();

            try {
                stringVal = (String) type.substring(parInd+1);
            } catch (ClassCastException e) {
                throw new InvalidConfigException("Expected str val is not a String");
            }

            stringVal = stringVal.strip();
            for (char c : stringVal.toCharArray()) {
                charVal.add(c);
            }

            return new PyStringNode(charVal);

        } else if (type.substring(0, parInd).equals("dict")) {
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

    private static List<? extends Number> parseDomain(String domain, APyNode<?> node) {
        domain = domain.strip();
        int parInd = domain.indexOf("(");

        if (parInd == -1 && node instanceof PyStringNode) {


        }

        return null;
    }

    private static List<? extends Number> parseDomainHelper(String domain, APyNode<?> node) {
        domain = domain.strip();
        int tilInd = domain.indexOf("~");

        if (tilInd == -1) {
            if (node instanceof PyIntNode) {

            }
        }


        return null;
    }

    private static List<? extends Number> parseArray(String domainArray, APyNode<?> node) throws InvalidConfigException {
        domainArray = domainArray.strip();
        domainArray = domainArray.substring(1,domainArray.length()-1);
        List<Number> domainParamNum = new ArrayList<>();

        for (String s : domainArray.split(",")) {
            domainParamNum.add(parseVal(s.strip(),node));
        }

        return null;
    }

    private static int parseVal(String domainVal, APyNode<?> node) throws InvalidConfigException {
        domainVal = domainVal.strip();

        if (node instanceof PyBoolNode || node instanceof PyIntNode) {
            return parseInt(domainVal);

        } else if (node instanceof PyStringNode) {
            if (parseInt(domainVal) < 0) {
                throw new InvalidConfigException("invalid negative domain for PyStringNode");
            } else {
                return parseInt(domainVal);
            }
        } else {
            throw new InvalidConfigException("invalid non-number attempted for domain");
        }
    }

    private static double parseDoubleVal(String domainVal, APyNode<?> node) throws InvalidConfigException {
        domainVal = domainVal.strip();
        if (node instanceof PyFloatNode) {
            return parseDouble(domainVal);

        }
    }


}

// TODO: implement the ConfigFileParser class here
