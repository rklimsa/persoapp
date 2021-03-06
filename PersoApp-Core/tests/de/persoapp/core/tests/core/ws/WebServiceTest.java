/**
 * 
 * COPYRIGHT (C) 2010, 2011, 2012, 2013, 2014 AGETO Innovation GmbH
 * 
 * Authors Christian Kahlo, Ralf Wondratschek
 * 
 * All Rights Reserved.
 * 
 * Contact: PersoApp, http://www.persoapp.de
 * 
 * @version 1.0, 30.09.2014 12:48:32
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

package de.persoapp.core.tests.core.ws;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import iso.std.iso_iec._24727.tech.schema.ConnectionHandleType;
import iso.std.iso_iec._24727.tech.schema.DIDAuthenticate;
import iso.std.iso_iec._24727.tech.schema.DIDAuthenticateResponse;
import iso.std.iso_iec._24727.tech.schema.DIDAuthenticationDataType;
import iso.std.iso_iec._24727.tech.schema.EAC1InputType;
import iso.std.iso_iec._24727.tech.schema.EAC1OutputType;
import iso.std.iso_iec._24727.tech.schema.EAC2InputType;
import iso.std.iso_iec._24727.tech.schema.EAC2OutputType;
import iso.std.iso_iec._24727.tech.schema.EACAdditionalInputType;
import iso.std.iso_iec._24727.tech.schema.InputAPDUInfoType;
import iso.std.iso_iec._24727.tech.schema.RequestType;
import iso.std.iso_iec._24727.tech.schema.Transmit;
import iso.std.iso_iec._24727.tech.schema.TransmitResponse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

import de.bund.bsi.ecard.api._1.InitializeFrameworkResponse;
import de.persoapp.core.ECardWorker;
import de.persoapp.core.card.CardHandler;
import de.persoapp.core.client.ECardSession;
import de.persoapp.core.client.IMainView;
import de.persoapp.core.client.MainViewEventListener;
import de.persoapp.core.tests.util.ConfigTestcase;
import de.persoapp.core.tests.util.EACPhases;
import de.persoapp.core.tests.util.TestMainView;
import de.persoapp.core.tests.util.TestSALService;
import de.persoapp.core.util.Hex;
import de.persoapp.core.util.Util;
import de.persoapp.core.util.TLV;
import de.persoapp.core.ws.*;
import de.persoapp.core.ws.engine.WSContainer;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.MethodSorters;

/**
 * @author Rico Klimsa, 2014
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class WebServiceTest{
	
	private WSContainer wsCtx;
	private String	serviceURL;
	
	private static Logger logger = Logger.getLogger(WebServiceTest.class.getName());
	private IMainView	mainView;

	private IFDService ifdservice;
	
	private static Properties properties;
	
	/**
	 * Test spy for indirect output
	 */
	private TestSALService salservice;
	private ManagementService managementservice;
	
	private CardHandler eCardHandler;
	private ECardSession session;
	
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
		  logger.severe(description.getMethodName()+"Failed!"+" "+e.getMessage());
	  }

	  @Override
	  protected void succeeded(Description description) {
		  logger.info(description.getMethodName()+" " + "success!");
	  }

	};

	@Before
	public void init(){
		final String DEFAULT_PIN = (String) properties.get("Default_PIN");
		
		serviceURL = (String) properties.get("eID_service_URL");
		
		if(mainView==null) {
			mainView = TestMainView.getInstance(DEFAULT_PIN);
			assertNotNull("no main view", mainView);		
		}
		
		if( eCardHandler == null) {
			eCardHandler = new CardHandler(mainView);
			assertNotNull("no card handler", eCardHandler);
			mainView.setEventLister(new MainViewEventListener(eCardHandler, mainView));
		}
		
		if(managementservice==null) {
			managementservice = new ManagementService();
		}
		
		if(salservice==null) {
			salservice = new TestSALService();
		}
		
		if(ifdservice==null) {
			ifdservice = new IFDService();
		}
		
		if(wsCtx==null) {
			wsCtx = new WSContainer();
			assertNotNull("no web service container", wsCtx);
			wsCtx.addService(managementservice);
			wsCtx.addService(salservice);
			wsCtx.addService(ifdservice);
			wsCtx.init(null);
		}
		
		if(session == null) {
			session = new ECardSession(mainView, eCardHandler);
			assertNotNull("no session",session);		
		}
		ECardWorker.init(mainView, wsCtx, eCardHandler);
	}
	
	/**
	 * Function invocation with null as inserted value.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-6, Section 3.2.5 <em>Transmit</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link IFDService#transmit(Transmit)} is invoked with <b>null</b>.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>An {@link NullPointerException} is thrown.</li>.
	 * </ul>
	 * 
	 * @see IFDService#transmit(Transmit)
	 */
	@Test
	public void testIFDServiceNull_1() {
		
		assertNotNull("No eID card inserted", eCardHandler.getECard());
		wsCtx.getMessageContext().clear();		
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		try{
			ifdservice.transmit(null);
		} catch(final NullPointerException e) {
			logger.log(Level.INFO, "NullPointerException is thrown: "+e.getStackTrace()[0]);
			return;
		} catch(final Throwable t) {
			fail("Unexpected exception: "+t.getMessage());
		}
		fail("No NullPointerException is thrown.");
	}
	
	/**
	 * Function invocation without an {@link ECardSession}.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-6, Section 3.2.5 <em>Transmit</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link IFDService#transmit(Transmit)} is invoked with empty
	 * {@link Transmit} and without an {@link ECardSession} in the {@link #wsCtx}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>An {@link NullPointerException} is thrown.</li>.
	 * </ul>
	 * 
	 * @see IFDService#transmit(Transmit)
	 * @see Transmit
	 */
	@Test
	public void testIFDServiceNull_2() {
		
		assertNotNull("No eID card inserted", eCardHandler.getECard());
		
		wsCtx.getMessageContext().clear();
		wsCtx.getMessageContext().put(ECardSession.class.getName(), null);
		Transmit parameters = new Transmit();
		try{
			ifdservice.transmit(parameters);
		} catch(final NullPointerException e) {
			logger.log(Level.INFO, "NullPointerException is thrown: "+e.getStackTrace()[0]);
			return;
		} catch(final Throwable t) {
			fail("Unexpected exception: "+t.getMessage());
		}
		fail("No NullPointerException is thrown.");
	}
	
	/**
	 * Function invocation without an output APDU in {@link Transmit}. <br/>
	 * <br/>
	 * <b>References: </b>TR-03112-6, Section 3.2.5 <em>Transmit</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link IFDService#transmit(Transmit)} is invoked with empty
	 * {@link Transmit}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>An {@link TransmitResponse} is returned with the major result
	 * {@link EcAPIProvider#ECARD_API_RESULT_OK}.</li>.
	 * </ul>
	 * 
	 * @see IFDService#transmit(Transmit)
	 * @see Transmit
	 * @see InputAPDUInfoType
	 */
	@Test
	public void testIFDServiceInvalidParameter_1() {
		
		assertNotNull("No eID card inserted", eCardHandler.getECard());
		
		wsCtx.getMessageContext().clear();		
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		
		Transmit parameters = new Transmit();
		
		TransmitResponse response = null;

		try{
			response = ifdservice.transmit(parameters);			
		} catch (final Throwable t) {
			fail("Unexpected throwable is thrown: "+t.getMessage());
		}

		assertNotNull("transmit result is null",response);
		assertNotNull("result is null",response.getResult());
		assertNotNull("major result is null",response.getResult().getResultMajor());
		assertEquals("wrong major result",EcAPIProvider.ECARD_API_RESULT_OK,response.getResult().getResultMajor());
	}
	
	/**
	 * Function invocation with an empty apdu.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-6, Section 3.2.5 <em>Transmit</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link IFDService#transmit(Transmit)} is invoked with
	 * {@link Transmit} which has an empty apdu.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>An {@link TransmitResponse} is returned with the major result
	 * {@link EcAPIProvider#ECARD_API_RESULT_OK}.</li>.
	 * </ul>
	 * 
	 * @see IFDService#transmit(Transmit)
	 * @see Transmit
	 * @see InputAPDUInfoType
	 * @see TransmitResponse
	 */
	@Test
	public void testIFDServiceInvalidParameter_2() {
		
		assertNotNull("No eID card inserted", eCardHandler.getECard());
		
		wsCtx.getMessageContext().clear();		
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		
		byte[] wrongApdu = new byte[0];
		
		InputAPDUInfoType apdu = new InputAPDUInfoType();		
		apdu.setInputAPDU(wrongApdu);

		Transmit parameters = new Transmit();
		parameters.getInputAPDUInfo().add(apdu);

		TransmitResponse response = null;

		try{
			response = ifdservice.transmit(parameters);			
		} catch (final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}

		assertNotNull("transmit result is null",response);
		assertNotNull("result is null",response.getResult());
		assertNotNull("major result is null",response.getResult().getResultMajor());
		assertEquals("wrong major result",EcAPIProvider.ECARD_API_RESULT_OK,response.getResult().getResultMajor());
	}
	
	
	/**
	 * Function invocation with forbidden apdu.<br/>
	 * <b>References: </b>TR-03112-6, Section 3.2.5 <em>Transmit</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link IFDService#transmit(Transmit)} is invoked with
	 * {@link Transmit} and an APDU which is not allowed to be send.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>An {@link TransmitResponse} is returned with the major result
	 * {@link EcAPIProvider#ECARD_API_RESULT_OK} and an OutputAPDU
	 * <code>0x6D,0x00</code>.</li>.
	 * </ul>
	 * 
	 * @see IFDService#transmit(Transmit)
	 * @see Transmit
	 * @see InputAPDUInfoType
	 * @see TransmitResponse
	 */
	@Test
	public void testIFDServiceInvalidParameter_3() {
		
		assertNotNull("No eID card inserted", eCardHandler.getECard());
		
		wsCtx.getMessageContext().clear();		
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		
		byte[] wrongApdu = new byte[1];
		wrongApdu[0] = (byte) 0xff;
		
		InputAPDUInfoType apdu = new InputAPDUInfoType();		
		apdu.setInputAPDU(wrongApdu);

		Transmit parameters = new Transmit();
		parameters.getInputAPDUInfo().add(apdu);
		
		
		TransmitResponse response = null;
		try {			
			response = ifdservice.transmit(parameters);			
		} catch (final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}

		assertNotNull("transmit result is null", response);
		assertEquals("apdu with cla = 0xff is not handled by the implementation", 1,response.getOutputAPDU().size());
		assertEquals("apdu with cla = 0xff is not handled by the implementation", "6D00",Hex.toString(response.getOutputAPDU().get(0)));
	}
		
	/**
	 * Function invocation with an malformed APDU.<br/><br/>
	 * <b>References: </b>TR-03112-6, Section 3.2.5 <em>Transmit</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link IFDService#transmit(Transmit)} is invoked with
	 * {@link Transmit} and an malformed APDU.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>An {@link IllegalArgumentException} is thrown.</li>
	 * </ul>
	 * 
	 * @see IFDService#transmit(Transmit)
	 * @see Transmit
	 * @see InputAPDUInfoType
	 * @see TransmitResponse
	 */
	@Test
	public void testIFDServiceInvalidParameter_4(){
		
		assertNotNull("No eID card inserted",eCardHandler.getECard());
		
		wsCtx.getMessageContext().clear();		
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		
		byte[] wrongApdu = new byte[1];
		wrongApdu[0] = (byte) 0x12;
		
		InputAPDUInfoType apdu = new InputAPDUInfoType();		
		apdu.setInputAPDU(wrongApdu);
		
		Transmit parameters = new Transmit();
		parameters.getInputAPDUInfo().add(apdu);
		

		
		try {
			ifdservice.transmit(parameters);		
			eCardHandler = null;
		} catch (final IllegalArgumentException e) {
			logger.log(Level.INFO, "IllegalArgumentException is thrown: "+e.getStackTrace()[0]);
			return;
		} catch (final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
		
		fail("No IllegalArgumentException is thrown.");
	}
	
	/**
	 * Function invocation with correct parameters.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-6, Section 3.2.5 <em>Transmit</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link IFDService#transmit(Transmit)} is invoked with
	 * {@link Transmit} and an valid APDU.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The APDU is correctly send, which is indicated through the response APDU <code>9000</code>.</li>
	 * </ul>
	 * 
	 * @see IFDService#transmit(Transmit)
	 * @see Transmit
	 * @see InputAPDUInfoType
	 * @see TransmitResponse
	 */
	@Test
	public void testIFDServiceValidParameter_1(){
		wsCtx.getMessageContext().clear();
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);

		assertNotNull("No eID card inserted",eCardHandler.getECard());

		short FID = 0x011C;
		byte[] correctApdu = new byte[]{0x00, (byte) 0xA4, 0x02, 0x0C, 0x02, (byte) (FID >> 8), (byte) (FID & 0xFF) };
		
		InputAPDUInfoType apdu = new InputAPDUInfoType();		
		apdu.setInputAPDU(correctApdu);
		
		Transmit parameters = new Transmit();
		parameters.getInputAPDUInfo().add(apdu);
		
		TransmitResponse response = null;
		

		
		try {
			response = ifdservice.transmit(parameters);
		} catch (final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
		assertNotNull("response is null",response);
		assertEquals("apdu not correct", "9000",Hex.toString(response.getOutputAPDU().get(0)));
	}
	
	
	/**
	 * Function invocation without an {@link ECardSession}.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-7, Section 3.6.4 <em>Overview of EAC protocol sequence</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link SALService#didAuthenticate(DIDAuthenticate)} is invoked with
	 * {@link DIDAuthenticate} and without an {@link ECardSession}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>An {@link NullPointerException} is thrown.</li>
	 * </ul>
	 * 
	 * @see TestSALService#didAuthenticate(DIDAuthenticate)
	 * @see DIDAuthenticate
	 * @see ConnectionHandleType
	 */
	@Test
	public void testSALServiceNull_1() {
		wsCtx.getMessageContext().clear();
		wsCtx.getMessageContext().put(ECardSession.class.getName(), null);
		salservice.getResponse().clear();
		
		assertNotNull("No eID card inserted",eCardHandler.getECard());
		
		Random sr = new Random();
		byte[] slotHandle = new byte[32];
		
		sr.nextBytes(slotHandle);
		
		ConnectionHandleType cht = new ConnectionHandleType();
		cht.setSlotHandle(slotHandle);
		
		DIDAuthenticate parameters = new DIDAuthenticate();
		parameters.setConnectionHandle(cht);
		
		try{
			salservice.didAuthenticate(parameters);
		} catch (final NullPointerException e) {
			logger.log(Level.INFO, "NullpointerException is thrown: "+e.getMessage());
			return;
		} catch (final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
		fail("No NullpointerException is thrown.");
	}
	
	/**
	 * <p>
	 * <b>Important: </b>Not handled exceptions in
	 * {@link de.persoapp.core.client.EAC_Info} and {@link WSContainer}
	 * preventing this test from completing and forcing the test suite into a
	 * deadlock until the callback in the {@link ECardWorker} is timed out.
	 * </p>
	 * Function invocation without certificates in the first EAC-phase.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-7, Section 3.6.4 <em>Overview of EAC protocol sequence</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link SALService#didAuthenticate(DIDAuthenticate)} is invoked with
	 * {@link DIDAuthenticate} and without certificates in the {@link EAC1InputType}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The whole authentication process fails.</li>
	 * </ul>
	 * 
	 * @see TestSALService#didAuthenticate(DIDAuthenticate)
	 * @see DIDAuthenticate
	 * @see DIDAuthenticateResponse
	 * @see EAC1InputType
	 */
	@Test
	public void testSALServiceNull_2() {
		wsCtx.getMessageContext().clear();
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		salservice.getResponse().clear();
		
		assertNotNull("No eID card inserted",eCardHandler.getECard());
		
		ArrayList<String> nullList = new ArrayList<String>();
		//Waiting on callback timeout
		nullList.add("certificate");
		
		try {
			makeNull(nullList, EACPhases.EAC_1);
		} catch(final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
	}	
	
	/**
	 * <p>
	 * <b>Important: </b>Not handled exceptions in {@link SALService} and
	 * {@link WSContainer} preventing this test from completing and forcing the
	 * test suite into a deadlock until the callback in the {@link ECardWorker}
	 * is timed out.
	 * </p>
	 * Function invocation without certificateDescription in the first EAC-phase.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-7, Section 3.6.4 <em>Overview of EAC protocol sequence</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link SALService#didAuthenticate(DIDAuthenticate)} is invoked
	 * with {@link DIDAuthenticate} and without certificateDescription in the
	 * {@link EAC1InputType}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The whole authentication process fails.</li>
	 * </ul>
	 * 
	 * @see TestSALService#didAuthenticate(DIDAuthenticate)
	 * @see DIDAuthenticate
	 * @see DIDAuthenticateResponse
	 * @see EAC1InputType
	 */
	@Test
	public void testSALServiceNull_3() {
		wsCtx.getMessageContext().clear();
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		salservice.getResponse().clear();
		
		assertNotNull("No eID card inserted",eCardHandler.getECard());
		
		ArrayList<String> nullList = new ArrayList<String>();
		nullList.add("certificateDescription");
		
		try {
			makeNull(nullList, EACPhases.EAC_1);
		} catch(final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
	}
	
	/**
	 * Function invocation without providerInfo in the first EAC-phase.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-7, Section 3.6.4 <em>Overview of EAC protocol sequence</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link SALService#didAuthenticate(DIDAuthenticate)} is invoked
	 * with {@link DIDAuthenticate} and without providerInfo in the
	 * {@link EAC1InputType}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The authentication process executes and completes normally
	 * because this element is deprecated and can be ignored.</li>
	 * </ul>
	 * 
	 * @see TestSALService#didAuthenticate(DIDAuthenticate)
	 * @see DIDAuthenticate
	 * @see DIDAuthenticateResponse
	 * @see EAC1InputType
	 */
	@Test
	public void testSALServiceNull_4() {
		wsCtx.getMessageContext().clear();
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		salservice.getResponse().clear();
		
		assertNotNull("No eID card inserted",eCardHandler.getECard());
		
		ArrayList<String> nullList = new ArrayList<String>();
		nullList.add("providerInfo");
		
		try {
			makeNull(nullList, EACPhases.EAC_1,false);
		} catch(final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
	}
	
	/**
	 * <p>
	 * <b>Important: </b>Not handled exceptions in {@link SALService} and
	 * {@link WSContainer} preventing this test from completing and forcing the
	 * test suite into a deadlock until the callback in the {@link ECardWorker} is
	 * timed out.
	 * </p>
	 * Function invocation without requiredCHAT in the first EAC-phase.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-7, Section 3.6.4 <em>Overview of EAC protocol sequence</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link SALService#didAuthenticate(DIDAuthenticate)} is invoked
	 * with {@link DIDAuthenticate} and without requiredCHAT in the
	 * {@link EAC1InputType}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>Causes the authentication process and therefore the whole
	 * alternative invocation to fail.</li>
	 * </ul>
	 * 
	 * @see TestSALService#didAuthenticate(DIDAuthenticate)
	 * @see DIDAuthenticate
	 * @see DIDAuthenticateResponse
	 * @see EAC1InputType
	 */
	@Test
	public void testSALServiceNull_5() {
		wsCtx.getMessageContext().clear();
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		salservice.getResponse().clear();
		
		assertNotNull("No eID card inserted",eCardHandler.getECard());
		
		ArrayList<String> nullList = new ArrayList<String>();
		nullList.add("requiredCHAT");
		
		try {
			makeNull(nullList, EACPhases.EAC_1);
		} catch(final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
	}	
	
	
	/**
	 * Function invocation without optionalCHAT in the first EAC-phase.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-7, Section 3.6.4 <em>Overview of EAC protocol sequence</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link SALService#didAuthenticate(DIDAuthenticate)} is invoked
	 * with {@link DIDAuthenticate} and without optionalCHAT in the
	 * {@link EAC1InputType}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The authentication process executes and completes normally
	 * because this element is not required and can be null.</li>
	 * </ul>
	 *
	 * @see TestSALService#didAuthenticate(DIDAuthenticate)
	 * @see DIDAuthenticate
	 * @see DIDAuthenticateResponse
	 * @see EAC1InputType
	 */
	@Test
	public void testSALServiceNull_6() {
		wsCtx.getMessageContext().clear();
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		salservice.getResponse().clear();
		
		assertNotNull("No eID card inserted",eCardHandler.getECard());
		
		ArrayList<String> nullList = new ArrayList<String>();
		nullList.add("optionalCHAT");
		
		try {
			makeNull(nullList, EACPhases.EAC_1,false);
		} catch(final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
	}	
	
	
	/**
	 * <p>
	 * <b>Important: </b>Not handled exceptions in
	 * {@link de.persoapp.core.util.ArrayTool} and {@link WSContainer}
	 * preventing this test from completing and forcing the test suite into a
	 * deadlock until the callback in the {@link ECardWorker} is timed out.
	 * </p>
	 * Function invocation without authenticatedAuxiliaryData in the first EAC-phase.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-7, Section 3.6.4 <em>Overview of EAC protocol sequence</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link SALService#didAuthenticate(DIDAuthenticate)} is invoked
	 * with {@link DIDAuthenticate} and without authenticatedAuxiliaryData in the
	 * {@link EAC1InputType}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>Causes the authentication process and therefore the whole
	 * alternative invocation to fail.</li>
	 * </ul>
	 * 
	 * @see TestSALService#didAuthenticate(DIDAuthenticate)
	 * @see DIDAuthenticate
	 * @see DIDAuthenticateResponse
	 * @see EAC1InputType
	 */
	@Test
	public void testSALServiceNull_7() {
		wsCtx.getMessageContext().clear();
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		salservice.getResponse().clear();
		
		assertNotNull("No eID card inserted",eCardHandler.getECard());
		
		ArrayList<String> nullList = new ArrayList<String>();
		nullList.add("authenticatedAuxiliaryData");
		
		try {
			makeNull(nullList, EACPhases.EAC_1);
		} catch(final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
	}
	
	
	/**
	 * Function invocation without transactionInfo in the first EAC-phase.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-7, Section 3.6.4 <em>Overview of EAC protocol sequence</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link SALService#didAuthenticate(DIDAuthenticate)} is invoked
	 * with {@link DIDAuthenticate} and without transactionInfo in the
	 * {@link EAC1InputType}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li> The authentication process executes and completes normally
	 * because this element is not required and can be null.</li>
	 * </ul>
	 * 
	 * @see TestSALService#didAuthenticate(DIDAuthenticate)
	 * @see DIDAuthenticate
	 * @see DIDAuthenticateResponse
	 * @see EAC1InputType
	 */
	@Test
	public void testSALServiceNull_8() {
		wsCtx.getMessageContext().clear();
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		salservice.getResponse().clear();
		
		assertNotNull("No eID card inserted",eCardHandler.getECard());
		
		ArrayList<String> nullList = new ArrayList<String>();
		nullList.add("transactionInfo");
		
		try {
			makeNull(nullList, EACPhases.EAC_1,false);
		} catch(final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
	}
	
	/**
	 * Function invocation without certificates in the second EAC-phase.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-7, Section 3.6.4
	 * <em>Overview of EAC protocol sequence</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link SALService#didAuthenticate(DIDAuthenticate)} is invoked
	 * with {@link DIDAuthenticate} and without certificate in the
	 * {@link EAC2InputType}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The authentication process succeeds because the <em>Certification
	 * Authority Reference</em> element in the {@link EAC1OutputType} is missing
	 * and thus this element is not provided.</li>
	 * </ul>
	 * 
	 * @see TestSALService#didAuthenticate(DIDAuthenticate)
	 * @see DIDAuthenticate
	 * @see DIDAuthenticateResponse
	 * @see EAC2InputType
	 */	
	@Test
	public void testSALServiceNull_9() {
		wsCtx.getMessageContext().clear();
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		salservice.getResponse().clear();
		
		assertNotNull("No eID card inserted",eCardHandler.getECard());
		
		ArrayList<String> nullList = new ArrayList<String>();
		nullList.add("certificate");
		
		try {
			makeNull(nullList,EACPhases.EAC_2,false);
		} catch(final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
	}
	
	/**
	 * <p>
	 * <b>Important: </b>Not handled exceptions in {@link TLV} and
	 * {@link WSContainer} preventing this test from completing and forcing the
	 * test suite into a deadlock until the callback in the {@link ECardWorker}
	 * is triggered.
	 * </p>
	 * Function invocation without an ephemeralPublicKey in the second
	 * EAC-phase.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-7, Section 3.6.4 <em>Overview of EAC protocol sequence</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link SALService#didAuthenticate(DIDAuthenticate)} is invoked
	 * with {@link DIDAuthenticate} and without ephemeralPublicKey in the
	 * {@link EAC2InputType}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The authentication process fails and can not complete in a normal
	 * way.</li>
	 * </ul>
	 * 
	 * @see TestSALService#didAuthenticate(DIDAuthenticate)
	 * @see DIDAuthenticate
	 * @see DIDAuthenticateResponse
	 * @see EAC2InputType
	 */	
	@Test
	public void testSALServiceNull_10() {
		wsCtx.getMessageContext().clear();
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		salservice.getResponse().clear();
		
		assertNotNull("No eID card inserted",eCardHandler.getECard());
		
		ArrayList<String> nullList = new ArrayList<String>();
		nullList.add("ephemeralPublicKey");
		
		try {
			makeNull(nullList,EACPhases.EAC_2);
		} catch(final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
	}	
	
	/**
	 * Function invocation without the signature in the second EAC-phase.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-7, Section 3.6.4 <em>Overview of EAC protocol sequence</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link SALService#didAuthenticate(DIDAuthenticate)} is invoked
	 * with {@link DIDAuthenticate} and without the signature in the
	 * {@link EAC2InputType}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The authentication process executes and completes normally
	 * because this element is not required.</li>
	 * </ul>
	 * 
	 * @see TestSALService#didAuthenticate(DIDAuthenticate)
	 * @see DIDAuthenticate
	 * @see DIDAuthenticateResponse
	 * @see EAC2InputType
	 */		
	@Test
	public void testSALServiceNull_11() {
		wsCtx.getMessageContext().clear();
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		salservice.getResponse().clear();
		
		assertNotNull("No eID card inserted",eCardHandler.getECard());
		
		ArrayList<String> nullList = new ArrayList<String>();
		nullList.add("signature");
		
		try {	
			makeNull(nullList,EACPhases.EAC_2,false);
		} catch(final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
	}
	
	/**
	 * Function invocation without authentication protocol data.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-7, Section 3.6.4 <em>Overview of EAC protocol sequence</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link SALService#didAuthenticate(DIDAuthenticate)} is invoked
	 * with {@link DIDAuthenticate}.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The function completes and returns null.</li>
	 * </ul>
	 * 
	 * @see TestSALService#didAuthenticate(DIDAuthenticate)
	 * @see DIDAuthenticate
	 * @see ConnectionHandleType
	 * @see DIDAuthenticateResponse
	 */
	@Test
	public void testSALServiceInvalidParameter_1() {
		wsCtx.getMessageContext().clear();
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		salservice.getResponse().clear();
		salservice.setConfigFlag(ConfigTestcase.DELETE_DATA);

		assertTrue("No session", wsCtx.getMessageContext().get(ECardSession.class.getName())!=null);
		assertNotNull("No eID card inserted",eCardHandler.getECard());
		
		try{
			makeNull(null, EACPhases.EAC_1);
		} catch (final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
		
		HashMap<DIDAuthenticate,DIDAuthenticateResponse> response = salservice.getResponse();
		
		assertEquals(0, response.size());
	}
	
	/**
	 * <p>
	 * <b>Important: </b>The thread does not return into the test case and thus
	 * the test case remains in a deadlock until the {@link ECardWorker} times
	 * out.
	 * </p>
	 * Function invocation with unknown authentication protocol data.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-7, Section 3.6.4 <em>Overview of EAC protocol sequence</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link SALService#didAuthenticate(DIDAuthenticate)} is invoked
	 * with {@link DIDAuthenticate} and {@link DIDAuthenticationDataType} as
	 * unknown authentication protocol data.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The function returns an {@link DIDAuthenticateResponse} with
	 * {@link EcAPIProvider#ECARD_API_RESULT_ERROR} as major result.</li>
	 * </ul>
	 * 
	 * @see TestSALService#didAuthenticate(DIDAuthenticate)
	 * @see DIDAuthenticate
	 * @see ConnectionHandleType
	 * @see DIDAuthenticationDataType
	 * @see DIDAuthenticateResponse
	 */
	@Test
	public void testSALServiceInvalidParameter_2() {
		wsCtx.getMessageContext().clear();
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		salservice.getResponse().clear();
		salservice.setConfigFlag(ConfigTestcase.UNKNOWN_AUTH_PROT_DATA);
		
		assertNotNull("No eID card inserted",eCardHandler.getECard());
		try {
			makeAlternativeInvocationFail();
		} catch(final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
		
		HashMap<DIDAuthenticate,DIDAuthenticateResponse> response = salservice.getResponse();
		for(Entry<DIDAuthenticate,DIDAuthenticateResponse> entry: response.entrySet())
		{
			assertNotNull("parameter is null", entry.getKey());
			
			assertNotNull("Response is null.",entry.getValue());
			assertNotNull("no result", entry.getValue().getResult());
			assertEquals("wrong result",entry.getValue().getResult().getResultMajor(),EcAPIProvider.ECARD_API_RESULT_ERROR);			
		}
	}	
	
	
	/**
	 * <p>
	 * <b>Important: </b>Not handled exceptions in {@link TLV} and
	 * {@link WSContainer} preventing this test from completing and forces the
	 * test suite in a deadlock until the timeout is triggered.
	 * </p>
	 * Function invocation with missing authentication data in the first
	 * EAC-Phase. <br/>
	 * <br/>
	 * <b>References: </b>TR-03112-7, Section 3.6.4 <em>Overview of EAC protocol sequence</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link SALService#didAuthenticate(DIDAuthenticate)} is invoked
	 * with {@link DIDAuthenticate} and the {@link EAC1InputType} is missing all
	 * data.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The authentication process does not complete normally.</li>
	 * </ul>
	 * 
	 * @see SALService#didAuthenticate(DIDAuthenticate)
	 * @see DIDAuthenticate
	 * @see DIDAuthenticateResponse
	 */
	@Test
	public void testSALServiceInvalidParameter_3() {
		wsCtx.getMessageContext().clear();
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		salservice.getResponse().clear();
		salservice.setConfigFlag(ConfigTestcase.RENEW_DATA_FIRST_PHASE_OF_EAC);
		
		assertNotNull("No eID card inserted",eCardHandler.getECard());
		try {
			makeAlternativeInvocationFail();
		} catch(final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}		
	}	
	
	/**
	 * <p>
	 * <b>Important: </b>Not handled exceptions in {@link TLV} and
	 * {@link WSContainer} preventing this test from completing and forces the
	 * test suite in a deadlock until the timeout is triggered.
	 * </p>
	 * Function invocation with missing authentication data in the second
	 * EAC-Phase. <br/>
	 * <br/>
	 * <b>References: </b>TR-03112-7, Section 3.6.4 <em>Overview of EAC protocol sequence</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link SALService#didAuthenticate(DIDAuthenticate)} is invoked
	 * with {@link DIDAuthenticate} and the {@link EAC2InputType} is missing all
	 * data.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The authentication process does not complete normally.</li>
	 * </ul>
	 * 
	 * @see SALService#didAuthenticate(DIDAuthenticate)
	 * @see DIDAuthenticate
	 * @see DIDAuthenticateResponse
	 */
	@Test
	public void testSALServiceInvalidParameter_4() {
		wsCtx.getMessageContext().clear();
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		salservice.getResponse().clear();
		salservice.setConfigFlag(ConfigTestcase.RENEW_DATA_SECOND_PHASE_OF_EAC);

		assertNotNull("No eID card inserted",eCardHandler.getECard());
		try {
			makeAlternativeInvocationFail();
		} catch(final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}		
	}
	

	
	/**
	 * Function invocation with valid parameters.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-7, Section 3.6.4 <em>Overview of EAC protocol sequence</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link SALService#didAuthenticate(DIDAuthenticate)} is invoked
	 * with {@link DIDAuthenticate} and the correct authentication protocol data
	 * according to the EAC-phases.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The authentication completes normally and the alternative call
	 * succeeds without an error.</li>
	 * </ul>
	 * 
	 * @see SALService#didAuthenticate(DIDAuthenticate)
	 * @see DIDAuthenticate
	 * @see DIDAuthenticateResponse
	 */
	@Test
	public void testSALServiceValidParameter_1() {
		wsCtx.getMessageContext().clear();
		wsCtx.getMessageContext().put(ECardSession.class.getName(), session);
		salservice.getResponse().clear();
		assertNotNull("No eID card inserted",eCardHandler.getECard());
		
		try {
			makeAlternativeInvocationSuccess();
		} catch(final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
		HashMap<DIDAuthenticate,DIDAuthenticateResponse> response = salservice.getResponse();
			
		assertFalse("no response from didAuthenticate",response.isEmpty());
		System.out.println(response.size());
		assertTrue("response",response.size()>1);
		for(Entry<DIDAuthenticate,DIDAuthenticateResponse> entry : response.entrySet())
		{
			assertNotNull("no authentication protocol data",entry.getKey().getAuthenticationProtocolData());
			assertNotNull("no authentication protocol return data",entry.getValue().getAuthenticationProtocolData());
			
			if(entry.getKey().getAuthenticationProtocolData() instanceof EAC1InputType){//EAC-phase 1 - Extended PACE - Protocol
				logger.log(Level.INFO,"First EAC-Phase");
				
				assertTrue("EAC1Output",entry.getValue().getAuthenticationProtocolData() instanceof EAC1OutputType);
				
				assertNotNull("no certificate",((EAC1InputType)entry.getKey().getAuthenticationProtocolData()).getCertificate());
				assertNotNull("no certificate description",((EAC1InputType)entry.getKey().getAuthenticationProtocolData()).getCertificateDescription());
				
				assertTrue("wrong authentication protocol return data", entry.getValue().getAuthenticationProtocolData() instanceof EAC1OutputType);
			
				assertNotNull("no Certificate Holder Authorization Template",((EAC1OutputType)entry.getValue().getAuthenticationProtocolData()).getCertificateHolderAuthorizationTemplate());
				assertNotNull("no EF.CardAccess",((EAC1OutputType)entry.getValue().getAuthenticationProtocolData()).getEFCardAccess());
				assertNotNull("no IDPICC",((EAC1OutputType)entry.getValue().getAuthenticationProtocolData()).getIDPICC());
				assertNotNull("no Challange",((EAC1OutputType)entry.getValue().getAuthenticationProtocolData()).getChallenge());
				
			}else if(entry.getKey().getAuthenticationProtocolData() instanceof EAC2InputType){//EAC-phase 2 - combination of Terminal and Chip Authentication
				logger.log(Level.INFO,"Second EAC-Phase");
				assertTrue("EAC2Output",entry.getValue().getAuthenticationProtocolData() instanceof EAC2OutputType);
					
				assertNotNull("no ephemeral public key", ((EAC2InputType)entry.getKey().getAuthenticationProtocolData()).getEphemeralPublicKey());
					
				assertTrue("wrong authentication protocol return data", entry.getValue().getAuthenticationProtocolData() instanceof EAC2OutputType);
					
				assertNotNull("no EFCardSecurity",((EAC2OutputType)entry.getValue().getAuthenticationProtocolData()).getEFCardSecurity());
				assertNotNull("no Authentication Token",((EAC2OutputType)entry.getValue().getAuthenticationProtocolData()).getAuthenticationToken());
				assertNotNull("no Nonce",((EAC2OutputType)entry.getValue().getAuthenticationProtocolData()).getNonce());
					
				if(((EAC2InputType)entry.getKey().getAuthenticationProtocolData()).getSignature()==null){//EAC-phase 2b - conditional additional message with signature 
					assertTrue("EAC2Output",entry.getValue().getAuthenticationProtocolData() instanceof EAC2OutputType);
					assertNotNull("No Signature and no Challange from the PICC",((EAC2OutputType)entry.getValue().getAuthenticationProtocolData()).getChallenge());	
				}
					
			}else {
				assertTrue("Unknown parameter",entry.getKey().getAuthenticationProtocolData() instanceof EACAdditionalInputType);
				assertNotNull("no signature",((EACAdditionalInputType)entry.getValue().getAuthenticationProtocolData()).getSignature());
			}
			assertNotNull("no result",entry.getValue().getResult());
			assertNotNull("no major result",entry.getValue().getResult().getResultMajor());
			assertEquals("no correct major result", EcAPIProvider.ECARD_API_RESULT_OK, entry.getValue().getResult().getResultMajor());
		}
	}
	
	/**
	 * Function invocation with null as inserted value.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-3, Section 3.1.1 <em>InitializeFramework</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card test is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link ManagementService#initializeFramework(RequestType)} is
	 * invoked with <b>null</b> as inserted value.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>An {@link NullPointerException} is thrown.</li>
	 * </ul>
	 * 
	 * @see ManagementService#initializeFramework(RequestType)
	 */
	@Test
	public void testManagementServiceNull_1() {
		wsCtx.getMessageContext().clear();
		
		assertNotNull("No eID card inserted",eCardHandler.getECard());
		
		try{
			managementservice.initializeFramework(null);
		} catch(final NullPointerException e) {
			logger.log(Level.INFO, "NullPointerException is thrown: "+e.getMessage());
			return;
		} catch(final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
		fail("No NullPointerException is thrown.");	//fail if no exception is thrown.
	}
	
	/**
	 * Function invocation without an requestID.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-3, Section 3.1.1 <em>InitializeFramework</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link ManagementService#initializeFramework(RequestType)} is
	 * invoked with {@link RequestType} which is missing an requestID.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The function completes normally.</li>
	 * </ul>
	 * 
	 * @see ManagementService#initializeFramework(RequestType)
	 * @see RequestType
	 * @see InitializeFrameworkResponse
	 */	
	@Test
	public void testManagementServiceInvalidParameter_1() {
		wsCtx.getMessageContext().clear();
		
		RequestType parameters = new RequestType();
		InitializeFrameworkResponse ifr = null;
		try{
			ifr = managementservice.initializeFramework(parameters);
		} catch (final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
		
		assertNotNull("Response is null.",ifr);
	}
	
	/**
	 * Function invocation with correct parameters.<br/>
	 * <br/>
	 * <b>References: </b>TR-03112-3, Section 3.1.1 <em>InitializeFramework</em><br/>
	 * <b>Preconditions:</b>
	 * <ul>
	 * <li>A single basic card reader is connected to the eID-Client system.</li>
	 * <li>A single active test eID-Card is connected to the card reader.</li>
	 * </ul>
	 * <b>TestStep: </b>
	 * <ul>
	 * <li>The {@link ManagementService#initializeFramework(RequestType)} is
	 * invoked with {@link RequestType} which has valid parameters.</li>
	 * </ul>
	 * <b>Expected Result: </b>
	 * <ul>
	 * <li>The function completes normally.</li>
	 * </ul>
	 * 
	 * @see ManagementService#initializeFramework(RequestType)
	 * @see RequestType
	 * @see InitializeFrameworkResponse
	 */
	@Test
	public void testManagementServiceValidParameter_1() {
		wsCtx.getMessageContext().clear();
		
		RequestType parameters = new RequestType();
		
		Random sr = new Random();
		byte[] reqID = new byte[32];
		sr.nextBytes(reqID);
		parameters.setRequestID(Hex.toString(reqID));
		
		InitializeFrameworkResponse ifr = null;
		
		assertNotNull("No eID card inserted",eCardHandler.getECard());
		
		try{
			ifr = managementservice.initializeFramework(parameters);
		} catch (final Throwable t) {
			fail("Unexpected Throwable is thrown: "+t.getMessage());
		}
		assertNotNull("Response is null",ifr);
	}
	
	/**
	 * Executes the alternative invocation and sets the given arguments
	 * in the authentication protocol data to null.
	 * 
	 * @param nullList
	 *            - List of arguments.
	 * @param flag
	 *            - Flag to distinguish between {@link EAC1InputType} and
	 *            {@link EAC2InputType}.
	 *            
	 * @see {@link SALService#didAuthenticate(DIDAuthenticate)
	 * @see {@link DIDAuthenticate}
	 * @see {@link EAC1InputType}
	 * @see {@link EAC2InputType}
	 */
	public void makeNull(ArrayList<String> nullList, EACPhases flag) {
		makeNull(nullList,flag,true);
	}
	
	/**
	 * Does the regular alternative invocation, in german <em>Alternativer Aufruf</em>,
	 * with the eID-Client.
	 */
	public void makeAlternativeInvocationSuccess() {
		makeNull(null,null,false);
	}
	
	public void makeAlternativeInvocationFail() {
		makeNull(null,null,true);
	}
	
	/**
	 * Executes the alternative invocation and sets the given arguments 
	 * in the authentication protocol data to null.
	 * 
	 * @param nullList
	 *            - List of arguments.
	 * @param flag
	 *            - Flag to distinguish between {@link EAC1InputType},
	 *            {@link EAC2InputType} and {@link EACAdditionalInputType}.
	 * @param processFailed
	 * 			  - Indicates that the missing attribute causes the alternative 
	 * 				invocation to fail.
	 *            
	 * @see {@link SALService#didAuthenticate(DIDAuthenticate)
	 * @see {@link DIDAuthenticate}
	 * @see {@link EAC1InputType}
	 * @see {@link EAC2InputType}
	 */
	public void makeNull(ArrayList<String> nullList, EACPhases flag, boolean processFailed) {
		
		URL tcTokenURL = null;
		
		try {
			tcTokenURL = new URL(serviceURL);
		} catch (final MalformedURLException e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
		
		try {
			if(flag!=null) {
			switch(flag){
				//Change EAC1InputType
				case EAC_1: {
					salservice.setNullListEAC1(nullList);
					break;
				}
				//Change EAC2InputType
				case EAC_2: {
					salservice.setNullListEAC2(nullList);
					break;
				}
				//Change EACAdditionalInputType
				case EAC_A: {
					salservice.setNullListEAC2b(nullList);
					break;
				}
				//Do nothing
				default: break;
			}
			}
			String refreshURL = ECardWorker.start(tcTokenURL);
			assertNotNull("no refresh URL", refreshURL);

			logger.log(Level.INFO,"refreshURL: " + refreshURL);
			if(processFailed){
				assertTrue("process is succeded", refreshURL.toLowerCase().indexOf("resultmajor=ok") < 0);
			} else {

				assertTrue("process failed", refreshURL.toLowerCase().indexOf("resultmajor=ok") > 0);
				final URL refresh = new URL(refreshURL);
				final HttpURLConnection uc = (HttpsURLConnection) Util.openURL(refresh);
				uc.setInstanceFollowRedirects(true);
				final Object content = uc.getContent();
				logger.log(Level.INFO, "HTTP Response " + uc.getResponseCode() + " " + uc.getResponseMessage());
				if (content instanceof InputStream) {
					final Scanner scanner = new Scanner((InputStream) content, "UTF-8").useDelimiter("\\A");
					System.out.println(scanner.next());
					scanner.close();
				} else {
					System.out.println(content);
				}
			}

			
		} catch(final NullPointerException e) {
			logger.log(Level.INFO, "NullPointerException occured: "+e.getMessage());
			return;
		}
		catch (final CertificateException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (final FileNotFoundException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (final IOException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (final GeneralSecurityException e) {
			e.printStackTrace();
			fail(e.getMessage());
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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
