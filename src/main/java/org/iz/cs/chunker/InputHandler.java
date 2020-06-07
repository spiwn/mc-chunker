package org.iz.cs.chunker;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class InputHandler extends InputStream {

    private Queue<byte[]> textQueue;
    private InputStream defaultInput;
    private byte[] currentText;
    private int position;
    private Lock lock;
    private Condition inputAvailable;
    private Condition inputHasBeenRead;


    public InputHandler(InputStream defaultInput) {
        this.defaultInput = defaultInput;
        lock = new ReentrantLock(false);
        inputAvailable = lock.newCondition();
        inputHasBeenRead = lock.newCondition();


        textQueue= new ConcurrentLinkedQueue<byte[]>();
        currentText = null;
        position = 0;

        if (!defaultInput.markSupported()) {
            this.defaultInput = new BufferedInputStream(defaultInput);
        }

        Thread t =  new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    while (true) {
                        if (defaultInput.available() == 0) {
                            defaultInput.mark(1);
                            defaultInput.read();
                            defaultInput.reset();
                        }
                        if (defaultInput.available() > 0) {
                            lock.lockInterruptibly();
                            inputAvailable.signal();
                            inputHasBeenRead.await();
                            lock.unlock();
                        }
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
        t.setName("InputListener");
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
        inputAvailable.signal();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        boolean locked = false;
        try {
            lock.lockInterruptibly();
            locked = true;
            if (currentText == null && textQueue.isEmpty() && defaultInput.available() == 0) {
                inputAvailable.await();
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
        } catch (InterruptedException e) {
            throw new IOException("Thread was interupted");
        } finally {
            if (locked) {
                inputHasBeenRead.signal();
                lock.unlock();
            }
        }
    }

    @Override
    public void close() throws IOException {
        defaultInput.close();
    }

    @Override
    public int read() throws IOException {
        boolean locked = false;
        try {
            lock.lockInterruptibly();
            locked = true;
            if (currentText == null && textQueue.isEmpty() && defaultInput.available() == 0) {
                inputAvailable.await();
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
        } catch (InterruptedException e) {
            throw new IOException("Thread was interupted");
        } finally {
            if (locked) {
                inputHasBeenRead.signal();
                lock.unlock();
            }
        }
    }

}
