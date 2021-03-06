/* 
 * Copyright (c) 2000 by Matt Welsh and The Regents of the University of 
 * California. All rights reserved.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose, without fee, and without written agreement is
 * hereby granted, provided that the above copyright notice and the following
 * two paragraphs appear in all copies of this software.
 * 
 * IN NO EVENT SHALL THE UNIVERSITY OF CALIFORNIA BE LIABLE TO ANY PARTY FOR
 * DIRECT, INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES ARISING OUT
 * OF THE USE OF THIS SOFTWARE AND ITS DOCUMENTATION, EVEN IF THE UNIVERSITY OF
 * CALIFORNIA HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * THE UNIVERSITY OF CALIFORNIA SPECIFICALLY DISCLAIMS ANY WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 * AND FITNESS FOR A PARTICULAR PURPOSE.  THE SOFTWARE PROVIDED HEREUNDER IS
 * ON AN "AS IS" BASIS, AND THE UNIVERSITY OF CALIFORNIA HAS NO OBLIGATION TO
 * PROVIDE MAINTENANCE, SUPPORT, UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 *
 * Author: Matt Welsh <mdw@cs.berkeley.edu>
 * 
 */

package seda.sandstorm.lib.socket;

import seda.sandstorm.api.EventElement;
import seda.sandstorm.api.EventHandler;
import seda.sandstorm.api.EventSource;
import seda.sandstorm.api.Manager;
import seda.sandstorm.api.internal.StageWrapper;
import seda.sandstorm.api.internal.ThreadManager;
import seda.sandstorm.internal.ThreadPool;

/**
 * aSocketThreadManager provides a thread manager for the aSocket layer: one
 * thread for each of the read, write, and listen stages.
 * 
 * @author Matt Welsh
 */
class SocketThreadManager implements ThreadManager, aSocketConst {

    private static final boolean DEBUG = false;

    private Manager mgr;

    SocketThreadManager(Manager mgr) {
        this.mgr = mgr;
    }

    protected aSocketThread makeThread(SocketStageWrapper wrapper) {
        return new aSocketThread(wrapper);
    }

    /**
     * Register a stage with this thread manager.
     */
    public void register(StageWrapper thestage) {
        SocketStageWrapper stage = (SocketStageWrapper) thestage;
        aSocketThread at = makeThread(stage);
        ThreadPool tp = new ThreadPool(stage, mgr, at, 1);
        at.registerTP(tp);
        tp.start();
    }

    /**
     * Deregister a stage with this thread manager.
     */
    public void deregister(StageWrapper stage) {
        throw new IllegalArgumentException(
                "aSocketThreadManager: deregister not supported");
    }

    /**
     * Deregister all stages from this thread manager.
     */
    public void deregisterAll() {
        throw new IllegalArgumentException(
                "aSocketThreadManager: deregisterAll not supported");
    }

    /**
     * Internal class representing a single aSocketTM-managed thread.
     */
    protected class aSocketThread implements Runnable {

        protected ThreadPool tp;
        protected StageWrapper wrapper;
        protected SelectSourceIF selsource;
        protected EventSource eventQ;
        protected String name;
        protected EventHandler handler;

        protected aSocketThread(SocketStageWrapper wrapper) {
            if (DEBUG)
                System.err.println("!!!!!aSocketThread init");
            this.wrapper = wrapper;
            this.name = "aSocketThread <" + wrapper.getStage().getName() + ">";
            this.selsource = wrapper.getSelectSource();
            this.eventQ = wrapper.getEventQueue();
            this.handler = wrapper.getEventHandler();
        }

        void registerTP(ThreadPool tp) {
            this.tp = tp;
        }

        public void run() {
            int aggTarget;
            if (DEBUG)
                System.err.println(name + ": starting, selsource=" + selsource
                        + ", eventQ=" + eventQ + ", handler=" + handler);

            while (true) {

                if (DEBUG)
                    System.err.println(name + ": Looping in run()");
                try {

                    aggTarget = tp.getAggregationTarget();

                    while (selsource != null && selsource.numActive() == 0) {
                        if (DEBUG)
                            System.err.println(name
                                    + ": numActive is zero, waiting on event queue");
                        EventElement qelarr[];
                        if (aggTarget == -1) {
                            qelarr = eventQ.blockingDequeueAll(EVENT_QUEUE_TIMEOUT);
                        } else {
                            qelarr = eventQ.blockingDequeue(
                                    EVENT_QUEUE_TIMEOUT, aggTarget);
                        }

                        if (qelarr != null) {
                            if (DEBUG)
                                System.err.println(name + ": got "
                                        + qelarr.length + " new requests");
                            handler.handleEvents(qelarr);
                        }
                    }

                    for (int s = 0; s < SELECT_SPIN; s++) {
                        if (DEBUG)
                            System.err
                                    .println(name + ": doing select, numActive "
                                            + selsource.numActive());
                        SelectQueueElement ret[];

                        if (aggTarget == -1) {
                            ret = (SelectQueueElement[]) selsource
                                    .blockingDequeueAll(SELECT_TIMEOUT);
                        } else {
                            ret = (SelectQueueElement[]) selsource
                                    .blockingDequeue(SELECT_TIMEOUT,
                                            aggTarget);
                        }

                        if (ret != null) {
                            if (DEBUG)
                                System.err.println(name + ": select got "
                                        + ret.length + " elements");
                            long tstart = System.currentTimeMillis();
                            handler.handleEvents(ret);
                            long tend = System.currentTimeMillis();
                            wrapper.getStats().recordServiceRate(ret.length,
                                    tend - tstart);

                        } else if (DEBUG)
                            System.err.println(name + ": select got null");
                    }

                    if (DEBUG)
                        System.err.println(name + ": Checking request queue");
                    for (int s = 0; s < EVENT_QUEUE_SPIN; s++) {
                        EventElement qelarr[];
                        if (aggTarget == -1) {
                            qelarr = eventQ.dequeueAll();
                        } else {
                            qelarr = eventQ.dequeue(aggTarget);
                        }
                        if (qelarr != null) {
                            if (DEBUG)
                                System.err.println(name + ": got "
                                        + qelarr.length + " new requests");
                            handler.handleEvents(qelarr);
                            break;
                        }
                    }

                    Thread.yield();
                } catch (Exception e) {
                    System.err.println(name + ": got exception " + e);
                    e.printStackTrace();
                }
            }
        }
    }

}
