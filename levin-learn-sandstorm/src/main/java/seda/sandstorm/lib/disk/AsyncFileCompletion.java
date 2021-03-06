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

package seda.sandstorm.lib.disk;

import seda.sandstorm.api.EventElement;

/**
 * Abstract base class of AFile I/O completion events.
 *
 * @author Matt Welsh
 * @see AFileIOCompletedEvent
 * @see AsyncFileEOFReached
 * @see AsyncFileIOExceptionOccurred
 */
public abstract class AsyncFileCompletion implements EventElement {
    protected AsyncFileRequest req;

    protected AsyncFileCompletion(AsyncFileRequest req) {
        this.req = req;
    }

    /**
     * Return the I/O request object associated with this completion.
     */
    public AsyncFileRequest getRequest() {
        return req;
    }

    /**
     * Return the AFile object associated with this completion.
     */
    public AsyncFile getFile() {
        return req.getAsyncFile();
    }

}
