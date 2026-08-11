package com.qring.print;

import java.util.Random;

/** Pure-Java PCM generator so the sound presets can be unit-tested without Android audio hardware. */
final class PrintSoundPattern {
    static final int SAMPLE_RATE=44100;
    private static final Random RNG=new Random();
    private PrintSoundPattern(){}

    static short[] preset(int i){
        switch(Math.max(0,Math.min(9,i))){
            case 0:return render(new double[]{760},new int[]{72},new int[]{0},0);
            case 1:return render(new double[]{980,1320},new int[]{52,72},new int[]{22,0},0);
            case 2:return render(new double[]{840,840},new int[]{42,42},new int[]{34,0},2);
            case 3:return render(new double[]{620,820,1080},new int[]{48,48,70},new int[]{12,12,0},0);
            case 4:return render(new double[]{420,640},new int[]{46,48},new int[]{18,0},1);
            case 5:return render(new double[]{880,1040,1220},new int[]{34,34,46},new int[]{18,18,0},2);
            case 6:return render(new double[]{390,520},new int[]{76,54},new int[]{16,0},0);
            case 7:return render(new double[]{1180},new int[]{92},new int[]{0},2);
            case 8:return render(new double[]{680,900,1260},new int[]{38,38,78},new int[]{8,8,0},0);
            default:return render(new double[]{1020},new int[]{38},new int[]{0},1);
        }
    }

    static short[] randomPattern(){
        int n=1+RNG.nextInt(4);double[] f=new double[n];int[] d=new int[n],g=new int[n];
        for(int i=0;i<n;i++){f[i]=360+RNG.nextInt(1050);d[i]=32+RNG.nextInt(72);g[i]=i==n-1?0:6+RNG.nextInt(28);}
        return render(f,d,g,RNG.nextInt(3));
    }

    /** wave: 0 sine, 1 triangle, 2 softened square. */
    static short[] render(double[] freq,int[] durMs,int[] gapMs,int wave){
        if(freq==null||durMs==null||gapMs==null||freq.length==0||freq.length!=durMs.length||freq.length!=gapMs.length)throw new IllegalArgumentException("sound pattern arrays");
        int total=0;for(int i=0;i<freq.length;i++)total+=msToSamples(Math.max(1,durMs[i])+Math.max(0,gapMs[i]));
        short[] out=new short[Math.max(1,total)];int at=0;
        for(int note=0;note<freq.length;note++){
            int n=msToSamples(Math.max(1,durMs[note])),gap=msToSamples(Math.max(0,gapMs[note]));
            double hz=Math.max(80,Math.min(4000,freq[note]));
            for(int i=0;i<n&&at<out.length;i++,at++){
                double phase=2.0*Math.PI*hz*i/SAMPLE_RATE,sample;
                if(wave==1)sample=2.0/Math.PI*Math.asin(Math.sin(phase));
                else if(wave==2)sample=Math.tanh(2.15*Math.sin(phase));
                else sample=Math.sin(phase);
                double edge=Math.min(1.0,Math.min(i/Math.max(1.0,n*.12),(n-1-i)/Math.max(1.0,n*.18)));
                out[at]=(short)(sample*Math.max(0,edge)*11800);
            }
            at=Math.min(out.length,at+gap);
        }
        return out;
    }
    static int msToSamples(int ms){return Math.max(0,(int)(SAMPLE_RATE*(ms/1000.0)));}
}
