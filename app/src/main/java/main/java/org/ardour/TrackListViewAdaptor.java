/**
 * 
 */
package org.ardour;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import org.ardour.OscService;

/**
 * @author lincoln
 *
 */
public class TrackListViewAdaptor extends ArrayAdapter<org.ardour.Track> {

	//private Context context;
	private OnClickListener onClickListener;
    private OnSeekBarChangeListener onSeekBarChangeListener;
    private SeekBar volumeBar;


	public TrackListViewAdaptor(Context context, int textViewResourceId, List<org.ardour.Track> tracks) {
		
		super(context, textViewResourceId, tracks);
		
		//this.context = context;
	}

	/* (non-Javadoc)
	 * @see android.widget.ArrayAdapter#getView(int, android.view.View, android.view.ViewGroup)
	 */
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
        View view = convertView;
        final org.ardour.Track track = this.getItem (position);
        
        if (track == null){
        	return view;
        }
        
        if (view == null) {
            
        	LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        	view = vi.inflate(R.layout.track_header, null);
        }
        
        //view.setTag(track);

        ImageView rec = (ImageView) view.findViewById(R.id.bRecEnable);
        if(track.type == org.ardour.Track.BUS)
            rec.setImageAlpha(0); // hide rec button on audiobus
        rec.setOnClickListener(onClickListener);
        rec.setTag(track);
        
        ImageView mute = (ImageView) view.findViewById(R.id.bMuteEnable);
        mute.setOnClickListener(onClickListener);
        mute.setTag(track);
        
        ImageView solo = (ImageView) view.findViewById(R.id.bSoloEnable);
        solo.setOnClickListener(onClickListener);
        solo.setTag(track);

        //ImageView isolate = (ImageView) view.findViewById(R.id.bSoloIsolateEnable);
        
        TextView name = (TextView) view.findViewById(R.id.tTrackName);
        name.setText(track.name);




        SeekBar VolBar = (SeekBar) view.findViewById(R.id.sbTrackVolume);
        VolBar.setMax(1000);
        VolBar.setOnSeekBarChangeListener(onSeekBarChangeListener);
        VolBar.setTag(track);





        
        if(track.recEnabled){
        	rec.setImageResource(R.drawable.rec_enabled);
        }
        else {
        	rec.setImageResource(R.drawable.act_disabled);
        }

        if(track.muteEnabled){
        	mute.setImageResource(R.drawable.mute_enabled);
        }
        else {
        	mute.setImageResource(R.drawable.act_disabled);
        }
        
        if(track.soloEnabled){
        	solo.setImageResource(R.drawable.solo_enabled);
        }
        else {
        	solo.setImageResource(R.drawable.act_disabled);
        }
        if(track.trackVolume>=0) {
            //System.out.println(track.trackVolume);

            VolBar.setProgress(track.trackVolume);
        }
        return view;
	}
	
	public void setOnClickListener(OnClickListener onClickListener) {
		this.onClickListener = onClickListener;
	}

    public void setOnSeekBarChangeListener (OnSeekBarChangeListener onSeekBarChangeListener) {
        this.onSeekBarChangeListener = onSeekBarChangeListener;
    }

	/* (non-Javadoc)
	 * @see android.widget.BaseAdapter#areAllItemsEnabled()
	 */
	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	/* (non-Javadoc)
	 * @see android.widget.BaseAdapter#isEnabled(int)
	 */
	@Override
	public boolean isEnabled(int position) {
		return false;
	}


}
