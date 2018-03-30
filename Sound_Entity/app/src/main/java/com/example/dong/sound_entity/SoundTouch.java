package com.example.dong.sound_entity;////////////////////////////////////////////////////////////////////////////////
///
/// Example class that invokes native com.example.dong.sound_entity.SoundTouch routines through the JNI
/// interface.
///
/// Author        : Copyright (c) Olli Parviainen
/// Author e-mail : oparviai 'at' iki.fi
/// WWW           : http://www.surina.net
///
////////////////////////////////////////////////////////////////////////////////
//
// $Id: com.example.dong.sound_entity.SoundTouch.java 211 2015-05-15 00:07:10Z oparviai $
//
////////////////////////////////////////////////////////////////////////////////

public final class SoundTouch
{
    // Native interface function that returns com.example.dong.sound_entity.SoundTouch version string.
    // This invokes the native c++ routine defined in "soundtouch-jni.cpp".
    public native final static String getVersionString();
    
    private native final void setTempo(long handle, float tempo);

    private native final void setPitchSemiTones(long handle, float pitch);
    
    private native final void setSpeed(long handle, float speed);

    private native final int processFile(long handle, String inputFile, String outputFile);

    public native final static String getErrorString();

    private native final static long newInstance();
    
    private native final void deleteInstance(long handle);

    private native final void putSamples(long handle, short[] buffer, int bufferSize, int channels);

    private native final float getMyBPM(long handle, String inputFile);

    private native final short[] realtimeprocessFile(long handle, short[] buffer, int buffer_size, int channels, short[] buffer_out, int voice_num);
    
    long handle = 0;
    
    
    public SoundTouch()
    {
    	handle = newInstance();    	
    }
    
    
    public void close()
    {
    	deleteInstance(handle);
    	handle = 0;
    }


    public void setTempo(float tempo)
    {
    	setTempo(handle, tempo);
    }


    public void setPitchSemiTones(float pitch)
    {
    	setPitchSemiTones(handle, pitch);
    }

    
    public void setSpeed(float speed)
    {
    	setSpeed(handle, speed);
    }


    public int processFile(String inputFile, String outputFile)
    {
    	return processFile(handle, inputFile, outputFile);
    }


    
    // Load the native library upon startup

    public void putSamples(short[] buffer, int bufferSize , int channels)
    {
        putSamples(handle, buffer, bufferSize, channels);
    }

    public float getMyBPM(String inputFile){
        return getMyBPM(handle, inputFile);
    }

    public short[] realtimeprocessFile(short[] buffer, int buffer_size, int channels, short[] buffer_out, int voice_num){
        return realtimeprocessFile(handle, buffer, buffer_size, channels, buffer_out, voice_num);
    }

    static
    {
        System.loadLibrary("soundtouch");
    }
}
