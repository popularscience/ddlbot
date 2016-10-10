package io.jabbari.popsci.Slack;

import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetResponse;
import org.jetbrains.annotations.NotNull;

/**
 * Created by joubin on 9/27/16.
 */
public interface ISlackBot {

    BatchUpdateSpreadsheetResponse insertItem(@NotNull final String description, @NotNull final String author);

    BatchUpdateSpreadsheetResponse setReadyForEdit(final int rowNumber);

    BatchUpdateSpreadsheetResponse setEditing(final int rowNumber, @NotNull final String editor);

    BatchUpdateSpreadsheetResponse setPublishEditing(final int rowNumber, @NotNull final String url);

}
