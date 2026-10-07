package com.jovycandy.anexo24.reports;

import org.junit.jupiter.api.*;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MSSQLServerContainer;
import java.nio.file.*; import java.sql.*; import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Valida el SP versionado de fracciones A31 contra SQL Server efímero. */
class Anexo30FraccionesSqlIT {
 static final MSSQLServerContainer<?> SQL=new MSSQLServerContainer<>("mcr.microsoft.com/mssql/server:2022-latest").acceptLicense(); static boolean ok;
 @BeforeAll static void start() throws Exception { ok=DockerClientFactory.instance().isDockerAvailable(); Assumptions.assumeTrue(ok,"Docker no disponible localmente"); SQL.start(); try(Connection c=conn("master");Statement s=c.createStatement()){s.execute("CREATE DATABASE CALE_IMMEX");} try(Connection c=conn("CALE_IMMEX");Statement s=c.createStatement()){s.execute("CREATE TABLE dbo.A31_DESCARGASF (A31_FRACCIONKEY BIGINT IDENTITY PRIMARY KEY,TIPO VARCHAR(2) NULL,CLAVEPEDIMENTO VARCHAR(5) NULL,EJERCICIO VARCHAR(5) NULL,PERIODO VARCHAR(5) NULL,FRACCION VARCHAR(10) NULL,VALOR NUMERIC(18,4) NULL,AF VARCHAR(5) NULL,ARCHIVO VARCHAR(50) NULL)"); apply(c,Path.of("..","infra/sql/procedures/queries/APP24_Q_ANEXO30_REVISION_FRACCIONES_LISTAR.sql"));}}
 @AfterAll static void stop(){if(ok)SQL.stop();}
 @BeforeEach void data() throws Exception {try(Connection c=conn("CALE_IMMEX");Statement s=c.createStatement()){s.execute("DELETE FROM dbo.A31_DESCARGASF");s.execute("INSERT dbo.A31_DESCARGASF(TIPO,CLAVEPEDIMENTO,EJERCICIO,PERIODO,FRACCION,VALOR,AF,ARCHIVO) VALUES ('F4','A1','2026','02','84715002',123.4567,'SI','uno.txt'),('F4','A1','2026','02','84715002',123.4567,'SI','dos.txt'),(NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL)");}}
 @Test void conservaPkTupleNoUnicoFiltrosPaginacionYNulls() throws Exception { Result r=list(null,1,2); assertEquals(3,r.total);assertEquals(2,r.rows.size());assertEquals(9,r.columns);assertEquals(2,list("F4",1,20).total);assertEquals(1,list("uno.txt",1,20).total);assertTrue(list(null,9,20).rows.isEmpty());assertThrows(SQLException.class,()->list(null,0,20));assertThrows(SQLException.class,()->list(null,1,101));assertEquals("2026",r.rows.get(0)[3]); }
 record Result(int columns,List<Object[]> rows,long total){}
 static Result list(String f,int p,int z)throws Exception{try(Connection c=conn("CALE_IMMEX");CallableStatement x=c.prepareCall("{call dbo.APP24_Q_ANEXO30_REVISION_FRACCIONES_LISTAR(?,?,?,?)}")){x.setString(1,f);x.setInt(2,p);x.setInt(3,z);x.registerOutParameter(4,Types.BIGINT);x.execute();List<Object[]> a=new ArrayList<>();ResultSet r=x.getResultSet();int n=r.getMetaData().getColumnCount();while(r.next()){Object[] q=new Object[n];for(int i=0;i<n;i++)q[i]=r.getObject(i+1);a.add(q);}return new Result(n,a,x.getLong(4));}}
 static Connection conn(String db)throws Exception{return DriverManager.getConnection("jdbc:sqlserver://"+SQL.getHost()+":"+SQL.getMappedPort(1433)+";databaseName="+db+";encrypt=false;trustServerCertificate=true",SQL.getUsername(),SQL.getPassword());}
 static void apply(Connection c,Path p)throws Exception{String t=Files.readString(Files.exists(p)?p:Path.of("infra/sql/procedures/queries/APP24_Q_ANEXO30_REVISION_FRACCIONES_LISTAR.sql"));for(String b:t.split("(?im)^GO\s*$")){if(!b.isBlank())try(Statement s=c.createStatement()){s.execute(b);}}}
}
