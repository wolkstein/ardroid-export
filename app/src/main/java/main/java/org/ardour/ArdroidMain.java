package org.ardour;

import java.util.ArrayList;
import java.util.List;

import org.ardour.widget.ToggleGroup;
import org.ardour.widget.ToggleImageButton;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

public class ArdroidMain extends Activity implements OnClickListener {
	
	private static final String TAG = "ArdroidMain";
	
	private org.ardour.OscService oscService = null;
	private Clock clock = null;

	private TextView clockView;
	private org.ardour.TrackListViewAdaptor trackListViewAdaptor;
	private SeekBar locationBar;
	

	private String host = "127.0.0.1";
	private int port = 3819;
	
	// The list of tracks in the session
	private List<org.ardour.Track> trackList = new ArrayList<org.ardour.Track>();
		
	private Long frameRate = new Long(0L);
	private Long maxFrame = new Long(0L);
	
	/**
	 * Transport state constants
	 */
	public final byte TRANSPORT_STOPPED = 0x01;
	public final byte TRANSPORT_RUNNING = 0x02;
	public final byte RECORD_ENABLED = 0x04;
		
	private byte transportState = TRANSPORT_STOPPED;
	
	/*
	 * Our transport buttons
	 */
	ImageButton gotoStartButton = null;
	ImageButton gotoEndButton = null;
	ImageButton jumpToPrevMarkerButton = null;
	ImageButton addMarkerButton = null;
	ImageButton jumpToNextMarkerButton = null;
	ToggleImageButton loopButton = null;
	ToggleImageButton playButton = null;
	ToggleImageButton stopButton = null;
	ToggleImageButton recordButton = null;
	
	ToggleGroup transportToggleGroup = null;
	
	org.ardour.Blinker blinker = null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	
        super.onCreate(savedInstanceState);
                
        // Restore preferences
        SharedPreferences settings = getSharedPreferences("Ardroid", 0);
        
        host = settings.getString("host", "127.0.0.1"); //if host setting not found default to 127.0.0.1
        port = settings.getInt("port", 3819); //if port setting not found default to 3819
        
        setContentView(R.layout.main);

        //Create the OscService
        //TODO: Move this to a service proper?
        oscService = new org.ardour.OscService(host, port);
        oscService.setTransportHandler(mHandler);
        //oscService.setListener(this);
        
    	clockView = (TextView) this.findViewById(R.id.transport_clock);

    	locationBar = (SeekBar) this.findViewById(R.id.location);
    	locationBar.setMax(10000);
    	locationBar.setOnSeekBarChangeListener( new SeekBar.OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar arg0) {
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar arg0) {
			}
			
			@Override
			public void onProgressChanged(SeekBar sb, int pos, boolean fromUser) {
				
				if (fromUser){
										
					if (!(RECORD_ENABLED == (transportState & RECORD_ENABLED) 
							&& TRANSPORT_RUNNING == (transportState & TRANSPORT_RUNNING))){

						int loc = Math.round(( locationBar.getProgress() * maxFrame.longValue()) / 10000);
						oscService.transportAction(org.ardour.OscService.LOCATE, loc);

						//transportState = TRANSPORT_STOPPED;

						//transportToggleGroup.toggle(stopButton, true);
						//recordButton.toggleOff();
					}
				}
			}
		}
		);
    	    	

    	//Create the transport button listeners
        gotoStartButton = (ImageButton) this.findViewById(R.id.bGotoStart);
        gotoStartButton.setOnClickListener(this);

        gotoEndButton = (ImageButton) this.findViewById(R.id.bGotoEnd);
        gotoEndButton.setOnClickListener(this);

    	playButton = (ToggleImageButton) this.findViewById(R.id.bPlay);
    	playButton.setOnClickListener(this);
    	playButton.setAutoToggle(false);
        
        stopButton = (ToggleImageButton) this.findViewById(R.id.bStop);
        stopButton.setOnClickListener(this);
        stopButton.setAutoToggle(false);
        stopButton.toggle(); //Set stop to toggled state
                
        loopButton = (ToggleImageButton) this.findViewById(R.id.bLoopEnable);
        loopButton.setOnClickListener(this);
        loopButton.setAutoToggle(false);
        
        recordButton = (ToggleImageButton) this.findViewById(R.id.bRec);
        recordButton.setOnClickListener(this);
        recordButton.setAutoToggle(false);
        
        transportToggleGroup = new ToggleGroup();
        
        transportToggleGroup.addToGroup(playButton);
        transportToggleGroup.addToGroup(stopButton);
        transportToggleGroup.addToGroup(loopButton);

		jumpToPrevMarkerButton = (ImageButton) this.findViewById(R.id.bJumpPrevMarker);
		jumpToPrevMarkerButton.setOnClickListener(this);

		addMarkerButton =  (ImageButton) this.findViewById(R.id.bAddMarker);
		addMarkerButton.setOnClickListener(this);

		jumpToNextMarkerButton =  (ImageButton) this.findViewById(R.id.bJumpNextMarker);
		jumpToNextMarkerButton.setOnClickListener(this);

        
        //Create the list view for track headers
        ListView listView = (ListView) this.findViewById(R.id.tracks);
        
        trackListViewAdaptor = new org.ardour.TrackListViewAdaptor(this, R.layout.track_header, trackList);
        trackListViewAdaptor.setOnClickListener(this);
		trackListViewAdaptor.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

                                              @Override
                                              public void onStopTrackingTouch(SeekBar arg0) {

												  org.ardour.Track st = (org.ardour.Track) arg0.getTag();
												  st.setTrackVolumeOnSeekBar( false );
												  System.out.println("Stop Seekbar Tracking on Track: " + st.name );

                                              }

                                              @Override
                                              public void onStartTrackingTouch(SeekBar arg0) {

												  org.ardour.Track st = (org.ardour.Track) arg0.getTag();
												  st.setTrackVolumeOnSeekBar( true );
												  System.out.println("Start Seekbar Tracking on Track: " + st.name );

                                              }
                                              @Override
                                              public void onProgressChanged(SeekBar sb, int pos, boolean fromUser) {
												  if(fromUser) {

													  org.ardour.Track st = (org.ardour.Track) sb.getTag();
													  oscService.trackListVolumeAction( st , pos);

													  //System.out.println("Vrom Main, Slider-Pos: " + pos + " ID: " + st.name );
												  }
                                              }
                                          }
                                        );

        
        listView.setAdapter(trackListViewAdaptor);

        //this.registerForContextMenu(listView);
        //listView.setOnItemLongClickListener(cl);
    }
    
    
    /**
     * Called when the activity is about to become visible.
     */
    @Override
    public void onStart(){
    	
    	super.onStart();
    }
    
    /**
	 * Activity is has become visible
	 * @see android.app.Activity#onResume()
	 */
	@Override
	protected void onResume() {
		
		Log.d(TAG, "Resuming...");
        
		if (!oscService.isConnected()){
			Log.d(TAG, "Not connected to OSC server... Connect");
			this.startConnectionToArdour();
		}
		
        blinker = new org.ardour.Blinker();
        blinker.setHandler(mHandler);
        blinker.addBlinker(recordButton);
        blinker.start();
		
		super.onResume();
	}

	/**
	 * Another activity is loosing focus
	 */
	@Override
    public void onPause(){    	
    	super.onPause();
    }

	/**
	 * This activity is no longer visible
	 */
    @Override
    public void onStop(){
        
    	clock.stopClock();
    	oscService.disconnect();
    	blinker.stopBlinker();
    	
    	super.onStop();
    }
    
    
    /**
     * The activity is being destroyed.
     * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		
		super.onDestroy();
	}


	/**
     * Save the application preferences
     */
    public void savePreferences(){

        // Save user preferences. We need an Editor object to
        // make changes. All objects are from android.context.Context
        SharedPreferences settings = getSharedPreferences("Ardroid", 0);
        SharedPreferences.Editor editor = settings.edit();
        
        editor.putString("host", host);
        editor.putInt("port", port);
        
        editor.commit();
    }




    
    /*
     * The Transport onClick handler that dispatches UI events to the OSC service
     * (non-Javadoc)
     * @see android.view.View.OnClickListener#onClick(android.view.View)
     */
	@Override
	public void onClick(View v) {

		Log.d(TAG, "--- onClick event received");

		switch (v.getId()) {


			case R.id.bPlay:

				oscService.transportAction(org.ardour.OscService.TRANSPORT_PLAY);

				if (RECORD_ENABLED == (RECORD_ENABLED & transportState)) {
					transportState = TRANSPORT_RUNNING | RECORD_ENABLED;
					recordButton.toggleOn();

					locationBar.setEnabled(false);
				} else {
					transportState = TRANSPORT_RUNNING;
				}

				transportToggleGroup.toggle(playButton, true);

				break;

			case R.id.bStop:

				oscService.transportAction(org.ardour.OscService.TRANSPORT_STOP);

				transportState = TRANSPORT_STOPPED;

				transportToggleGroup.toggle(stopButton, true);
				recordButton.toggleOff();
				locationBar.setEnabled(true);

				break;

			case R.id.bGotoStart:
				/*
				if (transportState == TRANSPORT_RUNNING) {
					transportToggleGroup.toggle(stopButton, true);
					transportState = TRANSPORT_STOPPED;
				} else if (TRANSPORT_RUNNING == (transportState & TRANSPORT_RUNNING)
						&& RECORD_ENABLED == (transportState & RECORD_ENABLED)) {
					break;
				}
				*/
				oscService.transportAction(org.ardour.OscService.GOTO_START);
				locationBar.setProgress(0);

				break;

			case R.id.bGotoEnd:
				/*
				if (transportState == TRANSPORT_RUNNING) {
					transportToggleGroup.toggle(stopButton, true);
					transportState = TRANSPORT_STOPPED;
				} else if (TRANSPORT_RUNNING == (transportState & TRANSPORT_RUNNING)
						&& RECORD_ENABLED == (transportState & RECORD_ENABLED)) {
					break;
				}
				*/
				oscService.transportAction(org.ardour.OscService.GOTO_END);
				locationBar.setProgress(10000);

				break;
			case R.id.bRec:

				oscService.transportAction(org.ardour.OscService.REC_ENABLE_TOGGLE);

				if (RECORD_ENABLED != (transportState & RECORD_ENABLED)) {

					if (TRANSPORT_STOPPED == (transportState & TRANSPORT_STOPPED)) {
						recordButton.toggleOnAndBlink();
					} else {
						recordButton.toggleOn();
						locationBar.setEnabled(false);
					}

					transportState = (byte) (transportState | RECORD_ENABLED);
				} else {
					transportState = (byte) (transportState ^ RECORD_ENABLED);
					recordButton.toggleOff();
					locationBar.setEnabled(true);
				}


				break;

			case R.id.bLoopEnable:

				if (!(TRANSPORT_RUNNING == (transportState & TRANSPORT_RUNNING)
						&& RECORD_ENABLED == (transportState & RECORD_ENABLED))) {

					oscService.transportAction(org.ardour.OscService.LOOP_ENABLE_TOGGLE);
					transportToggleGroup.toggle(loopButton, true);
				}

				break;

			case R.id.bJumpPrevMarker:
				oscService.transportAction(org.ardour.OscService.GOTO_PREV_MARKER);
				break;

			case R.id.bAddMarker:
				oscService.transportAction(org.ardour.OscService.ADD_MARKER);
				break;
			case R.id.bJumpNextMarker:
				oscService.transportAction(org.ardour.OscService.GOTO_NEXT_MARKER);
				break;

			//Config Handler click events
			case R.id.config_ok:
				configDialog.dismiss();
				break;

			case R.id.config_cancel:
				configDialog.cancel();
				break;

			// Config Handler click events
			case R.id.track_diag_ok:
				configDialog.dismiss();
				break;

			case R.id.track_diag_cancel:
				configDialog.cancel();
				break;


			// Track header events
			case R.id.bRecEnable:

				org.ardour.Track rt = (org.ardour.Track) v.getTag();
				Log.d(TAG, "Rec enable track: " + rt);

				oscService.trackListAction(org.ardour.OscService.REC_CHANGED, rt);

				break;

			case R.id.bSoloEnable:

				org.ardour.Track st = (org.ardour.Track) v.getTag();
				Log.d(TAG, "Solo enable track: " + st);

				oscService.trackListAction(org.ardour.OscService.SOLO_CHANGED, st);

				break;

			case R.id.bMuteEnable:

				org.ardour.Track mt = (org.ardour.Track) v.getTag();
				Log.d(TAG, "Mute enable track: " + mt);

				oscService.trackListAction(org.ardour.OscService.MUTE_CHANGED, mt);

				break;


		}

	}


	
	
	/**
	 * A private implementation of the message handler
	 */
	private Handler mHandler = new Handler(){

		/* (non-Javadoc)
		 * @see android.os.Handler#handleMessage(android.os.Message)
		 */
		@Override
		public void handleMessage(Message msg) {

			switch (msg.what){

			case 400: // Max frame message
				maxFrame = (Long) msg.obj;
				break;
			case 500: // Frame rate message
				//Set the frame rate first
				frameRate = (Long) msg.obj;				
				break;
			case 1000: // Refresh track list
				trackList.clear();
				
				List<org.ardour.Track> l = (List<org.ardour.Track>) msg.obj;
				
				for(org.ardour.Track t: l){
					trackList.add(t);
				}
				
				trackListViewAdaptor.notifyDataSetChanged();
	
				break;
			case 2000: // A Track in the track list has changed.			
				trackListViewAdaptor.notifyDataSetChanged();		
				break;
			case 3000: // Return message with latest frame location
				updateClock((Long) msg.obj);
				break;
				
			case 5000: // Return message with latest frame location
				blinker.doBlink();
				break;

			}	
		}
	};
	
	
	/**
	 * Call back to update the clock display.
	 * HH:MM:SS.DDD for now.
	 * @param clock
	 */
	private void updateClock(Long clock){
		
		long frameRate = this.frameRate.longValue();
		long left = clock.longValue();
		
		int hrs = (int) Math.floor(left / (frameRate * 60.0f * 60.0f));
		left -= Math.floor(hrs * frameRate * 60.0f * 60.0f);
		
		int mins = (int) Math.floor(left / (frameRate * 60.0f));
		left -= Math.floor(mins * frameRate * 60.0f);
		
		float secs = (float) (left / (float) frameRate);
		
		clockView.setText(String.format("%02d:%02d:%06.3f", hrs, mins, secs));
		clockView.refreshDrawableState();
		
		//Update the progress bar only if the transport is running.
		//if(TRANSPORT_RUNNING == (transportState & TRANSPORT_RUNNING)){
			locationBar.setProgress(Math.round(( (float) clock.longValue()/ (float) maxFrame.longValue()) * 10000));
			locationBar.refreshDrawableState();
		//}
	}
	
	
	/**
	 * A thread to poll Ardour for the present clock position. We do this approx 20 times per sec.
	 */
	private class Clock extends Thread {
		
		private boolean clockRunning = true;
		
		public void run(){
			
			while(clockRunning){
				
				oscService.getClock();
				
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			
			Log.d("Ardroid", "Stopping transport clock poll thread");
		}
		
		/**
		 * Start the clock thread
		 */
		public void startClock(){
			
			clockRunning = true;
			
			if(!isAlive()){
				setDaemon(true);
				start();
			}
		}

		/**
		 * Stops the clock thread
		 */
		public void stopClock(){
			clockRunning = false;
		}		
	}


	private int selectedTrack = -1;
	
	OnItemLongClickListener cl = new OnItemLongClickListener() {

		@Override
		public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
			selectedTrack = position;
			showDialog(2);
			return true;
		}
	};
	
	
	/**
	 * Set up the options menu
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		
		menu.add(0, 0, 0, "Connect");
		menu.add(0, 1, 0, "Refresh");
		menu.add(0, 2, 0, "Configure");
		
		return true;
	}

	/**
	 * Handle menu options selections
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch(item.getItemId()){
		case 0: // Connect
			startConnectionToArdour();
	        
	        break;
		case 1: // Refresh
			break;
		case 2: // Configure
			
			//start the configuration activity
			this.showDialog(1);
			break;
		}
		
		return true;
	}


	/**
	 * 
	 */
	private void startConnectionToArdour() {
		oscService.setHost(host);
		oscService.setPort(port);
		
		//Reconnect to the OSC server. We need to ensure that the clock thread is stopped.
		if(clock != null){
			clock.stopClock();
		}
		
		if (oscService.isConnected()){
			oscService.disconnect();
		}
		
		clock = new Clock();
			
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		oscService.connect();
		clock.startClock();
	}
	
	
	// Activity Dialog handling

	private Dialog configDialog = null;
	private Dialog trackDialog = null;
	
	@Override
	protected Dialog onCreateDialog(int id) {
		
		Dialog dialog = null;
		
		switch(id){
		case 1:
			
			if(configDialog == null){
				configDialog = createConfigDialog();
				configDialog.setOnDismissListener(configDialogOnDismissListener);
			}	
			
			dialog = configDialog;
			
			break;
			
		case 2:
			
			if(trackDialog == null){
				trackDialog = createTrackDialog();
				trackDialog.setOnDismissListener(trackDialogOnDismissListener);
			}	
			
			dialog = trackDialog;
			
			break;			
		}
		
		return dialog;
	}
	
	
	/**
	 * Create the configuration dialog and show it.
	 */
	public Dialog createConfigDialog(){
		
		Dialog dialog = new Dialog(this);

		dialog.setContentView(R.layout.options_dialog);
		dialog.setTitle("Setup");
		
		TextView tv = (TextView) dialog.findViewById(R.id.config_host_label);
		tv.setText("Host");
		
		tv = (TextView) dialog.findViewById(R.id.config_port_label);
		tv.setText("Port");
		
		Button button = (Button) dialog.findViewById(R.id.config_ok);
		button.setOnClickListener(this);
		
		button = (Button) dialog.findViewById(R.id.config_cancel);
		button.setOnClickListener(this);
		
		
		return dialog;
	}
	
	/**
	 * 
	 * @return
	 */
	public Dialog createTrackDialog(){
		
		Dialog dialog = new Dialog(this);

		dialog.setContentView(R.layout.track_dialog);
		dialog.setTitle("Track");
		
		CheckBox recEnabledRb = (CheckBox) trackDialog.findViewById(R.id.rec_enabled_rb);
		CheckBox muteRb = (CheckBox) trackDialog.findViewById(R.id.mute_rb);
		CheckBox soloRb = (CheckBox) trackDialog.findViewById(R.id.solo_rb);

		recEnabledRb.setOnCheckedChangeListener(checkChangedListener);
		muteRb.setOnCheckedChangeListener(checkChangedListener);
		muteRb.setOnCheckedChangeListener(checkChangedListener);
		
		Button button = (Button) dialog.findViewById(R.id.track_diag_ok);
		button.setOnClickListener(this);
		
		button = (Button) dialog.findViewById(R.id.track_diag_cancel);
		button.setOnClickListener(this);
		
		
		return dialog;
	}
	
	OnCheckedChangeListener checkChangedListener = new OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			
			switch(buttonView.getId()){
			case R.id.rec_enabled_rb:
				break;
			case R.id.solo_rb:
				break;
			case R.id.mute_rb:
				break;
			}	
		}	
	};
	
	/**
	 * 
	 */
	OnMultiChoiceClickListener mcl = new OnMultiChoiceClickListener(){

		@Override
		public void onClick(DialogInterface dialog, int which, boolean isChecked) {
			// TODO Auto-generated method stub
			
		}
	};
	
	/**
	 * 
	 */
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {

		switch(id){
		case 1:
			
			EditText editText = (EditText) configDialog.findViewById(R.id.config_host);
			editText.setText(host);
			
			editText = (EditText) configDialog.findViewById(R.id.config_port);
			editText.setText(Integer.toString(port));
			
			break;
			
		case 2:
			
			CheckBox recEnabledRb = (CheckBox) trackDialog.findViewById(R.id.rec_enabled_rb);
			CheckBox muteRb = (CheckBox) trackDialog.findViewById(R.id.mute_rb);
			CheckBox soloRb = (CheckBox) trackDialog.findViewById(R.id.solo_rb);
			
			org.ardour.Track track = trackList.get(selectedTrack);
			
			recEnabledRb.setChecked(track.recEnabled);
			muteRb.setChecked(track.muteEnabled);
			soloRb.setChecked(track.soloEnabled);
			
			break;
		}
	}
	
	
	/**
	 *  Config Dialog on dismiss event handler
	 */
	DialogInterface.OnDismissListener configDialogOnDismissListener = new DialogInterface.OnDismissListener() {
		
		@Override
		public void onDismiss(DialogInterface dialog) {
			
			EditText editText = (EditText) configDialog.findViewById(R.id.config_host);
			setHost(editText.getText().toString());
			
			editText = (EditText) configDialog.findViewById(R.id.config_port);
			
			int port;
			
			try {
				port = Integer.parseInt(editText.getText().toString());
			}
			catch(NumberFormatException e){
				port = 0;
			}
			
			setPort(port);
			
			//Save the preferences
			savePreferences();
		}
	};

	
	/**
	 *  Track Dialog on dismiss event handler
	 */
	DialogInterface.OnDismissListener trackDialogOnDismissListener = new DialogInterface.OnDismissListener() {
		
		@Override
		public void onDismiss(DialogInterface dialog) {
			

		}
	};
	

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}
}
