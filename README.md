rubis-osgi
==========
This project has been refactored to fit the OSGi bundle structure.
This has been taken from this repository : https://github.com/sguazt/dcsj-rubis
Thanks to Marco Guazzone (https://github.com/sguazt) for bringing a revival to RUBiS to the current technologies.

A java implementation of the _Rice University Bidding System_ (RUBiS).

RUBiS is a free, open source initiative.
This implementation is a fork of version 1.4.3 of [http://rubis.ow2.org](OW2 RUBiS).
As of 2012, the original 1.4.3 version can still be downloaded at the [http://forge.objectweb.org/project/showfiles.php?group_id=44](OW2 site)

<b>This refactored project is meant to work as a OSGi bundle for the ScienceCloudPlatform project</b>
Trac of the SCP : http://svn.pst.ifi.lmu.de/trac/scp

## Overview

RUBiS is an auction site prototype modeled after [eBay](http://www.ebay.com/) site (one of the largest online marketplace) that is especially used to evaluate application design patterns and application servers performance scalability.

The auction site benchmark implements the core functionality of an auction site: selling, browsing and bidding.
Complementary services like instant messaging or newsgroups are not currently implemented.
There are three kinds of user sessions: visitor, buyer, and seller.
For a visitor session, users need not register but are only allowed to browse.
Buyer and seller sessions require registration.
In addition to the functionality provided during visitor sessions, during a buyer session users can bid on items and consult a summary of their current bids, rating and comments left by other users.
Seller sessions require a fee before a user is allowed to put up an item for sale.
An auction starts immediately and lasts typically for no more than a week.
The seller can specify a reserve (minimum) price for an item.


## Comparison with OW2 RUBiS
 
In the _OW2 RUBiS_, several versions of RUBiS are implemented by using three different technologies: PHP, Java servlets and Enterprise Java Bean (EJB).
The main purpose is to compare the trade-off between performance and complexity obtained with these technologies, as shown in the paper [(Checchet et al.,2002)][checchet-2002-performance].

In our implementation, unlike the OW2 one, we do not focus on the performance evaluation of different technologies and design patterns.
Instead, our aim is to provide a fully functional implementation that can be used to benchmark real computing systems and evaluate their performance.
Most of the changes will focus on stability and improvements to the original OW2 version.

For these reasons, currently we only provide an implementation based on Java servlets.

## Compile and Install

To compile RUBiS you need Java &ge; 1.5 and [Apache Ant](http://ant.apache.org).
To run RUBiS you need a DBMS like [MySQL](http://www.mysql.com).

While the code should compile and run with every DBMS, we have currently tested it only with MySQL 5.6.\* (with MySQL Connector/J 5.0.\*).

### Setup the database

In the setup/db directory, you will find a readme describing each *.sql file.
Execute these ones in your database to start with :
```
rubis.sql			contains the SQL statements to create the RUBiS database using MySQL.
categories.sql			SQL requests to initalize the categories table
regions.sql
```

You may have to isnert initial profiles for starting logging/bidding.

### Compilation Steps

In the following we refer to the variable `$RUBIS_HOME` as a variable containing the path pointing to this version of RUBiS.

1. Move to the `$RUBIS_HOME` directory.

	$ cd $RUBIS_HOME

2. Copy the template property file `setup/user.properties.template` to file `user.properties`

	$ cp setup/user.properties.template user.properties

3. Edit the file `user.properties` to set properties to a proper value (e.g., if you use Apache Tomcat this is the same of `$CATALINA_HOME`).

	$ vi user.properties

4. Edit the file `src/edu/rice/rubis/servlets/Config.java` to set the following properties
 * `DatabaseConnectionStrategy`: this is the type of strategy you want to use to connect to the database.
   You can choose among the following values:
   - `UNPOOLED_DRIVERMANAGER_DB_CONNECTION_STRATEGY`: don't use connection pooling and connect to the database by means the `java.sql.DriverManager` class.
   - `POOLED_DRIVERMANAGER_DB_CONNECTION_STRATEGY`: use connection pooling and connect to the database by means the `java.sql.DriverManager` class.
   - `DATASOURCE_DB_CONNECTION_STRATEGY`: setup and manage database connections by means of `javax.sql.DataSource` class (recommended).

5. Edit the database properties
   - If you chose `DATASOURCE_DB_CONNECTION_STRATEGY` as database connection strategy, you have to edit the context file to set properties to a proper value:

     $ vi META-INF/context.xml

   - Otherwise, if you chose either `UNPOOLED_DRIVERMANAGER_DB_CONNECTION_STRATEGY` or`POOLED_DRIVERMANAGER_DB_CONNECTION_STRATEGY` as database connection strategy, you have to create a property file in `dbms.property` directory (optionally, replace `dbms` with the name of your DBMS).
     For instance, if you use MySQL you can edit the file `mysql.properties` to set properties to a proper value (e.g., the host name where the DBMS runs).

	 $ vi mysql.properties

     Then you have to edit the file `src/edu/rice/rubis/servlets/Config.java` and change the value of `DatabaseProperties` to point to the name of your `dbms.property` file.
     If you use MySQL, you can use the default value.

6. Copy the JAR file of the JDBC database driver in `$RUBIS_HOME/lib`.
   For instance, if you use MySQL you have to copy the Connector/J JAR file in that location:

   $ cp mysql-connector-java-5.1.22-bin.jar lib/

7. Run Ant, this will build the project and create the jar into the out directory

	$ ant export

## References

[checchet-2002-performance]: _Emmanuel Cecchet, Julie Marguerite and Willy Zwaenepoel_, __Performance and Scalability of EJB Applications__. In Proc. of the OOPSLA'02, Seattle, WA, USA, Nov. 4-8, 2002
