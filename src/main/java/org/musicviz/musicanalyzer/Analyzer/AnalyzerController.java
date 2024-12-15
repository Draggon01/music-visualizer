package org.musicviz.musicanalyzer.Analyzer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.jtransforms.fft.DoubleFFT_1D;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.sound.sampled.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

@Controller
public class AnalyzerController {
    HashMap<String, WebSocketSession> sessionMap = new HashMap<>();
    private final int bufferSize = 2048;
    private final int packagingSize = 8;
    private byte[] buffer = new byte[bufferSize];
    private AudioInputStream audioStream;

    public AnalyzerController() {
        AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
        // Set up the target data line
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        TargetDataLine targetLine = null;
        try {
            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(format);
            targetLine.start();
            autoRouteAudio("VirtualSink", "alsa_capture.java");
            audioStream = new AudioInputStream(targetLine);
        } catch (LineUnavailableException e) {
            throw new RuntimeException(e);
        }
    }

    @Async
    public void startStreaming(WebSocketSession session) throws IOException {
        if (sessionMap.containsKey(session.getId())) {
            return;
        }
        int x = 0;
        sessionMap.put(session.getId(), session);

        //init vars for analysis
        int bytesRead;
        int dividedBufferSize = bufferSize / 4;
        DoubleFFT_1D fft = new DoubleFFT_1D(dividedBufferSize);
        DoubleFFT_1D testFFT = new DoubleFFT_1D(bufferSize);
        double[] leftChannel = new double[dividedBufferSize];
        double[] rightChannel = new double[dividedBufferSize];
        double[] testRightChannel = new double[bufferSize];
        double[] packedLeftChannel = new double[150];
        double[] packedRightChannel = new double[150];
        ObjectMapper objectMapper = new ObjectMapper();
        AnalyzerData analyzerData = new AnalyzerData(44100, null, null);
        AnalyzerData lastAnalyzerData = null;

        System.out.println("fft for frequencies:");
        for (int i = 0; i < bufferSize / 8; i++) {
            System.out.println(i * ((44100 / (bufferSize / 4))));
        }


        while (!sessionMap.isEmpty()) {
            while ((bytesRead = audioStream.read(buffer)) != -1 && !sessionMap.isEmpty()) {
                int si = 0;
                for (int i = 0; i < bytesRead; i += 4, si++) { // 16-bit PCM samples (2 bytes per sample)
                    // Convert bytes to sample
                    int sample = (buffer[i + 1] << 8) | (buffer[i] & 0xFF) * 4;
                    // Clamp to prevent overflow
                    sample = Math.max(-32768, Math.min(32767, sample));
                    leftChannel[si] = sample / 32768.0;

                    sample = (buffer[i + 3] << 8) | (buffer[i + 2] & 0xFF) * 4;
                    // Clamp to prevent overflow
                    sample = Math.max(-32768, Math.min(32767, sample));
                    rightChannel[si] = sample / 32768.0;


                    testRightChannel[si] = testRightChannel[dividedBufferSize + si];
                    testRightChannel[dividedBufferSize + si] = testRightChannel[dividedBufferSize * 2 + si];
                    testRightChannel[dividedBufferSize * 2 + si] = testRightChannel[dividedBufferSize * 3 + si];
                    testRightChannel[dividedBufferSize * 3 + si] = rightChannel[si];
                }

                double[] nextTest = new double[bufferSize];
                nextTest = Arrays.copyOf(testRightChannel, bufferSize);

                fft.realForward(leftChannel);
                testFFT.realForward(nextTest);
                for (int i = 0; i < bufferSize / 2; i++) {
                    double testRightMagnitude = Math.sqrt(nextTest[i * 2] * nextTest[i * 2] + nextTest[i * 2 + 1] * nextTest[i * 2 + 1]);
                    System.out.printf("Freq %d Hz: Right %.2f%n", i * (44100 / (bufferSize)), testRightMagnitude);
                }
                //testFFT.realForward(nextTest);
                //testFFT.realForward(nextTest);
                //testFFT.realForward(nextTest);

                //fft.realForward(rightChannel);

                for (int i = 0; i < 150; i++) {
                    double leftMagnitude = Math.sqrt(leftChannel[i * 2] * leftChannel[i * 2] + leftChannel[i * 2 + 1] * leftChannel[i * 2 + 1]);
                    double rightMagnitude = Math.sqrt(rightChannel[i * 2] * rightChannel[i * 2] + rightChannel[i * 2 + 1] * rightChannel[i * 2 + 1]);
                    packedLeftChannel[i] = leftMagnitude;
                    packedRightChannel[i] = rightMagnitude;
                    //System.out.printf("Freq %d Hz: Left %.2f, Right %.2f%n", i * (44100 / (bufferSize / 4)), leftMagnitude, rightMagnitude);
                }
                analyzerData.setRightFFTData(packedRightChannel);
                analyzerData.setLeftFFTData(packedLeftChannel);
                analyzerData.optimize(lastAnalyzerData);
                String json = objectMapper.writeValueAsString(analyzerData);

                for (WebSocketSession value : sessionMap.values()) {
                    value.sendMessage(new TextMessage(json));
                }
                lastAnalyzerData = new AnalyzerData(44100, analyzerData.getLeftFFTData(), analyzerData.getRightFFTData());
            }
        }
        Thread.currentThread().interrupt();
    }

    //TODO: synchronize sesseion array with the startStreaming function.
    public void endStreaming(WebSocketSession session) {
        sessionMap.remove(session.getId());
    }

    /**
     * Automatically route system audio to Java capture using PipeWire.
     *
     * @param outputNode Name of the output node (e.g., system audio output)
     * @param inputNode  Name of the input node (e.g., Java capture sink monitor)
     */
    private static void autoRouteAudio(String outputNode, String inputNode) {
        try {
            // Construct the pw-link command
            String command = String.format("pw-link %s %s", outputNode, inputNode);

            // Execute the command
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Successfully routed audio from " + outputNode + " to " + inputNode);
            } else {
                System.err.println("Failed to route audio. Exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error executing pw-link command: " + e.getMessage());
        }
    }
}
