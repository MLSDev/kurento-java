/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */
package org.kurento.client.test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.kurento.client.HttpGetEndpoint;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.TFuture;
import org.kurento.client.Transaction;
import org.kurento.client.TransactionExecutionException;
import org.kurento.client.TransactionNotCommitedException;
import org.kurento.client.TransactionRollbackException;
import org.kurento.client.ZBarFilter;
import org.kurento.client.internal.server.KurentoServerException;
import org.kurento.client.test.util.AsyncResultManager;
import org.kurento.test.base.KurentoClientTest;

public class TransactionTest extends KurentoClientTest {

	@Test
	public void transactionTest() {

		// Pipeline creation (no transaction)
		MediaPipeline pipeline = kurentoClient.createMediaPipeline();

		PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				"http://files.kurento.org/video/small.webm").useEncodedMedia()
				.build();

		HttpGetEndpoint httpGetEndpoint = new HttpGetEndpoint.Builder(pipeline)
				.build();

		player.connect(httpGetEndpoint);

		String url = httpGetEndpoint.getUrl();
		// End pipeline creation

		// Explicit transaction
		Transaction tx = pipeline.beginTransaction();
		player.play(tx);
		TFuture<String> fUrl = httpGetEndpoint.getUrl(tx);
		pipeline.release(tx);
		tx.commit();
		// End explicit transaction

		assertThat(url, is(fUrl.get()));
	}

	@Test
	public void multipleTransactionTest() {

		// Pipeline creation (transaction)
		Transaction tx1 = kurentoClient.beginTransaction();
		MediaPipeline pipeline = kurentoClient.createMediaPipeline(tx1);

		HttpGetEndpoint httpGetEndpoint = new HttpGetEndpoint.Builder(pipeline)
				.build(tx1);
		TFuture<String> url1 = httpGetEndpoint.getUrl(tx1);
		tx1.commit();
		// End pipeline creation

		// Pipeline creation (transaction)
		Transaction tx2 = kurentoClient.beginTransaction();
		MediaPipeline pipeline2 = kurentoClient.createMediaPipeline(tx2);

		HttpGetEndpoint httpGetEndpoint2 = new HttpGetEndpoint.Builder(
				pipeline2).build(tx2);
		TFuture<String> url2 = httpGetEndpoint2.getUrl(tx2);
		tx2.commit();
		// End pipeline creation

		assertThat(url1.get(), is(not(url2.get())));
	}

	@Test
	public void creationInTransaction() {

		// Pipeline creation (transaction)
		Transaction tx1 = kurentoClient.beginTransaction();

		MediaPipeline pipeline = kurentoClient.createMediaPipeline(tx1);

		PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				"http://files.kurento.org/video/small.webm").useEncodedMedia()
				.build(tx1);

		HttpGetEndpoint httpGetEndpoint = new HttpGetEndpoint.Builder(pipeline)
				.build(tx1);

		player.connect(tx1, httpGetEndpoint);
		TFuture<String> url1 = httpGetEndpoint.getUrl(tx1);
		tx1.commit();
		// End pipeline creation

		// Explicit transaction
		Transaction tx2 = pipeline.beginTransaction();
		player.play(tx2);
		TFuture<String> url2 = httpGetEndpoint.getUrl(tx2);
		pipeline.release(tx2);
		tx2.commit();
		// End explicit transaction

		assertThat(url1.get(), is(url2.get()));
	}

	@Test(expected = TransactionNotCommitedException.class)
	public void usePlainMethodsInNewObjectsInsideTx() {

		// Pipeline creation (no transaction)
		MediaPipeline pipeline = kurentoClient.createMediaPipeline();
		PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				"http://files.kurento.org/video/small.webm").build();

		// Creation in explicit transaction
		Transaction tx = pipeline.beginTransaction();
		HttpGetEndpoint httpGetEndpoint = new HttpGetEndpoint.Builder(pipeline)
				.build(tx);

		// TransactionNotExecutedExcetion should be thrown
		httpGetEndpoint.connect(player);

	}

	// In the current KMS impl, the error is MediaElementImpl not found and
	// should be another error to control non-commited objects
	// @Ignore
	@Test(expected = TransactionNotCommitedException.class)
	public void usePlainMethodsWithNewObjectsAsParamsInsideTx() {

		// Pipeline creation (no transaction)
		MediaPipeline pipeline = kurentoClient.createMediaPipeline();
		PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				"http://files.kurento.org/video/small.webm").build();

		// Creation in explicit transaction
		Transaction tx = pipeline.beginTransaction();
		HttpGetEndpoint httpGetEndpoint = new HttpGetEndpoint.Builder(pipeline)
				.build(tx);

		// TransactionNotExecutedExcetion should be thrown
		player.connect(httpGetEndpoint);

	}

	@Test
	public void isCommitedTest() {

		Transaction tx = kurentoClient.beginTransaction();

		MediaPipeline pipeline = kurentoClient.createMediaPipeline(tx);

		PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				"http://files.kurento.org/video/small.webm").build(tx);

		HttpGetEndpoint httpGetEndpoint = new HttpGetEndpoint.Builder(pipeline)
				.build(tx);

		player.connect(tx, httpGetEndpoint);

		assertThat(player.isCommited(), is(false));

		tx.commit();

		assertThat(player.isCommited(), is(true));
	}

	@Test
	public void asyncTransaction() {

		Transaction tx = kurentoClient.beginTransaction();

		MediaPipeline pipeline = kurentoClient.createMediaPipeline();

		PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				"http://files.kurento.org/video/small.webm").build();

		HttpGetEndpoint httpGetEndpoint = new HttpGetEndpoint.Builder(pipeline)
				.build();

		player.connect(httpGetEndpoint);

		AsyncResultManager<Void> async = new AsyncResultManager<>("async start");

		tx.commit(async.getContinuation());

		async.waitForResult();

		assertThat(pipeline.isCommited(), is(true));
	}

	@Test
	public void waitCommitedTest() throws InterruptedException {

		// Pipeline creation (transaction)

		Transaction tx = kurentoClient.beginTransaction();

		MediaPipeline pipeline = kurentoClient.createMediaPipeline(tx);

		final PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				"http://files.kurento.org/video/small.webm").build(tx);

		HttpGetEndpoint httpGetEndpoint = new HttpGetEndpoint.Builder(pipeline)
				.build(tx);

		player.connect(tx, httpGetEndpoint);

		final CountDownLatch readyLatch = new CountDownLatch(1);

		new Thread() {
			@Override
			public void run() {
				try {
					player.waitCommited();
					readyLatch.countDown();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}.start();

		assertThat(readyLatch.getCount(), is(1l));

		tx.commit();

		if (!readyLatch.await(5000, TimeUnit.SECONDS)) {
			fail("waitForReady not unblocked in 5s");
		}
	}

	@Test
	public void whenCommitedTest() {

		// Pipeline creation (transaction)

		Transaction tx = kurentoClient.beginTransaction();

		MediaPipeline pipeline = kurentoClient.createMediaPipeline(tx);

		final PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				"http://files.kurento.org/video/small.webm").build(tx);

		HttpGetEndpoint httpGetEndpoint = new HttpGetEndpoint.Builder(pipeline)
				.build(tx);

		player.connect(tx, httpGetEndpoint);

		AsyncResultManager<PlayerEndpoint> async = new AsyncResultManager<>(
				"whenCommited");

		player.whenCommited(async.getContinuation());

		tx.commit();

		PlayerEndpoint newPlayer = async.waitForResult();

		assertThat(player, is(newPlayer));
	}

	@Test
	public void futureTest() {

		// Pipeline creation (no transaction)

		MediaPipeline pipeline = kurentoClient.createMediaPipeline();

		PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				"http://files.kurento.org/video/small.webm").build();

		HttpGetEndpoint httpGetEndpoint = new HttpGetEndpoint.Builder(pipeline)
				.build();

		player.connect(httpGetEndpoint);

		// End pipeline creation

		// Atomic operation
		String url = httpGetEndpoint.getUrl();
		MediaPipeline rPipeline = httpGetEndpoint.getMediaPipeline();
		String uri = player.getUri();
		// End atomic operation

		// Explicit transaction
		Transaction tx = pipeline.beginTransaction();
		TFuture<String> fUrl = httpGetEndpoint.getUrl(tx);
		TFuture<MediaPipeline> fRPipeline = httpGetEndpoint
				.getMediaPipeline(tx);
		TFuture<String> fUri = player.getUri(tx);
		tx.commit();
		// End explicit transaction

		assertThat(url, is(fUrl.get()));
		assertThat(uri, is(fUri.get()));

		MediaPipeline fRPipelineGet = fRPipeline.get();

		System.out.println(rPipeline);
		System.out.println(fRPipelineGet);

		assertThat(rPipeline, is(fRPipelineGet));
	}

	@Test
	public void userRollbackTest() {

		Transaction tx = kurentoClient.beginTransaction();

		MediaPipeline pipeline = kurentoClient.createMediaPipeline(tx);

		PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				"http://files.kurento.org/video/small.webm").build(tx);

		TFuture<String> uri = player.getUri(tx);

		tx.rollback();

		try {
			player.release();
		} catch (TransactionRollbackException e) {
			log.info("Captured exception of class " + e.getClass()
					+ " with message '" + e.getMessage() + "'");
			assertThat(e.isUserRollback(), is(true));
		}

		try {
			uri.get();
		} catch (TransactionRollbackException e) {
			log.info("Captured exception of class " + e.getClass()
					+ " with message '" + e.getMessage() + "'");
			assertThat(e.isUserRollback(), is(true));
		}
	}

	@Test
	public void transactionErrorTest() {

		// Pipeline creation (no transaction)

		Transaction tx = kurentoClient.beginTransaction();

		MediaPipeline pipeline = kurentoClient.createMediaPipeline(tx);

		PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				"http://files.kurento.org/video/small.webm").build(tx);

		tx.commit();

		player.release();

		try {
			player.play();
		} catch (KurentoServerException e) {
			log.info("Captured exception of class " + e.getClass()
					+ " with message '" + e.getMessage() + "'");
			assertThat(e.getCode(), is(40101));
			assertThat(e.getServerMessage(), containsString(" not found"));
		}

		tx = pipeline.beginTransaction();

		ZBarFilter filter = new ZBarFilter.Builder(pipeline).build(tx);
		player.play(tx);

		try {
			tx.commit();
			fail("Exception 'TransactionExecutionException' should be thrown");
		} catch (TransactionExecutionException e) {
			log.info("Captured exception of class " + e.getClass()
					+ " with message '" + e.getMessage() + "'");
			assertThat(e.getCode(), is(40101));
			assertThat(e.getServerMessage(), containsString(" not found"));
		}

		try {
			filter.connect(player);
			fail("Exception 'TransactionExecutionException' should be thrown");
		} catch (TransactionRollbackException e) {
			log.info("Captured exception of class " + e.getClass()
					+ " with message '" + e.getMessage() + "'");

			KurentoServerException kse = e.getKurentoServerException();
			assertThat(kse, is(not(nullValue())));
			assertThat(kse.getCode(), is(40101));
			assertThat(kse.getServerMessage(), containsString(" not found"));
		}
	}

	@Test
	public void asyncCommit() {

		// Pipeline creation (transaction)

		Transaction tx = kurentoClient.beginTransaction();

		MediaPipeline pipeline = kurentoClient.createMediaPipeline(tx);

		final PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline,
				"http://files.kurento.org/video/small.webm").build(tx);

		HttpGetEndpoint httpGetEndpoint = new HttpGetEndpoint.Builder(pipeline)
				.build(tx);

		player.connect(tx, httpGetEndpoint);

		AsyncResultManager<Void> async = new AsyncResultManager<>("commit");

		tx.commit(async.getContinuation());

		async.waitForResult();

		assertThat(player.isCommited(), is(true));

	}
}
