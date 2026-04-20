package com.noh.stpclient.utils;

import javax.xml.crypto.Data;
import javax.xml.crypto.OctetStreamData;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.XMLCryptoContext;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Custom URIDereferencer that handles a null-URI reference by returning
 * the pre-serialized content of the {@code <Document>} element.
 * All other references are delegated to the default dereferencer.
 */
public class NoUriDereferencer implements URIDereferencer {

    private final byte[] content;

    public NoUriDereferencer(InputStream inputStream) throws IOException {
        this.content = inputStream.readAllBytes();
    }

    @Override
    public Data dereference(URIReference uriReference, XMLCryptoContext context) throws URIReferenceException {
        return new OctetStreamData(new ByteArrayInputStream(content));
    }
}