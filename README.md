# Motor
Motor is a Java application launcher toolkit, that include a launcher and a maven plugin, simplify application development, packaging, deployment, elegant start and stop the application.

# How
we can add motor plugin for the maven project,then execute: mvn clean package.

```xml
<plugins>
	   <plugin>
            <groupId>com.dinstone.motor</groupId>
            <artifactId>motor-maven-plugin</artifactId>
            <version>1.2.0</version>
            <configuration>
                <includeBaseDirectory>true</includeBaseDirectory>
                <launcher>
                    <properties>
                        <property>
                            <name>Xmn</name>
                            <value>128m</value>
                        </property>
                        <property>
                            <name>Xms</name>
                            <value>1g</value>
                        </property>
                        <property>
                            <name>Xmx</name>
                            <value>1g</value>
                        </property>
                        <property>
                            <name>launcher.listen.enabled</name>
                            <value>true</value>
                        </property>
                        <property>
                            <name>launcher.listen.port</name>
                            <value>4444</value>
                        </property>
                    </properties>
                </launcher>
                <application>
                    <activator>com.dinstone.grape.server.ApplicationActivator</activator>
                    <configs>
                        <config>
                            <directory>src/main/resources</directory>
                        </config>
                    </configs>
                    <resources>
                        <resource>
                            <directory>webroot</directory>
                        </resource>
                    </resources>
                </application>
            </configuration>
            <executions>
                <execution>
                    <id>launcher-package</id>
                    <phase>package</phase>
                    <goals>
                        <goal>publish</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
</plugins>
```
