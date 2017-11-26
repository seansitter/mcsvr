package net.seansitter.mcsvr;

import com.google.inject.Guice;
import com.google.inject.Injector;
import net.seansitter.mcsvr.jmx.MCServerManagement;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class McMain {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getLogger(McServer.class);

        Injector injector = Guice.createInjector(new McServerConfig(args));
        CommandLine cmdLn = injector.getInstance(CommandLine.class);

        if (cmdLn.hasOption("help")) {
            new HelpFormatter().printHelp("mcsvr", injector.getInstance(Options.class));
            System.exit(0);
        }

        // try to start jmx management
        try {
            injector.getInstance(MCServerManagement.class).start();
        } catch (Exception e) {
            logger.error("failed to start jmx management", e);
            System.exit(1);
        }

        // try to start the server
        try {
            logger.info("starting memcache server");
            McServer mcServer = injector.getInstance(McServer.class);
            mcServer.start();
        }
        catch (Exception e) {
            logger.error("failed to start server: ", e);
            System.exit(1);
        }
    }
}
