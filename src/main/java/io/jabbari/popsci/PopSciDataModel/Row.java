package io.jabbari.popsci.PopSciDataModel;

import com.google.api.services.sheets.v4.model.*;
import org.jetbrains.annotations.NotNull;
import io.jabbari.popsci.GoogleDocs.Google;
import io.jabbari.popsci.Util;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by joubin on 9/26/16.
 */
public class Row {

    public static final Color GREEN = new Color().setRed(0.6078431373f).setGreen(1f).setBlue(0.6039215686f).setAlpha(1f);
    public static final Color YELLOW = new Color().setRed(1f).setGreen(0.9607843137f).setBlue(0.6039215686f).setAlpha(1f);
    public static final Color RED = new Color().setRed(1f).setGreen(0.6039215686f).setBlue(0.6039215686f).setAlpha(1f);
    public static final Row DOESNOTEXIST = new Row();
    /**
     * The person that wrote the article
     */
    @NotNull
    private String writer;

    /**
     * The person that edited the article
     */
    @NotNull
    private String editor;

    /**
     * Published date
     */
    @NotNull
    private String published;

    /**
     * Description of the article
     */
    @NotNull
    private String description;


    /**
     * The last time it was modified
     */
    @NotNull
    private String dateModified;

    /**
     * Row number within the excel sheet
     */
    @NotNull
    private int rowNumber;

    /**
     * The publish URL
     */
    @NotNull
    private String url;

    /**
     * Creates an entry with no data associated with it
     */
    public Row(@NotNull final String writer) {
        dateModified = Instant.now().toString();
        this.writer = writer;
    }

    private Row() {

    }

    /**
     * writer	Editor	Published	Article Discrioption	id	Date modified
     *
     * @param row
     * @return
     */
    public static
    @NotNull
    Row parseRowFromSheet(@NotNull final List<String> row, final int rowNumber) {
        Row result = new Row();
        result.setRowNumber(rowNumber);

        try {
            result.setWriter(row.get(0));
        } catch (final IndexOutOfBoundsException ex) {
            result.setWriter("");
        }

        try {
            result.setEditor(row.get(1));
        } catch (final IndexOutOfBoundsException ex) {
            result.setEditor("");
        }

        try {
            result.setPublished(row.get(2));
        } catch (final IndexOutOfBoundsException ex) {
            result.setPublished("");
        }

        try {
            result.setDescription(row.get(3));

        } catch (final IndexOutOfBoundsException ex) {
            result.setDescription("");
        }

        try {
            result.setDateModified(row.get(4));

        } catch (final IndexOutOfBoundsException ex) {
            result.setDateModified("");
        }

        try {
            result.setUrl(row.get(5));
        } catch (final IndexOutOfBoundsException ex) {
            result.setUrl("");
        }


        return result;
    }

    public static String getRange(final int row) {
        return Google.getInstance().getColumnRange() + row + ":" + Google.getInstance().getColumnRangeEnd() + row;
    }

    public String getWriter() {
        return writer;
    }

    public void setWriter(String writer) {
        this.writer = writer;
    }

    public String getEditor() {
        return editor;
    }

    public void setEditor(String editor) {
        this.editor = editor;
    }

    public String getPublished() {
        return published;
    }

    public void setPublished(String published) {
        this.published = published;
    }

    public void setPublished() {
        setPublished(Util.FORMATTER.format(Instant.now()));
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDateModified() {
        return dateModified;
    }

    public void setDateModified(String dateModified) {
        this.dateModified = dateModified;
    }

    public void setDateModified() {
        setDateModified(Util.FORMATTER.format(Instant.now()));
    }

    @Override
    public String toString() {
        return "Row{" +
                "writer='" + writer + '\'' +
                ", editor='" + editor + '\'' +
                ", published='" + published + '\'' +
                ", description='" + description + '\'' +
                ", dateModified='" + dateModified + '\'' +
                ", rowNumber=" + rowNumber +
                ", url='" + url + '\'' +
                '}';
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    @NotNull
    public String getRange() {
        return Google.getInstance().getColumnRangeStart() + getRowNumber() + ":" + Google.getInstance().getColumnRangeEnd() + getRowNumber();
    }

    @NotNull
    public List<CellData> getData() {
        final List<CellData> values = new ArrayList<>();
        /*
        Order of columns: writer, editor, published, description, id, modified date
         */

        CellData writer = new CellData();
        writer.setUserEnteredValue(new ExtendedValue().setStringValue(this.writer));

        values.add(writer);

        CellData editor = new CellData();
        editor.setUserEnteredValue(new ExtendedValue().setStringValue(this.editor));

        values.add(editor);

        CellData publishedDate = new CellData();
        publishedDate.setUserEnteredValue(new ExtendedValue().setStringValue(this.published));

        values.add(publishedDate);

        CellData description = new CellData();
        description.setUserEnteredValue(new ExtendedValue().setStringValue(this.description));
        values.add(description);

        CellData dateMod = new CellData();
        dateMod.setUserEnteredValue(new ExtendedValue().setStringValue(this.dateModified));
        values.add(dateMod);

        CellData url = new CellData();
        url.setUserEnteredValue(new ExtendedValue().setStringValue(this.url));
        values.add(url);


        return values;

    }

    @NotNull
    private List<CellData> getDataWithBackgroundColor(@NotNull Color color) {
        List<CellData> data = getData();

        data.forEach(item -> item.setUserEnteredFormat(new CellFormat().setBackgroundColor(color)));

        return data;

    }

    public BatchUpdateSpreadsheetRequest updateInfoWithoutColor() {
        final List<Request> requests = new ArrayList<>();

        requests.add(new Request()
                .setUpdateCells(new UpdateCellsRequest()
                        .setStart(new GridCoordinate()
                                .setSheetId(0)
                                .setRowIndex(getRowNumber())
                                .setColumnIndex(0))
                        .setRows(Collections.singletonList(
                                new RowData().setValues(getData())))
                        .setFields("userEnteredValue,userEnteredFormat.backgroundColor")));

        return new BatchUpdateSpreadsheetRequest()
                .setRequests(requests);
    }

    public BatchUpdateSpreadsheetRequest updateInfoWithColor(Color color) {
        final List<Request> requests = new ArrayList<>();

        requests.add(new Request()
                .setUpdateCells(new UpdateCellsRequest()
                        .setStart(new GridCoordinate()
                                .setSheetId(0)
                                .setRowIndex(getRowNumber())
                                .setColumnIndex(0))
                        .setRows(Collections.singletonList(
                                new RowData().setValues(getDataWithBackgroundColor(color))))
                        .setFields("userEnteredValue,userEnteredFormat.backgroundColor")));

        return new BatchUpdateSpreadsheetRequest()
                .setRequests(requests);
    }

    public BatchUpdateSpreadsheetRequest insertAsNewRow() {
        final List<Request> requests = new ArrayList<>();
        RowData rowData = new RowData();
        this.setDateModified();
        rowData.setValues(getData());
        List<RowData> rowDataList = new ArrayList<>();
        rowDataList.add(rowData);
        AppendCellsRequest appendCellReq = new AppendCellsRequest();
        appendCellReq.setSheetId(0);
        appendCellReq.setRows(rowDataList);
        appendCellReq.setFields("*");
        requests.add(new Request().setAppendCells(appendCellReq));
        return new BatchUpdateSpreadsheetRequest()
                .setRequests(requests);
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }


}
