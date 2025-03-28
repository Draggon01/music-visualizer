package org.musicviz.musicanalyzer.Analyzer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.psambit9791.jdsp.filter.Butterworth;
import org.jtransforms.fft.DoubleFFT_1D;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Controller;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.sound.sampled.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

@Controller
public class AnalyzerController {
    HashMap<String, WebSocketSession> sessionMap = new HashMap<>();
    private final int bufferSize = 1024; //TODO: prob change to 1024 for better output
    private final int packagingSize = 8;
    private byte[] buffer = new byte[bufferSize];
    private AudioInputStream audioStream;
    private boolean debug = false;
    private final int sampleRate = 48000;

    public AnalyzerController() {
        AudioFormat format = new AudioFormat(sampleRate, 16, 2, true, false);
        // Set up the target data line
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        TargetDataLine targetLine = null;
        try {
            targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open(format);
            targetLine.start();
            disconnectAllInputsFromAudio("alsa_capture.java");
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
        DoubleFFT_1D fft = new DoubleFFT_1D(dividedBufferSize * 16);
        double[] leftChannel = new double[dividedBufferSize * 16];
        double[] rightChannel = new double[dividedBufferSize];
        double[] testRightChannel = new double[bufferSize];
        double[] packedLeftChannel = new double[150];
        double[] packedRightChannel = new double[150];
        ObjectMapper objectMapper = new ObjectMapper();
        AnalyzerData analyzerData = new AnalyzerData(sampleRate, null, null);
        AnalyzerData lastAnalyzerData = null;

        Butterworth flt = new Butterworth(sampleRate);
        System.out.println("fft for frequencies:");
        for (int i = 0; i < dividedBufferSize * 8; i++) {
            //System.out.println(i * ((sampleRate / (bufferSize / 4))));
            System.out.println(i * ((sampleRate / (dividedBufferSize * 8))));
        }


        while (!sessionMap.isEmpty()) {
            while ((bytesRead = audioStream.read(buffer)) != -1 && !sessionMap.isEmpty()) {
                for (int i = dividedBufferSize; i < dividedBufferSize * 16; i++) {
                    leftChannel[i] = 0;
                }
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
                }

                leftChannel = flt.lowPassFilter(leftChannel, 1, 20000);
                fft.realForward(leftChannel);

                for (int i = 0; i < 127; i++) {
                    double leftMagnitude = Math.sqrt(leftChannel[i * 2] * leftChannel[i * 2] + leftChannel[i * 2 + 1] * leftChannel[i * 2 + 1]);
                    double rightMagnitude = Math.sqrt(rightChannel[i * 2] * rightChannel[i * 2] + rightChannel[i * 2 + 1] * rightChannel[i * 2 + 1]);
                    packedLeftChannel[i] = leftMagnitude;
                    packedRightChannel[i] = rightMagnitude;
                    if (debug) {
                        System.out.printf("Freq %d Hz: Left %.2f, Right %.2f%n", i * (sampleRate / (bufferSize / 4)), leftMagnitude, rightMagnitude);
                    }
                }

                analyzerData.setRightFFTData(packedRightChannel);
                analyzerData.setLeftFFTData(packedLeftChannel);
                analyzerData.optimize(lastAnalyzerData);
                String json = objectMapper.writeValueAsString(analyzerData);

                for (WebSocketSession value : sessionMap.values()) {
                    value.sendMessage(new TextMessage(json));
                }
                lastAnalyzerData = new AnalyzerData(sampleRate, analyzerData.getLeftFFTData(), analyzerData.getRightFFTData());
            }
        }
        Thread.currentThread().interrupt();
    }

    //TODO: synchronize sesseion array with the startStreaming function.
    public void endStreaming(WebSocketSession session) {
        sessionMap.remove(session.getId());
    }

    /**
     * Automatically remove all inputs from a node using pipewire commands.
     *
     * @param node Name of the node to remove inputs (e.g., VirtualSink)
     */
    private static void disconnectAllInputsFromAudio(String node) {
        try {
            String getIdsCommand = "pw-link -l -I | grep java";
            String tmp;
            int rem;
            Process p = Runtime.getRuntime().exec(getIdsCommand);
            p.waitFor();
            BufferedReader bufferedReader = p.inputReader(StandardCharsets.UTF_8);
            while (bufferedReader.ready()) {
                tmp = bufferedReader.readLine();
                if (tmp.contains("|") && tmp.contains("alsa_capture.java")) {
                    rem = findFirstNumberInString(tmp);

                    // Construct the pw-link command
                    String command = String.format("pw-link -d %d", rem);

                    // Execute the command
                    Process process = Runtime.getRuntime().exec(command);
                    int exitCode = process.waitFor();

                    if (exitCode == 0) {
                        System.out.println("Successfully removed all input channels from node: " + node);
                    } else {
                        System.err.println("Failed to remove audio inputs. Exit code: " + exitCode);
                    }
                }
            }
            bufferedReader.close();
        } catch (IOException | InterruptedException e) {
            System.err.println("Error executing pw-link command: " + e.getMessage());
        }
    }

    /**
     * Automatically route system audio to Java capture using PipeWire.
     *
     * @param outputNode Name of the output node (e.g., system audio output)
     * @param inputNode  Name of the input node (e.g., Java capture sink monitor)
     */
    private static void autoRouteAudio(String outputNode, String inputNode) {
        try {
            String tmp;

            //get ids of output and input nodes
            int outputNodeFl = 0;
            int outputNodeFr = 0;
            Process p = Runtime.getRuntime().exec("pw-link -o -I " + outputNode);
            p.waitFor();
            BufferedReader bufferedReader = p.inputReader(StandardCharsets.UTF_8);
            while (bufferedReader.ready()) {
                tmp = bufferedReader.readLine();
                if (tmp.toLowerCase(Locale.ROOT).contains("fl")) {
                    outputNodeFl = findFirstNumberInString(tmp);
                }
                if (tmp.toLowerCase(Locale.ROOT).contains("fr")) {
                    outputNodeFr = findFirstNumberInString(tmp);
                }
            }

            bufferedReader.close();


            int inputNodeFl = 0;
            int inputNodeFr = 0;
            p = Runtime.getRuntime().exec("pw-link -i -I");
            p.waitFor();
            bufferedReader = p.inputReader(StandardCharsets.UTF_8);
            while (bufferedReader.ready()) {
                tmp = bufferedReader.readLine();
                if (tmp.toLowerCase(Locale.ROOT).contains("fl") && tmp.toLowerCase(Locale.ROOT).contains(inputNode)) {
                    inputNodeFl = findFirstNumberInString(tmp);
                }
                if (tmp.toLowerCase(Locale.ROOT).contains("fr") && tmp.toLowerCase(Locale.ROOT).contains(inputNode)) {
                    inputNodeFr = findFirstNumberInString(tmp);
                }
            }

            bufferedReader.close();

            // Construct the pw-link command for fl
            String command = String.format("pw-link %d %d", outputNodeFl, inputNodeFl);
            // Execute the command
            Process process = Runtime.getRuntime().exec(command);
            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Successfully routed audio from " + outputNode + " to " + inputNode);
            } else {
                System.err.println("Failed to route audio. Exit code: " + exitCode);
            }

            // Construct the pw-link command for fr
            command = String.format("pw-link %d %d", outputNodeFr, inputNodeFr);
            // Execute the command
            process = Runtime.getRuntime().exec(command);
            exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("Successfully routed audio from " + outputNode + " to " + inputNode);
            } else {
                System.err.println("Failed to route audio. Exit code: " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error executing pw-link command: " + e.getMessage());
        }
    }

    private static int findFirstNumberInString(String string) {
        int i;
        int startInd = -1;
        int endInd = -1;
        for (i = 0; i < string.length(); i++) {
            int val = string.charAt(i);
            if (startInd == -1 && val >= '0' && val <= '9') {
                startInd = i;
            }
            if (startInd != -1 && (val < '0' || val > '9')) {
                endInd = i;
                break;
            }
        }
        return Integer.parseInt(string.substring(startInd, endInd));
    }
}
