# Bee2j
git clone https://github.com/bcrypto/bee2j.git
cd bee2j
mvn clean install

# Apache Santuario
git clone https://github.com/apache/santuario-xml-security-java.git
cd santuario-xml-security-java
git checkout xmlsec-4.0.2
git apply ../santuario-4.0.2-bee2j.patch 
mvn clean install -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
## Dependency hell solution 
[mvn dependency:purge-local-repository -DreResolve=false]

# Hyperledger Fabric 
## Add dependencies to build.gradle:
+   implementation 'by.bcrypto:bee2j:1.0'
+   implementation 'org.apache.santuario:xmlsec:4.0.2'

repositories {
+   mavenLocal()
    mavenCentral()

## Initialize security provider (InitLedger)
    Bee2SecurityProvider bee2j = new Bee2SecurityProvider();
    Security.addProvider(bee2j);
