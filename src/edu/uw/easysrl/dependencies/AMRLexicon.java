package edu.uw.easysrl.dependencies;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableBiMap;
import edu.uw.easysrl.util.Util;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class AMRLexicon {

    private final BiMap<String, String> manualLexicon;

    public AMRLexicon(final File lexiconFile) throws IOException {
        this(ImmutableBiMap.copyOf(loadLexicon(lexiconFile)));
    }

    public AMRLexicon() {
        this(ImmutableBiMap.of());
    }

    private AMRLexicon(final BiMap<String, String> manualLexicon) {
        this.manualLexicon = manualLexicon;
    }
    
    public String getAmrLabel(String srlLabel) {
        return manualLexicon.getOrDefault(srlLabel, ":" + srlLabel);
    }
    
    private static Map<String, String> loadLexicon(final File file) throws IOException {
        Map<String, String> map = HashBiMap.create();
        for (final String line2 : Util.readFile(file)) {
            final int commentIndex = line2.indexOf("//");
            final String line = (commentIndex > -1 ? line2.substring(0, commentIndex) : line2).trim();

            if (line.isEmpty()) {
                continue;
            }
            final String[] fields = line.split("\t+");
            if (fields.length < 2) {
                throw new IllegalArgumentException("Must be at least two tab-separated fields on line: \"" + line2
                        + "\" in file: " + file.getPath());
            }

            if (fields.length == 2) {
                boolean isAmrRole = fields[0].startsWith(":");
                String srlLabel = !isAmrRole ? fields[0]: fields[1];
                String amrRole = isAmrRole ? fields[0]: fields[1];
                map.put(srlLabel, amrRole);
            }
        }
        return map;
    }
}
