I copied the relevant embedded tomcat configuration and the associated keys from: https://github.com/jthoms/spring-boot-embedded-tomcat-ssl/blob/master/src/main/java/demo/Application.java 

Thanks John Thoms! 
===============================

Working example of spring boot with embedded tomcat running ssl with a self-signed cert from project root, keystore.p12, created with:

keytool -genkey -alias tomcat -storetype PKCS12 -keyalg RSA -keysize 2048 -keystore keystore.p12 -validity 3650

Example uses password localhost.

See src/test/java/demo/SampleTomcatApplicationTests.java
