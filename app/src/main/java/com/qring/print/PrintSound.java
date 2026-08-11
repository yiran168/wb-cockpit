package com.qring.print;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;

/**
 * Tiny dependency-free print-confirm sound engine.
 *
 * Nothing is downloaded and no audio files are bundled.  Ten presets are synthesized as short PCM
 * waveforms so they work offline from API 21 upward.  The user can also choose "random preset" or
 * "procedural random"; the latter creates a new safe short note pattern for each print press.
 */
final class PrintSound {
    static final String KEY_ENABLED="print_sound_enabled";
    static final String KEY_MODE="print_sound_mode";
    static final String KEY_VOLUME="print_sound_volume";
    static final int RANDOM_PRESET=10;
    static final int RANDOM_GENERATED=11;

    static final String[] MODE_NAMES={
            "01 · 纸张轻响","02 · 清脆确认","03 · 短促双击","04 · 柔和上扬","05 · 机械点按",
            "06 · 轻快三连","07 · 低频确认","08 · 电子短鸣","09 · 扫描完成","10 · 极简点击",
            "随机使用 10 种","随机生成新音效"
    };

    private PrintSound(){}

    static void play(Context c){
        if(c==null)return;
        try{
            SharedPreferences sp=c.getSharedPreferences("settings",0);
            if(!sp.getBoolean(KEY_ENABLED,true))return;
            int mode=Math.max(0,Math.min(RANDOM_GENERATED,sp.getInt(KEY_MODE,0)));
            int volume=Math.max(0,Math.min(100,sp.getInt(KEY_VOLUME,62)));
            play(c,mode,volume);
        }catch(Throwable ignored){}
    }

    static void play(Context c,int mode,int volume){
        final int resolved=mode==RANDOM_PRESET?(int)(Math.random()*10):mode;
        final int vol=Math.max(0,Math.min(100,volume));
        if(vol<=0)return;
        new Thread(()->{
            AudioTrack track=null;
            try{
                short[] pcm=resolved==RANDOM_GENERATED?PrintSoundPattern.randomPattern():PrintSoundPattern.preset(resolved);
                int bytes=pcm.length*2;
                track=new AudioTrack(AudioManager.STREAM_MUSIC,PrintSoundPattern.SAMPLE_RATE,
                        AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT,
                        Math.max(bytes,AudioTrack.getMinBufferSize(PrintSoundPattern.SAMPLE_RATE,AudioFormat.CHANNEL_OUT_MONO,AudioFormat.ENCODING_PCM_16BIT)),
                        AudioTrack.MODE_STATIC);
                float gain=Math.max(.03f,vol/100f)*.72f;
                try{track.setVolume(gain);}catch(Throwable ignored){}
                track.write(pcm,0,pcm.length);
                track.play();
                long wait=Math.max(90,Math.min(700,(pcm.length*1000L/PrintSoundPattern.SAMPLE_RATE)+45));
                try{Thread.sleep(wait);}catch(InterruptedException ignored){Thread.currentThread().interrupt();}
            }catch(Throwable ignored){}
            finally{
                if(track!=null){try{track.stop();}catch(Throwable ignored){}try{track.release();}catch(Throwable ignored){}}
            }
        },"CuotiPrint-sound").start();
    }

}
