package de.persoapp.core.tests.core.client;

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
import java.util.List;
import java.util.Properties;
import java.util.Scanner;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

import mockit.Mock;
import mockit.MockUp;
import mockit.integration.junit4.JMockit;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
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

import java.util.Date;

import de.persoapp.core.client.EAC_Info;

/**
 * Testcases facing {@link EAC_Info}
 * 
 * @author Rico Klimsa, 2015
 */
@RunWith(JMockit.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EAC_InfoTest {
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
	 * <em>de.persoapp.core.client.EAC_Info.newInstance()</em> is tested</br>.
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
	public void eac_infoTest_1() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public EAC_Info newInstance(mockit.Invocation inv,
					List<byte[]> cvcerts, byte[] cvcertDescription,
					String transactionInfo, byte[] requiredCHAT,
					byte[] optionalCHAT, byte[] auxData) {

				try {
					assertNotNull("cvcerts is null.", cvcerts);
					assertNotNull("cvcertDescription is null.",
							cvcertDescription);
					assertNotNull("auxData is null.", auxData);

					final EAC_Info result = inv.proceed(cvcerts,
							cvcertDescription, transactionInfo, requiredCHAT,
							optionalCHAT, auxData);
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
	 * <em>de.persoapp.core.client.EAC_Info.isValid()</em> is tested</br>.
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
	public void eac_infoTest_2() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public boolean isValid(mockit.Invocation inv) {

				try {

					final boolean result = inv.proceed();
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
	 * <em>de.persoapp.core.client.EAC_Info.cvDate2Date()</em> is tested</br>.
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
	public void eac_infoTest_3() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			private Date cvDate2Date(mockit.Invocation inv, byte[] cvDate,
					boolean endOfDay) {

				try {
					assertNotNull("cvDate is null.", cvDate);
					assertNotNull("endOfDay is null.", endOfDay);

					final Date result = inv.proceed(cvDate, endOfDay);
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
	 * <em>de.persoapp.core.client.EAC_Info.initAuxData()</em> is tested</br>.
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
	public void eac_infoTest_4() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			private void initAuxData(mockit.Invocation inv, byte[] auxData) {

				try {
					assertNotNull("auxData is null.", auxData);

					inv.proceed(auxData);

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
	 * <em>de.persoapp.core.client.EAC_Info.initDescription()</em> is
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
	public void eac_infoTest_5() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			private void initDescription(mockit.Invocation inv, byte[] cvcert,
					byte[] certDescription) throws IOException {

				try {
					assertNotNull("cvcert is null.", cvcert);
					assertNotNull("certDescription is null.", certDescription);

					inv.proceed(cvcert, certDescription);

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
	 * <em>de.persoapp.core.client.EAC_Info.initDescription()</em> </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>An IOException is throwed in the method
	 * <em>de.persoapp.core.client.EAC_Info.initDescription()</em>
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
	public void eac_infoTest_6() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();
		spy.setValue(false);

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			private void initDescription(mockit.Invocation inv, byte[] cvcert,
					byte[] certDescription) throws IOException {

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
	 * <em>de.persoapp.core.client.EAC_Info.initCHAT()</em> is tested</br>.
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
	public void eac_infoTest_7() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			private int initCHAT(mockit.Invocation inv, byte[] origCHAT) {

				try {
					assertNotNull("origCHAT is null.", origCHAT);

					final int result = inv.proceed(origCHAT);
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
	 * <em>de.persoapp.core.client.EAC_Info.getTerminalCertificate()</em> is
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
	public void eac_infoTest_8() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public byte[] getTerminalCertificate(mockit.Invocation inv) {

				try {

					final byte[] terminalCertificate = inv.proceed();
					assertNotNull("terminalCertificate is null.",
							terminalCertificate);
					return terminalCertificate;

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
	 * <em>de.persoapp.core.client.EAC_Info.getTerminalDescription()</em> is
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
	public void eac_infoTest_9() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public byte[] getTerminalDescription(mockit.Invocation inv) {

				try {

					final byte[] terminalDescription = inv.proceed();
					assertNotNull("terminalDescription is null.",
							terminalDescription);
					return terminalDescription;

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
	 * <em>de.persoapp.core.client.EAC_Info.getAuthorityReference()</em> is
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
	public void eac_infoTest_10() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public String getAuthorityReference(mockit.Invocation inv) {

				try {

					final String authorityReference = inv.proceed();
					assertNotNull("authorityReference is null.",
							authorityReference);
					return authorityReference;

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
	 * <em>de.persoapp.core.client.EAC_Info.getHolderReference()</em> is
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
	public void eac_infoTest_11() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public String getHolderReference(mockit.Invocation inv) {

				try {

					final String holderReference = inv.proceed();
					assertNotNull("holderReference is null.", holderReference);
					return holderReference;

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
	 * <em>de.persoapp.core.client.EAC_Info.getEffectiveDate()</em> is
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
	public void eac_infoTest_12() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public Date getEffectiveDate(mockit.Invocation inv) {

				try {

					final Date effectiveDate = inv.proceed();
					assertNotNull("effectiveDate is null.", effectiveDate);
					return effectiveDate;

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
	 * <em>de.persoapp.core.client.EAC_Info.getExpirationDate()</em> is
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
	public void eac_infoTest_13() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public Date getExpirationDate(mockit.Invocation inv) {

				try {

					final Date expirationDate = inv.proceed();
					assertNotNull("expirationDate is null.", expirationDate);
					return expirationDate;

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
	 * <em>de.persoapp.core.client.EAC_Info.getTransactionInfo()</em> is
	 * tested</br>. <b>Preconditions:</b>
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
	public void eac_infoTest_14() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public String getTransactionInfo(mockit.Invocation inv) {

				try {

					final String transactionInfo = inv.proceed();
					return transactionInfo;

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
	 * <em>de.persoapp.core.client.EAC_Info.getDescriptionType()</em> is
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
	public void eac_infoTest_15() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public String getDescriptionType(mockit.Invocation inv) {

				try {

					final String descriptionType = inv.proceed();
					assertNotNull("descriptionType is null.", descriptionType);
					return descriptionType;

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
	 * <em>de.persoapp.core.client.EAC_Info.getIssuerName()</em> is tested</br>.
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
	public void eac_infoTest_16() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public String getIssuerName(mockit.Invocation inv) {

				try {

					final String issuerName = inv.proceed();
					assertNotNull("issuerName is null.", issuerName);
					return issuerName;

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
	 * <em>de.persoapp.core.client.EAC_Info.getIssuerURL()</em> is tested</br>.
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
	public void eac_infoTest_17() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public String getIssuerURL(mockit.Invocation inv) {

				try {

					final String issuerURL = inv.proceed();
					assertNotNull("issuerURL is null.", issuerURL);
					return issuerURL;

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
	 * <em>de.persoapp.core.client.EAC_Info.getSubjectName()</em> is
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
	public void eac_infoTest_18() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public String getSubjectName(mockit.Invocation inv) {

				try {

					final String subjectName = inv.proceed();
					assertNotNull("subjectName is null.", subjectName);
					return subjectName;

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
	 * <em>de.persoapp.core.client.EAC_Info.getSubjectURL()</em> is tested</br>.
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
	public void eac_infoTest_19() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public String getSubjectURL(mockit.Invocation inv) {

				try {

					final String subjectURL = inv.proceed();
					assertNotNull("subjectURL is null.", subjectURL);
					return subjectURL;

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
	 * <em>de.persoapp.core.client.EAC_Info.getTermsOfUsage()</em> is
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
	public void eac_infoTest_20() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public String getTermsOfUsage(mockit.Invocation inv) {

				try {

					final String termsOfUsage = inv.proceed();
					assertNotNull("termsOfUsage is null.", termsOfUsage);
					return termsOfUsage;

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
	 * <em>de.persoapp.core.client.EAC_Info.getRedirectURL()</em> is
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
	public void eac_infoTest_21() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public String getRedirectURL(mockit.Invocation inv) {

				try {

					final String redirectURL = inv.proceed();
					assertNotNull("redirectURL is null.", redirectURL);
					return redirectURL;

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
	 * <em>de.persoapp.core.client.EAC_Info.getCertificateHashes()</em> is
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
	public void eac_infoTest_22() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public byte[][] getCertificateHashes(mockit.Invocation inv) {

				try {

					final byte[][] certificateHashes = inv.proceed();
					assertNotNull("certificateHashes is null.",
							certificateHashes);
					return certificateHashes;

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
	 * <em>de.persoapp.core.client.EAC_Info.getRequiredChat()</em> is
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
	public void eac_infoTest_23() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public long getRequiredChat(mockit.Invocation inv) {

				try {

					final long requiredChat = inv.proceed();
					assertNotNull("requiredChat is null.", requiredChat);
					return requiredChat;

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
	 * <em>de.persoapp.core.client.EAC_Info.getOptionalChat()</em> is
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
	public void eac_infoTest_24() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public long getOptionalChat(mockit.Invocation inv) {

				try {

					final long optionalChat = inv.proceed();
					assertNotNull("optionalChat is null.", optionalChat);
					return optionalChat;

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
	 * <em>de.persoapp.core.client.EAC_Info.getAuxiliaryData()</em> is
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
	public void eac_infoTest_25() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public byte[] getAuxiliaryData(mockit.Invocation inv) {

				try {

					final byte[] auxiliaryData = inv.proceed();
					assertNotNull("auxiliaryData is null.", auxiliaryData);
					return auxiliaryData;

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
	 * <em>de.persoapp.core.client.EAC_Info.getVerifyCommunityID()</em> is
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
	public void eac_infoTest_26() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public String getVerifyCommunityID(mockit.Invocation inv) {

				try {

					final String verifyCommunityID = inv.proceed();
					assertNotNull("verifyCommunityID is null.",
							verifyCommunityID);
					return verifyCommunityID;

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
	 * <em>de.persoapp.core.client.EAC_Info.getVerifyAge()</em> is tested</br>.
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
	public void eac_infoTest_27() throws IOException, URISyntaxException,
			GeneralSecurityException {
		final TestSpy spy = new TestSpy();

		final URL tcTokenURL = new URL(serviceURL);

		MockUp<EAC_Info> mockUp = new MockUp<EAC_Info>() {
			@Mock
			public long getVerifyAge(mockit.Invocation inv) {

				try {

					final long verifyAge = inv.proceed();
					assertNotNull("verifyAge is null.", verifyAge);
					return verifyAge;

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