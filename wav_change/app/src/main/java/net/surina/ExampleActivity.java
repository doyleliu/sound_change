/////////////////////////////////////////////////////////////////////////////
///
/// Example Android Application/Activity that allows processing WAV 
/// audio files with SoundTouch library
///
/// Copyright (c) Olli Parviainen
///
////////////////////////////////////////////////////////////////////////////////
//
// $Id: SoundTouch.java 210 2015-05-14 20:03:56Z oparviai $
//
////////////////////////////////////////////////////////////////////////////////


package net.surina;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import net.surina.soundtouch.SoundTouch;
import net.surina.soundtouchexample.R;
import android.app.Activity;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class ExampleActivity extends Activity implements OnClickListener 
{
	TextView textViewConsole = null;
	EditText editSourceFile = null;
	EditText editOutputFile = null;
	EditText editPCMFile = null;
	EditText editTempo = null;
	EditText editPitch = null;
	CheckBox checkBoxPlay = null;
	
	StringBuilder consoleText = new StringBuilder();

	
	/// Called when the activity is created
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_example);
		
		textViewConsole = (TextView)findViewById(R.id.textViewResult);
		editSourceFile = (EditText)findViewById(R.id.editTextSrcFileName);
		editOutputFile = (EditText)findViewById(R.id.editTextOutFileName);
		editPCMFile = (EditText)findViewById(R.id.editPCMName);

		editTempo = (EditText)findViewById(R.id.editTextTempo);
		editPitch = (EditText)findViewById(R.id.editTextPitch);
		
		Button buttonFileSrc = (Button)findViewById(R.id.buttonSelectSrcFile);
		Button buttonFileOutput = (Button)findViewById(R.id.buttonSelectOutFile);
		Button buttonProcess = (Button)findViewById(R.id.buttonProcess);
		Button buttonRealTime = (Button)findViewById(R.id.buttonRealTime);
		Button buttonPCM = (Button)findViewById(R.id.buttonPCM);
		buttonFileSrc.setOnClickListener(this);
		buttonFileOutput.setOnClickListener(this);
		buttonProcess.setOnClickListener(this);
		buttonRealTime.setOnClickListener(this);
		buttonPCM.setOnClickListener(this);

		checkBoxPlay = (CheckBox)findViewById(R.id.checkBoxPlay);

		// Check soundtouch library presence & version
		checkLibVersion();
	}
	
	
		
	/// Function to append status text onto "console box" on the Activity
	public void appendToConsole(final String text)
	{
		// run on UI thread to avoid conflicts
		runOnUiThread(new Runnable() 
		{
		    public void run() 
		    {
				consoleText.append(text);
				consoleText.append("\n");
				textViewConsole.setText(consoleText);
		    }
		});
	}
	

	
	/// print SoundTouch native library version onto console
	protected void checkLibVersion()
	{
		String ver = SoundTouch.getVersionString();
		appendToConsole("SoundTouch native library version = " + ver);
	}



	/// Button click handler
	@Override
	public void onClick(View arg0) 
	{
		switch (arg0.getId())
		{
			case R.id.buttonSelectSrcFile:
			case R.id.buttonSelectOutFile:
				// one of the file select buttons clicked ... we've not just implemented them ;-)
				Toast.makeText(this, "File selector not implemented, sorry! Enter the file path manually ;-)", Toast.LENGTH_LONG).show();
				break;
				
			case R.id.buttonProcess:
				// button "process" pushed
				process();
				break;
			case R.id.buttonRealTime:
				realtimeprocess();
				break;
			case R.id.buttonPCM:
				PCMplayer();
				break;
		}
		
	}
	
	
	/// Play audio file
	protected void playWavFile(String fileName)
	{
		File file2play = new File(fileName);
		Intent i = new Intent();
		i.setAction(android.content.Intent.ACTION_VIEW);
		i.setDataAndType(Uri.fromFile(file2play), "audio/wav");
		startActivity(i);		
	}
	
				

	/// Helper class that will execute the SoundTouch processing. As the processing may take
	/// some time, run it in background thread to avoid hanging of the UI.
	protected class ProcessTask extends AsyncTask<ProcessTask.Parameters, Integer, Long>
	{
		/// Helper class to store the SoundTouch file processing parameters
		public final class Parameters
		{
			String inFileName;
			String outFileName;
			float tempo;
			float pitch;
		}

		
		
		/// Function that does the SoundTouch processing
		public final long doSoundTouchProcessing(Parameters params)
		{

			SoundTouch st = new SoundTouch();
			st.setTempo(params.tempo);
			st.setPitchSemiTones(params.pitch);
			float myBPM= st.getMyBPM(params.inFileName);
			Log.i("SoundTouch", "process file " + params.inFileName);
			long startTime = System.currentTimeMillis();
			int res = st.processFile(params.inFileName, params.outFileName);
			long endTime = System.currentTimeMillis();
			float duration = (endTime - startTime) * 0.001f;

			Log.i("SoundTouch", "process file done, duration = " + duration);
			appendToConsole("Processing done, duration " + duration + " sec.");
//			appendToConsole("The BPM is " + myBPM);
			if (res != 0)
			{
				String err = SoundTouch.getErrorString();
				appendToConsole("Failure: " + err);
				return -1L;
			}

			// Play file if so is desirable
			if (checkBoxPlay.isChecked())
			{
				playWavFile(params.outFileName);
			}
			return 0L;
		}

		public final long doRealTimeSoundProcessing(Parameters params)
		{
			int mFrequency = 16000;
			String test2 = "";
			SoundTouch st = new SoundTouch();
			st.setTempo(params.tempo);
			st.setPitchSemiTones(params.pitch);
			boolean mIsrecording = true;
//			int bufferSize = AudioRecord.getMinBufferSize(mFrequency, 1, AudioFormat.ENCODING_PCM_16BIT);
			int bufferSize = 4096;
			AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, mFrequency,  1, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
			short[] buffer = new short[bufferSize];

			File fpath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/voice_data/");
			fpath.mkdir();
			File audioFile;

			try{
				audioFile = File.createTempFile("record", ".pcm", fpath);
				DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(audioFile)));
				FileInputStream fileInputStream;

				record.startRecording();
				int loop = 50;
				while (loop > 0)
				{
					loop = loop - 1;
					short[] buffer_out = new short[bufferSize];
					int voice_num = 0;
					test2 = "";
					int buffferReadResult = record.read(buffer, 0 ,bufferSize);
					short[] buffer_copy = buffer;
					short[] buffer_recieve = st.realtimeprocessFile(buffer_copy, bufferSize, 1, buffer_out, voice_num);


//					for(int i = 0; i < buffer_recieve.length; i++ )
//					{
//
//						dos.writeShort(buffer_recieve[i]);
//					}

//					dos.flush();
					int musicLength = buffer_recieve.length;
					short[] music = new short[musicLength];

					for(int i = 0; i < musicLength; i++)
					{
						music[i] = buffer_recieve[i];
						i++;
					}
//				open(musicLength);
					AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mFrequency,1,AudioFormat.ENCODING_PCM_16BIT , musicLength, AudioTrack.MODE_STREAM);
					audioTrack.play();
					audioTrack.write(music,0 ,musicLength);
					audioTrack.stop();
					audioTrack.release();
//					Log.v("finally_out", test2);
				}

			}catch (IOException e){
				e.printStackTrace();
				appendToConsole("Wrong!!! ");
			}
			record.stop();
			record.release();
			return 0L;
		}

		/// Overloaded function that get called by the system to perform the background processing
		@Override
		protected Long doInBackground(Parameters... aparams)
		{
			return doSoundTouchProcessing(aparams[0]);
		}

	}




//		public final long doSoundTouchProcessing(Parameters params)
//		{
//
//			SoundTouch st = new SoundTouch();
//			st.setTempo(params.tempo);
//			st.setPitchSemiTones(params.pitch);
//			float myBPM= st.getMyBPM(params.inFileName);
//			Log.i("SoundTouch", "process file " + params.inFileName);
//			long startTime = System.currentTimeMillis();
//			int res = st.processFile(params.inFileName, params.outFileName);
//			long endTime = System.currentTimeMillis();
//			float duration = (endTime - startTime) * 0.001f;
//
//			Log.i("SoundTouch", "process file done, duration = " + duration);
//			appendToConsole("Processing done, duration " + duration + " sec.");
////			appendToConsole("The BPM is " + myBPM);
//			if (res != 0)
//			{
//				String err = SoundTouch.getErrorString();
//				appendToConsole("Failure: " + err);
//				return -1L;
//			}
//
//			// Play file if so is desirable
//			if (checkBoxPlay.isChecked())
//			{
//				playWavFile(params.outFileName);
//			}
//			return 0L;
//		}
//
//		public final long doRealTimeSoundProcessing(Parameters params)
//		{
//			int mFrequency = 16000;
//			String test2 = "";
//			SoundTouch st = new SoundTouch();
//			st.setTempo(params.tempo);
//			st.setPitchSemiTones(params.pitch);
//			boolean mIsrecording = true;
////			int bufferSize = AudioRecord.getMinBufferSize(mFrequency, 1, AudioFormat.ENCODING_PCM_16BIT);
//			int bufferSize = 4096;
//			AudioRecord record = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, mFrequency,  1, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
//			short[] buffer = new short[bufferSize];
//
//			File fpath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/voice_data/");
//			fpath.mkdir();
//			File audioFile;
//
//			try{
//				audioFile = File.createTempFile("record", ".pcm", fpath);
//				DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(audioFile)));
//				record.startRecording();
//				int loop = 50;
//				while (loop > 0)
//				{
//					loop = loop - 1;
//					short[] buffer_out = new short[bufferSize];
//					int voice_num = 0;
//					test2 = "";
//					int buffferReadResult = record.read(buffer, 0 ,bufferSize);
//					short[] buffer_copy = buffer;
//					short[] buffer_recieve = st.realtimeprocessFile(buffer_copy, bufferSize, 1, buffer_out, voice_num);
//
//
//					for(int i = 0; i < buffer_recieve.length; i++ )
//					{
//
//						dos.writeShort(buffer_recieve[i]);
//					}
//
//					dos.flush();
//					Log.v("finally_out", test2);
//				}
//
//			}catch (IOException e){
//				e.printStackTrace();
//				appendToConsole("Wrong!!! ");
//			}
//			record.stop();
//			record.release();
//			return 0L;
//		}
//
//		/// Overloaded function that get called by the system to perform the background processing
//		@Override
//		protected Long doInBackground(Parameters... aparams)
//		{
//			return doSoundTouchProcessing(aparams[0]);
//		}
//
//	}


	/// process a file with SoundTouch. Do the processing using a background processing
	/// task to avoid hanging of the UI
	protected void process()
	{
		try 
		{
			ProcessTask task = new ProcessTask();
			ProcessTask.Parameters params = task.new Parameters();
			// parse processing parameters
			params.inFileName = editSourceFile.getText().toString();
			params.outFileName = editOutputFile.getText().toString();
			params.tempo = 0.01f * Float.parseFloat(editTempo.getText().toString());
			params.pitch = Float.parseFloat(editPitch.getText().toString());

			// update UI about status
			appendToConsole("Process audio file :" + params.inFileName +" => " + params.outFileName);
			appendToConsole("Tempo = " + params.tempo);
			appendToConsole("Pitch adjust = " + params.pitch);
			
			Toast.makeText(this, "Starting to process file " + params.inFileName + "...", Toast.LENGTH_SHORT).show();

			// start SoundTouch processing in a background thread
//			task.execute(params);
			task.doSoundTouchProcessing(params);	// this would run processing in main thread
			
		}
		catch (Exception exp)
		{
			exp.printStackTrace();
		}
	
	}

	protected void realtimeprocess()
	{
		try
		{
			ProcessTask task = new ProcessTask();
			ProcessTask.Parameters params = task.new Parameters();
			// parse processing parameters
//			params.inFileName = editSourceFile.getText().toString();
//			params.outFileName = editOutputFile.getText().toString();
			params.tempo = 0.01f * Float.parseFloat(editTempo.getText().toString());
			params.pitch = Float.parseFloat(editPitch.getText().toString());

			// update UI about status
//			appendToConsole("Process audio file :" + params.inFileName +" => " + params.outFileName);
			appendToConsole("Tempo = " + params.tempo);
			appendToConsole("Pitch adjust = " + params.pitch);

//			Toast.makeText(this, "Starting to process file " + params.inFileName + "...", Toast.LENGTH_SHORT).show();

			// start SoundTouch processing in a background thread
//			task.execute(params);
			task.doRealTimeSoundProcessing(params);	// this would run processing in main thread

		}
		catch (Exception exp)
		{
			exp.printStackTrace();
		}

	}

	protected void	PCMplayer()
	{
//		track.play();
//		File fpath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/data/files/");
//
		File audioFile;
//		AudioTrack track;

		try{
			audioFile = new File(editPCMFile.getText().toString());
			FileInputStream fileInputStream;
			int musicLength = (int)(audioFile.length() /2);

			int mFrequency = 16000;

			AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, mFrequency,1,AudioFormat.ENCODING_PCM_16BIT , musicLength * 2, AudioTrack.MODE_STREAM);

			short[] music = new short[musicLength];
//			record.startRecording();
//			while (true)
//			{
				fileInputStream = new FileInputStream(audioFile);
				BufferedInputStream bis= new BufferedInputStream(fileInputStream);
				DataInputStream dis = new DataInputStream(bis);
				int i = 0;
				while(dis.available() > 0)
				{
					music[i] = dis.readShort();
					i++;
				}
				dis.close();
//				open(musicLength);
				audioTrack.play();
				audioTrack.write(music,0 ,musicLength);
				audioTrack.stop();

//			}

		}catch (IOException e){
			e.printStackTrace();
			appendToConsole("Wrong!!! ");
		}
	}


	//detect whether there is any audio input
	public static int calculateVolume(byte[] var0, int var1) {
		int[] var3 = null;
		int var4 = var0.length;
		int var2;
		if(var1 == 8) {
			var3 = new int[var4];
			for(var2 = 0; var2 < var4; ++var2) {
				var3[var2] = var0[var2];
			}
		} else if(var1 == 16) {
			var3 = new int[var4 / 2];
			for(var2 = 0; var2 < var4 / 2; ++var2) {
				byte var5 = var0[var2 * 2];
				byte var6 = var0[var2 * 2 + 1];
				int var13;
				if(var5 < 0) {
					var13 = var5 + 256;
				} else {
					var13 = var5;
				}
				short var7 = (short)(var13 + 0);
				if(var6 < 0) {
					var13 = var6 + 256;
				} else {
					var13 = var6;
				}
				var3[var2] = (short)(var7 + (var13 << 8));
			}
		}

		int[] var8 = var3;
		if(var3 != null && var3.length != 0) {
			float var10 = 0.0F;
			for(int var11 = 0; var11 < var8.length; ++var11) {
				var10 += (float)(var8[var11] * var8[var11]);
			}
			var10 /= (float)var8.length;
			float var12 = 0.0F;
			for(var4 = 0; var4 < var8.length; ++var4) {
				var12 += (float)var8[var4];
			}
			var12 /= (float)var8.length;
			var4 = (int)(Math.pow(2.0D, (double)(var1 - 1)) - 1.0D);
			double var14 = Math.sqrt((double)(var10 - var12 * var12));
			int var9;
			if((var9 = (int)(10.0D * Math.log10(var14 * 10.0D * Math.sqrt(2.0D) / (double)var4 + 1.0D))) < 0) {
				var9 = 0;
			}
			if(var9 > 10) {
				var9 = 10;
			}
			return var9;
		} else {
			return 0;
		}
	}

	//convert byte[] to int[]
}