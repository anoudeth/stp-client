# Project: Spring Boot SOAP Integration Service
Role: Senior Backend Developer & Security Architect

## 1. Tech Stack & Versioning
- Framework: Spring Boot 3.x (Java 21+)
- SOAP Library: Spring Web Services (Spring WS)
- Build Tool: Maven (pom.xml)
- XML Binding: JAXB (Jakarta XML Binding)
- Security: Spring Security + WSS4J (for WS-Security)

## 2. SOAP Integration Standards
- Client Implementation: Extend 'WebServiceGatewaySupport' for SOAP clients.
- Package Structure:
    - `com.project.remote`: SOAP Client logic
    - `com.project.config`: Bean configurations (Marshaller, MessageSender)
    - `com.project.model`: Generated JAXB classes from WSDL
- WSDL Management: Use Maven plugins (jaxb2-maven-plugin) to generate classes from WSDL.

## 3. Coding Guidelines
- Architecture: Use Service-Repository pattern.
- Error Handling: Use @EndpointExceptionResolver for SOAP faults.
- Logging: Log all incoming/outgoing XML payloads in 'DEBUG' mode (sanitize sensitive data).