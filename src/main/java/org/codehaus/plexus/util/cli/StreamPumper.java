package org.codehaus.plexus.util.cli;

/*
 * Copyright The Codehaus Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/********************************************************************************
 * CruiseControl, a Continuous Integration Toolkit
 * Copyright (c) 2001-2003, ThoughtWorks, Inc.
 * 651 W Washington Ave. Suite 500
 * Chicago, IL 60661 USA
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     + Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *
 *     + Redistributions in binary form must reproduce the above
 *       copyright notice, this list of conditions and the following
 *       disclaimer in the documentation and/or other materials provided
 *       with the distribution.
 *
 *     + Neither the name of ThoughtWorks, Inc., CruiseControl, nor the
 *       names of its contributors may be used to endorse or promote
 *       products derived from this software without specific prior
 *       written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 ********************************************************************************/

/* ====================================================================
 * Copyright 2003-2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ====================================================================
 */

import org.codehaus.plexus.util.IOUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * Class to pump the error stream during Process's runtime. Copied from the Ant
 * built-in task.
 *
 * @author <a href="mailto:fvancea@maxiq.com">Florin Vancea </a>
 * @author <a href="mailto:pj@thoughtworks.com">Paul Julius </a>
 * @version $Id$
 * @since June 11, 2001
 */
public class StreamPumper
    extends Thread
{
    private final BufferedReader in;

    private final StreamConsumer consumer;

    private final PrintWriter out;

    private volatile Exception exception = null;

    private static final int SIZE = 1024;

    boolean done;

    volatile boolean disabled;

    public StreamPumper( InputStream in )
    {
        this( in, (StreamConsumer) null );
    }

    public StreamPumper( InputStream in, StreamConsumer consumer )
    {
        this( in, null, consumer );
    }

    public StreamPumper( InputStream in, PrintWriter writer )
    {
        this( in, writer, null );
    }

    public StreamPumper( InputStream in, PrintWriter writer, StreamConsumer consumer )
    {
        this.in = new BufferedReader( new InputStreamReader( in ), SIZE );
        this.out = writer;
        this.consumer = consumer;
    }

    public void run()
    {
        try
        {
            for ( String line = in.readLine(); line != null; line = in.readLine() )
            {
                try
                {
                    if ( exception == null )
                    {
                        consumeLine( line );
                    }
                }
                catch ( Exception t )
                {
                    exception = t;
                }

                if ( out != null )
                {
                    out.println( line );

                    out.flush();
                }
            }
        }
        catch ( IOException e )
        {
            exception = e;
        }
        finally
        {
            IOUtil.close( in );

            synchronized ( this )
            {
                done = true;

                this.notifyAll();
            }
        }
    }

    public void flush()
    {
        if ( out != null )
        {
            out.flush();
        }
    }

    public void close()
    {
        IOUtil.close( out );
    }

    public boolean isDone()
    {
        return done;
    }

    public Exception getException()
    {
        return exception;
    }

    private void consumeLine( String line )
    {
        if ( consumer != null && !disabled )
        {
            consumer.consumeLine( line );
        }
    }

    public synchronized void waitUntilDone()
        throws InterruptedException
    {
        while ( !isDone() )
        {
            wait();
        }
    }

    public void disable()
    {
        disabled = true;
    }
}
