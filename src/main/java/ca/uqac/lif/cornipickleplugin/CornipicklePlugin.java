package ca.uqac.lif.cornipickleplugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.crawljax.core.CrawlerContext;
import com.crawljax.core.CrawljaxException;
import com.crawljax.core.plugin.GeneratesOutput;
import com.crawljax.core.plugin.HostInterface;
import com.crawljax.core.plugin.HostInterfaceImpl;
import com.crawljax.core.plugin.OnNewStatePlugin;
import com.crawljax.core.plugin.OnRevisitStatePlugin;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.Eventable.EventType;
import com.crawljax.core.state.Identification;
import com.crawljax.core.state.StateVertex;
import com.crawljax.core.plugin.Plugin;

import ca.uqac.lif.cornipickle.CornipickleParser.ParseException;
import ca.uqac.lif.cornipickle.Interpreter;
import ca.uqac.lif.cornipickle.Interpreter.StatementMetadata;
import ca.uqac.lif.cornipickle.Verdict;
import ca.uqac.lif.json.JsonElement;
import ca.uqac.lif.json.JsonList;
import ca.uqac.lif.json.JsonMap;
import ca.uqac.lif.json.JsonParser;
import ca.uqac.lif.json.JsonParser.JsonParseException;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin for Crawljax that tests every new state with Cornipickle
 * @author fguerin
 *
 */
public class CornipicklePlugin implements Plugin, OnNewStatePlugin, OnRevisitStatePlugin, GeneratesOutput {
	private HostInterface m_hostInterface;
	
	private String m_outputFolder;
	
	private Interpreter m_corniInterpreter;
	
	private Interpreter m_initialInterpreter;
	
	private Set<String> m_attributes;
	
	private Set<String> m_tagNames;
	
	private int m_cornipickleIdCounter = 0;
	
	private enum Include {INCLUDE, DONT_INCLUDE, DONT_INCLUDE_RECURSIVE};
	
	private static final Logger LOG = LoggerFactory.getLogger(CornipicklePlugin.class);
	
	/**
	 * Constructor for the plugin
	 */
	public CornipicklePlugin() {
		this.m_hostInterface = new HostInterfaceImpl(null, null);
		this.m_outputFolder = "";
		this.m_corniInterpreter = new Interpreter();
	}
	
	/**
	 * Constructor for the plugin
	 * @param hostInterface
	 * @throws ParseException 
	 */
	public CornipicklePlugin(HostInterface hostInterface) throws ParseException {
		this.m_hostInterface = hostInterface;
		this.m_outputFolder = hostInterface.getOutputDirectory().getAbsolutePath();
		this.m_corniInterpreter = new Interpreter();
		this.m_cornipickleIdCounter = 0;
		
		try {
			FileInputStream fis = new FileInputStream(this.m_hostInterface.getParameters().get("properties"));
			BufferedReader bf = new BufferedReader(new InputStreamReader(fis));
			String inputLine;
			String properties = "";
	        while ((inputLine = bf.readLine()) != null) {
	        	properties = properties + inputLine + "\n";
	        }
	        setProperties(properties);
	        bf.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			FileWriter fw = new FileWriter(new File(hostInterface.getOutputDirectory().getPath() + "/out.txt"), false);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onNewState(CrawlerContext context, StateVertex newState) {
		double begin = (double)System.currentTimeMillis();
		
		boolean newInterpreterNeeded = false;
		
		try {
			context.getCrawlPath();
			if(context.getCrawlPath().size() == 1) {
				this.m_corniInterpreter = this.m_initialInterpreter;
			}
		} catch (NullPointerException e) {
			newInterpreterNeeded = true;
		}
		
		String script = readJS();
		
		StringBuilder attribute_string = new StringBuilder();
		for (String att : this.m_attributes)
		{
			attribute_string.append("\"").append(att).append("\",");
		}
		
		StringBuilder tag_string = new StringBuilder();
		for (String tag : this.m_tagNames)
		{
			tag_string.append("\"").append(tag).append("\",");
		}
		
		script = script.replace("%%ATTRIBUTELIST%%", attribute_string);
		script = script.replace("%%TAGLIST%%", tag_string);
		
		String path;
		try {
			path = context.getCrawlPath().last().getIdentification().getValue();
			script = script.replace("%%PATH%%", path);
			script = script.replace("%%BOOL%%", "true");
		}
		catch(Exception e) {
			script = script.replace("%%BOOL%%", "false");
		}
		
		String content = (String)context.getBrowser().executeJavaScript(script);
		JsonElement j;
		try {
			j = new JsonParser().parse(content);
			this.m_corniInterpreter.evaluateAll(j);
		} catch (JsonParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		Map<StatementMetadata, ca.uqac.lif.cornipickle.Verdict> verdicts = this.m_corniInterpreter.getVerdicts();
		
		double end = (double)System.currentTimeMillis();
		double difference = (end - begin);
		
		try {
			FileWriter fw = new FileWriter(new File(this.m_hostInterface.getOutputDirectory().getPath() + "/out.txt"), true);
			fw.write("New State " + String.valueOf(newState.getId() + "\n\n"));
			fw.write("URL:\n" + newState.getUrl() + "\n\n");
			fw.write("Path: " + getStatePath(context) + "\n\n");
			fw.write("Time taken: " + String.valueOf(difference) + " milliseconds \n\n");
			for(Map.Entry<StatementMetadata, ca.uqac.lif.cornipickle.Verdict> statement : verdicts.entrySet()) {
				fw.write("Statement:\n" + statement.getKey().toString() + "\n\n");
				fw.write("Verdict:\n" + statement.getValue().getValue().toString() + "\n\n");
				fw.write("Witness:\n" + statement.getValue().getWitnessFalse().toString() + "\n\n");
			}
			//fw.write("JSON: \n" + content.toString() + "\n\n");
			fw.write("----------------------------------------------------------------------------\n\n");
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		if(newInterpreterNeeded) {
			this.m_initialInterpreter = this.m_corniInterpreter;
		}
	}
	
	/*
	 * Function executed everytime a state is revisited (happens when backtracking too except the first state).
	 * @see com.crawljax.core.plugin.OnRevisitStatePlugin#onRevisitState(com.crawljax.core.CrawlerContext, com.crawljax.core.state.StateVertex)
	 */
	@Override
	public void onRevisitState(CrawlerContext context, StateVertex currentState) {
		if(context.getCrawlPath().size() == 1) {
			this.m_corniInterpreter = this.m_initialInterpreter;
		}
		
		double begin = (double)System.currentTimeMillis();
		
		String script = readJS();
		
		StringBuilder attribute_string = new StringBuilder();
		for (String att : this.m_attributes)
		{
			attribute_string.append("\"").append(att).append("\",");
		}
		
		StringBuilder tag_string = new StringBuilder();
		for (String tag : this.m_tagNames)
		{
			tag_string.append("\"").append(tag).append("\",");
		}
		
		script = script.replace("%%ATTRIBUTELIST%%", attribute_string);
		script = script.replace("%%TAGLIST%%", tag_string);
		
		String path;
		try {
			path = context.getCrawlPath().last().getIdentification().getValue();
			script = script.replace("%%PATH%%", path);
			script = script.replace("%%BOOL%%", "true");
		}
		catch(Exception e) {
			script = script.replace("%%BOOL%%", "false");
		}
		
		String content = (String)context.getBrowser().executeJavaScript(script);
		JsonElement j;
		try {
			j = new JsonParser().parse(content);
			this.m_corniInterpreter.evaluateAll(j);
		} catch (JsonParseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		Map<StatementMetadata, ca.uqac.lif.cornipickle.Verdict> verdicts = this.m_corniInterpreter.getVerdicts();
		
		double end = (double)System.currentTimeMillis();
		double difference = (end - begin);
		
		try {
			FileWriter fw = new FileWriter(new File(this.m_hostInterface.getOutputDirectory().getPath() + "/out.txt"), true);
			fw.write("Revisit State " + String.valueOf(currentState.getId() + "\n\n"));
			fw.write("URL:\n" + currentState.getUrl() + "\n\n");
			fw.write("Path: " + getStatePath(context) + "\n\n");
			fw.write("Time taken: " + String.valueOf(difference) + " milliseconds \n\n");
			for(Map.Entry<StatementMetadata, ca.uqac.lif.cornipickle.Verdict> statement : verdicts.entrySet()) {
				fw.write("Statement:\n" + statement.getKey().toString() + "\n\n");
				fw.write("Verdict:\n" + statement.getValue().getValue().toString() + "\n\n");
				fw.write("Witness:\n" + statement.getValue().getWitnessFalse().toString() + "\n\n");
			}
			//fw.write("JSON: \n" + content.toString() + "\n\n");
			fw.write("----------------------------------------------------------------------------\n\n");
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * Function used to send the properties to the interpreter.
	 * @param properties  a string of containing Cornipickle code
	 * @return  true if it parsed successfully, false otherwise
	 */
	public void setProperties(String properties) throws ParseException {
		this.m_corniInterpreter.clear();
		this.m_corniInterpreter.parseProperties(properties);
		
		this.m_attributes = m_corniInterpreter.getAttributes();
		this.m_tagNames = m_corniInterpreter.getTagNames();
	}

	@Override
	public String getOutputFolder() {
		return this.m_outputFolder;
	}

	@Override
	public void setOutputFolder(String arg0) {
		this.m_outputFolder = arg0;
	}
	
	private String getStatePath(CrawlerContext context) {
		String ret = "";
		try {
			for(Eventable event : context.getCrawlPath()) {
				ret = ret + event.getSourceStateVertex().getName() + " -> ";
			}
			ret = ret + context.getCrawlPath().last().getTargetStateVertex().getName();
		} catch (NullPointerException e) {
			ret = ret + "index";
		}
		
		return ret;
	}
	
	private String readJS() {
		InputStream is;
		try {
			is = getClass().getResourceAsStream("resources/serialization.js");
			BufferedReader bf = new BufferedReader(new InputStreamReader(is));
			String inputLine;
			String script = "";
	        while ((inputLine = bf.readLine()) != null) {
	        	script = script + inputLine + "\n";
	        }
	        
	        return script;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
}