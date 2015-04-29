/**
 * 
 * COPYRIGHT (C) 2010, 2011, 2012, 2013 AGETO Innovation GmbH
 * 
 * Authors Christian Kahlo, Ralf Wondratschek
 * 
 * All Rights Reserved.
 * 
 * Contact: PersoApp, http://www.persoapp.de
 * 
 * @version 1.0, 21.04.2015 14:02:10
 * 
 *          This file is part of PersoApp.
 * 
 *          PersoApp is free software: you can redistribute it and/or modify it
 *          under the terms of the GNU Lesser General Public License as
 *          published by the Free Software Foundation, either version 3 of the
 *          License, or (at your option) any later version.
 * 
 *          PersoApp is distributed in the hope that it will be useful, but
 *          WITHOUT ANY WARRANTY; without even the implied warranty of
 *          MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *          Lesser General Public License for more details.
 * 
 *          You should have received a copy of the GNU Lesser General Public
 *          License along with PersoApp. If not, see
 *          <http://www.gnu.org/licenses/>.
 * 
 *          Diese Datei ist Teil von PersoApp.
 * 
 *          PersoApp ist Freie Software: Sie können es unter den Bedingungen der
 *          GNU Lesser General Public License, wie von der Free Software
 *          Foundation, Version 3 der Lizenz oder (nach Ihrer Option) jeder
 *          späteren veröffentlichten Version, weiterverbreiten und/oder
 *          modifizieren.
 * 
 *          PersoApp wird in der Hoffnung, dass es nützlich sein wird, aber OHNE
 *          JEDE GEWÄHRLEISTUNG, bereitgestellt; sogar ohne die implizite
 *          Gewährleistung der MARKTFÄHIGKEIT oder EIGNUNG FÜR EINEN BESTIMMTEN
 *          ZWECK. Siehe die GNU Lesser General Public License für weitere
 *          Details.
 * 
 *          Sie sollten eine Kopie der GNU Lesser General Public License
 *          zusammen mit diesem Programm erhalten haben. Wenn nicht, siehe
 *          <http://www.gnu.org/licenses/>.
 * 
 */

package de.persoapp.core.component.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import de.persoapp.core.ECardWorker;
import de.persoapp.core.card.CardHandler;
import de.persoapp.core.client.IMainView;
import de.persoapp.core.client.MainViewEventListener;
import de.persoapp.core.paos.MiniHttpClient;
import de.persoapp.core.paos.PAOSInitiator;
import de.persoapp.core.tests.util.TestMainView;
import de.persoapp.core.tls.BCTlsSocketFactoryImpl;
import de.persoapp.core.tls.BCTlsSocketImpl;
import de.persoapp.core.util.Util;
import de.persoapp.core.ws.IFDService;
import de.persoapp.core.ws.ManagementService;
import de.persoapp.core.ws.SALService;
import de.persoapp.core.ws.engine.WSContainer;

/**
 * <p>
 * Integration test facing the ssl libary of the PersoApp.
 * </p>
 * <p>
 * The address and the port which are used to create the
 * socket, are checked to be bound accurate.
 * </p>
 * 
 * @author Rico Klimsa, 2015
 */
@RunWith(JMockit.class)
public class BCTlsComponentTest {

	
	private URL socketURL;
	
	private URL refereshURL;
	
	private String serviceURL;

	private String defaultPIN;

	private CardHandler eCardHandler;
	private WSContainer wsCtx;

	private static Properties properties;

	private MainViewEventListener mainViewEventListener;
	@Rule
	public TestWatcher watchman = new TestWatcher() {
		@Override
		protected void failed(Throwable e, Description description) {
			Logger.getGlobal().severe(
					description.getMethodName() + "Failed!" + " "
							+ e.getMessage());
		}

		@Override
		protected void succeeded(Description description) {
			Logger.getGlobal().info(
					description.getMethodName() + " " + "success!");
		}

	};

	private static class TestSpy {

		private boolean value;
		private String stringValue;

		/**
		 * @return the value
		 */
		public boolean isValue() {
			return value;
		}

		/**
		 * @param value
		 *            the value to set
		 */
		public void setValue(final boolean value) {
			this.value = value;
		}

		/**
		 * @return the stringValue
		 */
		public String getStringValue() {
			return stringValue;
		}

		/**
		 * @param stringValue
		 *            the stringValue to set
		 */
		public void setStringValue(final String stringValue) {
			this.stringValue = stringValue;
		}
	}

	/**
	 * Load the resource file for default pin and service url. If the resource
	 * file does not exist, it must be created by the developer per hand.
	 */
	@BeforeClass
	public static void setUp() throws FileNotFoundException, IOException {
		final String resourcePath = "/tests/resources/test_config.properties";
		final File res = new File(new File("").getAbsolutePath() + resourcePath);

		if (res.exists()) {
			properties = new Properties();
			properties.load(new FileInputStream(res));
		} else {
			throw new FileNotFoundException("File not found: " + resourcePath);
		}
	}

	/**
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link IMainView} is created</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The mainView was successfully created.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link CardHandler} is created.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The {@link CardHandler} is successfully created.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link MainViewEventListener} is created and set.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>No Exception occurred, which indicates an successful result.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link WSContainer} is created.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The {@link WSContainer} is successfully created.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link ManagementService} is created and added as a service to
	 * the {@link WSContainer}. The specific {@link WSEndpoint} is added to the
	 * {@link WSContainer}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>No Exception occurred, which indicates an successful result.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link SALService} is created and added as a service to the
	 * {@link WSContainer}. The specific {@link WSEndpoint} is added to the
	 * {@link WSContainer}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>No Exception occurred, which indicates an successful result.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link IFDService} is created and added as a service to the
	 * {@link WSContainer}. The specific {@link WSEndpoint} is added to the
	 * {@link WSContainer}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>No Exception occurred, which indicates an successful result.</li>.
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>Initializes the {@link WSContainer} and injects all created
	 * endpoints.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>No Exception occurred, which indicates an successful result.</li>.
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>Initializes the {@link ECardWorker}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>No Exception occurred, which indicates an successful result.</li>.
	 * </ul>
	 */
	@Before
	public void init() {

		defaultPIN = (String) properties.get("Default_PIN");

		serviceURL = (String) properties.get("eID_service_URL");

		final IMainView mainView = TestMainView.getInstance(defaultPIN);
		assertNotNull("no main view", mainView);

		eCardHandler = new CardHandler(mainView);

		assertNotNull("no card handler", eCardHandler);
		assertNotNull("No eID card inserted", eCardHandler.getECard());
		eCardHandler.reset();

		mainViewEventListener = new MainViewEventListener(eCardHandler,
				mainView);
		mainView.setEventLister(mainViewEventListener);

		wsCtx = new WSContainer();
		assertNotNull("no web service container", wsCtx);

		wsCtx.addService(new ManagementService());
		wsCtx.addService(new SALService());
		wsCtx.addService(new IFDService());
		wsCtx.init(null);

		ECardWorker.init(mainView, wsCtx, eCardHandler);
	}
	
	
	/**
	 * Online authentication is triggered and the component to establish and
	 * handle tls connections is tested.</br>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestSteps: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>All parameters are checked being <b>not null</b></li>
	 * <li>The remote address of the socket is checked to be similar to the URL, which is used to create the internal client.</li>
	 * <li>The socket of the internal client is checked to be connected and bound.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication completes with no errors.</li>
	 * </ul>
	 * 
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	@Test
	public void bcTlsComponentTest_1() throws IOException, URISyntaxException, GeneralSecurityException {

		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);
		
		
		MockUp<PAOSInitiator> mockUpPAOSInitiator = new MockUp<PAOSInitiator>() {
			@Mock
			public Certificate getPeerCertificate(mockit.Invocation inv) {

				try {

					final Certificate peerCertificate = inv.proceed();
					assertNotNull("peerCertificate is null.", peerCertificate);
					return peerCertificate;

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}

			}

			@Mock
			private MiniHttpClient createClient(mockit.Invocation inv)
					throws IOException {

				try {

					
					final Field field = PAOSInitiator.class.getDeclaredField("serviceURL");
					
					field.setAccessible(true);
					
					final PAOSInitiator currentInstance = ((PAOSInitiator)inv.getInvokedInstance());
					
					socketURL = (URL) field.get(currentInstance);
					
					System.out.println("### Created client URL: " + socketURL);
					
					field.setAccessible(false);
					
					final MiniHttpClient result = inv.proceed();
					assertNotNull("result is null.", result);
					return result;

				} catch (final AssertionError | NoSuchFieldException
						| SecurityException | IllegalArgumentException
						| IllegalAccessException ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}

			}

		};
		
		final MockUp<MiniHttpClient> mockUpMiniHttpClient = new MockUp<MiniHttpClient>() {
			
			
			@Mock
			public SSLSession getSSLSession(mockit.Invocation inv) {

				try {
					final SSLSession sSLSession = inv.proceed();
					assertNotNull("sSLSession is null.", sSLSession);
					return sSLSession;

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}

			}

			@Mock
			private Socket getSocket(mockit.Invocation inv) throws IOException {

				try {
					final Socket socket = inv.proceed();
					assertNotNull("socket is null.", socket);
					return socket;

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}

			}
			
			
			@Mock
			public byte[] transmit(mockit.Invocation inv, byte[] in)
					throws IOException {
				
				try {
				
					assertNotNull("in is null.", in);

					final byte[] result = inv.proceed(in);
					assertNotNull("result is null.", result);
					return result;
					
				} catch ( final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}
				
			}
		};
		
		
		
		MockUp<BCTlsSocketFactoryImpl> mockUpBCTlsSocketFactoryImpl = new MockUp<BCTlsSocketFactoryImpl>() {
			@Mock
			public Socket createSocket(mockit.Invocation inv, Socket socket, final String host, final int port, final boolean autoClose)
					throws IOException {

				try {

					assertNotNull("host is null.", host);
					assertNotNull("port is null.", port);

					final BCTlsSocketImpl result = inv.proceed(socket,host, port,autoClose);

					/*
					 * Use the socket reference to check the properties, because
					 * the BCTlsSocket is not connected.
					 * Perform the check only, when a socket is not null.
					 */
					if(socket != null) {
						assertEquals("Host is incorrect",host,socket.getInetAddress().getHostName());
						assertEquals("Port is incorrect", port,socket.getPort());
						assertTrue("Socket is not connected.", socket.isConnected());
						assertTrue("Socket is not bound.", socket.isBound());
						assertFalse("Socket is already closed.", socket.isClosed());
					}
					
					return result;

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}

			}
		};
		
		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);

		if (spy.getStringValue() != null
				&& !spy.getStringValue().trim().isEmpty()) {
			mockUpPAOSInitiator.tearDown();
			mockUpMiniHttpClient.tearDown();
			mockUpBCTlsSocketFactoryImpl.tearDown();
			fail(spy.getStringValue());
		}

		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		
		
		mockUpPAOSInitiator.tearDown();
		mockUpMiniHttpClient.tearDown();
		mockUpBCTlsSocketFactoryImpl.tearDown();

	}
	
	
	
	
	private void connectToRefreshURL(final String refreshURL, boolean success)
			throws IOException {

		if (success) {
			assertFalse("process failed",
					refreshURL.toLowerCase().indexOf("resultmajor=ok") < 0);
		} else {
			assertFalse("process succeeded",
					refreshURL.toLowerCase().indexOf("resultmajor=ok") > 0);
		}

		final URL refresh = new URL(refreshURL);
		final HttpURLConnection uc = (HttpsURLConnection) Util.openURL(refresh);
		uc.setInstanceFollowRedirects(true);
		final Object content = uc.getContent();
		System.out.println("HTTP Response " + uc.getResponseCode() + " "
				+ uc.getResponseMessage());
		if (content instanceof InputStream) {
			final Scanner scanner = new Scanner((InputStream) content, "UTF-8")
					.useDelimiter("\\A");
			System.out.println(scanner.next());
			scanner.close();
		} else {
			System.out.println(content);
		}
	}
	
	/**
	 * Reset the ECardWorker. Delay test execution to prevent race condition
	 * between testcases.
	 * 
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	@After
	public synchronized void cleanUp() throws NoSuchFieldException,
			SecurityException, IllegalArgumentException, IllegalAccessException {
		final long end = System.currentTimeMillis() + 6000;

		while (System.currentTimeMillis() < end) {
			// wait to prevent race condition between testcases.
		}

		final Field field = ECardWorker.class.getDeclaredField("mainView");
		field.setAccessible(true);
		field.set(null, null);
		field.setAccessible(false);
		ECardWorker.init(null, null, null);

	}
	
}
