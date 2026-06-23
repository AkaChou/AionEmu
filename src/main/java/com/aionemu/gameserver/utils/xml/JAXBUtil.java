package com.aionemu.gameserver.utils.xml;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.net.URL;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.validation.SchemaFactory;


/**
 * @author ginho1
 * @author Dezalmado (compilation error correction)
 */
public class JAXBUtil {

	private static final Logger log = LoggerFactory.getLogger(JAXBUtil.class);

	public static <T> T unmarshal(InputStream is, Class<T> clazz) {
		try {
			JAXBContext jc = JAXBContext.newInstance(clazz);
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			unmarshaller.setSchema(getSchema(clazz));
			unmarshaller.setEventHandler(new XmlValidationHandler());
			return (T) unmarshaller.unmarshal(is);
		}
		catch (JAXBException e) {
			log.error("Error unmarshalling file.", e);
		}
		return null;
	}

    public static <T> T unmarshal(File file, Class<T> clazz) throws JAXBException {
        try (InputStream is = new java.io.FileInputStream(file)) {
            return unmarshal(is, clazz);
        } catch (java.io.IOException e) {
            throw new JAXBException("Error reading file: " + file.getAbsolutePath(), e);
        }
    }

	public static <T> T unmarshal(String stream, Class<T> clazz) {
		try {
			JAXBContext jc = JAXBContext.newInstance(clazz);
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			unmarshaller.setSchema(getSchema(clazz));
			unmarshaller.setEventHandler(new XmlValidationHandler());
			return (T) unmarshaller.unmarshal(new StringReader(stream));
		}
		catch (JAXBException e) {
			log.error("Error unmarshalling file.", e);
		}
		return null;
	}

	public static <T> void marshal(String file, Class<T> clazz, T object) {
		try {
			JAXBContext jc = JAXBContext.newInstance(clazz);
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setEventHandler(new XmlValidationHandler());
			marshaller.marshal(object, new File(file));
		}
		catch (JAXBException e) {
			log.error("Error marshalling file.", e);
		}
	}

	public static <T> String marshal(Class<T> clazz, T object) {
		try {
			StringWriter sw = new StringWriter();
			JAXBContext jc = JAXBContext.newInstance(clazz);
			Marshaller marshaller = jc.createMarshaller();
			marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			marshaller.setEventHandler(new XmlValidationHandler());
			marshaller.marshal(object, sw);
			return sw.toString();
		}
		catch (JAXBException e) {
			log.error("Error marshalling file.", e);
		}
		return null;
	}

	public static <T> boolean validate(String xml, Class<T> clazz) {
		try {
			JAXBContext jc = JAXBContext.newInstance(clazz);
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			unmarshaller.setSchema(getSchema(clazz));
			unmarshaller.setEventHandler(new XmlValidationHandler());
			unmarshaller.unmarshal(new StringReader(xml));
			return true;
		}
		catch (JAXBException e) {
			log.warn("Validation failed for input XML", e);
			return false;
		}
	}

	public static <T> boolean validate(InputStream is, Class<T> clazz) {
		try {
			JAXBContext jc = JAXBContext.newInstance(clazz);
			Unmarshaller unmarshaller = jc.createUnmarshaller();
			unmarshaller.setSchema(getSchema(clazz));
			unmarshaller.setEventHandler(new XmlValidationHandler());
			unmarshaller.unmarshal(is);
			return true;
		}
		catch (JAXBException e) {
			log.warn("Validation failed for input XML", e);
			return false;
		}
	}
	
	private static Schema getSchema(Class<?> clazz) {
        try {
            SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            StringSchemaOutputResolver ssor = new StringSchemaOutputResolver();
            JAXBContext jaxbContext = JAXBContext.newInstance(clazz);
            jaxbContext.generateSchema(ssor);
            String schemaString = ssor.getSchemaString();
            if (schemaString != null && !schemaString.isEmpty()) {
                InputStream schemaStream = new ByteArrayInputStream(schemaString.getBytes(StandardCharsets.UTF_8));
                return sf.newSchema(new StreamSource(schemaStream));
            }
        } catch (JAXBException | SAXException | IOException e) {
            log.error("Error getting schema for class: " + clazz.getName(), e);
        }
        return null;
    }
	
	public static boolean validateSchema(String xmlString, URL schemaUrl) {
        try {
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(schemaUrl);
            Validator validator = schema.newValidator();
            validator.validate(new StreamSource(new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8))));
            return true;
        } catch (SAXException | java.io.IOException e) {
            log.warn("Schema validation failed: " + e.getMessage());
            return false;
        }
    }
}