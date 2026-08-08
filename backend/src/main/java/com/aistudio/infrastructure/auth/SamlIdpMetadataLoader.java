package com.aistudio.infrastructure.auth;

import com.aistudio.domain.common.DomainException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import javax.xml.parsers.DocumentBuilderFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Component
class SamlIdpMetadataLoader {

    private final RestClient restClient;

    SamlIdpMetadataLoader(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    SamlIdpMetadata load(String metadataUrl) {
        if (metadataUrl == null || metadataUrl.isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "SAML metadata URL is required");
        }
        String xml = restClient.get()
                .uri(metadataUrl.trim())
                .retrieve()
                .body(String.class);
        if (xml == null || xml.isBlank()) {
            throw new DomainException("VALIDATION_ERROR", "SAML metadata response was empty");
        }
        return parse(xml);
    }

    SamlIdpMetadata parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            Document doc = factory.newDocumentBuilder()
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            Element root = doc.getDocumentElement();
            String entityId = root.getAttribute("entityID");
            if (entityId == null || entityId.isBlank()) {
                throw new DomainException("VALIDATION_ERROR", "SAML metadata missing entityID");
            }

            Element idpDescriptor = firstChildByLocalName(root, "IDPSSODescriptor");
            if (idpDescriptor == null) {
                NodeList descriptors = doc.getElementsByTagNameNS("*", "IDPSSODescriptor");
                if (descriptors.getLength() == 0) {
                    descriptors = doc.getElementsByTagName("IDPSSODescriptor");
                }
                if (descriptors.getLength() == 0) {
                    throw new DomainException("VALIDATION_ERROR", "SAML metadata missing IDPSSODescriptor");
                }
                idpDescriptor = (Element) descriptors.item(0);
            }

            String ssoUrl = findSsoUrl(idpDescriptor);
            String certificate = findSigningCertificate(idpDescriptor);
            if (ssoUrl == null || ssoUrl.isBlank()) {
                throw new DomainException("VALIDATION_ERROR", "SAML metadata missing SingleSignOnService URL");
            }
            if (certificate == null || certificate.isBlank()) {
                throw new DomainException("VALIDATION_ERROR", "SAML metadata missing signing X509Certificate");
            }
            return new SamlIdpMetadata(entityId.trim(), ssoUrl.trim(), normalizeCertificate(certificate));
        } catch (DomainException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DomainException("VALIDATION_ERROR", "Failed to parse SAML metadata");
        }
    }

    private static String findSsoUrl(Element idpDescriptor) {
        NodeList services = idpDescriptor.getElementsByTagNameNS("*", "SingleSignOnService");
        if (services.getLength() == 0) {
            services = idpDescriptor.getElementsByTagName("SingleSignOnService");
        }
        String redirectUrl = null;
        String postUrl = null;
        String fallbackUrl = null;
        for (int i = 0; i < services.getLength(); i++) {
            Element service = (Element) services.item(i);
            String binding = service.getAttribute("Binding").toLowerCase(Locale.ROOT);
            String location = service.getAttribute("Location");
            if (location == null || location.isBlank()) {
                continue;
            }
            if (fallbackUrl == null) {
                fallbackUrl = location;
            }
            if (binding.contains("http-redirect")) {
                redirectUrl = location;
            } else if (binding.contains("http-post")) {
                postUrl = location;
            }
        }
        if (redirectUrl != null) {
            return redirectUrl;
        }
        if (postUrl != null) {
            return postUrl;
        }
        return fallbackUrl;
    }

    private static String findSigningCertificate(Element idpDescriptor) {
        NodeList keyDescriptors = idpDescriptor.getElementsByTagNameNS("*", "KeyDescriptor");
        for (int i = 0; i < keyDescriptors.getLength(); i++) {
            Element keyDescriptor = (Element) keyDescriptors.item(i);
            String use = keyDescriptor.getAttribute("use");
            if (use != null && !use.isBlank() && !"signing".equalsIgnoreCase(use)) {
                continue;
            }
            NodeList certs = keyDescriptor.getElementsByTagNameNS("*", "X509Certificate");
            if (certs.getLength() > 0) {
                return certs.item(0).getTextContent();
            }
        }
        NodeList certs = idpDescriptor.getElementsByTagNameNS("*", "X509Certificate");
        if (certs.getLength() > 0) {
            return certs.item(0).getTextContent();
        }
        return null;
    }

    private static Element firstChildByLocalName(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child
                    && child.getLocalName() != null
                    && child.getLocalName().equals(localName)) {
                return child;
            }
        }
        NodeList byTag = parent.getElementsByTagNameNS("*", localName);
        if (byTag.getLength() > 0) {
            return (Element) byTag.item(0);
        }
        return null;
    }

    private static String normalizeCertificate(String certificate) {
        return certificate.replaceAll("\\s+", "");
    }

    record SamlIdpMetadata(String entityId, String singleSignOnUrl, String signingCertificate) {
    }
}
