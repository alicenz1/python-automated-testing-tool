package main.rice.parse;


import java.io.*;
import java.nio.file.Files;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.nio.file.Paths;

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




        return null;
    }
}

// TODO: implement the ConfigFileParser class here
