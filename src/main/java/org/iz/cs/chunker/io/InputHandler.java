package org.iz.cs.chunker.io;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import org.iz.cs.chunker.Chunker;

public class InputHandler extends InputStream {

    private Queue<byte[]> textQueue;
    private InputStream defaultInput;
    private byte[] currentText;
    private int position;
    private Lock lock;
    private Condition inputAvailable;
    private Condition inputHasBeenRead;
    private volatile boolean hasAvailable = false;

    public InputHandler(InputStream defaultInput) {
        this.defaultInput = defaultInput;
        lock = new ReentrantLock(false);
        inputAvailable = lock.newCondition();
        inputHasBeenRead = lock.newCondition();

        textQueue= new ConcurrentLinkedQueue<byte[]>();
        currentText = null;
        position = 0;

        if (!this.defaultInput.markSupported()) {
            this.defaultInput = new BufferedInputStream(this.defaultInput);
        }
        Thread t =  new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    int read = 0;
                    while (true) {
                        if (defaultInput.available() == 0) {
                            hasAvailable = false;
                            defaultInput.mark(1);
                            read = defaultInput.read();
                            defaultInput.reset();
                        }
                        if (defaultInput.available() > 0) {
                            if (read == 's') {
                                byte[] buffer = new byte[32];
                                defaultInput.mark(32);
                                read = defaultInput.read(buffer);
                                if (read > 4) {
                                    if (LazyLoader.stopRe.matcher(new String(buffer, 0, read)).matches()) {
                                        Chunker.server.scheduleShutdown();
                                        return;
                                    }
                                }
                                defaultInput.reset();
                            }


                            hasAvailable = true;
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

    public void enqueue(String text, Charset charset, boolean wait) {
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

        boolean locked = false;
        try {
            lock.lockInterruptibly();
            locked = true;
            inputAvailable.signal();
            if (wait) {
                inputHasBeenRead.await();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (locked) {
                lock.unlock();
            }
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        boolean locked = false;
        try {
            lock.lockInterruptibly();
            locked = true;
            while (true) {
                if (currentText == null && textQueue.isEmpty()
                        && (!hasAvailable || defaultInput.available() == 0)) {
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
                if (hasAvailable && defaultInput.available() > 0) {
                    return defaultInput.read(b, off, len);
                }
            }
        } catch (InterruptedException e) {
            throw new IOException("Thread was interupted");
        } finally {
            if (locked) {
                inputHasBeenRead.signalAll();
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
            while (true) {
                if (currentText == null
                        && textQueue.isEmpty()
                        && (!hasAvailable || defaultInput.available() == 0)) {
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
                if (hasAvailable && defaultInput.available() > 0) {
                    return defaultInput.read();
                }
            }
        } catch (InterruptedException e) {
            throw new IOException("Thread was interupted");
        } finally {
            if (locked) {
                inputHasBeenRead.signalAll();
                lock.unlock();
            }
        }
    }

    private static class LazyLoader {
        static Pattern stopRe = Pattern.compile("stop(?:\r|\n|\r\n)");
    }

}
