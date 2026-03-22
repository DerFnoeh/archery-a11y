package com.example.christophers.soundgeneration.SoundPipeline;

/**
 * Takes the current x&y position and handles the sound output based on these variables.
 * Created by christopherS on 12.12.2016.
 */

public interface SoundHandler {
    /**

     * Wert zwischen 0 und 100 in Prozentangabe

     * @author Christopher Schrewing

     * @version 1.0

     */
    void changeVol(int vol);

    /**

     * Wert zwischen 0 (Punkt ist links) und 255 (Punkt ist rechts)

     * @author Christopher Schrewing

     * @version 1.0

     */
    void changeX(int xValue);

    /**

     * Wert zwischen 0 (Punkt ist unten) und 255 (Punkt ist oben)

     * @author Christopher Schrewing

     * @version 1.0

     */
    void changeY(int yValue);

    void startSound();

    void stopSound();

    /**

     * Manche Geräte verdrehen den Stereo-Sound,
     * deswegen soll diese Methode schnell den Stereo-Sound invertieren.
     * Bei gerader Anzahl Aufrufen passiert dementsprechend nichts

     * @author Christopher Schrewing

     * @version 1.0

     */
    void invertX();

    void invertY();

    boolean xIsInverted();

    boolean yIsInverted();

    void setBaseFreq(int baseFreq);

    /**

     * Manche hören rechts lauter als links, deswegen soll hiermit nachgetrimmt werden können.
     * -1.0 bedeutet 100% Dämpfung der linken Spur. 1.0 100% der rechten Spur
     * Somit sind Werte ziwschen 1.0 und -1.0 erlaubt.

     * @author Christopher Schrewing

     * @version 1.0

     */
    void trimBalance(float balancePercentage);

    boolean toggleBullseye();

    void setBullseyeRadius(int progress);
}
