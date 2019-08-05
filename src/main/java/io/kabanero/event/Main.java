package io.kabanero.event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String args[] ) {
        try {
            logger.info("Starting io.kabanero.event");
        }  catch(Throwable t) {
            logger.error("Error in WASController", t);
        }
        // If we get here, something is wrong. Should loop forever
        System.exit(1);
    }
}
