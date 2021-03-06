package com.epichust.cxf.ws.sap.impl;

import java.net.MalformedURLException;
import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import com.epichust.cxf.ws.sap.ISapToMesService;
import javax.xml.ws.Service;

/**
 * This class was generated by Apache CXF 3.3.6
 * 2020-05-06T23:05:25.815+08:00
 * Generated source version: 3.3.6
 *
 */
@WebServiceClient(name = "sapToMesWebService",
                  wsdlLocation = "http://192.168.5.28:8180/fingu/ws/sapToMesWebService?wsdl",
                  targetNamespace = "http://impl.sap.ws.cxf.epichust.com/")
public class SapToMesWebService extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://impl.sap.ws.cxf.epichust.com/", "sapToMesWebService");
    public final static QName SapToMesServiceImplPort = new QName("http://impl.sap.ws.cxf.epichust.com/", "SapToMesServiceImplPort");
    static {
        URL url = null;
        try {
            url = new URL("http://192.168.5.28:8180/fingu/ws/sapToMesWebService?wsdl");
        } catch (MalformedURLException e) {
            java.util.logging.Logger.getLogger(SapToMesWebService.class.getName())
                .log(java.util.logging.Level.INFO,
                     "Can not initialize the default wsdl from {0}", "http://192.168.5.28:8180/fingu/ws/sapToMesWebService?wsdl");
        }
        WSDL_LOCATION = url;
    }

    public SapToMesWebService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public SapToMesWebService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public SapToMesWebService() {
        super(WSDL_LOCATION, SERVICE);
    }

    public SapToMesWebService(WebServiceFeature ... features) {
        super(WSDL_LOCATION, SERVICE, features);
    }

    public SapToMesWebService(URL wsdlLocation, WebServiceFeature ... features) {
        super(wsdlLocation, SERVICE, features);
    }

    public SapToMesWebService(URL wsdlLocation, QName serviceName, WebServiceFeature ... features) {
        super(wsdlLocation, serviceName, features);
    }




    /**
     *
     * @return
     *     returns ISapToMesService
     */
    @WebEndpoint(name = "SapToMesServiceImplPort")
    public ISapToMesService getSapToMesServiceImplPort() {
        return super.getPort(SapToMesServiceImplPort, ISapToMesService.class);
    }

    /**
     *
     * @param features
     *     A list of {@link WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns ISapToMesService
     */
    @WebEndpoint(name = "SapToMesServiceImplPort")
    public ISapToMesService getSapToMesServiceImplPort(WebServiceFeature... features) {
        return super.getPort(SapToMesServiceImplPort, ISapToMesService.class, features);
    }

}
