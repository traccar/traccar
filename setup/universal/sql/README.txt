I have created these SQL files to help in the creation of the initial Traccar
database for the three popular DBs used with Traccar:

	*) H2          (h2.sql)
	*) MySQL       (mysql.sql)
	*) PostgreSQL  (postgresql.sql)

Each database has an SQL script (in parentheses above) to perform the
following:

	1) Create the Traccar database ("traccar_db")
	2) Create the Traccar tables in the database
	3) Create the keys and foreign keys for the tables
	4) Create the Traccar user ("traccar_user") and
	   grant limited permissions to the tables.

Use diff(1) to see the minor variations for each RDBMS.

I created a specific database ("traccar_db") to segregate this activity from any
other activity within the same database server.  I created a specific user
("traccar_user" with password "traccar-is-awesome") to properly limit the access
of the application to its specific needs within the database.

To execute the SQL script, the interactive SQL shell must be run with
administrative right to create a database in the RDBMS server (i.e., not the
Traccar user account).

	============================================================
	= H2
	============================================================
	You need to know where the JAR file is for the H2 database
	<H2 JAR file>.  And, you need to choose the location of where
	the H2 database will be located <DB filename>.  This assumes
	you are executing from the ./src/sql directory.

	$ java -cp <H2 JAR file>                \
	        org.h2.tools.Shell              \
	        -url jdbc:h2:<DB filename>      \
	        -sql "runscript from 'h2.sql';"
	$

	To connect to the database in the Traccar configuration file, use the
	following:

	  <entry key='database.driver'>org.h2.Driver</entry>
	  <entry key='database.dataSource'>org.h2.jdbcx.JdbcDataSource</entry>
	  <entry key='database.url'>jdbc:h2:<DB filename>;SCHEMA=traccar_db</entry>

	Be sure to replace "<DB filename>" in the above line.


	============================================================
	= MySQL
	============================================================
	This assumes "root" has administratrive privilege to perform
	the SQL commands in the source'd *.sql files.  And, this
	assumes you are executing from the ./src/sql directory.

	$ mysql --user=root --password
	Enter password: <root password>
	mysql> source mysql.sql;
	mysql> quit
	$

	To connect to the database in the Traccar configuration file, use the
	following:

	  <entry key='database.driver'>com.mysql.jdbc.Driver</entry>
	  <entry key='database.dataSource'>com.mysql.jdbc.jdbc2.optional.MysqlDataSource</entry>
	  <entry key='database.url'>jdbc:mysql://localhost:3306/traccar_db?allowMultiQueries=true&amp;autoReconnect=true</entry>


	============================================================
	= PostgreSQL
	============================================================
	This assumes "root" has administratrive privilege to perform
	the SQL commands in the source'd *.sql files.  And, this
	assumes you are executing from the ./src/sql directory.

	$ psql --username postgres --password
	Password for user postgres: <postgres password>
	postgres=# \i postgresql.sql
	traccar_db=# \q
	$

	To connect to the database in the Traccar configuration file, use the
	following:

	  <entry key='database.driver'>org.postgresql.Driver</entry>
	  <entry key='database.url'>jdbc:postgresql://localhost:5432/traccar_db</entry>
	  <entry key='database.user'>traccar_user</entry>
	  <entry key='database.password'>traccar-is-awesome</entry>

        The INSERT statement is different due to the use of the PostgreSQL-native XML type:

	  <entry key='database.insertPosition'>
	    INSERT INTO positions (device_id, time, valid, latitude, longitude,
	                           altitude, speed, course, power, address,
	                           other)
	    VALUES (:device_id, :time, :valid, :latitude, :longitude,
	            :altitude, :speed, :course, :power, :address,
	            XMLPARSE(CONTENT :extended_info));
	  </entry>


I hope this helps explain a few things.

- jss

