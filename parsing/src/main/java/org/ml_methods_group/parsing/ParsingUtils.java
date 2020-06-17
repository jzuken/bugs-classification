package org.ml_methods_group.parsing;

import org.ml_methods_group.common.Dataset;
import org.ml_methods_group.common.Solution;
import org.ml_methods_group.common.Solution.Verdict;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntPredicate;

import static org.ml_methods_group.common.Solution.Verdict.FAIL;
import static org.ml_methods_group.common.Solution.Verdict.OK;

public class ParsingUtils {
    public static Dataset parse(InputStream stream) throws IOException {
        CSVParser<Column> parser = new CSVParser<>(stream, Column::byName, Column.class);
        final Map<String, Solution> lastSolution = new HashMap<>();

        while (parser.hasNextLine()) {
            parser.nextLine();
            final String id = parser.getToken(Column.ID);
            String code = parser.getToken(Column.CODE);
            if (code.isEmpty()) {
                continue;
            }

            String codeText = Files.readString(Paths.get(code));

            final Verdict verdict = parser.getBoolean(Column.VERDICT) ? OK : FAIL;
            final String solutionId = id + verdict.ordinal();

            final Solution solution = new Solution(
                        codeText,
                        id,
                        solutionId,
                        verdict);
            lastSolution.put(solutionId, solution);

        }
        return new Dataset(lastSolution.values());
    }

    public static Dataset parseJavaSolutions(InputStream stream) throws IOException {
        return parse(stream);
    }

    private enum Column {
        ID("\\S*id\\S*"),
        VERDICT("\\S*is_passed\\S*", "\\S*status\\S*"),
        CODE("\\S*submission_code\\S*", "\\S*code\\S*");

        final String[] names;

        Column(String... names) {
            this.names = names;
        }

        static Optional<Column> byName(String token) {
            for (Column column : values()) {
                if (Arrays.stream(column.names).anyMatch(token::matches)) {
                    return Optional.of(column);
                }
            }
            return Optional.empty();
        }
    }
}
