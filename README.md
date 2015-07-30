Apache Streams (incubating)
Licensed under Apache License 2.0 - http://www.apache.org/licenses/LICENSE-2.0
--------------------------------------------------------------------------------

streams-ibm
=====
Apache Streams connector modules for Watson and Alchemy APIs, plus streams and spark jobs used by
People Pattern team during Watson World 2015 hackathon.

Release Notes
=============

[LICENSE.md](LICENSE.md "LICENSE.md")

System Requirements
===================
  -  Java SDK 7 or higher.
  -  The project is built with Apache Maven 3+ (suggested is 3.2.5).
    
Building
====================
To build from source code:

    git clone https://github.com/apache/incubator-streams-master
    
    cd incubator-streams-master
    
    mvn clean install
    
    cd ..
    
    git clone https://github.com/apache/incubator-streams
    
    cd incubator-streams
    
    mvn clean install
    
    cd ..
    
    git clone https://github.com/apache/incubator-streams-examples
    
    cd incubator-streams-examples
        
    mvn clean install
    
    cd ..
    
    git clone https://github.com/peoplepattern/streams-ibm
        
    cd streams-ibm
        
    mvn clean install
    
Running
=======

[hackathon_install.sh](src/main/resources/hackathon_install.sh "hackathon_install.sh")

[hackathon_configure.sh](src/main/resources/hackathon_configure.sh "hackathon_configure.sh")

[hackathon_collect.sh](src/main/resources/hackathon_collect.sh "hackathon_collect.sh")
