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
 * @version 1.0, 17.03.2015 13:07:59
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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
import de.persoapp.core.util.Util;
import de.persoapp.core.ws.IFDService;
import de.persoapp.core.ws.ManagementService;
import de.persoapp.core.ws.SALService;
import de.persoapp.core.ws.engine.WSContainer;
import de.persoapp.core.ws.engine.WSEndpoint;

/**
 * Testcases facing {@link WSEndpoint}
 * 
 * @author Rico Klimsa, 2015
 */
@RunWith(JMockit.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WSEndpointTest {
	
	private String serviceURL;
	private String DEFAULT_PIN;
	
	private static Properties properties = null;
	
	private IMainView mainView;
	
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
	 * Constructing a new {@link WSEndpoint}.</br>
	 * </br>
	 * <b>Preconditions:</b>
	 * nothing </br>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>Passing a implementation class of an web service {@link WSEndpoint}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The constructing succeeds. No error occurs.</li>
	 * </ul>
	 */
	@Test
	public void WSEndpointTest_1() {
		
		assertNotNull("Endpoint is null",new WSEndpoint(new SALService()));
	
	}
	
	
	/**
	 * Error behavior of the constructor of {@link WSEndpoint}.</br>
	 * </br>
	 * <b>Preconditions:</b>
	 * nothing </br>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>Passing <b>null</b> to the constructor of {@link WSEndpoint}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The construction fails with an {@link NullPointerException}.</li>
	 * </ul>
	 */
	@Test
	public void WSEndpointTest_2() {
		
		boolean throwed = false;
		
		try {
			new WSEndpoint(null);			
		} catch(final NullPointerException ne) {
			throwed = true;
		} finally {
			assertTrue("Exception not throwed",throwed);
		}
		
	}
	
	/**
	 * Adding a webservice endpoint to the wsendpoint container.</br>
	 * </br>
	 * <b>Preconditions:</b>
	 * nothing </br>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>Creating a new WSEndpoint and adding it to the webservice endpoint container.</li>
	 * <li>The portname of an previously added webservice endpoint is checked being <b>not null</b>.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The constructing succeeds and the port name can be successful retrieved. No error occurs.</li>
	 * </ul>
	 */
	@Test
	public void WSEndpointTest_3() {
		
		WSEndpoint wsEndpoint = new WSEndpoint(new SALService());
		assertNotNull("Portname is null.",wsEndpoint.getPortName());

	}
	
	/**
	 * Adding a webservice endpoint to the wsendpoint container.</br>
	 * </br>
	 * <b>Preconditions:</b>
	 * nothing </br>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>Creating a new WSEndpoint and adding it to the webservice endpoint container.</li>
	 * <li>The port of an previously added webservice endpoint is checked being <b>not null</b>.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The constructing succeeds and the port can be retrieved. No error occurs.</li>
	 * </ul>
	 */
	@Test
	public void WSEndpointTest_4() {
	
		WSEndpoint wsEndpoint = new WSEndpoint(new SALService());
		assertNotNull("Port is null.",wsEndpoint.getPort());

	}
	
	
	/**
	 * Adding a webservice endpoint to the wsendpoint container.</br>
	 * </br>
	 * <b>Preconditions:</b>
	 * nothing </br>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>Creating a new WSEndpoint and adding it to the webservice endpoint container.</li>
	 * <li>The service name of an previously added webservice endpoint is checked being <b>not null</b>.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The constructing succeeds and the service name can be retrieved. No error occurs.</li>
	 * </ul>
	 */
	@Test
	public void WSEndpointTest_5() {
	
		WSEndpoint wsEndpoint = new WSEndpoint(new SALService());
		assertNotNull("Service name is null.",wsEndpoint.getServiceName());

	}
	
	
	/**
	 * Adding a webservice endpoint to the webservice sendpoint container.</br>
	 * </br>
	 * <b>Preconditions:</b>
	 * nothing </br>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>Creating a new WSEndpoint and adding it to the webservice endpoint container.</li>
	 * <li>The set of bound classes is checked of an previously added webservice endpoint being <b>not null</b>.</li>
	 * <li>The set of bound classes is checked containing at least one element.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The constructing succeeds and the bound classes can be retrieved. No error occurs.</li>
	 * </ul>
	 */
	@Test
	public void WSEndpointTest_6() {
		
		WSEndpoint wsEndpoint = new WSEndpoint(new SALService());
		assertNotNull("Bound classes are null.",wsEndpoint.getXmlSeeAlso());
	}
	
	
	/**
	 * Adding a webservice endpoint to the wsendpoint container.</br>
	 * </br>
	 * <b>Preconditions:</b>
	 * nothing </br>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>Creating a new WSEndpoint and adding it to the webservice endpoint container.</li>
	 * <li>The string representation of the created endpoint is checked being <b>not null</b>.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The constructing succeeds and the string representation can be retrieved. No error occurs.</li>
	 * </ul>
	 */
	@Test
	public void WSEndpointTest_7() {
		
		WSEndpoint wsEndpoint = new WSEndpoint(new SALService());
		assertNotNull("To String is null.",wsEndpoint.toString());
		
	}
	
	
	/**
	 * Online authentication is triggered and resolved methods are checked.<br/>
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
	 * <li>The resolved methods of an implementor of an enpoint are checked being <b>not null</b>.</li>
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
	public void WSEndpointTest_8() throws IOException, URISyntaxException, GeneralSecurityException {
		
		final URL tcTokenURL = new URL(serviceURL);

		final TestSpy spy = new TestSpy();
		
		MockUp<WSEndpoint> mockUp = new MockUp<WSEndpoint>() {
			
			@Mock
			public final Method resolveMethod(mockit.Invocation inv, final String qname) {
				try{
					final Method method = inv.proceed();
					
					assertNotNull("Resolved method is null", method);
					
					return method;
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
