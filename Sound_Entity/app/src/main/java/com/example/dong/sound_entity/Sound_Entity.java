package com.example.dong.sound_entity;

import com.example.dong.sound_entity.SoundTouch;

/**
 * Created by dong on 18-3-30.
 */

public class Sound_Entity {
    private String name;
    private float tempo;
    private float pitch;

    public Sound_Entity(){
        this.name = "";
        this.tempo = 100;
        this.pitch = 0;
    }
    public Sound_Entity(String name, float tempo, float pitch){
        this.name = name;
        this.tempo = tempo;
        this.pitch = pitch;
    }

    public short[] Voice_Change(short[] buffer, int bufferSize){
        SoundTouch st = new SoundTouch();
        st.setTempo(this.tempo);
        st.setPitchSemiTones(this.pitch);
        int voice_num = 0;
        short[] buffer_copy = buffer;
        short[] buffer_out = new short[bufferSize];
        short[] buffer_recieve = st.realtimeprocessFile(buffer_copy, bufferSize, 1, buffer_out, voice_num);
        return buffer_recieve;
    }

    public void set_Pitch(float pitch){
        this.pitch = pitch;
    }

    public void set_Name(String name){
        this.name = name;
    }

    public void set_Tempo(float tempo){
        this.tempo = tempo;
    }

}
