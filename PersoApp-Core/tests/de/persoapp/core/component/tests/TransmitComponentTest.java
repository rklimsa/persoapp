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
 * @version 1.0, 22.04.2015 11:37:36
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
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import de.persoapp.core.ECardWorker;
import de.persoapp.core.card.CardHandler;
import de.persoapp.core.card.ISOSMTransport;
import de.persoapp.core.card.TransportProvider;
import de.persoapp.core.client.IMainView;
import de.persoapp.core.client.MainViewEventListener;
import de.persoapp.core.client.SecureHolder;
import de.persoapp.core.tests.util.TestMainView;
import de.persoapp.core.util.TLV;
import de.persoapp.core.util.Util;
import de.persoapp.core.ws.IFDService;
import de.persoapp.core.ws.ManagementService;
import de.persoapp.core.ws.SALService;
import de.persoapp.core.ws.engine.WSContainer;

/**
 * <p>
 * Testcase facing the transmission mechanism of the PersoApp.
 * </p>
 * <p>
 * The component testcase covers the complete way from establishing a channel
 * up to sending messages.
 * </p>
 * 
 * @author Rico Klimsa, 2015
 */
@RunWith(JMockit.class)
public class TransmitComponentTest {

	private byte[] taKey;
	private byte[] CHAT;
	
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
	 * Online authentication is triggered and the components to send data on a
	 * secure channel are tested. </br> 
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestSteps: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>All commonly used parameters are checked being <b>not null</b></li>
	 * <li>The terminal authentication key is checked not to change during the authentication process.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The transmission of data is successful. An error is not signaled.</li>
	 * </ul>
	 * 
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	@Test
	public void transmitComponentTest_1() throws IOException, URISyntaxException, GeneralSecurityException {
		
		final TestSpy spy = new TestSpy();
		
		final URL tcTokenURL = new URL(serviceURL);
		
		final MockUp<ISOSMTransport> mockUpISOSMTransport = new MockUp<ISOSMTransport>() {
		
			@Mock
			public byte[] transmit(mockit.Invocation inv, byte[] apdu) {

				try {
					assertNotNull("apdu is null.", apdu);

					final byte[] result = inv.proceed(apdu);
					assertNotNull("result is null.", result);
					return result;

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}

			}
			
		
		};

		final MockUp<CardHandler> mockUpCardHandler = new MockUp<CardHandler>() {
			@Mock
			private final byte[] generalAUTH(mockit.Invocation inv,
					TransportProvider tp, byte[] authData, boolean lastCommand) {

				try {
					assertNotNull("tp is null.", tp);
					assertNotNull("lastCommand is null.", lastCommand);
					
					if(lastCommand) {
						assertNotNull("authData is null", authData);
						
						final byte[] data = TLV.get(authData, (byte)0x80);
						
						final Field field = CardHandler.class.getDeclaredField("TAKey");
						
						field.setAccessible(true);
						
						taKey = (byte[]) field.get((CardHandler)inv.getInvokedInstance());
						
						field.setAccessible(false);
						
						Assert.assertArrayEquals("TaKey different", taKey, data);
					}

					final byte[] result = inv
							.proceed(tp, authData, lastCommand);

					return result;

				} catch (final AssertionError | IllegalArgumentException
						| IllegalAccessException | NoSuchFieldException
						| SecurityException ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}

			}
			
			@Mock
			public byte[] execCA(mockit.Invocation inv) {

				try {

					final byte[] result = inv.proceed();
					assertNotNull("result is null.", result);
					return result;

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}
			}
			
			@Mock
			public boolean startAuthentication(mockit.Invocation inv,
					byte CHAT[], SecureHolder secret, byte[] termDesc) {

				try {
					
					Logger.getGlobal().info("start authentication.");
					
					assertNotNull("CHAT is null.", CHAT);
					assertNotNull("secret is null.", secret);
					assertNotNull("termDesc is null.", termDesc);

					final boolean result = inv.proceed(CHAT, secret, termDesc);
					
					assertNotNull("result is null.", Boolean.valueOf(result));
					assertTrue("Initialization of PACE is false",Boolean.valueOf(result));
					
					return result;

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}

			}
			
			@Mock
			private int executePACE(mockit.Invocation inv, byte keyReference,
					byte[] secret, byte[] CHAT) {

				try {
					assertNotNull("keyReference is null.", keyReference);
					assertNotNull("secret is null.", secret);
					assertNotNull("CHAT is null.", CHAT);

					final int result = inv.proceed(keyReference, secret, CHAT);
					assertNotNull("result is null.", result);
					return result;

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}

			}
			
			
			@Mock
			private int executeLocalPACE(mockit.Invocation inv,
					String cryptoMechanism, byte keyReference, byte[] secret,
					byte[] CHAT) {

				try {
					assertNotNull("cryptoMechanism is null.", cryptoMechanism);
					assertNotNull("keyReference is null.", keyReference);
					assertNotNull("secret is null.", secret);
					assertNotNull("CHAT is null.", CHAT);

					final int result = inv.proceed(cryptoMechanism,
							keyReference, secret, CHAT);
					assertNotNull("result is null.", result);
					return result;

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}

			}
			
			
			
			@Mock
			private byte[] KDF(mockit.Invocation inv, MessageDigest md,
					byte[] secret, int counter, int limit) {

				try {
					assertNotNull("md is null.", md);
					assertNotNull("secret is null.", secret);
					assertNotNull("counter is null.", counter);
					assertNotNull("limit is null.", limit);

					final byte[] result = inv.proceed(md, secret, counter,
							limit);
					assertNotNull("result is null.", result);
					return result;

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}

			}
		};
		
		
		final MockUp<SALService> mockUpSALService = new MockUp<SALService>() {
			@Mock
			public iso.std.iso_iec._24727.tech.schema.DIDAuthenticateResponse didAuthenticate(
					mockit.Invocation inv,
					iso.std.iso_iec._24727.tech.schema.DIDAuthenticate parameters) {

				try {
					assertNotNull("parameters is null.", parameters);

					final iso.std.iso_iec._24727.tech.schema.DIDAuthenticateResponse result = inv
							.proceed(parameters);

					assertNotNull("Result is null",result);
					
					Logger.getGlobal().info(String.format("Result: %s", result));
					
					return result;

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}

			}
		};
		
		init();
		
		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);

		if (spy.getStringValue() != null
				&& !spy.getStringValue().trim().isEmpty()) {
			mockUpISOSMTransport.tearDown();
			mockUpCardHandler.tearDown();
			mockUpSALService.tearDown();
			fail(spy.getStringValue());
		}

		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		
		
		mockUpISOSMTransport.tearDown();
		mockUpCardHandler.tearDown();
		mockUpSALService.tearDown();
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
