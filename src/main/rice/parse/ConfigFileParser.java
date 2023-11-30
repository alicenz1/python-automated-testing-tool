package main.rice.parse;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigFileParser {

    public static String readFile(String filepath) throws IOException {
        try {
            return Files.readString(Path.of(filepath));
        } catch (IOException e) {
            return "Error: IO Exception";
        }

    }

    public static ConfigFile parse(String contents) throws InvalidConfigException {

        if (!contents.contains("fname")) {
            throw new InvalidConfigException("Missing fname key");
        }

        if (!contents.contains("types")) {
            throw new InvalidConfigException("Missing types key");
        }

        if (!contents.contains("exhaustive domain")) {
            throw new InvalidConfigException("Missing exhaustive domain key");
        }

        if (!contents.contains("random domain")) {
            throw new InvalidConfigException("Missing random domain key");
        }

        if (!contents.contains("num random")) {
            throw new InvalidConfigException("Missing num random key");
        }

        String typesVal = contents.substring(contents.indexOf("types") + 5, contents.indexOf("exhaustive domain"));
        String exDomainVal = contents.substring(contents.indexOf("exhaustive domain") + 17, contents.indexOf("random domain"));
        String ranDomainVal = contents.substring(contents.indexOf("random domain") + 13, contents.indexOf("num random"));
        String numRandomVal = contents.substring(contents.indexOf("num random") + 10, contents.indexOf("}"));







        return null;
    }
}

// TODO: implement the ConfigFileParser class here