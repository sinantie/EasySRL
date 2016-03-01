package edu.uw.easysrl.syntax.tagger;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import edu.uw.Taggerflow;
import edu.uw.TaggerflowProtos.TaggedSentence;
import edu.uw.TaggerflowProtos.TaggingInput;
import edu.uw.TaggerflowProtos.TaggingInputSentence;
import edu.uw.easysrl.main.InputReader.InputWord;
import edu.uw.easysrl.syntax.grammar.Category;
import edu.uw.easysrl.syntax.model.CutoffsDictionaryInterface;

public class TaggerflowLSTM extends Tagger {
	private final Taggerflow tagger;

	TaggerflowLSTM(final File modelFolder, final double beta, final int maxTagsPerWord,
			final CutoffsDictionaryInterface cutoffs) throws IOException {
		this(makeTaggerflow(modelFolder), beta, TaggerEmbeddings.loadCategories(new File(modelFolder, "categories")),
				maxTagsPerWord, cutoffs);
	}

	private static Taggerflow makeTaggerflow(final File modelFolder) {
		// Apparently this is the easiest way to set the library path in code...
		System.setProperty("java.library.path", "lib");
		Field fieldSysPath;
		try {
			fieldSysPath = ClassLoader.class.getDeclaredField("sys_paths");
			fieldSysPath.setAccessible(true);
			fieldSysPath.set(null, null);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
		File taggerflowModelFolder = new File(modelFolder, "taggerflow");
		return new Taggerflow(new File(taggerflowModelFolder, "graph.pb").getAbsolutePath(),
				taggerflowModelFolder.getAbsolutePath());
	}

	public TaggerflowLSTM(final Taggerflow tagger, final double beta, List<Category> categories,
			final int maxTagsPerWord, final CutoffsDictionaryInterface cutoffs) throws IOException {
		super(cutoffs, beta, categories, maxTagsPerWord);
		this.tagger = tagger;
	}

	public static List<List<ScoredCategory>> getScoredCategories(TaggedSentence sentence, List<Category> categories) {
		return sentence.getTokenList().stream().map(token -> token.getScoreList().stream()
				.map(indexedScore -> new ScoredCategory(categories.get(indexedScore.getIndex()),
						indexedScore.getValue())).collect(Collectors.toList())).collect(Collectors.toList());
	}

	@Override
	public List<List<ScoredCategory>> tag(final List<InputWord> words) {
		TaggingInput.Builder input = TaggingInput.newBuilder();
		input.addSentence(toSentence(words));
		return getScoredCategories(tagger.predict(input.build()).getSentence(0), lexicalCategories);
	}

	private TaggingInputSentence toSentence(List<InputWord> words) {
		return TaggingInputSentence.newBuilder()
				.addAllWord(() -> words.stream().map(w -> translateBrackets(w.word)).iterator()).build();
	}

	@Override
	public Stream<List<List<ScoredCategory>>> tagBatch(Stream<List<InputWord>> sentences) {
		TaggingInput.Builder input = TaggingInput.newBuilder();
		input.addAllSentence(() -> sentences.map(this::toSentence).iterator());
		return tagger.predict(input.build()).getSentenceList().stream()
				.map(taggedSentence -> getScoredCategories(taggedSentence, lexicalCategories));
	}

	@Override
	public Map<Category, Double> getCategoryScores(final List<InputWord> sentence, final int wordIndex,
			final double weight, final Collection<Category> categories) {
		throw new RuntimeException("TODO");
	}
}
