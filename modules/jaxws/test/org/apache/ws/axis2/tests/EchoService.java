
package org.apache.ws.axis2.tests;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;


/**
 * This class was generated by the JAXWS SI.
 * JAX-WS RI 2.0-b26-ea3
 * Generated source version: 2.0
 * 
 */
@WebServiceClient(name = "EchoService", targetNamespace = "http://ws.apache.org/axis2/tests", wsdlLocation = "\\work\\apps\\eclipse\\workspace\\axis2-live\\modules\\jaxws\\test-resources\\wsdl\\WSDLTests.wsdl")
public class EchoService
    extends Service
{

    private final static URL WSDL_LOCATION;
    private final static QName ECHOSERVICE = new QName("http://ws.apache.org/axis2/tests", "EchoService");
    private final static QName ECHOPORT = new QName("http://ws.apache.org/axis2/tests", "EchoPort");

    static {
        URL url = null;
        try {
            url = new URL("file:/C:/work/apps/eclipse/workspace/axis2-live/modules/jaxws/test-resources/wsdl/WSDLTests.wsdl");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        WSDL_LOCATION = url;
    }

    public EchoService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public EchoService() {
        super(WSDL_LOCATION, ECHOSERVICE);
    }

    /**
     * 
     * @return
     *     returns EchoPort
     */
    @WebEndpoint(name = "EchoPort")
    public EchoPort getEchoPort() {
        return (EchoPort)super.getPort(ECHOPORT, EchoPort.class);
    }

}
