package os.tool.utils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class UserAgent {
    private final List<String> userAgents;

    public UserAgent(List<String> userAgents) {
        this.userAgents = userAgents;
    }


    public String random() {
        return userAgents.get(ThreadLocalRandom.current().nextInt(userAgents.size()));
    }

    public static UserAgent get() {
        try {
            Path path = Paths.get(UserAgent.class.getClassLoader()
                    .getResource("user-agents.txt").toURI());
            return new UserAgent(Files.readAllLines(path));
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
