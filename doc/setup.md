

# Working with an IDE


## The Environment

Our environment should run on the most common platforms (Linux, Windows and OSX). The minimal requirement is a **64-bit JDK** (Java 8 or later).


Additionally we need the build tool **sbt**. This would be enough, but for convenience we propose to install also a **git** client and the **Intellij IDEA**.


### Installation

Please install the following tools:

| Tool           | Download Link                                                | Test command (will be executed in the next step of the tutorial) |
| -------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| JDK 1.8 64-bit | [all OS](http://www.oracle.com/technetwork/java/javase/downloads/index.html) | `java -version`                                              |
| git            | [all OS](https://git-scm.com/)                               | `git --version`                                              |
| sbt            | [all OS](http://www.scala-sbt.org/)                          | `sbt sbtVersion`                                             |
| Intellij IDEA  | [all OS](https://www.jetbrains.com/idea/)                    | *(first start later in this tutorial)*                       |



## First Steps: Getting the project

In this step you clone the example project. This project contains example programs, 
which illustrate how the programs we saw in the ScalismoLab tutorials look when we 
use the latest version of Scalismo as a library. 

First, we download the project, then we run it from the command line and set up the IDE for the later usage.



### JAVA - do i have the right version?

To test your java installation you can use the following command:

```
java -version
```

The output should look similar to:

```
java version "1.8.0_131"
Java(TM) SE Runtime Environment (build 1.8.0_131-b11)
Java HotSpot(TM) 64-Bit Server VM (build 25.131-b11, mixed mode)
```

>*Note:* Make sure that your architecture is 64-Bit!



### Git - Get the project

Next, to check your installation of git enter the following on a command line.:

```
git --version
```

>*Note:* On windows you should have a program called **Git BASH** after the installation of git.

While the exact version does not matter, the output of the above command should be something as simple as:

```
git version 1.9.1
```

To get the project, we need to clone the repository using git. 

Alternatively you can download the project also from the seed [project's page](https://github.com/unibas-gravis/pmm2018). To download the project using git, change in the console to somewhere where you want the new project to be added. The cloning of the project creates a new folder in the directory where you execute the command. You can now get the project with the following command:

```
git clone https://github.com/unibas-gravis/pmm2018.git
```

The IDE we use supports git. So we do not need to know more about the commandline usage of git. However, a good starting point to learn more about the commandline interface is the official [documentation](https://git-scm.com/doc).



### Sbt - Building from the command line

To check whether sbt is installed correctly execute:

```
sbt sbtVersion
```

Looking at the output you should see at the end of the output a line similar to:

```
 ...
 ...
[info] 1.0.4
```

Change in the console to the directory `pmm2018`. We will now build the project by calling
```
sbt compile
```

This will trigger the project to be built by sbt. Note that the initial build will download some dependencies specific to the project. This may take a while. The command to run the project is:

```
sbt run
```

A successful run should display a dialog with different program, which you can start. Choose the program ```RegistrationExample``` to start a registration. You should see the Scalismo UI popping up, and (after some computation time) how a registration is performed.

Sbt is integrated in the IDE, so we will not explain more about the usage. If you want more information go to the official [documentation](http://www.scala-sbt.org/0.13/docs/index.html).



### IntelliJ Idea

Now it is time to start the IDE. What we need to make sure is that you enable the scala plugin. Then we will import the project.

 When you start the IDE for the first time you can configure which parts are enabled and or downloaded. 

We recommend to go with the default settings as long as you have enough disk space. Go through the dialog step by step until you encounter the point **Featured Plugins**. Then select to install the **Scala Plugin**.



 If you have already used the IDE but have not yet installed  the Scala plugin, you can enable it through the menu **File**/**Settings**/**Plugins**



When the scala plugin is installed and you get displayed the welcome screen, choose **Open ..**. Then navigate to the folder containing the seed project directory.

In the next dialog check that the **Project SDK** points to the location where you installed the Java SDK. If the checkbox **auto import** is shown, activate it. Then continue by clicking onto the **OK** button.



When the dialog **SBT Project Data To Import** is shown you can deselect the **pmm2018-build** entry and continue with **OK**.

Now the IDE should change and display the project. When you start the IDE for the first time, there is a lot of processing that is done in the background. In the bottom right you can spot an indication for the ongoing work. Due to the workload it may take a while until the IDE reacts responsive.

To see what is already present in the project hit **[Alt+1]** which should display the project structure tab to the left. If you do not see the project structure, then have a look at the top of the newly opened view. There should be a drop down list where you can select **Project**. You should then be able to navigate through the project folder to `pmm2018/src/main/scala/pmm2018/tutorials/` and double-click **RegistrationExample**. This will open the code of the application we have already executed before from the console using sbt.

To execute the file from within the IDE right-click the source file and select **Run 'RegistrationExample'**. Alternatively you can use the shortcut which is marked after the menu entry. Depending on your setting it might be  **[Ctrl+Shift+F10]**.

To use git or sbt from within the IDE go to the menu **View/Tool Windows** and select what you want to do. If not yet enabled, you have to activate the git plugin over the menu **File/Settings/Plugins**.



