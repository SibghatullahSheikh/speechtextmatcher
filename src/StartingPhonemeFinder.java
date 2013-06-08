import java.util.ArrayList;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import edu.cmu.sphinx.tools.audio.AudioData;


public class StartingPhonemeFinder {
	ArrayList<Data> allData;
	Text text;
	AudioLabel[] matched = null;
	double[] averages = null;
	double[] variances = null;
	
	public StartingPhonemeFinder(ArrayList<Data> allData, Text text, AudioLabel[] matched) {
		this.allData = allData;
		this.text = text;
		this.matched = matched;
		this.averages = calcAverages();
		this.variances = calcVariances(averages);
	}
	
	AudioLabel[] process()
	{
		int prefixSize = 3;
		String searchedFor = matched[0].getLabel().substring(2, 2 + prefixSize).toLowerCase();
		double estTime = (prefixSize + 1) * text.getEstimatedTimePerCharacter();
		
		ArrayList<ArrayList<Speech>> candidates = new ArrayList<ArrayList<Speech>>();
		candidates.add(new ArrayList<Speech>());
		candidates.get(0).add(createSpeech(matched[0].getStart(), matched[0].getEnd()));
		int neigh = (int)Math.ceil(Math.sqrt(Math.sqrt(matched.length)));
		
		for (int i = 1; i < matched.length; ++i) {
			AudioLabel matching = matched[i];
			String labelText = matching.getLabel().toLowerCase();
			if (labelText.indexOf(searchedFor) >= 0) {
				System.err.println(labelText);
				ArrayList<Speech> aux = new ArrayList<Speech>();
				for (int j = Math.max(0, i - neigh); j < Math.min(matched.length, i + neigh); ++j) {
					aux.add(createSpeech(matching.getStart(), matching.getEnd()));
				}
				candidates.add(aux);
			}
		}
		System.err.println("found `" + searchedFor + "` " + candidates.size() + " times");
		System.err.println("neigh " + neigh);

		double frameTime = allData.get(1).getStartTime() - allData.get(0).getStartTime();
		int frames = (int)Math.ceil(estTime / frameTime);
		AudioLabel[] result = new AudioLabel[candidates.size()];
		int targetStartIndex = candidates.get(0).get(0).getStartDataIndex();
		result[0] = findBestInSpeeches(candidates.get(0), "orig", targetStartIndex, frames);
		for (int i = 1; i < candidates.size(); ++i) {
			result[i] = findBestInSpeeches(candidates.get(i), "" + i, targetStartIndex, frames);
		}
		
		return result;
	}
	
	private AudioLabel findBestInSpeeches(ArrayList<Speech> speeches, String label, int targetStartIndex, int frames)
	{
		PhonemeDiff bestDiff = null;
		for (Speech speech : speeches) {
			PhonemeDiff diff = findBestWithin(targetStartIndex, frames, speech.getStartDataIndex(), speech.getEndDataIndex());
			if ((bestDiff == null) || (diff.compare(diff, bestDiff) < 0)) {
				bestDiff = diff;
			}
		}
		System.err.println(label + " " + bestDiff.getDiff());
		double bestStartTime = allData.get(bestDiff.getSecondStart()).getStartTime();
		double bestEndTime = allData.get(bestDiff.getSecondEnd()).getEndTime();
		return new AudioLabel(label, bestStartTime, bestEndTime);
	}

	private Speech createSpeech(double start, double end)
	{
		int startIndex = findIndex(start, 0, allData.size());
		int endIndex = findIndex(end, 0, allData.size());
		return new Speech(start, end, startIndex, endIndex);
	}

	private int findIndex(double time, int bottom, int top)
	{
		if (bottom == top) return bottom;
		int between = (top + bottom) / 2;
		double auxTime = (allData.get(between).getStartTime() + allData.get(between + 1).getEndTime()) / 2;
		if (time < auxTime) return findIndex(time, bottom, between);
		else return findIndex(time, between + 1, top);
	}

	private double[] calcVariances(double[] averages) {
		int spectrumSize = allData.get(0).getSpectrum().length;
		double[] out = new double[spectrumSize];
		
		for (Data data : allData) {
			for (int i = 0; i < spectrumSize; ++i)
				out[i] += (averages[i] - data.getSpectrum()[i]) * (averages[i] - data.getSpectrum()[i]);
		}
		for (int i = 0; i < spectrumSize; ++i) out[i] /= allData.size();
//		for (int i = 0; i < spectrumSize; ++i) out[i] = Math.sqrt(out[i]);
		return out;
	}

	private double[] calcAverages() {
		int spectrumSize = allData.get(0).getSpectrum().length;
		double[] out = new double[spectrumSize];
		
		for (Data data : allData) {
			for (int i = 0; i < spectrumSize; ++i)
				out[i] += data.getSpectrum()[i];
		}
		for (int i = 0; i < spectrumSize; ++i) out[i] /= allData.size();
		return out;
	}

	private PhonemeDiff findBestWithin(int targetStartIndex, int frames, int start, int end)
	{
		double smallestDiff = Double.MAX_VALUE;
		int bestIndex = 0;
		for (int i = start; i < end - frames; ++i)
		{
			double diff = 0;
			for (int j = i; j < i + frames; ++j)
			{
				double[] spectrum = allData.get(j).getSpectrum();
				double[] targetSpectrum = allData.get(targetStartIndex + j - i).getSpectrum();
				for (int k = 0; k < spectrum.length / 3; ++k)
				{
					double aux = (spectrum[k] - targetSpectrum[k]) * (spectrum[k] - targetSpectrum[k]);
//					double aux = Math.abs(spectrum[k] - targetSpectrum[k]);
					if (aux < variances[k]) aux = variances[k];
//					aux = Math.floor(aux / variances[k]);
					diff += aux;// * variances[k];
				}
			}
			if (diff < smallestDiff) {
				smallestDiff = diff;
				bestIndex = i;
			}
		}
		
		return new PhonemeDiff(smallestDiff, targetStartIndex, bestIndex, frames);
	}
}