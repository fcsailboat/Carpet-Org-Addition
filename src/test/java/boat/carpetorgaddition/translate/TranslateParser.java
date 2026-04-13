package boat.carpetorgaddition.translate;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.jspecify.annotations.NonNull;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TranslateParser {
    private static final String TRANSLATE_PATH = "src/main/resources/assets/carpet-org-addition/lang/%s.json";
    private static final Gson GSON = new Gson();
    private final LinkedHashSet<Entry> translates = new LinkedHashSet<>();

    public TranslateParser(String lang) throws FileNotFoundException {
        String path = TRANSLATE_PATH.formatted(lang);
        BufferedReader reader = new BufferedReader(new FileReader(path));
        JsonObject json = GSON.fromJson(reader, JsonObject.class);
        json.entrySet().forEach(e -> translates.add(new Entry(e.getKey(), e.getValue().getAsString())));
    }

    public List<Entry> listAll() {
        return this.translates.stream().toList();
    }

    public List<Entry> listRuleTranslate() {
        return this.translates.stream().filter(Entry::isRule).toList();
    }

    public List<Entry> listOtherTranslate() {
        return this.translates.stream().filter(entry -> !entry.isRule()).toList();
    }

    public Set<String> listAllRule() {
        List<Entry> entries = this.listRuleTranslate();
        return entries.stream()
                .map(Entry::key)
                .map(str -> str.split("\\."))
                .map(arr -> arr[2])
                .collect(Collectors.toSet());
    }

    public record Entry(String key, String value) {
        public boolean isRule() {
            return this.key.matches("carpet\\.rule\\..*\\.(name|desc|(extra\\.\\d))");
        }

        public boolean isRuleNonName() {
            return this.key.matches("carpet\\.rule\\..*\\.(desc|(extra\\.\\d))");
        }

        public boolean isCategory() {
            String[] split = this.key.split("\\.");
            return split.length == 3 && "category".equals(split[1]);
        }

        @Override
        public boolean equals(Object obj) {
            return this == obj || (this.getClass() == obj.getClass() && this.key.equals(((Entry) obj).key));
        }

        @Override
        public int hashCode() {
            return this.key.hashCode();
        }

        @Override
        public @NonNull String toString() {
            return this.key + "=" + this.value;
        }
    }
}