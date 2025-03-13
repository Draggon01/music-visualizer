package org.musicviz.musicanalyzer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sound.sampled.*;

@SpringBootTest
class MusicanalyzerApplicationTests {

    @Test
    void checkSupportedSampleRates() throws LineUnavailableException {
//        AudioFormat format = new AudioFormat(48000, 16, 2, true, false);
//        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
//
//        Line line = AudioSystem.getLine(info);
//        if(line != null) {
//            System.out.println("toll");
//        }
//
//
//        Mixer.Info[] mixerInfo = AudioSystem.getMixerInfo();
//
//        if(AudioSystem.isLineSupported(info)) {
//            System.out.println("Supported");
//        } else {
//            System.out.println("Not supported");
//        }

    }

}
