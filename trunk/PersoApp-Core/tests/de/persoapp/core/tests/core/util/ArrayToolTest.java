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
import de.persoapp.core.card.TransportProvider;
import de.persoapp.core.card.CCID;
import de.persoapp.core.paos.PAOSInitiator;
import de.persoapp.core.paos.PAOSInitiatorFactory;
import de.persoapp.core.paos.MiniHttpClient;
import de.persoapp.core.util.ArrayTool;

/**
 * Testcases facing {@link ArrayTool}
 * 
 * @author Rico Klimsa, 2015
 */
@RunWith(JMockit.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ArrayToolTest {
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
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.ArrayTool.arrayconcat()</em>
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
public void arraytoolTest_1() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<ArrayTool> mockUp = new MockUp<ArrayTool>() {
@Mock
public byte[] arrayconcat(mockit.Invocation inv, byte[] b1, int offset1, int length1, byte[] b2, int offset2, int length2) { 

				try {
					assertNotNull("b1 is null.",b1);
assertNotNull("offset1 is null.",offset1);
assertNotNull("length1 is null.",length1);
assertNotNull("b2 is null.",b2);
assertNotNull("offset2 is null.",offset2);
assertNotNull("length2 is null.",length2);

					final byte[] result = inv.proceed(b1, offset1, length1, b2, offset2, length2);
assertNotNull("result is null.",result);
return result;

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
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.ArrayTool.arrayconcat()</em>
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
public void arraytoolTest_2() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<ArrayTool> mockUp = new MockUp<ArrayTool>() {
@Mock
public byte[] arrayconcat(mockit.Invocation inv, byte[] b1, byte[] b2) { 

				try {
					assertNotNull("b1 is null.",b1);
assertNotNull("b2 is null.",b2);

					final byte[] result = inv.proceed(b1, b2);
assertNotNull("result is null.",result);
return result;

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
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.ArrayTool.subArray()</em>
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
public void arraytoolTest_3() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<ArrayTool> mockUp = new MockUp<ArrayTool>() {
@Mock
public byte[] subArray(mockit.Invocation inv, byte[] b1, int offset, int length) { 

				try {
					assertNotNull("b1 is null.",b1);
assertNotNull("offset is null.",offset);
assertNotNull("length is null.",length);

					final byte[] result = inv.proceed(b1, offset, length);
assertNotNull("result is null.",result);
return result;

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
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.ArrayTool.arrayequal()</em>
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
public void arraytoolTest_4() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<ArrayTool> mockUp = new MockUp<ArrayTool>() {
@Mock
public boolean arrayequal(mockit.Invocation inv, byte[] b1, byte[] b2) { 

				try {
					assertNotNull("b1 is null.",b1);
assertNotNull("b2 is null.",b2);

					final boolean result = inv.proceed(b1, b2);
assertNotNull("result is null.",result);
return result;

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
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.ArrayTool.insertShortInByteArray()</em>
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
public void arraytoolTest_5() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<ArrayTool> mockUp = new MockUp<ArrayTool>() {
@Mock
public void insertShortInByteArray(mockit.Invocation inv, byte[] b, int ofs, int s) { 

				try {
					assertNotNull("b is null.",b);
assertNotNull("ofs is null.",ofs);
assertNotNull("s is null.",s);

					inv.proceed(b, ofs, s);

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
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.ArrayTool.insertIntInByteArray()</em>
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
public void arraytoolTest_6() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<ArrayTool> mockUp = new MockUp<ArrayTool>() {
@Mock
public void insertIntInByteArray(mockit.Invocation inv, byte[] b, int ofs, int i) { 

				try {
					assertNotNull("b is null.",b);
assertNotNull("ofs is null.",ofs);
assertNotNull("i is null.",i);

					inv.proceed(b, ofs, i);

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
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.ArrayTool.insertLongInByteArray()</em>
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
public void arraytoolTest_7() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<ArrayTool> mockUp = new MockUp<ArrayTool>() {
@Mock
public void insertLongInByteArray(mockit.Invocation inv, byte[] b, int ofs, long l) { 

				try {
					assertNotNull("b is null.",b);
assertNotNull("ofs is null.",ofs);
assertNotNull("l is null.",l);

					inv.proceed(b, ofs, l);

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
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.ArrayTool.createShortfromByteArray()</em>
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
public void arraytoolTest_8() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<ArrayTool> mockUp = new MockUp<ArrayTool>() {
@Mock
public short createShortfromByteArray(mockit.Invocation inv, byte[] b, int ofs) { 

				try {
					assertNotNull("b is null.",b);
assertNotNull("ofs is null.",ofs);

					final short result = inv.proceed(b, ofs);
assertNotNull("result is null.",result);
return result;

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
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.ArrayTool.createIntfromByteArray()</em>
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
public void arraytoolTest_9() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<ArrayTool> mockUp = new MockUp<ArrayTool>() {
@Mock
public int createIntfromByteArray(mockit.Invocation inv, byte[] b, int ofs) { 

				try {
					assertNotNull("b is null.",b);
assertNotNull("ofs is null.",ofs);

					final int result = inv.proceed(b, ofs);
assertNotNull("result is null.",result);
return result;

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
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.ArrayTool.createLongfromByteArray()</em>
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
public void arraytoolTest_10() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<ArrayTool> mockUp = new MockUp<ArrayTool>() {
@Mock
public long createLongfromByteArray(mockit.Invocation inv, byte[] b, int ofs) { 

				try {
					assertNotNull("b is null.",b);
assertNotNull("ofs is null.",ofs);

					final long result = inv.proceed(b, ofs);
assertNotNull("result is null.",result);
return result;

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
	 * Online authentication is triggered and the method <em>de.persoapp.core.util.ArrayTool.overwrite()</em>
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
public void arraytoolTest_11() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();


		final URL tcTokenURL = new URL(serviceURL);


MockUp<ArrayTool> mockUp = new MockUp<ArrayTool>() {
@Mock
public void overwrite(mockit.Invocation inv, byte[] b) { 

				try {
					assertNotNull("b is null.",b);

					inv.proceed(b);

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