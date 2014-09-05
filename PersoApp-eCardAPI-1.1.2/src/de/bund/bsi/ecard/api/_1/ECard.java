
package de.bund.bsi.ecard.api._1;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import iso.std.iso_iec._24727.tech.schema.ResponseType;
import oasis.names.tc.dss._1_0.core.schema.RequestBaseType;
import oasis.names.tc.dss._1_0.core.schema.ResponseBaseType;


/**
 * This class was generated by the JAX-WS RI.
 * JAX-WS RI 2.1.6 in JDK 6
 * Generated source version: 2.0
 * 
 */
@WebService(name = "eCard", targetNamespace = "http://www.bsi.bund.de/ecard/api/1.1")
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
public interface ECard {


    /**
     * 
     * @param parameters
     * @return
     *     returns de.bund.bsi.ecard.api._1.GetCertificateResponse
     */
    @WebMethod(operationName = "GetCertificate", action = "http://www.bsi.bund.de/ecard/api/1.1#GetCertificate")
    @WebResult(name = "GetCertificateResponse", targetNamespace = "http://www.bsi.bund.de/ecard/api/1.1", partName = "parameters")
    public GetCertificateResponse getCertificate(
        @WebParam(name = "GetCertificate", targetNamespace = "http://www.bsi.bund.de/ecard/api/1.1", partName = "parameters")
        GetCertificate parameters);

    /**
     * 
     * @param parameters
     * @return
     *     returns de.bund.bsi.ecard.api._1.SignResponse
     */
    @WebMethod(operationName = "SignRequest", action = "http://www.bsi.bund.de/ecard/api/1.1#SignRequest")
    @WebResult(name = "SignResponse", targetNamespace = "http://www.bsi.bund.de/ecard/api/1.1", partName = "parameters")
    public SignResponse signRequest(
        @WebParam(name = "SignRequest", targetNamespace = "http://www.bsi.bund.de/ecard/api/1.1", partName = "parameters")
        RequestBaseType parameters);

    /**
     * 
     * @param parameters
     * @return
     *     returns oasis.names.tc.dss._1_0.core.schema.ResponseBaseType
     */
    @WebMethod(operationName = "VerifyRequest", action = "http://www.bsi.bund.de/ecard/api/1.1#VerifyRequest")
    @WebResult(name = "VerifyResponse", targetNamespace = "http://www.bsi.bund.de/ecard/api/1.1", partName = "parameters")
    public ResponseBaseType verifyRequest(
        @WebParam(name = "VerifyRequest", targetNamespace = "http://www.bsi.bund.de/ecard/api/1.1", partName = "parameters")
        VerifyRequest parameters);

    /**
     * 
     * @param parameters
     * @return
     *     returns iso.std.iso_iec._24727.tech.schema.ResponseType
     */
    @WebMethod(operationName = "ShowViewer", action = "http://www.bsi.bund.de/ecard/api/1.1#ShowViewer")
    @WebResult(name = "ShowViewerResponse", targetNamespace = "http://www.bsi.bund.de/ecard/api/1.1", partName = "parameters")
    public ResponseType showViewer(
        @WebParam(name = "ShowViewer", targetNamespace = "http://www.bsi.bund.de/ecard/api/1.1", partName = "parameters")
        ShowViewer parameters);

    /**
     * 
     * @param parameters
     * @return
     *     returns oasis.names.tc.dss._1_0.core.schema.ResponseBaseType
     */
    @WebMethod(operationName = "EncryptRequest", action = "http://www.bsi.bund.de/ecard/api/1.1#EncryptRequest")
    @WebResult(name = "EncryptResponse", targetNamespace = "urn:oasis:names:tc:dss-x:1.0:profiles:encryption:schema#", partName = "parameters")
    public ResponseBaseType encryptRequest(
        @WebParam(name = "EncryptRequest", targetNamespace = "urn:oasis:names:tc:dss-x:1.0:profiles:encryption:schema#", partName = "parameters")
        RequestBaseType parameters);

    /**
     * 
     * @param parameters
     * @return
     *     returns oasis.names.tc.dss._1_0.core.schema.ResponseBaseType
     */
    @WebMethod(operationName = "DecryptRequest", action = "http://www.bsi.bund.de/ecard/api/1.1#DecryptRequest")
    @WebResult(name = "DecryptResponse", targetNamespace = "urn:oasis:names:tc:dss-x:1.0:profiles:encryption:schema#", partName = "parameters")
    public ResponseBaseType decryptRequest(
        @WebParam(name = "DecryptRequest", targetNamespace = "urn:oasis:names:tc:dss-x:1.0:profiles:encryption:schema#", partName = "parameters")
        RequestBaseType parameters);

}