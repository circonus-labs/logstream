srcdir=.
top_srcdir=..
prefix=/usr/local
exec_prefix=${prefix}
bindir=${exec_prefix}/bin
sysconfdir=${prefix}/etc
JAVA=/usr/bin/java
JAVAC=/usr/bin/javac
JAR=/usr/bin/jar
JPARAMS=-g
INSTALL=/usr/bin/install -c

JAVAF=src/com/omniti/labs/logstream/logstream.java \
	src/com/omniti/labs/logstream/Engine.java \
	src/com/omniti/labs/logstream/EngineEPLDesc.java \
	src/com/omniti/labs/logstream/EngineException.java \
	src/com/omniti/labs/logstream/EngineIngestorHandler.java \
	src/com/omniti/labs/logstream/EngineListener.java \
	src/com/omniti/labs/logstream/EngineMQ.java \
	src/com/omniti/labs/logstream/EngineOutput.java \
	src/com/omniti/labs/logstream/EngineQuery.java \
	src/com/omniti/labs/logstream/EngineServer.java \
	src/com/omniti/labs/logstream/EngineSet.java \
	src/com/omniti/labs/logstream/EngineStatement.java \
	src/com/omniti/labs/logstream/EngineType.java \
	src/com/omniti/labs/logstream/EngineJSONUtil.java \
	src/com/omniti/labs/logstream/JSONDirectory.java \
	src/com/omniti/labs/logstream/ManipulationHandler.java \
	src/com/omniti/labs/logstream/StringStuffs.java

CP=.:../lib/commons-logging-1.1.1.jar:../lib/esper-4.0.0.jar:../lib/jetty-servlet-7.2.2.v20101205.jar:../lib/jetty-util-7.2.2.v20101205.jar:../lib/jetty-server-7.2.2.v20101205.jar:../lib/jetty-continuation-7.2.2.v20101205.jar:../lib/jetty-io-7.2.2.v20101205.jar:../lib/json.jar:../lib/log4j-1.2.15.jar:../lib/rabbitmq-client.jar:../lib/servlet-api-2.5.jar:../lib/javassist-3.14.0.jar

all:	lib/logstream-cep.jar
	@chmod a+x logstream-cep

lib/logstream-cep.jar:	$(JAVAF)
	test -d classes || mkdir classes
	(cd src && $(JAVAC) $(JPARAMS) -cp $(CP) -d ../classes `find . -name \*.java`)
	(cd classes && $(JAR) cf ../$@ com)

clean:
	rm -f lib/logstream-cep.jar
	rm -rf classes
