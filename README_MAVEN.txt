==============================ECLIPSE===========================================

If you use Eclipse for Jave EE, you can simply import the generated project
into Eclipse. We've tested against Eclipse 3.6. Later versions will likely also
work, earlier versions may not. 

Eclipse users will need to install the following plugin components:
- m2eclipse Core
- Maven Integration for WTP (in m2eclipse extras), Instructions for installing
  the maven plugins can be found here:
  http://m2eclipse.sonatype.org/installing-m2eclipse.html

Ensure Eclipse is configured to use Java 1.6 as this sample uses AppEngine.

In Eclipse, go to the File menu and choose:
- File -> Import... -> Existing Maven Projects into Workspace
- Select the directory containing this file.
- Click Finish.

You can now browse the project in Eclipse. To compile your project for
deployment, just type 'mvn package'.

==============================NETBEANS==========================================

From the NetBeans IDE, install the Maven plugin. To do this, select Tools->
Plugins, select Maven, click Install, and follow the prompts.

Configure Maven in NetBeans IDE. To do this, select Tools->Options, select
Miscellaneous from the top panel, then select the Maven tab.

For the External Maven Home field, browse to your Maven installation.

If the option is available, check Always use external Maven for building
projects. Close the dialog.

From the NetBeans IDE, select File->Open Project, and then browse to the
location of the project you'd like to open.

Check Open as Main Project, then click Open Project.

Right-click the project and select Run.

