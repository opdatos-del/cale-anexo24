package com.jovycandy.anexo24.operations.pediments.infrastructure.persistence;


import com.jovycandy.anexo24.operations.pediments.domain.model.PedimentoError;
import com.jovycandy.anexo24.operations.pediments.domain.model.PedimentoFila;
import com.jovycandy.anexo24.operations.pediments.domain.port.PedimentoReglasRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.SqlReturnResultSet;
import org.springframework.stereotype.Repository;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.sql.CallableStatement;
import java.sql.Types;

import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Adaptador del SP batch read-only de reglas de pedimentos en CALE_IMMEX. */
@Repository
public class PedimentoReglasJdbcAdapter implements PedimentoReglasRepository {
    static final String PROCEDURE = "dbo.APP24_Q_PEDIMENTO_VALIDAR_REGLAS";
    private static final String RESULTADO = "errores";
    private final JdbcTemplate jdbcTemplate;


    public PedimentoReglasJdbcAdapter(@Qualifier("jdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<PedimentoError> validar(List<PedimentoFila> filas) {
        String xml = serializar(filas);
        Map<String, Object> result = jdbcTemplate.call(connection -> {
            CallableStatement statement = connection.prepareCall("{call " + PROCEDURE + "(?)}");
            statement.setString(1, xml);
            return statement;
        }, List.of(
                new SqlParameter("FilasXml", Types.SQLXML),
                new SqlReturnResultSet(RESULTADO, (rs, row) -> new PedimentoError(
                        rs.getString("hoja"), rs.getObject("fila", Integer.class), rs.getString("columna"),
                        "no almacenado", rs.getString("codigo"), rs.getString("mensaje")))));
        return (List<PedimentoError>) result.getOrDefault(RESULTADO, List.of());
    }

    static String serializar(List<PedimentoFila> filas) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            Document document = factory.newDocumentBuilder().newDocument();
            Element root = document.createElement("filas");
            document.appendChild(root);
            for (PedimentoFila fila : filas) {
                Element filaElement = document.createElement("fila");
                filaElement.setAttribute("hoja", valueOrEmpty(fila.hoja()));
                filaElement.setAttribute("fila", Integer.toString(fila.numero()));
                Element datos = document.createElement("datos");
                append(document, datos, "UnidadComercial", fila.datos().get("UnidadComercial"));
                append(document, datos, "UnidadTarifa", fila.datos().get("UnidadTarifa"));
                append(document, datos, "TipoOperacion", fila.datos().get("TipoOperacion"));
                append(document, datos, "Clave", fila.datos().get("Clave"));
                filaElement.appendChild(datos);
                root.appendChild(filaElement);
            }
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
            var transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            StringWriter output = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(output));
            return output.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("No fue posible serializar el lote XML de validación.", exception);
        }
    }

    private static void append(Document document, Element parent, String name, String value) {
        Element element = document.createElement(name);
        element.setTextContent(valueOrEmpty(value));
        parent.appendChild(element);
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }
}
