package moments4j.core;

import moments4j.Moments4jApplication;
import moments4j.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Port of moments/core/commands.py: init-db, init-app and lorem run as
 * `java -jar moments4j.jar <command> [--option value]`; the process exits after
 * the command. Without a command the web app boots normally.
 */
@Component
public class Commands implements ApplicationRunner {

    private final UserService userService;
    private final List<JpaRepository<?, ?>> repositories;
    private final Lorem lorem;
    private final ConfigurableApplicationContext ctx;

    public Commands(UserService userService, Lorem lorem, List<JpaRepository<?, ?>> repositories,
                    ConfigurableApplicationContext ctx) {
        this.userService = userService;
        this.lorem = lorem;
        this.repositories = repositories;
        this.ctx = ctx;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> command = args.getNonOptionArgs();
        if (command.isEmpty()) {
            return;  // normal web boot
        }
        Map<String, List<String>> options = new HashMap<>();
        for (String name : args.getOptionNames()) {
            options.put(name, args.getOptionValues(name));
        }
        try {
            execute(command, options);
        } finally {
            new Thread(() -> {
                try {
                    ctx.close();
                } catch (Exception ignored) {
                }
                System.exit(0);
            }).start();
        }
    }

    private void execute(List<String> command, Map<String, List<String>> options) {
        switch (command.get(0)) {
            case "init-db" -> {
                if (options.containsKey("drop")) {
                    System.out.println("This operation will delete the database, do you want to continue? [y/N] aborting");
                }
                dropAll();
                System.out.println("Initialized database.");
            }
            case "init-app" -> {
                dropAll();
                System.out.println("Initialized the database.");
                userService.initRole();
                System.out.println("Initialized the roles and permissions.");
            }
            case "lorem" -> {
                int user = intOption(options, "user", 10);
                int follow = intOption(options, "follow", 30);
                int photo = intOption(options, "photo", 30);
                int tag = intOption(options, "tag", 20);
                int collect = intOption(options, "collect", 50);
                int comment = intOption(options, "comment", 100);

                dropAll();
                userService.initRole();
                System.out.println("Initialized the roles and permissions.");
                lorem.fakeAdmin();
                System.out.println("Generated the administrator.");
                lorem.fakeUser(user);
                System.out.println("Generated " + user + " users.");
                lorem.fakeFollow(follow);
                System.out.println("Generated " + follow + " follows.");
                lorem.fakeTag(tag);
                System.out.println("Generated " + tag + " tags.");
                lorem.fakePhoto(photo);
                System.out.println("Generated " + photo + " photos.");
                lorem.fakeCollect(collect);
                System.out.println("Generated " + collect + " collects.");
                lorem.fakeComment(comment);
                System.out.println("Generated " + comment + " comments.");
                System.out.println("Done.");
            }
            default -> throw new IllegalArgumentException(
                    "Unknown command: " + command.get(0) + " (available: init-db, init-app, lorem)");
        }
    }

    private void dropAll() {
        for (JpaRepository<?, ?> repo : repositories) {
            repo.deleteAll();
        }
    }

    private static int intOption(Map<String, List<String>> options, String name, int def) {
        List<String> values = options.get(name);
        return values == null || values.isEmpty() ? def : Integer.parseInt(values.get(0));
    }
}
