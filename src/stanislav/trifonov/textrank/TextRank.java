package stanislav.trifonov.textrank;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class TextRank {

	protected static final String WORD_REGEX = "\\w+";
	protected static final String SENTENCE_SPLITTER_REGEX = "\\b+";
	
	protected static final int MIN_SUMMARY_SENTENCES = 1;
	protected static final int DEFAULT_SUMMARY_LENGTH_PERCENT = 10;
	
	protected static final float ERROR_RATE_THRESHOLD = 0.0001f;
	protected static final float DAMPING_FACTOR = 0.85f;
	
	
	private final List<String> _sentences;
	/*
	 * for each pair of edges place the weight/similarity on the crossing point
	 * (i.e. sentence 9 and 4 have w=0.15, so [9][4] = [4][9] = 0.15)
	 * 
	 */
	private final float _weight[][];

	
	public TextRank(List<String> sentences) {
		_sentences = new ArrayList<String>(sentences);
		_weight = new float[_sentences.size()][_sentences.size()];
	}

	public List<String> summarize(int summarySize) {
		float similarity = 0.0f;
		for(int i=0; i<_sentences.size()-1; ++i) {
			for(int j=i+1; j<_sentences.size(); ++j) {
				similarity = similarity( _sentences.get(i), _sentences.get(j) );
				if(similarity > 0f)
					_weight[i][j] = _weight[j][i] = similarity;
			}
		}
		
//		for(int i=0; i<_weight.length; ++i) {
//			System.out.print((i+3) + ": ");
//			for(int j=0; j<_weight[i].length; ++j) {
//				System.out.print(String.format("%.2g\t", _weight[i][j]));
//			}
//			
//			System.out.println("");
//		}
//		for(int i=0; i<_sentences.size(); ++i)
//			System.out.println(String.format("%.2f: ", _weight[6][i]) + _sentences.get(i));
		
		float scores[] = new float[_sentences.size()];
		for (int i = 0; i < scores.length; i++)
			scores[i] = 1f;
		float score = 0f;
		
		float errorRate = Float.MAX_VALUE;
		float vertexOutWeights = 0f;
		
		
		while(isErrorRateAboveThreshold(errorRate)) {
			for(int i=0; i<_sentences.size(); ++i) {
				score = 0f;
				for(int j=0; j<_sentences.size(); ++j) {
					vertexOutWeights = 0f;
					if(_weight[j][i] != 0f) {
						for(int k=0; k<_sentences.size(); ++k)
							vertexOutWeights += _weight[j][k];
						
						score += (_weight[j][i] / vertexOutWeights) * scores[j];
					}
				}
				
				score = score*DAMPING_FACTOR + (1-DAMPING_FACTOR);
				errorRate = score - scores[i];
				scores[i] = score;
				if( !isErrorRateAboveThreshold(errorRate) )
					break;
			}
		}
		
		
		
		Integer indices[] = new Integer[_sentences.size()];
		for(int i=0; i<_sentences.size(); ++i)
			indices[i] = i;
		
		Arrays.sort(indices, new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				return Float.compare(scores[o1], scores[o2]);
			}
		});
		
		
		
		int numberOfSentencesInSummary = Double.
				valueOf(Math.ceil(indices.length*summarySize/100.0)).
				intValue();
		List<String> summary = new ArrayList<String>();
		for (int i=indices.length-1; i>=indices.length-numberOfSentencesInSummary; i--) {
			System.out.println(scores[indices[i]] + " for " + _sentences.get(indices[i]));
			summary.add( _sentences.get(indices[i]) );
		}
		
		return summary;
	}

	//if similarity is 0 (the number of common words is 0), the 2 sentences are not connected in the graph
	protected float similarity(String sentence1, String sentence2) {
		if( sentence1 == null || sentence2 == null ||
				sentence1.length() == 0 || sentence2.length() == 0 )
			return 0.0f;
		
		String words1[] = wordsInSentence(sentence1);
		String words2[] = wordsInSentence(sentence2);
		
		Set<String> commonWords = new HashSet<String>();
		
		for(int i=0; i<words1.length; ++i) {
			if(commonWords.contains(words1[i]))
				continue;
			
			for(int j=0; j<words2.length; ++j) {
				if(words1[i].equals(words2[j])) {
					commonWords.add(words1[i]);
					break;
				}
			}
		}
		
		float count = commonWords.size();
		double logSi = Math.log10( Integer.valueOf(words1.length).doubleValue() );
		double logSj = Math.log10( Integer.valueOf(words2.length).doubleValue() );
		return  count / Double.valueOf( logSi + logSj ).floatValue();
	}
	
	
	protected String[] wordsInSentence(String sentence) {
		Matcher wordMatcher = Pattern.compile(WORD_REGEX).matcher(sentence);
		List<String> words = new ArrayList<String>();
		String word;
		
		while(wordMatcher.find()) {
			word = wordMatcher.group(0);
			if(word != null)
				words.add(word);
		}
		
		return words.toArray( new String[words.size()] );
	}
	
	protected boolean isErrorRateAboveThreshold(float errorRate) {
		return errorRate > ERROR_RATE_THRESHOLD || errorRate < (-ERROR_RATE_THRESHOLD);		
	}
}
