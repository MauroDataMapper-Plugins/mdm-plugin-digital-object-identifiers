# mdm-plugin-digital-object-identifiers

| Branch | Build Status |
| ------ | ------------ |
| master | [![Build Status](https://jenkins.cs.ox.ac.uk/buildStatus/icon?job=Mauro+Data+Mapper+Plugins%2Fmdm-plugin-digital-object-identifiers%2Fmain)](https://jenkins.cs.ox.ac.uk/blue/organizations/jenkins/Mauro%20Data%20Mapper%20Plugins%2Fmdm-plugin-digital-object-identifiers/branches) |
| develop | [![Build Status](https://jenkins.cs.ox.ac.uk/buildStatus/icon?job=Mauro+Data+Mapper+Plugins%2Fmdm-plugin-digital-object-identifiers%2Fdevelop)](https://jenkins.cs.ox.ac.uk/blue/organizations/jenkins/Mauro%20Data%20Mapper%20Plugins%2Fmdm-plugin-digital-object-identifiers/branches) |

## Requirements

* Java 17 (Temurin)
* Grails 5.1.9+
* Gradle 7.3.3+

All of the above can be installed and easily maintained by using [SDKMAN!](https://sdkman.io/install).

## Applying the Plugin

The preferred way of running Mauro Data Mapper is using the [mdm-docker](https://github.com/MauroDataMapper/mdm-docker) deployment. However you can
also run the backend on its own from [mdm-application-build](https://github.com/MauroDataMapper/mdm-application-build).

### mdm-docker

In the `docker-compose.yml` file add:

```yml
mauro-data-mapper:
    build:
        args:
            ADDITIONAL_PLUGINS: "uk.ac.ox.softeng.maurodatamapper.plugins:mdm-plugin-digital-object-identifiers:2.2.0"
```

Please note, if adding more than one plugin, this is a semicolon-separated list

### mdm-application-build

In the `build.gradle` file add:

```groovy
grails {
    plugins {
        runtimeOnly 'uk.ac.ox.softeng.maurodatamapper.plugins:mdm-plugin-digital-object-identifiers:2.2.0'
    }
}
```


## DataCite Documentation

* https://support.datacite.org/docs/api-create-dois
* https://schema.datacite.org/
