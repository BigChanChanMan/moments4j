package moments4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Java port of greyli/moments (app.py). CLI commands:
 *   java -jar moments4j.jar init-db [--drop]
 *   java -jar moments4j.jar init-app
 *   java -jar moments4j.jar lorem [--user 10 --follow 30 --photo 30 --tag 20 --collect 50 --comment 100]
 */
@SpringBootApplication
public class Moments4jApplication {

    public static void main(String[] args) {
        SpringApplication.run(Moments4jApplication.class, args);
    }
}
