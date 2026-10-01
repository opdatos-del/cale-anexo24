package com.jovycandy.anexo24.operations.pediments.infrastructure.persistence;

import com.jovycandy.anexo24.operations.pediments.domain.model.PedimentoFila;
import org.junit.jupiter.api.Test;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PedimentoReglasJdbcAdapterTest {
    @Test
    void serializaCaracteresXmlSinRomperLaEstructura() throws Exception {
        String valor = "& < > \" '";
        PedimentoFila fila = new PedimentoFila(valor, 2, Map.of(
                "UnidadComercial", valor,
                "UnidadTarifa", valor,
                "TipoOperacion", "1",
                "NumeroPedimento", valor,
                "Clave", valor));

        String xml = PedimentoReglasJdbcAdapter.serializar(List.of(fila));
        var factory = DocumentBuilderFactory.newInstance();
        var document = factory.newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        NodeList filas = document.getElementsByTagName("fila");

        assertEquals(1, filas.getLength());
        assertEquals(valor, filas.item(0).getAttributes().getNamedItem("hoja").getNodeValue());
        assertNotNull(document.getElementsByTagName("datos").item(0));
        assertEquals(valor, document.getElementsByTagName("Clave").item(0).getTextContent());
        assertEquals(valor, document.getElementsByTagName("NumeroPedimento").item(0).getTextContent());
    }

    @Test
    void serializaNumeroPedimentoVacioCuandoEstáAusente() throws Exception {
        PedimentoFila fila = new PedimentoFila("Hoja1", 2, Map.of("TipoOperacion", "1", "Clave", "MAT-1"));

        String xml = PedimentoReglasJdbcAdapter.serializar(List.of(fila));
        var document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertEquals("", document.getElementsByTagName("NumeroPedimento").item(0).getTextContent());
    }
}
