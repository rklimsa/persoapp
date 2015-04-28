package de.persoapp.core.tests.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

import mockit.Mock;
import mockit.MockUp;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.MethodSorters;

import de.persoapp.core.ECardWorker;

import de.persoapp.core.card.CardHandler;
import de.persoapp.core.client.IMainView;
import de.persoapp.core.client.MainViewEventListener;
import de.persoapp.core.tests.util.TestMainView;
import de.persoapp.core.ws.IFDService;
import de.persoapp.core.ws.ManagementService;
import de.persoapp.core.ws.SALService;
import de.persoapp.core.ws.engine.WSContainer;
import de.persoapp.core.util.Util;
import de.persoapp.core.card.TransportProvider;
import de.persoapp.core.card.CCID;
import de.persoapp.core.paos.PAOSInitiator;
import de.persoapp.core.paos.PAOSInitiatorFactory;
import de.persoapp.core.paos.MiniHttpClient;
import iso.std.iso_iec._24727.tech.schema.ChannelHandleType;
import iso.std.iso_iec._24727.tech.schema.ResponseType;
import java.net.URI;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Exchanger;
import de.persoapp.core.card.ICardHandler;
import de.persoapp.core.client.EAC_Info;
import de.persoapp.core.client.ECardSession;
import de.persoapp.core.client.PropertyResolver;
import de.persoapp.core.util.ArrayTool;
import de.persoapp.core.util.Hex;
import de.persoapp.core.ws.EcAPIProvider;
import de.persoapp.core.ECardWorker;

/**
 * Testcases facing {@link ECardWorker}
 * 
 * @author Rico Klimsa, 2015
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ECardWorkerTest {
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
	 * Online authentication is triggered and the method
	 * <em>de.persoapp.core.ECardWorker.init()</em> is tested</br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>All parameters are checked being <b>not null</b></li>
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
	public void ecardworkerTest_1() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<ECardWorker> mockUp = new MockUp<ECardWorker>() {
			@Mock
			public void init(mockit.Invocation inv, IMainView mainView,
					WSContainer wsCtx, ICardHandler eCardHandler) {

				try {
					assertNotNull("mainView is null.", mainView);
					assertNotNull("wsCtx is null.", wsCtx);
					assertNotNull("eCardHandler is null.", eCardHandler);

					inv.proceed(mainView, wsCtx, eCardHandler);

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
			mockUp.tearDown();
			fail(spy.getStringValue());
		}

		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

	}

	/**
	 * Online authentication is triggered and the method
	 * <em>de.persoapp.core.ECardWorker.checkCertificate()</em> is tested</br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>All parameters are checked being <b>not null</b></li>
	 * <li>The return value is checked being <b>not null</b></li>
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
	public void ecardworkerTest_2() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<ECardWorker> mockUp = new MockUp<ECardWorker>() {
			@Mock
			private X509Certificate checkCertificate(mockit.Invocation inv,
					URLConnection uc) throws IOException, URISyntaxException {

				try {
					assertNotNull("uc is null.", uc);

					final X509Certificate result = inv.proceed(uc);
					assertNotNull("result is null.", result);
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
			mockUp.tearDown();
			fail(spy.getStringValue());
		}

		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

	}

	/**
	 * Online authentication is triggered and an IOException occurs during the
	 * execution of the method
	 * <em>de.persoapp.core.ECardWorker.checkCertificate()</em> </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>An IOException is throwed in the method
	 * <em>de.persoapp.core.ECardWorker.checkCertificate()</em>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication fails.</li>
	 * </ul>
	 * 
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 * @throws MalformedURLException 
	 */

	@Test
	public void ecardworkerTest_3() throws URISyntaxException,
			GeneralSecurityException, MalformedURLException {
		final TestSpy spy = new TestSpy();
		spy.setValue(false);

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<ECardWorker> mockUp = new MockUp<ECardWorker>() {
			@Mock
			private X509Certificate checkCertificate(mockit.Invocation inv,
					URLConnection uc) throws IOException, URISyntaxException {

				spy.setValue(true);
				throw new IOException();

			}
		};

		try {
			final String refreshURL = ECardWorker.start(tcTokenURL);
			assertNotNull("no refresh URL", refreshURL);

			System.out.println("refreshURL: " + refreshURL);
			connectToRefreshURL(refreshURL, false);
			
		} catch (final IOException ioe) {
			if (!spy.isValue()) {
				Logger.getGlobal().info("Test function was not called. Aborting.");
				return;
			}			
		} finally {
			mockUp.tearDown();
		}

	}

	/**
	 * Online authentication is triggered and an URISyntaxException occurs
	 * during the execution of the method
	 * <em>de.persoapp.core.ECardWorker.checkCertificate()</em> </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>An URISyntaxException is throwed in the method
	 * <em>de.persoapp.core.ECardWorker.checkCertificate()</em>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication fails.</li>
	 * </ul>
	 * 
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */

	@Test
	public void ecardworkerTest_4() throws IOException, 
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();
		spy.setValue(false);

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<ECardWorker> mockUp = new MockUp<ECardWorker>() {
			@Mock
			private X509Certificate checkCertificate(mockit.Invocation inv,
					URLConnection uc) throws IOException, URISyntaxException {

				spy.setValue(true);
				throw new URISyntaxException("", "");

			}
		};

		try {
			final String refreshURL = ECardWorker.start(tcTokenURL);
			assertNotNull("no refresh URL", refreshURL);

			System.out.println("refreshURL: " + refreshURL);
			connectToRefreshURL(refreshURL, false);
			
		} catch (final URISyntaxException use) {
			if (!spy.isValue()) {
				Logger.getGlobal().info("Test function was not called. Aborting.");
				return;
			}			
		} finally {
			mockUp.tearDown();
		}

	}

	/**
	 * Online authentication is triggered and the method
	 * <em>de.persoapp.core.ECardWorker.start()</em> is tested</br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>All parameters are checked being <b>not null</b></li>
	 * <li>The return value is checked being <b>not null</b></li>
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
	public void ecardworkerTest_5() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<ECardWorker> mockUp = new MockUp<ECardWorker>() {
			@Mock
			public String start(mockit.Invocation inv, URL tcTokenURL)
					throws IOException, URISyntaxException,
					GeneralSecurityException {

				try {
					assertNotNull("tcTokenURL is null.", tcTokenURL);

					final String result = inv.proceed(tcTokenURL);
					assertNotNull("result is null.", result);
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
			mockUp.tearDown();
			fail(spy.getStringValue());
		}

		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

	}

	/**
	 * Online authentication is triggered and an IOException occurs during the
	 * execution of the method <em>de.persoapp.core.ECardWorker.start()</em>
	 * </br>. <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>An IOException is throwed in the method
	 * <em>de.persoapp.core.ECardWorker.start()</em>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication fails.</li>
	 * </ul>
	 * 
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 * @throws MalformedURLException 
	 */

	@Test
	public void ecardworkerTest_6() throws URISyntaxException,
			GeneralSecurityException, MalformedURLException {
		final TestSpy spy = new TestSpy();
		spy.setValue(false);

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<ECardWorker> mockUp = new MockUp<ECardWorker>() {
			@Mock
			public String start(mockit.Invocation inv, URL tcTokenURL)
					throws IOException, URISyntaxException,
					GeneralSecurityException {

				spy.setValue(true);
				throw new IOException();

			}
		};

		try {
			final String refreshURL = ECardWorker.start(tcTokenURL);
			assertNotNull("no refresh URL", refreshURL);

			System.out.println("refreshURL: " + refreshURL);
			connectToRefreshURL(refreshURL, false);
			
		} catch (final IOException ioe) {
			if (!spy.isValue()) {
				Logger.getGlobal().info("Test function was not called. Aborting.");
				return;
			}			
		} finally {
			mockUp.tearDown();
		}

	}

	/**
	 * Online authentication is triggered and an URISyntaxException occurs
	 * during the execution of the method
	 * <em>de.persoapp.core.ECardWorker.start()</em> </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>An URISyntaxException is throwed in the method
	 * <em>de.persoapp.core.ECardWorker.start()</em>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication fails.</li>
	 * </ul>
	 * 
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */

	@Test
	public void ecardworkerTest_7() throws IOException, 
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();
		spy.setValue(false);

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<ECardWorker> mockUp = new MockUp<ECardWorker>() {
			@Mock
			public String start(mockit.Invocation inv, URL tcTokenURL)
					throws IOException, URISyntaxException,
					GeneralSecurityException {

				spy.setValue(true);
				throw new URISyntaxException("", "");

			}
		};

		try {
			final String refreshURL = ECardWorker.start(tcTokenURL);
			assertNotNull("no refresh URL", refreshURL);

			System.out.println("refreshURL: " + refreshURL);
			connectToRefreshURL(refreshURL, false);
			
		} catch (final URISyntaxException use) {
			if (!spy.isValue()) {
				Logger.getGlobal().info("Test function was not called. Aborting.");
				return;
			}			
		} finally {
			mockUp.tearDown();
		}

	}

	/**
	 * Online authentication is triggered and an GeneralSecurityException occurs
	 * during the execution of the method
	 * <em>de.persoapp.core.ECardWorker.start()</em> </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>An GeneralSecurityException is throwed in the method
	 * <em>de.persoapp.core.ECardWorker.start()</em>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication fails.</li>
	 * </ul>
	 * 
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */

	@Test
	public void ecardworkerTest_8() throws IOException, URISyntaxException {
		final TestSpy spy = new TestSpy();
		spy.setValue(false);

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<ECardWorker> mockUp = new MockUp<ECardWorker>() {
			@Mock
			public String start(mockit.Invocation inv, URL tcTokenURL)
					throws IOException, URISyntaxException,
					GeneralSecurityException {

				spy.setValue(true);
				throw new GeneralSecurityException();

			}
		};

		try {
			final String refreshURL = ECardWorker.start(tcTokenURL);
			assertNotNull("no refresh URL", refreshURL);

			System.out.println("refreshURL: " + refreshURL);
			connectToRefreshURL(refreshURL, false);
			
		} catch (final GeneralSecurityException gse) {
			if (!spy.isValue()) {
				Logger.getGlobal().info("Test function was not called. Aborting.");
				return;
			}			
		} finally {
			mockUp.tearDown();
		}

	}

	/**
	 * Online authentication is triggered and the method
	 * <em>de.persoapp.core.ECardWorker.startECardWorker()</em> is tested</br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>All parameters are checked being <b>not null</b></li>
	 * <li>The return value is checked being <b>not null</b></li>
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
	public void ecardworkerTest_9() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<ECardWorker> mockUp = new MockUp<ECardWorker>() {
			@Mock
			private String startECardWorker(mockit.Invocation inv,
					Map<String, String> params, List<Certificate> serverCerts,
					URI tcTokenURL) throws GeneralSecurityException,
					IOException, URISyntaxException {

				try {
					assertNotNull("params is null.", params);
					assertNotNull("serverCerts is null.", serverCerts);
					assertNotNull("tcTokenURL is null.", tcTokenURL);

					final String result = inv.proceed(params, serverCerts,
							tcTokenURL);
					assertNotNull("result is null.", result);
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
			mockUp.tearDown();
			fail(spy.getStringValue());
		}

		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

	}

	/**
	 * Online authentication is triggered and an GeneralSecurityException occurs
	 * during the execution of the method
	 * <em>de.persoapp.core.ECardWorker.startECardWorker()</em> </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>An GeneralSecurityException is throwed in the method
	 * <em>de.persoapp.core.ECardWorker.startECardWorker()</em>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication fails.</li>
	 * </ul>
	 * 
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */

	@Test
	public void ecardworkerTest_10() throws IOException, URISyntaxException {
		final TestSpy spy = new TestSpy();
		spy.setValue(false);

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<ECardWorker> mockUp = new MockUp<ECardWorker>() {
			@Mock
			private String startECardWorker(mockit.Invocation inv,
					Map<String, String> params, List<Certificate> serverCerts,
					URI tcTokenURL) throws GeneralSecurityException,
					IOException, URISyntaxException {

				spy.setValue(true);
				throw new GeneralSecurityException();

			}
		};

		try {
			final String refreshURL = ECardWorker.start(tcTokenURL);
			assertNotNull("no refresh URL", refreshURL);

			System.out.println("refreshURL: " + refreshURL);
			connectToRefreshURL(refreshURL, false);
			
		} catch (final GeneralSecurityException gse) {
			if (!spy.isValue()) {
				Logger.getGlobal().info("Test function was not called. Aborting.");
				return;
			}			
		} finally {
			mockUp.tearDown();
		}
		

	}

	/**
	 * Online authentication is triggered and an IOException occurs during the
	 * execution of the method
	 * <em>de.persoapp.core.ECardWorker.startECardWorker()</em> </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>An IOException is throwed in the method
	 * <em>de.persoapp.core.ECardWorker.startECardWorker()</em>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication fails.</li>
	 * </ul>
	 * 
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */

	@Test
	public void ecardworkerTest_11() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();
		spy.setValue(false);

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<ECardWorker> mockUp = new MockUp<ECardWorker>() {
			@Mock
			private String startECardWorker(mockit.Invocation inv,
					Map<String, String> params, List<Certificate> serverCerts,
					URI tcTokenURL) throws GeneralSecurityException,
					IOException, URISyntaxException {

				spy.setValue(true);
				throw new IOException();

			}
		};

		try {
			final String refreshURL = ECardWorker.start(tcTokenURL);
			assertNotNull("no refresh URL", refreshURL);

			System.out.println("refreshURL: " + refreshURL);
			connectToRefreshURL(refreshURL, false);
			
		} catch (final IOException ioe) {
			if (!spy.isValue()) {
				Logger.getGlobal().info("Test function was not called. Aborting.");
				return;
			}			
		} finally {
			mockUp.tearDown();
		}

	}

	/**
	 * Online authentication is triggered and an URISyntaxException occurs
	 * during the execution of the method
	 * <em>de.persoapp.core.ECardWorker.startECardWorker()</em> </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>An URISyntaxException is throwed in the method
	 * <em>de.persoapp.core.ECardWorker.startECardWorker()</em>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication fails.</li>
	 * </ul>
	 * 
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */

	@Test
	public void ecardworkerTest_12() throws IOException,	GeneralSecurityException {
		final TestSpy spy = new TestSpy();
		spy.setValue(false);

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<ECardWorker> mockUp = new MockUp<ECardWorker>() {
			@Mock
			private String startECardWorker(mockit.Invocation inv,
					Map<String, String> params, List<Certificate> serverCerts,
					URI tcTokenURL) throws GeneralSecurityException,
					IOException, URISyntaxException {

				spy.setValue(true);
				throw new URISyntaxException("", "");

			}
		};

		try {
			final String refreshURL = ECardWorker.start(tcTokenURL);
			assertNotNull("no refresh URL", refreshURL);

			System.out.println("refreshURL: " + refreshURL);
			connectToRefreshURL(refreshURL, false);
			
		} catch (final URISyntaxException use) {
			if (!spy.isValue()) {
				Logger.getGlobal().info("Test function was not called. Aborting.");
				return;
			}			
		} finally {
			mockUp.tearDown();
		}

	}

	/**
	 * Online authentication is triggered and the method
	 * <em>de.persoapp.core.ECardWorker.start()</em> is tested</br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>All parameters are checked being <b>not null</b></li>
	 * <li>The return value is checked being <b>not null</b></li>
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
	public void ecardworkerTest_13() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<ECardWorker> mockUp = new MockUp<ECardWorker>() {
			@Mock
			public Object[] start(mockit.Invocation inv, ChannelHandleType ch,
					byte sessionPSK[], URI origin, Certificate[] certs) {

				try {
					assertNotNull("ch is null.", ch);
					assertNotNull("sessionPSK is null.", sessionPSK);
					assertNotNull("origin is null.", origin);
					assertNotNull("certs is null.", certs);

					final Object[] result = inv.proceed(ch, sessionPSK, origin,
							certs);
					assertNotNull("result is null.", result);
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
			mockUp.tearDown();
			fail(spy.getStringValue());
		}

		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

	}

	/**
	 * Online authentication is triggered and the method
	 * <em>de.persoapp.core.ECardWorker.run()</em> is tested</br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
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
	public void ecardworkerTest_14() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<ECardWorker> mockUp = new MockUp<ECardWorker>() {
			@Mock
			public void run(mockit.Invocation inv) {

				try {

					inv.proceed();

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
			mockUp.tearDown();
			fail(spy.getStringValue());
		}

		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

	}

	/**
	 * Online authentication is triggered and the method
	 * <em>de.persoapp.core.ECardWorker.getCurrentState()</em> is tested</br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>The return value is checked being <b>not null</b></li>
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
	public void ecardworkerTest_15() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<ECardWorker> mockUp = new MockUp<ECardWorker>() {
			@Mock
			public Object getCurrentState(mockit.Invocation inv) {

				try {

					final Object currentState = inv.proceed();
					assertNotNull("currentState is null.", currentState);
					return currentState;

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
			mockUp.tearDown();
			fail(spy.getStringValue());
		}

		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

	}

	/**
	 * Online authentication is triggered and the method
	 * <em>de.persoapp.core.ECardWorker.run()</em> is tested</br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
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
	public void ecardworkerTest_16() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<ECardWorker> mockUp = new MockUp<ECardWorker>() {
			@Mock
			private void run(mockit.Invocation inv) {

				try {

					inv.proceed();

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
			mockUp.tearDown();
			fail(spy.getStringValue());
		}

		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

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