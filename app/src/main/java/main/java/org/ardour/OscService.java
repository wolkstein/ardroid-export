/**
 * 
 */
package org.ardour;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Objects;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import de.sciss.net.OSCClient;
import de.sciss.net.OSCListener;
import de.sciss.net.OSCMessage;

/**
 * @author lincoln
 *
 */
public class OscService {

	private static String TAG = "OscService";
	
	// States
	public static final int READY = 0;
	public static final int ROUTES_REQUESTED = 1;
	
	// Transport actions
	public static final int TRANSPORT_PLAY = 0;
	public static final int TRANSPORT_STOP = 1;
	public static final int GOTO_START = 2;
	public static final int GOTO_END = 3;
	public static final int REC_ENABLE_TOGGLE = 4;
	public static final int LOOP_ENABLE_TOGGLE = 5;
	public static final int FFWD = 6;
	public static final int REWIND = 7;
	public static final int LOCATE = 8;

	public static final int GOTO_PREV_MARKER = 9;
	public static final int ADD_MARKER = 10;
	public static final int GOTO_NEXT_MARKER = 11;
	// Change Ids
	public static final int REC_CHANGED = 0;
	public static final int MUTE_CHANGED = 1;
	public static final int SOLO_CHANGED = 2;
	public static final int NAME_CHANGED = 3;
	public static final int GAIN_CHANGED = 4;
	
	private OSCClient oscClient;
	
	
	private ArrayList<org.ardour.Track> routes= new ArrayList<org.ardour.Track>();
	
	private int state = 0;
	
	private String host = "127.0.0.1";
	private int port = 3819;

	/** The handler where we shall post transport state updates on the UI thread. */
	private Handler transportHandler = null;

	//private ArdroidMain ardroidMainActivity = null;
	
	/**
	 * 
	 * @param host
	 * @param port
	 */
	public OscService(String host, int port){
		this.host = host;
		this.port = port;
	}
	
	/**
	 * @return the transportHandler
	 */
	public Handler getTransportHandler() {
		return transportHandler;
	}

	/**
	 * @param transportHandler the transportHandler to set
	 */
	public void setTransportHandler(Handler transportHandler) {
		this.transportHandler = transportHandler;
	}
	
	//public void setListener(ArdroidMain at){
	//	this.ardroidMainActivity = at;
	//}
	
	/**
	 * Connect to the Ardour OSC server
	 */
	public void connect(){
		
		try {

			Log.d(TAG, "Connetecting to Ardour");
			
			if(oscClient != null && oscClient.isConnected()){
				disconnect();
			}

	        oscClient = OSCClient.newUsing (OSCClient.UDP);    // create UDP client with any free port number
	        oscClient.setTarget (new InetSocketAddress (InetAddress.getByName(host), port)); 
	        	        
	        Log.d(TAG, "Starting connection...");
	        
	        oscClient.start();  // open channel and (in the case of TCP) connect, then start listening for replies
			
	        Log.d(TAG, "Started. Starting listener");
	        
	        oscClient.addOSCListener(replyListener);
	        
	        Log.d(TAG, "Listening.");
			
			if (oscClient.isConnected()){

				routes.clear();
				state = OscService.ROUTES_REQUESTED;

				System.out.println("OSC State: " + state);
				sendOSCMessage("/routes/list");
				//Object[] args = {1,2};
				//sendOSCMessage("/routes/gainabs", args);
			}
			
		} catch (UnknownHostException e) {
			Log.d(TAG, "Unknown host");
			e.printStackTrace();
		} catch (IOException e) {
			Log.d(TAG, "IO Exception");
			e.printStackTrace();
		}
	}
	
	/**
	 * Method to disconnect from the present connected session.
	 * Ask Ardour to stop listening to our observed tracks.
	 */
	public void disconnect(){
		
		Integer[] args = new Integer[routes.size()];
		
		for(int i = 0; i < routes.size(); i++){
			org.ardour.Track t = routes.get(i);
			args[i] = t.remoteId;
		}
		
		OSCMessage message = new OSCMessage("/routes/ignore", args);
		
		try {
			oscClient.send(message);
			oscClient.dispose();
		}
		catch (IOException e){
			e.printStackTrace();
		}
	}
	
	/**
	 * Check if the OSC client is connected
	 * @return
	 */
	public boolean isConnected(){
		
		if (oscClient != null){
			return oscClient.isConnected();
		}
		
		return false;
	}
	
	/**
	 * Map an OSC URI path with the Transport Action in the UI
	 * @param cmd
	 */
	public void transportAction(int cmd){
		
		String uri = "";
		
		switch(cmd){
		case TRANSPORT_PLAY:
			uri = "/ardour/transport_play";
			break;
			
		case TRANSPORT_STOP:
			uri = "/ardour/transport_stop";
			break;
			
		case GOTO_START:
			uri = "/ardour/goto_start";
			break;
			
		case GOTO_END:
			uri = "/ardour/goto_end";
			break;
			
		case REC_ENABLE_TOGGLE:
			uri = "/ardour/rec_enable_toggle";
			break;
			
		case LOOP_ENABLE_TOGGLE:
			uri = "/ardour/loop_toggle";
			break;
			
		case FFWD:
			uri = "/ardour/ffwd";
			break;
			
		case REWIND:
			uri = "/ardour/rewind";
			break;

		case GOTO_PREV_MARKER:
			uri = "/ardour/prev_marker";
			break;

		case ADD_MARKER:
			uri = "/ardour/add_marker";
			break;

		case GOTO_NEXT_MARKER:
			uri = "/ardour/next_marker";
			break;
		}
		
		sendOSCMessage(uri);
	}

	/**
	 * Send a track list event to Ardour
	 * @param cmd
	 * @param trackIdx
	 */
	public void transportAction(int cmd, int i){
		
		String uri = "";
		
		Integer[] args = new Integer[2];
		args[0] = Integer.valueOf(i);
		
		
		switch(cmd){
		case LOCATE:
			uri = "/ardour/locate";
			args[1] = Integer.valueOf(0);
		
			break;		
		}
		
		sendOSCMessage(uri, args);
	}

	/**
	 * Send a track list event to Ardour
	 * @param track
	 * @param position
	 */
public void trackListVolumeAction( org.ardour.Track track, int position){

		String uri = "/ardour/routes/gainabs";

	    Object[] args = new Object[2];
	    args[0] = Integer.valueOf(track.remoteId);

	    track.trackVolume = (int)track.sliderToValue(position);
	    args[1] = Double.valueOf(track.sliderToValue(position) / 1000.0);

		sendOSCMessage(uri, args);
}
	
	/**
	 * Send a track list event to Ardour
	 * @param cmd
	 * @param trackIdx
	 */
	public void trackListAction(int cmd, org.ardour.Track track){
		
		String uri = "";

		Integer[] args = new Integer[2];
		args[0] = Integer.valueOf(track.remoteId);
		
		
		switch(cmd){
		case REC_CHANGED:
			uri = "/ardour/routes/recenable";
			args[1] = Integer.valueOf(track.recEnabled ? 0 : 1);
			
			break;
		
		case MUTE_CHANGED:
			uri = "/ardour/routes/mute";
			args[1] = Integer.valueOf(track.muteEnabled ? 0 : 1);
			
			break;
			
		case SOLO_CHANGED:
			uri = "/ardour/routes/solo";
			args[1] = Integer.valueOf(track.soloEnabled ? 0 : 1);

			break;
		}
		
		sendOSCMessage(uri, args);
	}
	
	
	/**
	 * Send a message with no arguments.
	 * @param messageUri
	 */
	private void sendOSCMessage(String messageUri){
		this.sendOSCMessage(messageUri, null);
	}
	
	
	/**
	 * Send an OSC message over the OSC socket.
	 * @param messageUri
	 */
	private void sendOSCMessage(String messageUri, Object[] args){
		
		OSCMessage message;
		
		if (args == null || args.length == 0){
			message = new OSCMessage(messageUri);
		}
		else {
			message = new OSCMessage(messageUri, args);
		}
				
		try {
			oscClient.send (message);
		}
		catch (IOException e){
			Log.d(TAG, "Could not send OSC message: " + messageUri);
			e.printStackTrace();
		}
	}

		
	/**
	 * The OSC reply listener handler
	 */
	private  OSCListener replyListener = new OSCListener(){
		
		@Override
		public void messageReceived(OSCMessage message, SocketAddress addr, long time) {
			
			//Log.d(TAG, "Received Reply: " + message.getName());
			
			switch (state){
			case READY:
				
				int changeId = -1;
				
				if(message.getName().equals("/route/solo")){
					changeId = SOLO_CHANGED;
					handleChange(message, changeId);
				}
				else if(message.getName().equals("/route/mute")){
					changeId = MUTE_CHANGED;
					handleChange(message, changeId);
				}
				else if(message.getName().equals("/route/rec")){
					changeId = REC_CHANGED;
					handleChange(message, changeId);
				}
				else if(message.getName().equals("/route/name")){
					changeId = NAME_CHANGED;
					handleChange(message, changeId);
				}
				else if(message.getName().equals("/route/gain")){
					changeId = GAIN_CHANGED;
					handleChange(message, changeId);
				}
				else if(message.getName().equals("/ardour/transport_frame")){
					
					Long clock = (Long) message.getArg(0);
					
					Message msg = transportHandler.obtainMessage(3000, clock);
					transportHandler.sendMessage(msg);
				}
				
				break;
				
			case ROUTES_REQUESTED:
				
				updateTrackList(message);
				
				break;
			}
		}
	};

	/**
	 * A handler for track state changes coming from Ardour
	 */
	private void handleChange(OSCMessage message, int whatChanged) {
	
		//Log.d(TAG, "Received Change: " + message.toString());
	
		if (state != OscService.READY){
		
			Log.d(TAG, "In non Ready state. Returning.");
			return;
		}
		
		//Route remote Id
		Integer i = (Integer) message.getArg (0);
		Float f = null;
		
		for (org.ardour.Track track : routes){
			
			if (track.remoteId == i.intValue ()) {

				switch (whatChanged) {
					case REC_CHANGED:
						f = (Float) message.getArg(1);
						track.recEnabled = (f.floatValue() == 1);
						break;
					case SOLO_CHANGED:
						f = (Float) message.getArg(1);
						track.soloEnabled = (f.floatValue() == 1);
						break;
					case MUTE_CHANGED:
						f = (Float) message.getArg(1);
						track.muteEnabled = (f.floatValue() == 1);
						break;
					case NAME_CHANGED:
						track.name = (String) message.getArg(1);
						break;
					case GAIN_CHANGED:
							double gainval = (float) message.getArg(1) * 1000.0;
							if (gainval <= 0.0000000001) gainval = 0.0000000001;
							track.trackVolume = track.valueToSlider(gainval);
							System.out.println("Gain as Int: " + track.valueToSlider(gainval) + ", fromArdour: " + gainval);
							if(track.getTrackVolumeOnSeekBar())
								return; // gein change from user on seekbar we return here

						break;
					}

			
				//send message to view
				Message msg = transportHandler.obtainMessage(2000, routes);
				transportHandler.sendMessage(msg);
									
				return;
			}
		}
	}	

	
	/**
	 * Update the tracklist as received from Ardour
	 * @param message
	 */
	private void updateTrackList(OSCMessage message){
		
		Log.d("A", message.toString());

		/*
		for(int i = 0;i < message.getArgCount(); i++){
			System.out.println("Message at " + i + " : " + message.getArg(i));
		}

		*/
		//First arg
		String s = (String) message.getArg(0);

		if (s.equals("end_route_list")){
			
			Long frameRate = (Long) message.getArg(1);
			
			Message msg = transportHandler.obtainMessage(500, frameRate);
			transportHandler.sendMessage(msg);

			Long maxFrame = (Long) message.getArg(2);
			
			Message msg1 = transportHandler.obtainMessage(400, maxFrame);
			transportHandler.sendMessage(msg1);

			Message msg2 = transportHandler.obtainMessage(1000, routes);
			transportHandler.sendMessage(msg2);
			
			state = OscService.READY;
		}
		else {
			
			org.ardour.Track t = new org.ardour.Track();

			//System.out.println("Track contains: " + s);

			if(s.equals("AT")){
				t.type = org.ardour.Track.AUDIO;
			}
			else if(s.equals("MT")){
				t.type = org.ardour.Track.MIDI;
			}
			else if(s.equals("B")){
				t.type = org.ardour.Track.BUS;
			}


			//Set the name of the track
			s = (String) message.getArg(1);
			t.name = s;
			
			//Set mute state
			Integer i = (Integer) message.getArg(4);
			t.muteEnabled = (i.intValue() > 0);

			//Set solo state
			i = (Integer) message.getArg(5);
			t.soloEnabled = (i.intValue() > 0);
			
			//Set remote id
			i = (Integer) message.getArg(6);
			t.remoteId = i.intValue();

			//Set record state
			if(t.type == org.ardour.Track.AUDIO || t.type == org.ardour.Track.MIDI){
				i = (Integer) message.getArg(7);
				t.recEnabled = (i.intValue() > 0);
			}

			routes.add(t);
		}
	}
	
	/**
	 * Make OSC call to get current Session transport frame position
	 */
	public void getClock(){
		if (state == READY){
			this.sendOSCMessage("/ardour/transport_frame");
		}
	}

	/*
	 * Class properties getters and setters
	 */
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
