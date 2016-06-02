
############################## REQUIREMENTS ####################################
1. Docker
2. Maven
3. Bash Shell


########################### BUILD INSTRUCTIONS #################################
At project root folder run:
sh ./setup/docker/build.sh


########################### RUNNING CONTAINER ##################################
# NOTICE 1: replace [VERSION] with project version from pom.xml.
#
# NOTICE 2: when providing custom config, always include bellow statement, since the included web folder is not minified.
#       <entry key='web.debug'>true</entry>

# running with internal h2 database and debug.xml
docker run -d --name traccar-server -p 5000-5150:5000-5150 -p 8082:8082 tananaev/traccar:[VERSION]

# running with external config file
docker run -d --name traccar-server -v /folder/myconfig.xml:/opt/traccar/traccar.xml -p 5000-5150:5000-5150 -p 8082:8082 tananaev/traccar:[VERSION]

# running with host folder mapped to container database folder
docker run -d --name traccar-server -v /my-database-folder:/opt/traccar/data/database -p 5000-5150:5000-5150 -p 8082:8082 tananaev/traccar:[VERSION]

# running with external mysql instance
# CONFIG:
#       <entry key='web.debug'>true</entry>
#	    <entry key='database.driver'>com.mysql.jdbc.Driver</entry> 
#	    <entry key='database.url'>jdbc:mysql://[IP-ADDRESS]:3306/[DATABASE]?allowMultiQueries=true&amp;autoReconnect=true&amp;useUnicode=yes&amp;characterEncoding=UTF-8&amp;sessionVariables=sql_mode=ANSI_QUOTES</entry>
#	    <entry key='database.user'>[USERNAME]</entry> 
#	    <entry key='database.password'>[PASSWORD]</entry>
#
docker run -d --name traccar-server docker -p 8082:8082 tananaev/traccar:[VERSION]

# running allong with mysql dockenized instance
# CONFIG:
#       <entry key='web.debug'>true</entry>
#	    <entry key='database.driver'>com.mysql.jdbc.Driver</entry> 
#	    <entry key='database.url'>jdbc:mysql://mysql-traccar:3306/traccar?allowMultiQueries=true&amp;autoReconnect=true&amp;useUnicode=yes&amp;characterEncoding=UTF-8&amp;sessionVariables=sql_mode=ANSI_QUOTES</entry>
#	    <entry key='database.user'>traccar</entry> 
#	    <entry key='database.password'>my-secret-pw</entry>
#
docker run -d --name mysql-traccar -e MYSQL_DATABASE=traccar -e MYSQL_USER=traccar -e MYSQL_PASSWORD=my-secret-pw -e MYSQL_ROOT_PASSWORD=my-root-secret-pw mysql:5.6.30
docker run -d --name traccar-server docker --link mysql-traccar:mysql-traccar -p 8082:8082 tananaev/traccar:[VERSION]

################################################################################
