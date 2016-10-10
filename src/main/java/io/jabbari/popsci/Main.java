package io.jabbari.popsci;

import io.jabbari.popsci.GoogleDocs.Google;
import io.jabbari.popsci.Slack.SlackBot;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.util.Properties;

/**
 * Created by joubin on 9/25/16.
 */
public class Main {
    public static final Logger logger = LogManager.getLogger(Main.class.getName());
    private static final File PROPERTIES_FILE = Util.PathBuilder(System.getProperty("user.home"), ".slackbot", "config.properties");

    public static void main(String[] args) {
        Main.logger.log(Level.INFO, "Start of main");
        Properties prop = new Properties();

        if (!PROPERTIES_FILE.exists()) {
            prop.setProperty("PATH_TO_CLIENT_SECRET", Google.CLIENT_SECRET_LOCATION.toPath().toString());
            OutputStream output = null;
            try {
                output = new FileOutputStream(PROPERTIES_FILE);
                prop.setProperty(Google.columnRangeStartProperty, Google.columnRangeStartDefault);
                prop.setProperty(Google.columnRangeEndProperty, Google.columnRangeEndDefault);
                prop.setProperty(Google.spreadsheetIdProperty, "");
                prop.setProperty(Google.applicationNameProperty, Google.applicationNameDefault);
                prop.setProperty(SlackBot.tokenProperty, "");
                prop.store(output, "Something");

            } catch (IOException ex) {
                logger.log(Level.FATAL, ex.getMessage(), ex);
            } finally {
                try {
                    if (output != null) {
                        output.close();
                    }
                } catch (IOException ex) {
                    logger.log(Level.FATAL, ex.getMessage(), ex);
                }
            }


        }

        InputStream input = null;

        try {

            input = new FileInputStream(PROPERTIES_FILE);

            // load a properties file
            prop.load(input);
            final String columnStart = prop.getProperty(Google.columnRangeStartProperty, Google.columnRangeStartDefault);
            final String columnEnd = prop.getProperty(Google.columnRangeEndProperty, Google.columnRangeEndDefault);
            final String spreadsheetid = prop.getProperty(Google.spreadsheetIdProperty, "");
            final String appname = prop.getProperty(Google.applicationNameProperty, Google.applicationNameDefault);
            final String token = prop.getProperty(SlackBot.tokenProperty, "");

            Google.createInstance(spreadsheetid, appname, columnStart, columnEnd);
            SlackBot.createInstance(token);
            logger.log(Level.INFO, "Created a google instance connection");
            logger.log(Level.INFO, "Created a slack bot");

        } catch (IOException ex) {
            logger.log(Level.FATAL, ex.getMessage(), ex);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    logger.log(Level.FATAL, e.getMessage(), e);
                }
            }
        }

    }


}
