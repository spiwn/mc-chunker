package org.iz.cs.chunker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class InputHandler extends InputStream {

    private Object lock = new Object();

    private Queue<byte[]> textQueue;
    private InputStream defaultInput;
    private byte[] currentText;
    private int position;

    public InputHandler(InputStream defaultInput) {
        this.defaultInput = defaultInput;

        textQueue= new ConcurrentLinkedQueue<byte[]>();
        currentText = null;
        position = 0;

        Thread t =  new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    boolean hasNotified = false;
                    while (true) {
                        if (defaultInput.available() == 0 || hasNotified) {
                            synchronized (defaultInput) {
                                defaultInput.wait();
                            }
                            hasNotified = false;
                        }
                        synchronized (lock) {
                            if (defaultInput.available() > 0) {
                                lock.notify();
                                hasNotified = true;
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        t.setDaemon(true);
        t.start();
    }

    public void enqueue(String text, Charset charset) {
        if (text.isEmpty()) {
            return;
        }
        byte[] bytes;
        if (charset == null) {
            bytes = text.getBytes();
        } else {
            bytes = text.getBytes(charset);
        }
        textQueue.add(bytes);
        synchronized (lock) {
            lock.notify();
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        synchronized (lock) {
            if (currentText == null && textQueue.isEmpty() && defaultInput.available() == 0) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (currentText == null && !textQueue.isEmpty()) {
                currentText = textQueue.poll();
                position = 0;
            }

            if (currentText != null && position != currentText.length) {
                int remaining = currentText.length - position;
                int toWrite = Math.min(remaining, len);
                System.arraycopy(currentText, position, b, off, toWrite);

                if (remaining <= b.length) {
                    currentText = null;
                } else {
                    position += toWrite;
                }
                return toWrite;
            }

            return defaultInput.read(b);
        }
    }

    @Override
    public void close() throws IOException {
        defaultInput.close();
    }

    @Override
    public int read() throws IOException {
        synchronized (lock) {
            if (currentText == null && textQueue.isEmpty() && defaultInput.available() == 0) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            if (currentText != null && position != currentText.length) {
                return currentText[position++];
            }
            if (!textQueue.isEmpty()) {
                currentText = textQueue.poll();
                position = 0;
                return currentText[position++];
            }
            return defaultInput.read();
        }
    }

}
