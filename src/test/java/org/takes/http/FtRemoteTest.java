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
package org.takes.http;

import com.jcabi.http.Request;
import com.jcabi.http.request.JdkRequest;
import com.jcabi.http.response.RestResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.takes.Response;
import org.takes.Take;
import org.takes.rq.RqForm;
import org.takes.rs.RsText;

/**
 * Test case for {@link FtRemote}.
 * @author Yegor Bugayenko (yegor@teamed.io)
 * @version $Id$
 * @since 0.21
 * @checkstyle ClassDataAbstractionCouplingCheck (500 lines)
 */
public final class FtRemoteTest {

    /**
     * FtRemote can work in parallel threads.
     * @throws Exception If some problem inside
     */
    @Test
    public void worksInParallelThreads() throws Exception {
        final Take take = new Take() {
            @Override
            public Response act(final org.takes.Request req)
                throws IOException {
                MatcherAssert.assertThat(
                    new RqForm.Base(req).param("alpha"),
                    Matchers.hasItem("123")
                );
                return new RsText("works fine");
            }
        };
        final Callable<Long> task = new Callable<Long>() {
            @Override
            public Long call() throws Exception {
                new FtRemote(take).exec(
                    new FtRemote.Script() {
                        @Override
                        public void exec(final URI home) throws IOException {
                            new JdkRequest(home)
                                .method(Request.POST)
                                .body().set("alpha=123").back()
                                .fetch()
                                .as(RestResponse.class)
                                .assertStatus(HttpURLConnection.HTTP_OK)
                                .assertBody(Matchers.startsWith("works"));
                        }
                    }
                );
                return 0L;
            }
        };
        final int total = Runtime.getRuntime().availableProcessors() << 2;
        final Collection<Callable<Long>> tasks =
            new ArrayList<Callable<Long>>(total);
        for (int idx = 0; idx < total; ++idx) {
            tasks.add(task);
        }
        final Collection<Future<Long>> futures =
            Executors.newFixedThreadPool(total).invokeAll(tasks);
        for (final Future<Long> future : futures) {
            MatcherAssert.assertThat(future.get(), Matchers.equalTo(0L));
        }
    }

}
