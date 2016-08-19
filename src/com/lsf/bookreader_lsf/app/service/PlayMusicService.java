package com.lsf.bookreader_lsf.app.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.widget.Toast;

import com.lsf.bookreader_lsf.app.R;

public class PlayMusicService extends Service {
	MediaPlayer player = null;
	MusicReceicer playMusicReceicer = null;
	MusicReceicer pauseMusicReceicer = null;
	
	private String pauseMusic = "com.android.music.musicservicecommand.pause";
	private String playMusic = "com.android.music.musicservicecommand.play";

	public PlayMusicService() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void onCreate() {
		super.onCreate();
		Toast.makeText(this, "MyService Createed!", Toast.LENGTH_SHORT).show();
		player = MediaPlayer.create(this, R.raw.burning);
//		MediaPlayer.create(this, uri);
		player.setLooping(false);
		pauseMusicReceicer = new MusicReceicer();
		IntentFilter pauseFilter = new IntentFilter(pauseMusic);
		registerReceiver(pauseMusicReceicer, pauseFilter);
		playMusicReceicer = new MusicReceicer();
		IntentFilter playFilter = new IntentFilter(playMusic);
		registerReceiver(playMusicReceicer, playFilter);
	}

	@Override
	public void onStart(Intent intent, int startId) {
		player.start();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		player.stop();
		unregisterReceiver(playMusicReceicer);
		unregisterReceiver(pauseMusicReceicer);
	}
	
	private class MusicReceicer extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String command = intent.getStringExtra("command");
			if(command.equals("pause")){
				player.pause();
			}
			if(command.equals("play")){
				player.start();
			}
		}
		
	}

}