package de.persoapp.core.tests.core.util;

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
import java.util.HashMap;
import java.util.Map;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import de.persoapp.core.util.EcApi_TCTokenHandler;

/**
 * Testcases facing {@link EcApi_TCTokenHandler}
 * 
 * @author Rico Klimsa, 2015
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class EcApi_TCTokenHandlerTest {
	private String	serviceURL;
	
	private String	defaultPIN;
	
	private CardHandler eCardHandler;
	private WSContainer wsCtx;
	
	private static Properties properties;
	
	private MainViewEventListener mainViewEventListener;
	@Rule
	public TestWatcher watchman= new TestWatcher() {
	  @Override
	  protected void failed(Throwable e, Description description) {
		  Logger.getGlobal().severe(description.getMethodName()+"Failed!"+" "+e.getMessage());
	  }

	  @Override
	  protected void succeeded(Description description) {
		  Logger.getGlobal().info(description.getMethodName()+" " + "success!");
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
			 * @param value the value to set
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
			  * @param stringValue the stringValue to set
			  */
			public void setStringValue(final String stringValue) {
				this.stringValue = stringValue;
			}
		}
	/**
	 * Load the resource file for default pin and
	 * service url.
	 * If the resource file does not exist, it
	 * must be created by the developer per hand.
	 */
	@BeforeClass
	public static void setUp() throws FileNotFoundException, IOException {
		final String resourcePath = "/tests/resources/test_config.properties";
		final File res = new File(new File("").getAbsolutePath()+resourcePath);

		if(res.exists()) {
			properties = new Properties();
			properties.load(new FileInputStream(res));
		}
		else {
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
		
		mainViewEventListener = new MainViewEventListener(eCardHandler, mainView);
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
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.setDocumentLocator()</em>
	 * is tested</br>.
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
public void ecapi_tctokenhandlerTest_1() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void setDocumentLocator(mockit.Invocation inv, Locator locator) { 

				try {
					assertNotNull("locator is null.",locator);

					inv.proceed(locator);

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



		if(spy.getStringValue()!=null&&!spy.getStringValue().trim().isEmpty()) { 
mockUp.tearDown();
			fail(spy.getStringValue());
		}


		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

}	/**
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.startDocument()</em>
	 * is tested</br>.
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
public void ecapi_tctokenhandlerTest_2() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void startDocument(mockit.Invocation inv) throws SAXException { 

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



		if(spy.getStringValue()!=null&&!spy.getStringValue().trim().isEmpty()) { 
mockUp.tearDown();
			fail(spy.getStringValue());
		}


		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

}	/**
  * Online authentication is triggered and an SAXException occurs during the execution of the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.startDocument()</em>
	 * </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
 	* <li>An SAXException is throwed in the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.startDocument()</em>
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
public void ecapi_tctokenhandlerTest_3() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();
 	spy.setValue(false);


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void startDocument(mockit.Invocation inv) throws SAXException { 

 		spy.setValue(true);
 			throw new SAXException();
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



 	if(!spy.isValue()) {
 	Logger.getGlobal().info("Test function was not called. Aborting.");
 	return; 
 	}


		System.out.println("refreshURL: " + refreshURL);
 	connectToRefreshURL(refreshURL, false);
		mockUp.tearDown();

}	/**
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.endDocument()</em>
	 * is tested</br>.
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
public void ecapi_tctokenhandlerTest_4() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void endDocument(mockit.Invocation inv) throws SAXException { 

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



		if(spy.getStringValue()!=null&&!spy.getStringValue().trim().isEmpty()) { 
mockUp.tearDown();
			fail(spy.getStringValue());
		}


		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

}	/**
  * Online authentication is triggered and an SAXException occurs during the execution of the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.endDocument()</em>
	 * </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
 	* <li>An SAXException is throwed in the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.endDocument()</em>
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
public void ecapi_tctokenhandlerTest_5() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();
 	spy.setValue(false);


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void endDocument(mockit.Invocation inv) throws SAXException { 

 		spy.setValue(true);
 			throw new SAXException();
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



 	if(!spy.isValue()) {
 	Logger.getGlobal().info("Test function was not called. Aborting.");
 	return; 
 	}


		System.out.println("refreshURL: " + refreshURL);
 	connectToRefreshURL(refreshURL, false);
		mockUp.tearDown();

}	/**
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.startPrefixMapping()</em>
	 * is tested</br>.
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
public void ecapi_tctokenhandlerTest_6() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void startPrefixMapping(mockit.Invocation inv, String prefix, String uri) throws SAXException { 

				try {
					assertNotNull("prefix is null.",prefix);
assertNotNull("uri is null.",uri);

					inv.proceed(prefix, uri);

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



		if(spy.getStringValue()!=null&&!spy.getStringValue().trim().isEmpty()) { 
mockUp.tearDown();
			fail(spy.getStringValue());
		}


		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

}	/**
  * Online authentication is triggered and an SAXException occurs during the execution of the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.startPrefixMapping()</em>
	 * </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
 	* <li>An SAXException is throwed in the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.startPrefixMapping()</em>
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
public void ecapi_tctokenhandlerTest_7() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();
 	spy.setValue(false);


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void startPrefixMapping(mockit.Invocation inv, String prefix, String uri) throws SAXException { 

 		spy.setValue(true);
 			throw new SAXException();
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



 	if(!spy.isValue()) {
 	Logger.getGlobal().info("Test function was not called. Aborting.");
 	return; 
 	}


		System.out.println("refreshURL: " + refreshURL);
 	connectToRefreshURL(refreshURL, false);
		mockUp.tearDown();

}	/**
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.endPrefixMapping()</em>
	 * is tested</br>.
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
public void ecapi_tctokenhandlerTest_8() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void endPrefixMapping(mockit.Invocation inv, String prefix) throws SAXException { 

				try {
					assertNotNull("prefix is null.",prefix);

					inv.proceed(prefix);

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



		if(spy.getStringValue()!=null&&!spy.getStringValue().trim().isEmpty()) { 
mockUp.tearDown();
			fail(spy.getStringValue());
		}


		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

}	/**
  * Online authentication is triggered and an SAXException occurs during the execution of the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.endPrefixMapping()</em>
	 * </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
 	* <li>An SAXException is throwed in the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.endPrefixMapping()</em>
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
public void ecapi_tctokenhandlerTest_9() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();
 	spy.setValue(false);


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void endPrefixMapping(mockit.Invocation inv, String prefix) throws SAXException { 

 		spy.setValue(true);
 			throw new SAXException();
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



 	if(!spy.isValue()) {
 	Logger.getGlobal().info("Test function was not called. Aborting.");
 	return; 
 	}


		System.out.println("refreshURL: " + refreshURL);
 	connectToRefreshURL(refreshURL, false);
		mockUp.tearDown();

}	/**
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.startElement()</em>
	 * is tested</br>.
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
public void ecapi_tctokenhandlerTest_10() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void startElement(mockit.Invocation inv, String uri, String localName, String qName, Attributes atts) throws SAXException { 

				try {
					assertNotNull("uri is null.",uri);
assertNotNull("localName is null.",localName);
assertNotNull("qName is null.",qName);
assertNotNull("atts is null.",atts);

					inv.proceed(uri, localName, qName, atts);

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



		if(spy.getStringValue()!=null&&!spy.getStringValue().trim().isEmpty()) { 
mockUp.tearDown();
			fail(spy.getStringValue());
		}


		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

}	/**
  * Online authentication is triggered and an SAXException occurs during the execution of the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.startElement()</em>
	 * </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
 	* <li>An SAXException is throwed in the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.startElement()</em>
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
public void ecapi_tctokenhandlerTest_11() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();
 	spy.setValue(false);


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void startElement(mockit.Invocation inv, String uri, String localName, String qName, Attributes atts) throws SAXException { 

 		spy.setValue(true);
 			throw new SAXException();
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



 	if(!spy.isValue()) {
 	Logger.getGlobal().info("Test function was not called. Aborting.");
 	return; 
 	}


		System.out.println("refreshURL: " + refreshURL);
 	connectToRefreshURL(refreshURL, false);
		mockUp.tearDown();

}	/**
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.endElement()</em>
	 * is tested</br>.
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
public void ecapi_tctokenhandlerTest_12() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void endElement(mockit.Invocation inv, String uri, String localName, String qName) throws SAXException { 

				try {
					assertNotNull("uri is null.",uri);
assertNotNull("localName is null.",localName);
assertNotNull("qName is null.",qName);

					inv.proceed(uri, localName, qName);

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



		if(spy.getStringValue()!=null&&!spy.getStringValue().trim().isEmpty()) { 
mockUp.tearDown();
			fail(spy.getStringValue());
		}


		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

}	/**
  * Online authentication is triggered and an SAXException occurs during the execution of the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.endElement()</em>
	 * </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
 	* <li>An SAXException is throwed in the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.endElement()</em>
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
public void ecapi_tctokenhandlerTest_13() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();
 	spy.setValue(false);


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void endElement(mockit.Invocation inv, String uri, String localName, String qName) throws SAXException { 

 		spy.setValue(true);
 			throw new SAXException();
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



 	if(!spy.isValue()) {
 	Logger.getGlobal().info("Test function was not called. Aborting.");
 	return; 
 	}


		System.out.println("refreshURL: " + refreshURL);
 	connectToRefreshURL(refreshURL, false);
		mockUp.tearDown();

}	/**
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.characters()</em>
	 * is tested</br>.
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
public void ecapi_tctokenhandlerTest_14() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void characters(mockit.Invocation inv, char[] ch, int start, int length) throws SAXException { 

				try {
					assertNotNull("ch is null.",ch);
assertNotNull("start is null.",start);
assertNotNull("length is null.",length);

					inv.proceed(ch, start, length);

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



		if(spy.getStringValue()!=null&&!spy.getStringValue().trim().isEmpty()) { 
mockUp.tearDown();
			fail(spy.getStringValue());
		}


		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

}	/**
  * Online authentication is triggered and an SAXException occurs during the execution of the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.characters()</em>
	 * </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
 	* <li>An SAXException is throwed in the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.characters()</em>
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
public void ecapi_tctokenhandlerTest_15() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();
 	spy.setValue(false);


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void characters(mockit.Invocation inv, char[] ch, int start, int length) throws SAXException { 

 		spy.setValue(true);
 			throw new SAXException();
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



 	if(!spy.isValue()) {
 	Logger.getGlobal().info("Test function was not called. Aborting.");
 	return; 
 	}


		System.out.println("refreshURL: " + refreshURL);
 	connectToRefreshURL(refreshURL, false);
		mockUp.tearDown();

}	/**
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.ignorableWhitespace()</em>
	 * is tested</br>.
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
public void ecapi_tctokenhandlerTest_16() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void ignorableWhitespace(mockit.Invocation inv, char[] ch, int start, int length) throws SAXException { 

				try {
					assertNotNull("ch is null.",ch);
assertNotNull("start is null.",start);
assertNotNull("length is null.",length);

					inv.proceed(ch, start, length);

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



		if(spy.getStringValue()!=null&&!spy.getStringValue().trim().isEmpty()) { 
mockUp.tearDown();
			fail(spy.getStringValue());
		}


		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

}	/**
  * Online authentication is triggered and an SAXException occurs during the execution of the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.ignorableWhitespace()</em>
	 * </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
 	* <li>An SAXException is throwed in the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.ignorableWhitespace()</em>
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
public void ecapi_tctokenhandlerTest_17() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();
 	spy.setValue(false);


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void ignorableWhitespace(mockit.Invocation inv, char[] ch, int start, int length) throws SAXException { 

 		spy.setValue(true);
 			throw new SAXException();
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



 	if(!spy.isValue()) {
 	Logger.getGlobal().info("Test function was not called. Aborting.");
 	return; 
 	}


		System.out.println("refreshURL: " + refreshURL);
 	connectToRefreshURL(refreshURL, false);
		mockUp.tearDown();

}	/**
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.processingInstruction()</em>
	 * is tested</br>.
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
public void ecapi_tctokenhandlerTest_18() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void processingInstruction(mockit.Invocation inv, String target, String data) throws SAXException { 

				try {
					assertNotNull("target is null.",target);
assertNotNull("data is null.",data);

					inv.proceed(target, data);

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



		if(spy.getStringValue()!=null&&!spy.getStringValue().trim().isEmpty()) { 
mockUp.tearDown();
			fail(spy.getStringValue());
		}


		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

}	/**
  * Online authentication is triggered and an SAXException occurs during the execution of the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.processingInstruction()</em>
	 * </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
 	* <li>An SAXException is throwed in the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.processingInstruction()</em>
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
public void ecapi_tctokenhandlerTest_19() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();
 	spy.setValue(false);


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void processingInstruction(mockit.Invocation inv, String target, String data) throws SAXException { 

 		spy.setValue(true);
 			throw new SAXException();
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



 	if(!spy.isValue()) {
 	Logger.getGlobal().info("Test function was not called. Aborting.");
 	return; 
 	}


		System.out.println("refreshURL: " + refreshURL);
 	connectToRefreshURL(refreshURL, false);
		mockUp.tearDown();

}	/**
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.skippedEntity()</em>
	 * is tested</br>.
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
public void ecapi_tctokenhandlerTest_20() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void skippedEntity(mockit.Invocation inv, String name) throws SAXException { 

				try {
					assertNotNull("name is null.",name);

					inv.proceed(name);

				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(), ae);
				}
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



		if(spy.getStringValue()!=null&&!spy.getStringValue().trim().isEmpty()) { 
mockUp.tearDown();
			fail(spy.getStringValue());
		}


		System.out.println("refreshURL: " + refreshURL);
		connectToRefreshURL(refreshURL, true);
		mockUp.tearDown();

}	/**
  * Online authentication is triggered and an SAXException occurs during the execution of the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.skippedEntity()</em>
	 * </br>.
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
 	* <li>An SAXException is throwed in the method <em>de.persoapp.core.util.EcApi_TCTokenHandler.skippedEntity()</em>
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
public void ecapi_tctokenhandlerTest_21() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();
 	spy.setValue(false);


		final URL tcTokenURL = new URL(serviceURL);


MockUp<EcApi_TCTokenHandler> mockUp = new MockUp<EcApi_TCTokenHandler>() {
@Mock
public void skippedEntity(mockit.Invocation inv, String name) throws SAXException { 

 		spy.setValue(true);
 			throw new SAXException();
 

}
 };

		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);



 	if(!spy.isValue()) {
 	Logger.getGlobal().info("Test function was not called. Aborting.");
 	return; 
 	}


		System.out.println("refreshURL: " + refreshURL);
 	connectToRefreshURL(refreshURL, false);
		mockUp.tearDown();

}
		private void connectToRefreshURL(final String refreshURL, boolean success) throws IOException {
		
		if(success) {
			assertFalse("process failed", refreshURL.toLowerCase().indexOf("resultmajor=ok") < 0);			
		} else {
			assertFalse("process succeeded",refreshURL.toLowerCase().indexOf("resultmajor=ok") > 0);
		}

		final URL refresh = new URL(refreshURL);
		final HttpURLConnection uc = (HttpsURLConnection) Util.openURL(refresh);
		uc.setInstanceFollowRedirects(true);
		final Object content = uc.getContent();
		System.out.println("HTTP Response " + uc.getResponseCode() + " " + uc.getResponseMessage());
		if (content instanceof InputStream) {
			final Scanner scanner = new Scanner((InputStream) content, "UTF-8").useDelimiter("\\A");
			System.out.println(scanner.next());
			scanner.close();
		} else {
			System.out.println(content);
		}
	}

		/**
		 * Reset the ECardWorker. Delay test execution to prevent race 
		 * condition between testcases.
		 * 
		 * @throws NoSuchFieldException
		 * @throws SecurityException
		 * @throws IllegalArgumentException
		 * @throws IllegalAccessException
		 */
		 @After
		 public synchronized void cleanUp() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		 final long end = System.currentTimeMillis() + 6000;

		 while(System.currentTimeMillis()<end) {
		 // wait to prevent race condition between testcases.
		 }

	final Field field = ECardWorker.class.getDeclaredField("mainView");
field.setAccessible(true);
field.set(null, null);
field.setAccessible(false);
ECardWorker.init(null, null, null);


	}

}