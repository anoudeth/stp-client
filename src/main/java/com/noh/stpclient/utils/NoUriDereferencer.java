package com.noh.stpclient.utils;

import javax.xml.crypto.Data;
import javax.xml.crypto.OctetStreamData;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import java.io.InputStream;

/**
 * Custom URIDereferencer that handles a null-URI reference by returning
 * the pre-serialized content of the {@code <Document>} element.
 * All other references are delegated to the default dereferencer.
 */
class NoUriDereferencer implements URIDereferencer {

    private final InputStream refData;

    NoUriDereferencer(InputStream refData) {
        this.refData = refData;
    }

    @Override
    public Data dereference(URIReference uriReference, XMLCryptoContext context) throws URIReferenceException {
        if (uriReference.getURI() == null) {
            return new OctetStreamData(refData);
        }
        return XMLSignatureFactory.getInstance("DOM").getURIDereferencer().dereference(uriReference, context);
    }
}