import java.util.ArrayList;


public class SpectrumWeights
{
	private double[] weights;

	public SpectrumWeights(ArrayList<Data> allData)
	{
		int spectrumSize = allData.get(0).getSpectrum().length;
		SpectrumHistograms histograms = new SpectrumHistograms(allData, 100);
		double[] spectrumFromNormalDiff = new double[spectrumSize];
		double max = 0;
		for (int i = 0; i < spectrumSize; ++i) {
			spectrumFromNormalDiff[i] = histograms.getHistograms()[i].differenceFromNormalDistribution();
			if (spectrumFromNormalDiff[i] > max) max = spectrumFromNormalDiff[i];
		}
		weights = new double[spectrumSize];
		for (int i = 0; i < spectrumSize; ++i)
			weights[i] = spectrumFromNormalDiff[i] / max;
	}

	public double[] getWeights() {
		return this.weights;
	}
	
}