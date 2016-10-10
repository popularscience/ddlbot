package io.jabbari.popsci.Slack;


import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import org.jetbrains.annotations.NotNull;
import com.ullink.slack.simpleslackapi.SlackSession;
import com.ullink.slack.simpleslackapi.events.SlackMessagePosted;
import com.ullink.slack.simpleslackapi.impl.SlackSessionFactory;
import io.jabbari.popsci.GoogleDocs.Google;
import io.jabbari.popsci.Main;
import io.jabbari.popsci.PopSciDataModel.Row;
import org.apache.logging.log4j.Level;

import org.jetbrains.annotations.Nullable;
import java.io.IOException;

import static io.jabbari.popsci.GoogleDocs.IGoogleRequests.ParsedResponse;

/**
 * Created by joubin on 9/27/16.
 */
public class SlackBot implements ISlackBot {

    public final static String tokenProperty = "slackTokenProperty";
    public static final String INSERT_KEY_WORD = "writing article about";
    public static final String SELECTOR = "row";
    public static final String READY_FOR_EDIT = "ready for edit";
    public static final String EDITING_ROW = "editing row";
    public static final String PUBLISH_ROW = "published row";
    private static SlackBot instance;
    @NotNull
    final SlackSession session;
    private SlackBot(@NotNull final String token) throws IOException {
        this.session = SlackSessionFactory.createWebSocketSlackSession(token);
        this.session.connect();
        this.addListeners();
    }

    public static SlackBot getInstance() {
        if (instance != null) {
            return instance;
        } else {
            throw new IllegalStateException("Please create an instance first");
        }
    }

    public static void createInstance(@NotNull final String token) throws IOException {
        if (instance == null) {
            instance = new SlackBot(token);
        } else {
            Main.logger.log(Level.DEBUG, "A SlackBot already exists; you can just call the getInstance");
        }
    }

    public String getHyperLinkUser(@NotNull final String userID) {
        return "<@" + userID + ">";
    }

    public void addListeners() {
        session.addMessagePostedListener((slackMessagePosted, slackSession) -> {
            if (isDirectedAtMe(slackMessagePosted.getMessageContent())) {
                final String sender = slackMessagePosted.getSender().getUserName();
                final String message = removeMyMention(slackMessagePosted.getMessageContent());
                final String senderTwo = slackMessagePosted.getSender().getId();
                if (sender.equals(getUsername())) {
                    return;
                }


                if (message.toLowerCase().contains(INSERT_KEY_WORD)) {
                    BatchUpdateSpreadsheetResponse response = insertItem(replaceKeyWord(INSERT_KEY_WORD, message), sender);

                    ParsedResponse(response);
                } else if (message.toLowerCase().contains(SELECTOR) && message.toLowerCase().contains(READY_FOR_EDIT)) {
                    String row = replaceKeyWord(SELECTOR, replaceKeyWord(READY_FOR_EDIT, message));
                    BatchUpdateSpreadsheetResponse response = setReadyForEdit(Integer.parseInt(row));
                    if (response == null) {
                        sendCannotFindRow(slackMessagePosted);
                    }
                    ParsedResponse(response);
                } else if (message.toLowerCase().contains(EDITING_ROW)) {
                    BatchUpdateSpreadsheetResponse response = setEditing(Integer.parseInt(replaceKeyWord(EDITING_ROW, message)), sender);
                    if (response == null) {
                        sendCannotFindRow(slackMessagePosted);
                    }
                    ParsedResponse(response);
                } else if (message.toLowerCase().contains(PUBLISH_ROW)) {
                    String parts[] = replaceKeyWord(PUBLISH_ROW, message).split(" ");
                    BatchUpdateSpreadsheetResponse response = setPublishEditing(Integer.parseInt(parts[0]), parts[1]);

                    if (response == null) {
                        sendCannotFindRow(slackMessagePosted);
                    }
                    ParsedResponse(response);

                } else {
                    session.sendMessage(slackMessagePosted.getChannel(), "*I can do the following:*");
                    session.sendMessage(slackMessagePosted.getChannel(), getUsername() + " *writing article about* jazz music\n" +
                            "\n" +
                            "*OR*\n" +
                            "" + getUsername() + " *row* 77 *ready for edit*\n" +
                            "\n" +
                            "*OR*\n" +
                            "" + getUsername() + " *editing row* 77\n" +
                            "\n" +
                            "*OR*\n" +
                            "" + getUsername() + " *published row* 77 example.com");
                    session.sendMessage(slackMessagePosted.getChannel(), "Where the *bold* is part of my syntax");
                    return;
                }
                session.sendMessage(slackMessagePosted.getChannel(), "Okay, " + this.getHyperLinkUser(senderTwo));

            }
        });
    }

    public void sendCannotFindRow(SlackMessagePosted slackMessagePosted) {
        session.sendMessage(slackMessagePosted.getChannel(), "Sorry, " + getHyperLinkUser(slackMessagePosted.getSender().getId()) + " I could not find that row");

    }

    public String replaceKeyWord(String keyWord, String message) {
        return message.replace(keyWord, "").trim();
    }

    public boolean isDirectedAtMe(@NotNull final String messageContent) {
        return messageContent.startsWith(getUsername()) || messageContent.startsWith("<@" + getUser() + ">");
    }

    public String removeMyMention(@NotNull final String message) {
        return message.replace(getUsername(), "").replace("<@" + getUser() + ">", "");

    }

    /**
     * @return the current username of the bot
     */
    public String getUsername() {
        return session.sessionPersona().getUserName();
    }

    public String getUser() {
        return session.sessionPersona().getId();
    }

    @Nullable
    @Override
    public BatchUpdateSpreadsheetResponse insertItem(@NotNull String description, @NotNull String author) {
        Row r = new Row(author);
        r.setDescription(description);
        return Google.getInstance().executeBatch(r.insertAsNewRow());
    }

    @Nullable
    @Override
    public BatchUpdateSpreadsheetResponse setReadyForEdit(int rowNumber) {
        Row row = Google.getInstance().getRowAtNumber(rowNumber);
        if (row.equals(Row.DOESNOTEXIST)) {
            return null;
        }
        Main.logger.debug("Got row: " + row);
        row.setDateModified();
        return Google.getInstance().executeBatch(row.updateInfoWithColor(Row.RED));

    }

    @Nullable
    @Override
    public BatchUpdateSpreadsheetResponse setEditing(int rowNumber, @NotNull String editor) {
        Row row = Google.getInstance().getRowAtNumber(rowNumber);
        if (row.equals(Row.DOESNOTEXIST)) {
            return null;
        }
        row.setDateModified();
        row.setEditor(editor);
        return Google.getInstance().executeBatch(row.updateInfoWithColor(Row.YELLOW));

    }

    @Nullable
    @Override
    public BatchUpdateSpreadsheetResponse setPublishEditing(int rowNumber, @NotNull String url) {
        Row row = Google.getInstance().getRowAtNumber(rowNumber);
        url = url.replace("<", "").replace(">", "").split("\\|")[0];
        if (row.equals(Row.DOESNOTEXIST)) {
            return null;
        }
        row.setDateModified();
        row.setPublished();
        row.setUrl(url);
        return Google.getInstance().executeBatch(row.updateInfoWithColor(Row.GREEN));


    }
}


