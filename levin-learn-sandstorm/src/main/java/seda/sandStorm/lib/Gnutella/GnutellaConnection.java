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

package seda.sandstorm.lib.gnutella;

import seda.sandstorm.api.*;
import seda.sandstorm.core.*;
import seda.sandstorm.lib.socket.*;

import java.util.*;
import java.io.*;
import java.net.*;

/**
 * A GnutellaConnection represents a virtual connection to the
 * Gnutella network. It may implement a single point-to-point connection
 * between two peers, or a one-to-many connection to multiple peers.
 *
 * @author Matt Welsh (mdw@cs.berkeley.edu)
 */
public class GnutellaConnection extends SimpleSink implements EventElement, GnutellaConst {

  private static final boolean DEBUG = false;

  private GnutellaServer gs;
  private GnutellaPacketReader gpr;
  private ATcpConnection conn;
  private InetAddress addr;
  private int port;
  private boolean closed;

  GnutellaConnection(GnutellaServer gs, ATcpConnection conn) {
    this.gs = gs;
    this.conn = conn;
    this.gpr = new GnutellaPacketReader();
    addr = conn.getAddress();
    port = conn.getPort();
    closed = false;
  }

  /**
   * Return the InetAddress of the peer. Returns null if this is a 
   * 'virtual' connection.
   */
  public InetAddress getAddress() {
    return addr;
  }

  /**
   * Return the port of the peer. Returns -1 if this is a 'virtual'
   * connection.
   */
  public int getPort() {
    return port;
  }

  /**
   * Send a ping to this connection.
   */
  public void sendPing() {
    GnutellaPingPacket ping = new GnutellaPingPacket();
    enqueueLossy(ping);
  }

  /**
   * Send a ping to this connection with the given TTL.
   */
  public void sendPing(int ttl) {
    GnutellaPingPacket ping = new GnutellaPingPacket(new GnutellaGUID(), ttl, 0);
    enqueueLossy(ping);
  }

  // Package access only
  ATcpConnection getConnection() {
    return conn;
  }

  // Package access only
  GnutellaPacketReader getReader() {
    return gpr;
  }

  /* SinkIF methods ******************************************************/

  public void enqueue(EventElement element) throws SinkException {
    GnutellaPacket packet = (GnutellaPacket)element;
    BufferEvent buf = packet.getBuffer();
    buf.compQ = gs.getSink();
    conn.enqueue(buf);
  }

  public boolean enqueueLossy(EventElement element) {
    GnutellaPacket packet = (GnutellaPacket)element;
    BufferEvent buf = packet.getBuffer();
    buf.compQ = gs.getSink();
    return conn.enqueueLossy(buf);
  }

  public void enqueueMany(EventElement elements[]) throws SinkException {
    for (int i = 0; i < elements.length; i++) {
      enqueue(elements[i]);
    }
  }

  public int size() {
    return conn.size();
  }

  public void close(EventSink compQ) throws SinkClosedException {
    // XXX For now, allow a connection to be closed multiple times
    // This is because clogged connections may push multiple
    // clogged events up, causing the server to attempt to close
    // multiple times
    //if (closed) throw new SinkClosedException("GnutellaConnection already closed!");
    gs.cleanupConnection(conn, this);
    gs.closeConnection(conn, compQ);
    closed = true;
  }

  public void flush(EventSink compQ) throws SinkClosedException {
    conn.flush(compQ);
  }

  public Object enqueuePrepare(EventElement enqueueMe[]) throws SinkException {
    return conn.enqueuePrepare(enqueueMe);
  }

  public void enqueueCommit(Object key) {
    conn.enqueueCommit(key);
  }

  public void enqueueAbort(Object key) {
    conn.enqueueAbort(key);
  }

  public String toString() {
    return "GnutellaConnection ["+addr.getHostAddress()+":"+port+"]";
  }


}
