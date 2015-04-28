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
 * @version 1.0, 17.03.2015 10:36:43
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

package de.persoapp.core.tests.core.ws.engine;

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
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.namespace.QName;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import mockit.Mock;
import mockit.MockUp;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import de.persoapp.core.ECardWorker;
import de.persoapp.core.card.CardHandler;
import de.persoapp.core.client.IMainView;
import de.persoapp.core.client.MainViewEventListener;
import de.persoapp.core.tests.util.TestMainView;
import de.persoapp.core.util.Util;
import de.persoapp.core.ws.IFDService;
import de.persoapp.core.ws.ManagementService;
import de.persoapp.core.ws.SALService;
import de.persoapp.core.ws.engine.WSContainer;
import de.persoapp.core.ws.engine.WSEndpoint;

/**
 * Test cases facing the {@link WSContainer}
 * 
 * @author Rico Klimsa, 2015
 */
public class WSContainerTest {

	private String serviceURL;
	private String DEFAULT_PIN;
	private static String tcToken = "";
	private static Map<String,String> ECApiParams = null;
	private IMainView mainView;
	private static Properties properties = null;
	
	private WSContainer wsCtx;
	private CardHandler eCardHandler;
	
	
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
	
	
	/**
	 * Test spy for advanced test control.
	 */
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
		DEFAULT_PIN = (String) properties.get("Default_PIN");
		
		serviceURL = (String) properties.get("eID_service_URL");
		
		mainView = TestMainView.getInstance(DEFAULT_PIN);
		assertNotNull("no main view", mainView);

		eCardHandler = new CardHandler(mainView);
		
		assertNotNull("no card handler", eCardHandler);
		assertNotNull("No eID card inserted", eCardHandler.getECard());
		eCardHandler.reset();
		mainView.setEventLister(new MainViewEventListener(eCardHandler, mainView));
		
		wsCtx = new WSContainer();
		assertNotNull("no web service container", wsCtx);

		wsCtx.addService(new ManagementService());
		wsCtx.addService(new SALService());
		wsCtx.addService(new IFDService());
		wsCtx.init(null);			

		ECardWorker.init(mainView, wsCtx, eCardHandler);
	}
	
	
	/**
	 * Online authentication is triggered and the processing of requests to webservice endpoints is checked.<br/>
	 * <br/>
	 * <b>References: </b>TR-03124-1, Section 2 <em>Online-Authentication</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>The qname and the data of an request are checked being <b>not null</b>.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication completes without errors.</li>
	 * </ul>
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 */
	@Test
	public void WSContainerTest_1() throws IOException, URISyntaxException, GeneralSecurityException {
		
		final TestSpy spy = new TestSpy();
		final URL tcTokenURL = new URL(serviceURL);
		
		MockUp<WSContainer> mockUp = new MockUp<WSContainer>(wsCtx){

			@Mock
			public final Object processRequest(mockit.Invocation inv, final QName name, final Object message) {
				try{
					
					assertNotNull("Qname is null", name);
					assertNotNull("Message is null", message);
					
					return inv.proceed(name,message);					
				} catch (final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(),ae);
				}

			}
		};
		
		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);
			
		System.out.println("refreshURL: " + refreshURL);
			
		if(spy.getStringValue()!=null&&!spy.getStringValue().trim().isEmpty()) { 
			mockUp.tearDown();
			fail(spy.getStringValue());
		}
		
		connectToRefreshURL(refreshURL, true);
		
		mockUp.tearDown();
	}
	
	
	/**
	 * Online authentication is triggered and the processing of requests to webservice endpoints is checked.<br/>
	 * <br/>
	 * <b>References: </b>TR-03124-1, Section 2 <em>Online-Authentication</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>The data of request is checked being <b>not null</b>.</li>
	 * <li>The qname is set to <b>null</b>.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication fails.</li>
	 * </ul>
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 */
	@Test
	public void WSContainerTest_2() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();
		final URL tcTokenURL = new URL(serviceURL);
		
		MockUp<WSContainer> mockUp = new MockUp<WSContainer>(wsCtx){

			@Mock
			public final Object processRequest(mockit.Invocation inv, final QName name, final Object message) {
				
				try {
					
					assertNotNull("Message is null", message);
					
					return inv.proceed(null,message);
				} catch(final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(),ae);
				}
			}			
		};
		
		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);
		
		System.out.println("refreshURL: " + refreshURL);

		
		if(spy.getStringValue()!=null&&!spy.getStringValue().trim().isEmpty()) { 
			mockUp.tearDown();
			fail(spy.getStringValue());
		}
		
		connectToRefreshURL(refreshURL, false);
		
		mockUp.tearDown();
	}
	
	
	/**
	 * Online authentication is triggered and the request data can not processed to the endpoints.<br/>
	 * <br/>
	 * <b>References: </b>TR-03124-1, Section 2 <em>Online-Authentication</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>The qname of an requested service is checked being <b>not null</b>.</li>
	 * <li>The request data is set to <b>null</b>.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication fails.</li>
	 * </ul>
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 */
	@Test
	public void WSContainerTest_3() throws IOException, URISyntaxException, GeneralSecurityException {
		final TestSpy spy = new TestSpy();
		
		final URL tcTokenURL = new URL(serviceURL);
		
		MockUp<WSContainer> mockUp = new MockUp<WSContainer>(wsCtx){

			@Mock
			public final Object processRequest(mockit.Invocation inv, final QName name, final Object message) {
				
				try {
					
					assertNotNull("Name is null", name);
					
					return inv.proceed(name,null);	
				} catch(final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(),ae);
				}
			}			
		};
		

		final String refreshURL = ECardWorker.start(tcTokenURL);

		assertNotNull("no refresh URL", refreshURL);
		
		System.out.println("refreshURL: " + refreshURL);
		
		
		if(spy.getStringValue()!=null&&!spy.getStringValue().trim().isEmpty()) { 
			mockUp.tearDown();
			fail(spy.getStringValue());
		}
		
		connectToRefreshURL(refreshURL, false);
		
		mockUp.tearDown();
	}
	
	/**
	 * Online authentication is triggered and the name of the implementor of the service can not correctly processed.<br/>
	 * <br/>
	 * <b>References: </b>TR-03124-1, Section 2 <em>Online-Authentication</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>The implementor of the service is set to <b>null</b>.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication fails.</li>
	 * </ul>
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 */
	@Test
	public void WSContainerTest_4() throws IOException, URISyntaxException, GeneralSecurityException {
		
		final URL tcTokenURL = new URL(serviceURL);
		
		final TestSpy spy = new TestSpy();
		spy.setValue(false);
		
		MockUp<WSContainer> mockUp = new MockUp<WSContainer>() {
			
			@Mock
			public void addService(mockit.Invocation inv, final Object impl) {
				spy.setValue(true);
				
				final Object tmp = null;
				
				inv.proceed(tmp);
			}
		};
		
		wsCtx = new WSContainer();
		assertNotNull("no web service container", wsCtx);

		try {
			wsCtx.addService(new ManagementService());

		} catch (final NullPointerException npe) {
			// endpoint could be added correctly. test succeeded.
			if(spy.isValue()) {
				return;
			} else {
				// The mocked method MUST be called during a correct workflow. If the method is not called, than an error happened.
				fail("Mocked method not called.");
			}
		}
		wsCtx.addService(new SALService());
		wsCtx.addService(new IFDService());
		wsCtx.init(null);			

		ECardWorker.init(mainView, wsCtx, eCardHandler);
		
		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);
		
		System.out.println("refreshURL: " + refreshURL);

		connectToRefreshURL(refreshURL, false);
		
		mockUp.tearDown();
	}

	
	/**
	 * Online authentication is triggered and the implementor of the different services is checked.<br/>
	 * <br/>
	 * <b>References: </b>TR-03124-1, Section 2 <em>Online-Authentication</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>The implementor of the specific services is checked being <b>not null</b>.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication completes without errors.</li>
	 * </ul>
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 */
	@Test
	public void WSContainerTest_5() throws IOException, URISyntaxException, GeneralSecurityException {
		
		final URL tcTokenURL = new URL(serviceURL);

		final TestSpy spy = new TestSpy();
		
		MockUp<WSContainer> mockUp = new MockUp<WSContainer>() {
			
			@Mock
			public void addService(mockit.Invocation inv, final Object impl) {
				try{
					assertNotNull("Implementor is null", impl);
					inv.proceed(impl);
				} catch(final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(),ae);
				}
			}
		};
		
		
		wsCtx = new WSContainer();
		assertNotNull("no web service container", wsCtx);

		wsCtx.addService(new ManagementService());
		wsCtx.addService(new SALService());
		wsCtx.addService(new IFDService());
		wsCtx.init(null);			

		ECardWorker.init(mainView, wsCtx, eCardHandler);
		
		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);
		
		System.out.println("refreshURL: " + refreshURL);

		if(spy.getStringValue()!=null&&!spy.getStringValue().trim().isEmpty()) { 
			mockUp.tearDown();
			fail(spy.getStringValue());
		}
		
		connectToRefreshURL(refreshURL, true);
		
		mockUp.tearDown();
	}
	
	
	/**
	 * Online authentication is triggered and the name of the endpoint of the service can not correctly processed.<br/>
	 * <br/>
	 * <b>References: </b>TR-03124-1, Section 2 <em>Online-Authentication</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>The endpoint of the service is set to <b>null</b>.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication fails.</li>
	 * </ul>
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 */
	@Test
	public void WSContainerTest_6() throws IOException, URISyntaxException, GeneralSecurityException {
		
		final URL tcTokenURL = new URL(serviceURL);

		final TestSpy spy = new TestSpy();
		spy.setValue(false);
		
		MockUp<WSContainer> mockUp = new MockUp<WSContainer>() {
			
			@Mock
			public void addEndpoint(mockit.Invocation inv,final WSEndpoint wse) {
				spy.setValue(true);
				
				final WSEndpoint tmp = null;
				
				inv.proceed(tmp);
			}
		};

		wsCtx = new WSContainer();
		assertNotNull("no web service container", wsCtx);

		try {
			wsCtx.addService(new ManagementService());

		} catch (final NullPointerException npe) {
			// endpoint could be added correctly. test succeeded.
			if(spy.isValue()) {
				return;
			} else {
				// The mocked method MUST be called during a correct workflow. If the method is not called, than an error happened.
				fail("Mocked method not called.");
			}
		}
		wsCtx.addService(new SALService());
		wsCtx.addService(new IFDService());
		wsCtx.init(null);		


		ECardWorker.init(mainView, wsCtx, eCardHandler);
		
		
		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);
		
		System.out.println("refreshURL: " + refreshURL);

		connectToRefreshURL(refreshURL, false);
		
		mockUp.tearDown();
	}

	
	/**
	 * Online authentication is triggered and the endpoint of the different services is checked.<br/>
	 * <br/>
	 * <b>References: </b>TR-03124-1, Section 2 <em>Online-Authentication</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>The endpoint of the specific services is checked being <b>not null</b>, before it is added.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication completes without errors.</li>
	 * </ul>
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 */
	@Test
	public void WSContainerTest_7() throws IOException, URISyntaxException, GeneralSecurityException {
		
		final URL tcTokenURL = new URL(serviceURL);

		final TestSpy spy = new TestSpy();
		
		MockUp<WSContainer> mockUp = new MockUp<WSContainer>() {
			
			@Mock
			public void addEndpoint(mockit.Invocation inv,final WSEndpoint wse) {
				try{
					assertNotNull("Webservice endpoint is null", wse);
					inv.proceed(wse);
				} catch(final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(),ae);
				}
			}
		};
		
		wsCtx = new WSContainer();
		assertNotNull("no web service container", wsCtx);

		wsCtx.addService(new ManagementService());
		wsCtx.addService(new SALService());
		wsCtx.addService(new IFDService());
		wsCtx.init(null);		


		ECardWorker.init(mainView, wsCtx, eCardHandler);
		
		
		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);
		
		System.out.println("refreshURL: " + refreshURL);

		if(spy.getStringValue()!=null&&!spy.getStringValue().trim().isEmpty()) { 
			mockUp.tearDown();
			fail(spy.getStringValue());
		}
		
		connectToRefreshURL(refreshURL, true);
		
		mockUp.tearDown();
	}
	
	
	
	/**
	 * Online authentication is triggered and the initialization of the webservice container is tested.<br/>
	 * <br/>
	 * <b>References: </b>TR-03124-1, Section 2 <em>Online-Authentication</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>The initialization of the webservice container is tested.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication fails.</li>
	 * </ul>
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 */
	@Test
	public void WSContainerTest_8() throws IOException, URISyntaxException, GeneralSecurityException {
		
		final URL tcTokenURL = new URL(serviceURL);

		final TestSpy spy = new TestSpy();
		spy.setValue(false);
		
		MockUp<WSContainer> mockUp = new MockUp<WSContainer>() {
			
			@Mock
			public void init(mockit.Invocation inv,final WebServiceContext externalContext) {
				spy.setValue(true);
				
				inv.proceed(externalContext);
			}
		};

		wsCtx = new WSContainer();
		assertNotNull("no web service container", wsCtx);

		wsCtx.addService(new ManagementService());
		wsCtx.addService(new SALService());
		wsCtx.addService(new IFDService());
		wsCtx.init(null);		


		ECardWorker.init(mainView, wsCtx, eCardHandler);
		
		
		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);
		
		System.out.println("refreshURL: " + refreshURL);

		if(spy.isValue()) { 
			mockUp.tearDown();
			fail(spy.getStringValue());
		}
		
		
		connectToRefreshURL(refreshURL, false);
		
		mockUp.tearDown();
	}

	
	/**
	 * Online authentication is triggered and the message context of the webservice container is checked.<br/>
	 * <br/>
	 * <b>References: </b>TR-03124-1, Section 2 <em>Online-Authentication</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>The returned message context is checked being <b>not null</b>.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication completes without errors.</li>
	 * </ul>
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 */
	@Test
	public void WSContainerTest_9() throws IOException, URISyntaxException, GeneralSecurityException {
		
		final URL tcTokenURL = new URL(serviceURL);

		final TestSpy spy = new TestSpy();
		
		MockUp<WSContainer> mockUp = new MockUp<WSContainer>() {
			
			@Mock
			public MessageContext getMessageContext(mockit.Invocation inv) {
				try{
					final MessageContext msgCtx = inv.proceed();
					
					assertNotNull("Messagecontext is null", msgCtx);
					
					return msgCtx;
				} catch(final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(),ae);
				}
			}
		};
		
		wsCtx = new WSContainer();
		assertNotNull("no web service container", wsCtx);

		wsCtx.addService(new ManagementService());
		wsCtx.addService(new SALService());
		wsCtx.addService(new IFDService());
		wsCtx.init(null);		


		ECardWorker.init(mainView, wsCtx, eCardHandler);
		
		
		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);
		
		System.out.println("refreshURL: " + refreshURL);

		if(spy.getStringValue()!=null&&!spy.getStringValue().trim().isEmpty()) { 
			mockUp.tearDown();
			fail(spy.getStringValue());
		}
		
		connectToRefreshURL(refreshURL, false);
		
		mockUp.tearDown();
	}
	
	
	/**
	 * Online authentication is triggered and the bound classes of an endpoint are checked.<br/>
	 * <br/>
	 * <b>References: </b>TR-03124-1, Section 2 <em>Online-Authentication</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The online authentication is triggered.</li>
	 * <li>The set of bound classes of an enpoint is checked being <b>not null</b> and contains at least one element.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The online authentication completes without errors.</li>
	 * </ul>
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 * @throws GeneralSecurityException
	 */
	@Test
	public void WSContainerTest_10() throws IOException, URISyntaxException, GeneralSecurityException {
		
		final URL tcTokenURL = new URL(serviceURL);

		final TestSpy spy = new TestSpy();
		
		MockUp<WSContainer> mockUp = new MockUp<WSContainer>() {
			
			@Mock
			public Set<Class<? extends Object>> getXmlSeeAlso(mockit.Invocation inv) {
				try{
					final Set<Class<? extends Object>> xmlSeeAlso = inv.proceed();
					
					assertNotNull("Set of bound classes are null", xmlSeeAlso);
					assertTrue("Set of bound classes is empty", xmlSeeAlso.size()>0);
					
					return xmlSeeAlso;
				} catch(final AssertionError ae) {
					spy.setStringValue(ae.getMessage());
					throw new AssertionError(ae.getMessage(),ae);
				}
			}
		};
		
		wsCtx = new WSContainer();
		assertNotNull("no web service container", wsCtx);

		wsCtx.addService(new ManagementService());
		wsCtx.addService(new SALService());
		wsCtx.addService(new IFDService());
		wsCtx.init(null);		


		ECardWorker.init(mainView, wsCtx, eCardHandler);
		
		
		final String refreshURL = ECardWorker.start(tcTokenURL);
		assertNotNull("no refresh URL", refreshURL);
		
		System.out.println("refreshURL: " + refreshURL);

		if(spy.getStringValue()!=null&&!spy.getStringValue().trim().isEmpty()) { 
			mockUp.tearDown();
			fail(spy.getStringValue());
		}
		
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
	 * Reset the ECardWorker
	 * 
	 * @throws NoSuchFieldException
	 * @throws SecurityException
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	@After
	public synchronized void cleanUp() throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		
		long end = System.currentTimeMillis() + 6000;
		
		while(System.currentTimeMillis()<end) {
			// wait to prevent race condition between testcases.
		}
		
		Field field = ECardWorker.class.getDeclaredField("mainView");
		field.setAccessible(true);
		field.set(null, null);
		field.setAccessible(false);
		ECardWorker.init(null, null, null);
	}
}
