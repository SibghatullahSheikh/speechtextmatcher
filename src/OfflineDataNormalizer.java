import java.util.ArrayList;


public class OfflineDataNormalizer
{
	private ArrayList<Data> allData;
	
	public OfflineDataNormalizer(ArrayList<Data> allData)
	{
		this.allData = allData;
	}
	
	ArrayList<Data> normalize()
	{
		int spectrumSize = allData.get(0).getSpectrum().length;
		double[] averages = new double[spectrumSize];
		double[] deviations = new double[spectrumSize];
		
//		for (Data data : allData) {
//			for (int k = 0; k < spectrumSize; ++k) {
//				data.getSpectrum()[k] = Math.log(data.getSpectrum()[k]);
//			}
//		}
		
		for (Data data : allData) {
			for (int k = 0; k < spectrumSize; ++k) {
				averages[k] += data.getSpectrum()[k];
			}
		}
		for (int k = 0; k < spectrumSize; ++k) averages[k] /= allData.size();
		
		for (Data data : allData) {
			for (int k = 0; k < spectrumSize; ++k) {
				deviations[k] += Math.abs(data.getSpectrum()[k] - averages[k]);
			}
		}
		for (int k = 0; k < spectrumSize; ++k) deviations[k] /= allData.size();

		for (Data data : allData) {
			for (int k = 0; k < spectrumSize; ++k) {
				data.getSpectrum()[k] = (data.getSpectrum()[k] - averages[k]) / deviations[k];
			}
		}
		
		return allData;
	}
}