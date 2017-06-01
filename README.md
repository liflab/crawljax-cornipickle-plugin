# crawljax-cornipickle-plugin
A plugin for Crawljax using Cornipickle testing tool.


## Installation

### 1. Install Cornipickle

Download the lastest [Cornipickle](https://github.com/liflab/cornipickle/releases) release and put it in your Java classpath.

### 2. Install Crawljax's web distribution

Download [Crawljax's web distribution](https://github.com/crawljax/crawljax/releases) as ZIP and test the installation by running it on your prefered port.

```
java -jar crawljax-web-3.6.jar -p [port]
```

### 3. Generate the plugin

```
mvn clean install
```

## Usage with Crawljax's web distribution

### 1. Write your Cornipickle properties

Create a .cp file with your properties following Cornipickle's syntax.

###2. Launch the Crawljax web distribution

If the Crawljax web distribution is not already running (from the installation steps), run it. 

```
java -jar crawljax-web-3.6.jar -p [port]
```

### 3. Launch the web distribution

Open a browser and type in the distribution's URL (e.g. localhost:[port]).

### 4. Upload the plugin

Go in the "Plugins" tab and upload the plugin in the section "Add Local Plugin" (the plugin should be in [Path to crawljax-cornipickle-plugin]/target/crawljax-cornipickle-plugin-[version]-SNAPSHOT.jar).

### 5. Create a configuration

Create a configuration in the Configurations tab. After saving it, go in the Plugins tab of your configuration and add the plugin. You have to put the path to your properties file in the field "Cornipickle properties".

### 6. Run

Start the crawl with the button "Run Configuration". The Cornipickle verdicts are going to show up next to the Logs and in the "out" folder of the web distribution.
