package io.jabbari.popsci.GoogleDocs;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.jetbrains.annotations.NotNull;
import io.jabbari.popsci.Main;
import io.jabbari.popsci.PopSciDataModel.Row;
import io.jabbari.popsci.Util;
import org.apache.logging.log4j.Level;

import org.jetbrains.annotations.Nullable;
import java.io.*;
import java.util.Collections;
import java.util.List;

public class Google implements IGoogleRequests {

    public static final String columnRangeStartProperty = "columnRangeStart";
    public static final String columnRangeStartDefault = "A";
    //    public static final String columnRange = columnRangeStart + ":" + columnRangeEnd;
    public static final String spreadsheetIdProperty = "spreadsheetId";
    public static final File CLIENT_SECRET_LOCATION = Util.PathBuilder(System.getProperty("user.home"), ".credentials", "client_secret.json");
    public static final String applicationNameProperty = "applicationName";
    public static final String applicationNameDefault = "PopSci SlackBot";
    /**
     * Directory to store user credentials for this application.
     */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
            System.getProperty("user.home"), ".credentials/slackbot.auth");
    /**
     * Global instance of the JSON factory.
     */
    private static final JsonFactory JSON_FACTORY =
            JacksonFactory.getDefaultInstance();
    /**
     * Global instance of the scopes required by this quickstart.
     * <p>
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/sheets.googleapis.com-java-quickstart
     */
    private static final List<String> SCOPES =
            Collections.singletonList(SheetsScopes.SPREADSHEETS);
    public static String columnRangeEndProperty = "columnRangeEnd";
    public static String columnRangeEndDefault = "F";
    public static Google instance = null;
    /**
     * Global instance of the {@link FileDataStoreFactory}.
     */
    private static FileDataStoreFactory DATA_STORE_FACTORY;
    /**
     * Global instance of the HTTP transport.
     */
    private static HttpTransport HTTP_TRANSPORT;

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    public final String columnRangeStart;
    public final String columnRangeEnd;
    public final String spreadsheetId;
    /**
     * Application name.
     */
    private final String applicationName;
    /**
     * THe google auth
     */
    private Credential credential;
    private Sheets service;

    private Google(@NotNull final String sheetid, @NotNull final String applicationName, @NotNull final String columnRangeStart, @NotNull final String columnRangeEnd) {

        this.spreadsheetId = sheetid;
        this.applicationName = applicationName;
        this.columnRangeStart = columnRangeStart;
        this.columnRangeEnd = columnRangeEnd;
        try {
            authorize();
            buildService();
        } catch (IOException e) {
            Main.logger.log(Level.FATAL, "Could not create a connection to google", e);
        }
        instance = this;
    }

    public static Google getInstance() {
        if (instance == null) {
            Main.logger.log(Level.FATAL, "Please create a google object first");
            throw new IllegalStateException("Please create an instance first");
        } else {
            return instance;
        }

    }

    public static void createInstance(@NotNull final String sheetid, @NotNull final String applicationName, @NotNull final String columnRangeStart, @NotNull final String columnRangeEnd) {
        if (instance == null) {
            instance = new Google(sheetid, applicationName, columnRangeStart, columnRangeEnd);
        } else {
            Main.logger.log(Level.WARN, "An instance already exists. Just use it");

        }
    }

    public String getColumnRangeStart() {
        return columnRangeStart;
    }

    public String getColumnRangeEnd() {
        return columnRangeEnd;
    }

    public String getSpreadsheetId() {
        return spreadsheetId;
    }

    public String getColumnRange() {
        return getColumnRangeStart() + ":" + getColumnRangeEnd();
    }

    /**
     * Creates an authorized Credential object.
     *
     * @throws IOException when authorization fails. Normally due to networking issues
     */
    private void authorize() throws IOException {
        // Load client secrets.
        if (!CLIENT_SECRET_LOCATION.exists()) {
            System.err.println(String.format("Please place the API client secret at %s", CLIENT_SECRET_LOCATION.toPath().toString()));
            System.exit(1);
        }
        Main.logger.info(CLIENT_SECRET_LOCATION.toPath().toString());
        InputStream in =
                new FileInputStream(CLIENT_SECRET_LOCATION);
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();
        Credential credential = new AuthorizationCodeInstalledApp(
                flow, new LocalServerReceiver()).authorize("user");
        Main.logger.info(
                "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        this.credential = credential;
    }


    public void buildService() {
        service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, this.credential)
                .setApplicationName(applicationName)
                .build();

    }

    /**
     * Build and return an authorized Sheets API client service.
     *
     * @return an authorized Sheets API client service
     */
    private Sheets getSheetsService() {
        return service;
    }


    /**
     * @param rowNumber the row number we are looking for
     * @return returns the row we are looking for or null if the row isn't found or api change breaks unsafe cast.
     */
    @NotNull
    public Row getRowAtNumber(int rowNumber) {
        rowNumber -= 1;
        final List<List<String>> values =  getValues();

        if (rowNumber >= values.size()) {
            Main.logger.log(Level.DEBUG, "returning DOESNOTEXIT row. getRowAtNumber(%s)", rowNumber);
            return Row.DOESNOTEXIST;
        }
        final List<String> rawRow = values.get(rowNumber);
        return Row.parseRowFromSheet(rawRow, rowNumber);
    }

    @Nullable
    public BatchUpdateSpreadsheetResponse executeBatch(BatchUpdateSpreadsheetRequest request) {
        try {
            return getSheetsService().spreadsheets().batchUpdate(getSpreadsheetId(), request).execute();
        } catch (IOException e) {
            Main.logger.log(Level.FATAL, e.getMessage(), e);
        }
        return null;
    }

    public ValueRange getSheet() {
        try {
            return this.getSheetsService().spreadsheets().values().get(spreadsheetId, getColumnRange()).execute();
        } catch (IOException e) {
            Main.logger.log(Level.FATAL, e.getMessage(), e);
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public List<List<String>> getValues(){
        return (List<List<String>>) getSheet().get("values");
    }


}