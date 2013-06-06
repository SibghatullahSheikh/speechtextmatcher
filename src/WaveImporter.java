import java.io.File;
import java.util.ArrayList;

import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataBlocker;
import edu.cmu.sphinx.frontend.DataProcessor;
import edu.cmu.sphinx.frontend.DoubleData;
import edu.cmu.sphinx.frontend.FloatData;
import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.endpoint.SpeechEndSignal;
import edu.cmu.sphinx.frontend.endpoint.SpeechStartSignal;
import edu.cmu.sphinx.frontend.filter.Preemphasizer;
import edu.cmu.sphinx.frontend.transform.DiscreteFourierTransform;
import edu.cmu.sphinx.frontend.util.AudioFileDataSource;
import edu.cmu.sphinx.frontend.window.RaisedCosineWindower;
import edu.cmu.sphinx.util.props.ConfigurationManager;


public class WaveImporter
{
	private String waveFilePath = "";
	private ArrayList<IWaveObserver> observers = new ArrayList<IWaveObserver>();
	private ArrayList<ISpeechObserver> speechObservers = new ArrayList<ISpeechObserver>();
	
	public WaveImporter(String waveFilePath)
	{
		this.waveFilePath = waveFilePath;
	}
	
	public void registerObserver(IWaveObserver observer)
	{
		this.observers.add(observer);
	}
	
	public void process()
	{
		ConfigurationManager cm = new ConfigurationManager(Main.class.getResource("config.xml"));
		FrontEnd frontend = (FrontEnd)cm.lookup("frontend");
		AudioFileDataSource audioSource = (AudioFileDataSource)cm.lookup("audioFileDataSource");
		File sourceFile = new File(this.waveFilePath);
		audioSource.setAudioFile(sourceFile, null);
		
		Data data = null;
		while ((data = frontend.getData()) != null) {

			double startTime = 0;
			double endTime = 0;
			double[] doubleData = null;
			
			if (data.getClass() == DoubleData.class) {
				DoubleData dataT = (DoubleData)data;
				startTime = (double)dataT.getFirstSampleNumber() / (double)dataT.getSampleRate();
				endTime = startTime + (double)dataT.getValues().length / (double)dataT.getSampleRate();
				doubleData = dataT.getValues();
			} else if (data.getClass() == FloatData.class) {
				FloatData dataT = (FloatData)data;
				startTime = (double)dataT.getFirstSampleNumber() / (double)dataT.getSampleRate();
				endTime = startTime + (double)dataT.getValues().length / (double)dataT.getSampleRate();
				doubleData = new double[dataT.getValues().length];
				for (int i = 0; i < doubleData.length; ++i)
					doubleData[i] = dataT.getValues()[i];
			} if (data.getClass() == SpeechStartSignal.class) {
				for (ISpeechObserver observer : speechObservers)
					observer.speechStarted();
			} else if (data.getClass() == SpeechEndSignal.class) {
				for (ISpeechObserver observer : speechObservers)
					observer.speechEnded();
			}
			if (doubleData != null)
				for (IWaveObserver observer : this.observers)
					observer.process(startTime, endTime, doubleData);
		}
		
		observers.clear();
		System.gc();
	}

	public void registerSpeechObserver(ISpeechObserver speechObserver) {
		this.speechObservers.add(speechObserver);
	}
}
