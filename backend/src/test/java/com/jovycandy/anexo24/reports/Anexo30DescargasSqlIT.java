package com.jovycandy.anexo24.reports;

import org.junit.jupiter.api.*;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MSSQLServerContainer;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Verifica el SP versionado de descargas A31 sin reconstruir snapshots. */
class Anexo30DescargasSqlIT {
    private static final String DB = "CALE_IMMEX";
    static final MSSQLServerContainer<?> SQL = new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense();
    private static boolean disponible;

    @BeforeAll static void iniciar() throws Exception {
        disponible = DockerClientFactory.instance().isDockerAvailable();
        Assumptions.assumeTrue(disponible, "Docker no disponible localmente"); SQL.start();
        try(Connection c=conectar("master"); Statement s=c.createStatement()){s.execute("CREATE DATABASE "+DB);}
        try(Connection c=conectar(DB); Statement s=c.createStatement()) {
            s.execute("CREATE TABLE dbo.A31_ENTRADAS(Entradaskey BIGINT IDENTITY PRIMARY KEY,Descarga varchar(5) NULL,Tipooperacion varchar(2) NULL,Pedimentoarmado varchar(30) NULL,Fecha datetime NULL,Fracccion varchar(10) NULL,Valocomercial numeric(18,4) NULL,Clavepedimento varchar(5) NULL,IVAFP21 numeric(18,4) NULL,IVAFP22 numeric(18,4) NULL,SALDO numeric(18,4) NULL,PEDIMENTOORIGINAL varchar(30) NULL,FECHAORIGINAL datetime NULL,ESAF varchar(5) NULL,OPERACION bigint NULL,PARTIDA varchar(10) NULL)");
            s.execute("CREATE TABLE dbo.A31_DESCARGASF(A31_FRACCIONKEY BIGINT IDENTITY PRIMARY KEY,TIPO varchar(2) NULL,CLAVEPEDIMENTO varchar(5) NULL,EJERCICIO varchar(5) NULL,PERIODO varchar(5) NULL,FRACCION varchar(10) NULL,VALOR numeric(18,4) NULL,AF varchar(5) NULL,ARCHIVO varchar(50) NULL)");
            s.execute("CREATE TABLE dbo.A31_DESCARGAS(A31_DESCARGAKEY BIGINT IDENTITY PRIMARY KEY,ENTRADALINK BIGINT NULL,FRACCION varchar(10) NULL,VALORDESCARGADO numeric(18,4) NULL,A31_FRACCIONLINK BIGINT NULL)");
            aplicarArchivo(c, rutaSql());
        }
    }
    @AfterAll static void detener(){if(disponible)SQL.stop();}
    @BeforeEach void sembrar() throws Exception {
        try(Connection c=conectar(DB); Statement s=c.createStatement()) {
            s.execute("TRUNCATE TABLE dbo.A31_DESCARGAS; TRUNCATE TABLE dbo.A31_ENTRADAS; TRUNCATE TABLE dbo.A31_DESCARGASF");
            s.execute("INSERT dbo.A31_ENTRADAS(Pedimentoarmado,PEDIMENTOORIGINAL,Fecha,Clavepedimento,Fracccion,Valocomercial,SALDO,ESAF,PARTIDA) VALUES "
                + "('PED-UNO','ORIG-UNO','2026-01-01','A1','11111111',100.0000,90.0000,'E1','P1'),"
                + "('PED-DOS','ORIG-DOS','2026-02-01','B2','22222222',200.0000,180.0000,'E2','P2'),"
                + "('PED-TRES','ORIG-TRES','2026-03-01','C3','33333333',300.0000,270.0000,'E3','P3')");
            s.execute("INSERT dbo.A31_DESCARGASF(TIPO,CLAVEPEDIMENTO,EJERCICIO,PERIODO,FRACCION,VALOR,AF,ARCHIVO) VALUES "
                + "('F4','A1','2026','01','11111111',100.0000,'AF1','uno.txt'),"
                + "('A3','B2','2025','12','22222222',200.0000,'AF2','dos.txt'),"
                + "('F4','C3','2024','11','33333333',300.0000,'AF3','tres.txt')");
            s.execute("INSERT dbo.A31_DESCARGAS(ENTRADALINK,FRACCION,VALORDESCARGADO,A31_FRACCIONLINK) VALUES "
                + "(1,'D-UNO',10.0000,1),(1,'D-UNO-2',20.0000,1),(2,'D-DOS',30.0000,2),(3,'D-TRES',40.0000,3),"
                + "(999,'98000000',123.4567,998),(NULL,'D-SIN-ENT',50.0000,1),(1,'D-SIN-FRA',60.0000,NULL),(NULL,'D-SIN-PAD',70.0000,NULL)");
        }
    }

    @Test void contratoColumnasPadresValidosRepetidosYPrecision() throws Exception {
        Resultado resultado=listar(null,1,20);
        assertEquals(List.of("DESCARGA_KEY","ENTRADA_KEY","A31_FRACCION_KEY","PEDIMENTO","PEDIMENTO_ORIGINAL","FECHA_ENTRADA","CLAVE_PEDIMENTO_ENTRADA","FRACCION_ENTRADA","VALOR_COMERCIAL_ENTRADA","SALDO_PERSISTIDO_A31","ESAF","PARTIDA","FRACCION_DESCARGA","VALOR_DESCARGADO","TIPO_A31","CLAVE_PEDIMENTO_A31","EJERCICIO","PERIODO","FRACCION_A31","VALOR_A31","AF","ARCHIVO"),resultado.columnas());
        assertEquals(8,resultado.total());
        assertEquals(2,resultado.filas().stream().filter(f->Long.valueOf(1).equals(((Number)f.get("ENTRADA_KEY")).longValue())).count());
        assertEquals(3,resultado.filas().stream().filter(f->Long.valueOf(1).equals(((Number)f.get("A31_FRACCION_KEY")).longValue())).count());
        assertEquals(new BigDecimal("10.0000"),fila(resultado,"D-UNO").get("VALOR_DESCARGADO"));
    }

    @Test void conservaDescargasHuerfanasConLeftJoin() throws Exception {
        Resultado resultado=listar("98000000",1,20); Map<String,Object> fila=resultado.filas().getFirst();
        assertEquals(1,resultado.total()); assertNotNull(fila.get("DESCARGA_KEY")); assertEquals("98000000",fila.get("FRACCION_DESCARGA"));
        assertEquals(new BigDecimal("123.4567"),fila.get("VALOR_DESCARGADO"));
        assertNull(fila.get("PEDIMENTO")); assertNull(fila.get("ARCHIVO")); assertNull(fila.get("FECHA_ENTRADA"));
    }

    @Test void conservaLinksNulosYProyeccionDePadresNulos() throws Exception {
        Map<String,Object> sinEntrada=fila(listar("D-SIN-ENT",1,20),"D-SIN-ENT");
        Map<String,Object> sinFraccion=fila(listar("D-SIN-FRA",1,20),"D-SIN-FRA");
        Map<String,Object> sinPadres=fila(listar("D-SIN-PAD",1,20),"D-SIN-PAD");
        assertNull(sinEntrada.get("PEDIMENTO")); assertEquals("uno.txt",sinEntrada.get("ARCHIVO"));
        assertEquals("PED-UNO",sinFraccion.get("PEDIMENTO")); assertNull(sinFraccion.get("ARCHIVO"));
        assertNull(sinPadres.get("PEDIMENTO")); assertNull(sinPadres.get("ARCHIVO"));
    }

    @Test void filtraPorCamposEntradaYFuenteA31YBlank() throws Exception {
        for(String filtro:List.of("PED-UNO","ORIG-DOS","A1","11111111","E1","P1","D-UNO","B2","2025","12","22222222","AF2","dos.txt")) assertTrue(listar(filtro,1,20).total()>0,filtro);
        assertEquals(listar(null,1,20).total(),listar("   ",1,20).total());
    }

    @Test void paginaAntesDelOffsetOrdenDeterministicoYErrores() throws Exception {
        Resultado primera=listar(null,1,2), segunda=listar(null,2,2), alta=listar(null,9,2);
        assertEquals(8,primera.total()); assertEquals(2,primera.filas().size()); assertEquals(2,segunda.filas().size()); assertTrue(alta.filas().isEmpty());
        assertEquals("PED-TRES",primera.filas().get(0).get("PEDIMENTO")); assertEquals("PED-DOS",primera.filas().get(1).get("PEDIMENTO"));
        assertThrows(SQLException.class,()->listar(null,0,20)); assertThrows(SQLException.class,()->listar(null,1,0)); assertThrows(SQLException.class,()->listar(null,1,101));
    }

    private static Map<String,Object> fila(Resultado resultado,String fraccion){return resultado.filas().stream().filter(f->fraccion.equals(f.get("FRACCION_DESCARGA"))).findFirst().orElseThrow();}
    private static Resultado listar(String filtro,int pagina,int tamano)throws SQLException {try(Connection c=conectar(DB);CallableStatement cs=c.prepareCall("{call dbo.APP24_Q_ANEXO30_REVISION_DESCARGAS_LISTAR(?,?,?,?)}")){if(filtro==null)cs.setNull(1,Types.VARCHAR);else cs.setString(1,filtro);cs.setInt(2,pagina);cs.setInt(3,tamano);cs.registerOutParameter(4,Types.BIGINT);cs.execute();List<String> columnas=new ArrayList<>();List<Map<String,Object>> filas=new ArrayList<>();try(ResultSet rs=cs.getResultSet()){ResultSetMetaData meta=rs.getMetaData();for(int i=1;i<=meta.getColumnCount();i++)columnas.add(meta.getColumnLabel(i));while(rs.next()){Map<String,Object> fila=new LinkedHashMap<>();for(String columna:columnas)fila.put(columna,rs.getObject(columna));filas.add(fila);}}return new Resultado(columnas,filas,cs.getLong(4));}}
    private record Resultado(List<String> columnas,List<Map<String,Object>> filas,long total){}
    private static Connection conectar(String db)throws SQLException{return DriverManager.getConnection("jdbc:sqlserver://"+SQL.getHost()+":"+SQL.getMappedPort(1433)+";databaseName="+db+";encrypt=false;trustServerCertificate=true",SQL.getUsername(),SQL.getPassword());}
    private static Path rutaSql(){Path p=Path.of("..","infra","sql","procedures","queries","APP24_Q_ANEXO30_REVISION_DESCARGAS_LISTAR.sql");return Files.exists(p)?p:Path.of("infra","sql","procedures","queries","APP24_Q_ANEXO30_REVISION_DESCARGAS_LISTAR.sql");}
    private static void aplicarArchivo(Connection c,Path archivo)throws Exception{StringBuilder batch=new StringBuilder();try(Statement s=c.createStatement()){for(String linea:Files.readString(archivo).split("\r?\n",-1)){if(linea.trim().equalsIgnoreCase("GO")){ejecutarBatch(s,batch,archivo);batch.setLength(0);}else batch.append(linea).append(System.lineSeparator());}ejecutarBatch(s,batch,archivo);}}
    private static void ejecutarBatch(Statement s,StringBuilder batch,Path archivo)throws SQLException{if(!batch.toString().isBlank())try{s.execute(batch.toString());}catch(SQLException error){throw new IllegalStateException("Fallo aplicando "+archivo.getFileName()+": "+error.getMessage(),error);}}
}
