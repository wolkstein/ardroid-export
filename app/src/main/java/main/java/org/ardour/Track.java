/**
 * 
 */
package org.ardour;

/**
 * @author lincoln
 *
 */
public class Track {
	
	public static final int AUDIO = 0;
	public static final int MIDI = 1;
	public static final int BUS = 2;
	
	public int remoteId;
	
	public int type; 
	public String name;
	public int trackVolume = 0;
	
	public boolean recEnabled = false;
	public boolean soloEnabled = false;
	public boolean muteEnabled = false;
	public boolean soloIsolateEnabled = false;

	// private
	private boolean trackVolumeOnSeekBar = false;
	//helper

	public double minpos = 0.0;
	public double maxpos = 1000.0;
	public double minlval = Math.cbrt(0.0000000001);
	public double maxlval = Math.cbrt(2000.0);

	public double scale = (maxlval - minlval) / (maxpos - minpos);

	public int valueToSlider(double value){
		return  (int)(minpos + (Math.cbrt(value) - minlval) / scale);
	}
	public double sliderToValue(int sliderposition){
		return Math.pow((sliderposition - minpos) * scale + minlval, 3.0);
	}
	public void setTrackVolumeOnSeekBar(boolean val){
		trackVolumeOnSeekBar = val;
	}
	public boolean getTrackVolumeOnSeekBar(){
		return trackVolumeOnSeekBar;
	}

}
