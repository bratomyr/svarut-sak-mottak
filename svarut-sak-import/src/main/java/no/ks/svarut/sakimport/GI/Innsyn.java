package no.ks.svarut.sakimport.GI;

import no.geointegrasjon.rep.arkiv.innsyn.xml_wsdl._2012_01_31.*;
import no.geointegrasjon.rep.arkiv.kjerne.xml_schema._2012_01.JournalpostListe;
import no.geointegrasjon.rep.felles.filter.xml_schema._2012_01.SoekeOperatorEnum;
import no.geointegrasjon.rep.felles.filter.xml_schema._2012_01.Soekefelt;
import no.geointegrasjon.rep.felles.filter.xml_schema._2012_01.Soekskriterie;
import no.geointegrasjon.rep.felles.filter.xml_schema._2012_01.SoekskriterieListe;
import no.geointegrasjon.rep.felles.teknisk.xml_schema._2012_01.ArkivKontekst;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.slf4j.LoggerFactory;

public class Innsyn {

    private org.slf4j.Logger log = LoggerFactory.getLogger(Innsyn.class);
    private ArkivInnsynPort service;

    private SakImportConfig sakImportConfig;
    private ArkivKontekst arkivKontekst;

    public Innsyn(SakImportConfig sakImportConfig, ArkivKontekst arkivKontekst) {
        this.sakImportConfig = sakImportConfig;
        this.arkivKontekst = arkivKontekst;
        this.service = createInnsynService();
    }

    public JournalpostListe sok(String forsendelseid) throws ApplicationException, ImplementationException, ValidationException, SystemException, FinderException, OperationalException {

        final SoekskriterieListe sok = new SoekskriterieListe();
        final Soekskriterie soekskriterie = new Soekskriterie();

        final Soekefelt soekefelt = new Soekefelt();
        soekefelt.setFeltnavn("Korrespondansepart.conversationId");
        soekefelt.setFeltverdi(forsendelseid);
        soekskriterie.setKriterie(soekefelt);
        soekskriterie.setOperator(SoekeOperatorEnum.EQ);
        sok.getListe().add(soekskriterie);
        try {
            final JournalpostListe journalpostListe = service.finnJournalposter(sok, true, false, false, false, arkivKontekst);
            return journalpostListe;
        } catch(FinderException e){
            log.warn("Klarte ikke å finne journalpost", e);
            return new JournalpostListe();
        }
    }

    private ArkivInnsynPort createInnsynService() {
        JaxWsProxyFactoryBean factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(ArkivInnsynPort.class);
        factory.setAddress(sakImportConfig.getSakInnsynUrl());
        factory.setUsername(sakImportConfig.getSakBrukernavn());
        factory.setPassword(sakImportConfig.getSakPassord());
        if(sakImportConfig.isDebug()) {
            log.debug("Adding debug logging for cxf");
            LoggingInInterceptor loggingInInterceptor = new LoggingInInterceptor();
            loggingInInterceptor.setPrettyLogging(true);
            LoggingOutInterceptor loggingOutInterceptor = new LoggingOutInterceptor();
            loggingOutInterceptor.setPrettyLogging(true);
            factory.getInInterceptors().add(loggingInInterceptor);
            factory.getOutInterceptors().add(loggingOutInterceptor);
        }
        ArkivInnsynPort serviceV3 = (ArkivInnsynPort) factory.create();
        Client proxy = ClientProxy.getClient(serviceV3);
        HTTPConduit conduit = (HTTPConduit) proxy.getConduit();

        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setConnectionTimeout(120000);
        httpClientPolicy.setReceiveTimeout(10 * 60 * 1000);
        conduit.setClient(httpClientPolicy);
        return serviceV3;
    }
}
