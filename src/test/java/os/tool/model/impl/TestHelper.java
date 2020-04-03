package os.tool.model.impl;

import os.tool.model.Document;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static java.nio.file.Files.readAllBytes;

public class TestHelper {
    static String loadResourceAsString(String name) throws URISyntaxException, IOException {
        return new String(readAllBytes(Paths.get(TestHelper.class.getResource(name).toURI())));
    }
}
