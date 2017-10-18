# Motor
Motor is a Java application launcher toolkit, that include a launcher and a maven plugin, simplify application development, packaging, deployment, elegant start and stop the application.

# How
we can add motor plugin for the maven project,then execute: mvn clean package.

```xml
<plugins>
	<plugin>
        <groupId>com.dinstone.motor</groupId>
        <artifactId>motor-maven-plugin</artifactId>
        <version>1.0.1</version>
        <configuration>
            <includeBaseDirectory>true</includeBaseDirectory>
            <launcher>
                <properties>
                    <property>
                        <name>Xmx</name>
                        <value>2g</value>
                    </property>
                </properties>
            </launcher>
            <application>
                <activator>com.dinstone.async.vertx.ApplicationActivator</activator>
                <configs>
                    <config>
                        <directory>src/main/resources</directory>
                        <excludes>
                            <exclude>test/**</exclude>
                            <exclude>online/**</exclude>
                        </excludes>
                    </config>
                    <config>
                        <directory>src/main/resources/online</directory>
                    </config>
                </configs>
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
