package de.persoapp.core.tests.core.paos;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

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
import de.persoapp.core.paos.PAOSInitiator;
import de.persoapp.core.paos.PAOSInitiatorFactory;
import de.persoapp.core.paos.MiniHttpClient;
import iso.std.iso_iec._24727.tech.schema.ResponseType;
import java.net.URI;
import java.security.cert.Certificate;
import javax.net.ssl.SSLSession;
import javax.xml.bind.JAXBContext;

/**
 * Testcases facing {@link PAOSInitiator}
 * 
 * @author Rico Klimsa, 2015
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class PAOSInitiatorTest {
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
	 * <em>de.persoapp.core.paos.PAOSInitiator.setPaosInitiatorFactory()</em> is
	 * tested</br>. <b>Preconditions:</b>
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
	public void paosinitiatorTest_1() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<PAOSInitiator> mockUp = new MockUp<PAOSInitiator>() {
			@Mock
			public void setPaosInitiatorFactory(mockit.Invocation inv,
					PAOSInitiatorFactory paosInitiatorFactory) {

				try {
					assertNotNull("paosInitiatorFactory is null.",
							paosInitiatorFactory);

					inv.proceed(paosInitiatorFactory);

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
	 * <em>de.persoapp.core.paos.PAOSInitiator.getPreStartInstance()</em> is
	 * tested</br>. <b>Preconditions:</b>
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
	public void paosinitiatorTest_2() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<PAOSInitiator> mockUp = new MockUp<PAOSInitiator>() {
			@Mock
			public PAOSInitiator getPreStartInstance(mockit.Invocation inv) {

				try {

					final PAOSInitiator preStartInstance = inv.proceed();
					assertNotNull("preStartInstance is null.", preStartInstance);
					return preStartInstance;

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
	 * <em>de.persoapp.core.paos.PAOSInitiator.createJAXBContext()</em> is
	 * tested</br>. <b>Preconditions:</b>
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
	public void paosinitiatorTest_3() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<PAOSInitiator> mockUp = new MockUp<PAOSInitiator>() {
			@Mock
			protected JAXBContext createJAXBContext(mockit.Invocation inv) {

				try {

					final JAXBContext result = inv.proceed();
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
	 * <em>de.persoapp.core.paos.PAOSInitiator.getInstance()</em> is
	 * tested</br>. <b>Preconditions:</b>
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
	public void paosinitiatorTest_4() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<PAOSInitiator> mockUp = new MockUp<PAOSInitiator>() {
			@Mock
			public PAOSInitiator getInstance(mockit.Invocation inv,
					WSContainer wsCtx, URI endpoint, String sessionID,
					byte[] pskKey) throws IOException {

				try {
					assertNotNull("wsCtx is null.", wsCtx);
					assertNotNull("endpoint is null.", endpoint);
					assertNotNull("sessionID is null.", sessionID);
					assertNotNull("pskKey is null.", pskKey);

					final PAOSInitiator instance = inv.proceed(wsCtx, endpoint,
							sessionID, pskKey);
					assertNotNull("instance is null.", instance);
					return instance;

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
	 * <em>de.persoapp.core.paos.PAOSInitiator.getInstance()</em> </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>An IOException is throwed in the method
	 * <em>de.persoapp.core.paos.PAOSInitiator.getInstance()</em>
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
	public void paosinitiatorTest_5() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();
		spy.setValue(false);

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<PAOSInitiator> mockUp = new MockUp<PAOSInitiator>() {
			@Mock
			public PAOSInitiator getInstance(mockit.Invocation inv,
					WSContainer wsCtx, URI endpoint, String sessionID,
					byte[] pskKey) throws IOException {

				spy.setValue(true);
				throw new IOException();

			}
		};

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);

		if (!spy.isValue()) {
			Logger.getGlobal().info("Test function was not called. Aborting.");
			return;
		}

		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, false);
		mockUp.tearDown();

	}

	/**
	 * Online authentication is triggered and the method
	 * <em>de.persoapp.core.paos.PAOSInitiator.createClient()</em> is
	 * tested</br>. <b>Preconditions:</b>
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
	public void paosinitiatorTest_6() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<PAOSInitiator> mockUp = new MockUp<PAOSInitiator>() {
			@Mock
			private MiniHttpClient createClient(mockit.Invocation inv)
					throws IOException {

				try {

					final MiniHttpClient result = inv.proceed();
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
	 * <em>de.persoapp.core.paos.PAOSInitiator.createClient()</em> </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>An IOException is throwed in the method
	 * <em>de.persoapp.core.paos.PAOSInitiator.createClient()</em>
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
	public void paosinitiatorTest_7() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();
		spy.setValue(false);

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<PAOSInitiator> mockUp = new MockUp<PAOSInitiator>() {
			@Mock
			private MiniHttpClient createClient(mockit.Invocation inv)
					throws IOException {

				spy.setValue(true);
				throw new IOException();

			}
		};

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);

		if (!spy.isValue()) {
			Logger.getGlobal().info("Test function was not called. Aborting.");
			return;
		}

		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, false);
		mockUp.tearDown();

	}

	/**
	 * Online authentication is triggered and the method
	 * <em>de.persoapp.core.paos.PAOSInitiator.getPeerCertificate()</em> is
	 * tested</br>. <b>Preconditions:</b>
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
	public void paosinitiatorTest_8() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<PAOSInitiator> mockUp = new MockUp<PAOSInitiator>() {
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
	 * <em>de.persoapp.core.paos.PAOSInitiator.getSSLSession()</em> is
	 * tested</br>. <b>Preconditions:</b>
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
	public void paosinitiatorTest_9() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<PAOSInitiator> mockUp = new MockUp<PAOSInitiator>() {
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
	 * <em>de.persoapp.core.paos.PAOSInitiator.createMessageID()</em> is
	 * tested</br>. <b>Preconditions:</b>
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
	public void paosinitiatorTest_10() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<PAOSInitiator> mockUp = new MockUp<PAOSInitiator>() {
			@Mock
			private String createMessageID(mockit.Invocation inv) {

				try {

					final String result = inv.proceed();
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
	 * <em>de.persoapp.core.paos.PAOSInitiator.dispatch()</em> is tested</br>.
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
	public void paosinitiatorTest_11() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<PAOSInitiator> mockUp = new MockUp<PAOSInitiator>() {
			@Mock
			public Object[] dispatch(mockit.Invocation inv, Object in)
					throws IOException {

				try {
					assertNotNull("in is null.", in);

					final Object[] result = inv.proceed(in);
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
	 * <em>de.persoapp.core.paos.PAOSInitiator.dispatch()</em> </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>An IOException is throwed in the method
	 * <em>de.persoapp.core.paos.PAOSInitiator.dispatch()</em>
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
	public void paosinitiatorTest_12() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();
		spy.setValue(false);

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<PAOSInitiator> mockUp = new MockUp<PAOSInitiator>() {
			@Mock
			public Object[] dispatch(mockit.Invocation inv, Object in)
					throws IOException {

				spy.setValue(true);
				throw new IOException();

			}
		};

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);

		if (!spy.isValue()) {
			Logger.getGlobal().info("Test function was not called. Aborting.");
			return;
		}

		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, false);
		mockUp.tearDown();

	}

	/**
	 * Online authentication is triggered and the method
	 * <em>de.persoapp.core.paos.PAOSInitiator.start()</em> is tested</br>.
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
	public void paosinitiatorTest_13() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<PAOSInitiator> mockUp = new MockUp<PAOSInitiator>() {
			@Mock
			public ResponseType start(mockit.Invocation inv,
					byte[] contextHandle, byte[] slotHandle) throws IOException {

				try {
					assertNotNull("contextHandle is null.", contextHandle);
					assertNotNull("slotHandle is null.", slotHandle);

					final ResponseType result = inv.proceed(contextHandle,
							slotHandle);
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
	 * <em>de.persoapp.core.paos.PAOSInitiator.start()</em> </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>An IOException is throwed in the method
	 * <em>de.persoapp.core.paos.PAOSInitiator.start()</em>
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
	public void paosinitiatorTest_14() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();
		spy.setValue(false);

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<PAOSInitiator> mockUp = new MockUp<PAOSInitiator>() {
			@Mock
			public ResponseType start(mockit.Invocation inv,
					byte[] contextHandle, byte[] slotHandle) throws IOException {

				spy.setValue(true);
				throw new IOException();

			}
		};

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);

		if (!spy.isValue()) {
			Logger.getGlobal().info("Test function was not called. Aborting.");
			return;
		}

		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, false);
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