package io.jabbari.popsci;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 * Created by joubin on 9/25/16.
 */
public final class Util {

    public final static DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                    .withZone(ZoneId.systemDefault());

    /**
     * Creates a path from a list of string paths
     *
     * @param paths
     * @return
     */
    @NotNull
    public static File PathBuilder(@NotNull String... paths) {
        Iterator<String> pathIterator = new ArrayList<>(Arrays.asList(paths)).iterator();
        File f = new File("/");
        if (pathIterator.hasNext()) {
            f = new File(pathIterator.next());
        }
        while (pathIterator.hasNext()) {
            f = new File(f, pathIterator.next());
        }

        return f;
    }


}
