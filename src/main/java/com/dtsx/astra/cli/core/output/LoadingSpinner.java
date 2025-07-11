package com.dtsx.astra.cli.core.output;

import com.dtsx.astra.cli.core.output.output.OutputType;
import lombok.val;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;

public class LoadingSpinner {
    private static final String[] SPINNER_FRAMES = PlatformChars.SPINNER_FRAMES;
    private static final int FRAME_DELAY_MS = 80;

    private final ArrayDeque<String> messageStack = new ArrayDeque<>();
    private final AtomicBoolean isRunning = new AtomicBoolean(false);
    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicInteger lastLineLength = new AtomicInteger(0);
    private Thread spinnerThread;
    private volatile CountDownLatch pauseLatch;
    
    public LoadingSpinner(String initialMessage) {
        messageStack.push(initialMessage);
    }
    
    public void start() {
        if (isRunning.compareAndSet(false, true)) {
            if (AstraConsole.isTty() && OutputType.isHuman()) {
                spinnerThread = new Thread(this::runSpinner);
                spinnerThread.start();
            }
        }
    }
    
    public void stop() {
        isRunning.set(false);
        if (spinnerThread != null) {
            try {
                spinnerThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            clearLine();
        }
    }
    
    public void updateMessage(String newMessage) {
        synchronized (messageStack) {
            if (!messageStack.isEmpty()) {
                messageStack.pop();
                messageStack.push(newMessage);
            }
        }
    }
    
    public void pushMessage(String message) {
        synchronized (messageStack) {
            messageStack.push(message);
        }
    }
    
    public void popMessage() {
        synchronized (messageStack) {
            if (!messageStack.isEmpty()) {
                messageStack.pop();
            }
        }
    }
    
    private String getCurrentMessage() {
        synchronized (messageStack) {
            return messageStack.isEmpty() ? "" : messageStack.peekLast();
        }
    }
    
    public void pause() {
        if (AstraConsole.isTty() && OutputType.isHuman() && spinnerThread != null) {
            pauseLatch = new CountDownLatch(1);
            isPaused.set(true);
            
            try {
                pauseLatch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public void resume() {
        if (AstraConsole.isTty() && OutputType.isHuman() && spinnerThread != null) {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            isPaused.set(false);
        }
    }
    
    private void runSpinner() {
        int frameIndex = 0;
        
        while (isRunning.get()) {
            if (isPaused.get()) {
                clearLine();
                CountDownLatch latch = pauseLatch;
                if (latch != null) {
                    latch.countDown();
                }
                
                while (isPaused.get() && isRunning.get()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                continue;
            }
            
            if (AstraLogger.getLevel() != AstraLogger.Level.QUIET && AstraConsole.isTty()) {
                val currentLine = AstraColors.BLUE_300.use(SPINNER_FRAMES[frameIndex] + " ") + getCurrentMessage() + "...";
                val clearLine = "\r" + " ".repeat(lastLineLength.get()) + "\r";
                AstraConsole.error(clearLine + currentLine);
                lastLineLength.set(AstraColors.stripAnsi(currentLine).length());
            }
            
            frameIndex = (frameIndex + 1) % SPINNER_FRAMES.length;
            
            try {
                Thread.sleep(FRAME_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void clearLine() {
        if (AstraLogger.getLevel() != AstraLogger.Level.QUIET && AstraConsole.isTty()) {
            val clearSpaces = Math.max(50, lastLineLength.get());
            AstraConsole.error("\r" + " ".repeat(clearSpaces) + "\r");
        }
    }
}
