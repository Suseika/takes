/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.takes.it.fm;

import com.jcabi.http.request.JdkRequest;
import com.jcabi.http.response.RestResponse;
import com.jcabi.http.wire.VerboseWire;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.util.concurrent.TimeUnit;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.takes.TakesServer;

/**
 * Test case for {@link TakesServer}.
 * @author Yegor Bugayenko (yegor@teamed.io)
 * @version $Id$
 * @since 0.1
 */
@SuppressWarnings("PMD.DoNotUseThreads")
public final class AppTest {

    /**
     * App can work.
     * @throws Exception If some problem inside
     */
    @Test
    public void justWorks() throws Exception {
        final TakesServer server = new TakesServer(
            new App(new File("/tmp")), 1
        );
        final int port = AppTest.port();
        final Thread thread = new Thread(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        server.listen(port);
                    } catch (final IOException ex) {
                        throw new IllegalStateException(ex);
                    }
                }
            }
        );
        thread.start();
        TimeUnit.SECONDS.sleep(1L);
        MatcherAssert.assertThat(
            new JdkRequest(String.format("http://localhost:%d/about", port))
                .through(VerboseWire.class)
                .fetch()
                .as(RestResponse.class)
                .assertStatus(HttpURLConnection.HTTP_OK)
                .body(),
            Matchers.containsString("simple demo")
        );
        thread.interrupt();
    }

    /**
     * Reserve new TCP port.
     * @return Reserved port.
     * @throws IOException If fails
     */
    public static int port() throws IOException {
        final ServerSocket socket = new ServerSocket(0);
        try {
            socket.setReuseAddress(true);
            return socket.getLocalPort();
        } finally {
            socket.close();
        }
    }

}
