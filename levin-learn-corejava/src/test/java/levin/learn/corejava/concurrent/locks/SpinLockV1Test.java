package levin.learn.corejava.concurrent.locks;

import junit.framework.Assert;

import org.junit.Test;

public class SpinLockV1Test {
    @Test(expected = IllegalStateException.class)
    public void testUnlockDirectly() {
        final SpinLockV1 lock = new SpinLockV1();
        lock.unlock();
    }
    
    @Test
    public void testUnlockInAnotherThread() throws InterruptedException {
        final SpinLockV1 lock = new SpinLockV1();
        new Thread(new Runnable() {
            public void run() {
                lock.lock();
            }
        }).start();
        Thread t2 = new Thread(new Runnable() {
            public void run() {
                // Add validation logic
                lock.unlock();
            }
        });
        
        t2.start();
        t2.join();
    }
    
    @Test
    public void testLockCorrectly() throws InterruptedException {
        final int COUNT = 100;
        Thread[] threads = new Thread[COUNT];
        SpinLockV1 lock = new SpinLockV1();
        AddRunner runner = new AddRunner(lock);
        for (int i = 0; i < COUNT; i++) { 
            threads[i] = new Thread(runner, "thread-" + i);
            threads[i].start();
        }
        
        for (int i = 0; i < COUNT; i++) {
            threads[i].join();
        }
        
        Assert.assertEquals(COUNT, runner.getState());
    }
    
    private static class AddRunner implements Runnable {
        private final SpinLockV1 lock;
        
        private int state = 0;
        
        public AddRunner(SpinLockV1 lock) {
            this.lock = lock;
        }
        
        public void run() {
            lock.lock();
            try {
                quietSleep(10);
                state++;
                System.out.println(Thread.currentThread().getName() + ": " + state);
            } finally {
                lock.unlock();
            }
        }
        
        public int getState() {
            return state;
        }
    }
    
    private static void quietSleep(long millis) {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
