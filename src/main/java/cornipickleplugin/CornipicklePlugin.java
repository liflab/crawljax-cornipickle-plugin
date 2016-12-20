package cornipickleplugin;

import java.io.BufferedReader;
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
public class CornipicklePlugin implements OnNewStatePlugin, OnRevisitStatePlugin, GeneratesOutput {
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
			FileWriter fw = new FileWriter(hostInterface.getOutputDirectory(), false);
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
			this.m_corniInterpreter = new Interpreter();
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
		
		Map<StatementMetadata, Verdict> verdicts = this.m_corniInterpreter.getVerdicts();
		
		double end = (double)System.currentTimeMillis();
		double difference = (end - begin) / 1000 / 60;
		
		try {
			FileWriter fw = new FileWriter(this.m_hostInterface.getOutputDirectory(), true);
			fw.write("<br>New State " + String.valueOf(newState.getId() + "</br>\n\n"));
			fw.write("URL:\n" + newState.getUrl() + "\n\n");
			fw.write("Path: " + getStatePath(context) + "\n\n");
			fw.write("Time taken: " + String.valueOf(difference) + " minutes \n\n");
			for(Map.Entry<StatementMetadata, Verdict> statement : verdicts.entrySet()) {
				fw.write("Statement:\n" + statement.getKey().toString() + "\n\n");
				fw.write("Verdict:\n" + statement.getValue().getValue().toString() + "\n\n");
				fw.write("Witness:\n" + statement.getValue().getWitnessFalse().toString() + "\n\n");
			}
			fw.write("JSON: \n" + content.toString() + "\n\n");
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
		
		Map<StatementMetadata, Verdict> verdicts = this.m_corniInterpreter.getVerdicts();
		
		double end = (double)System.currentTimeMillis();
		double difference = (end - begin) / 1000 / 60;
		
		try {
			FileWriter fw = new FileWriter(this.m_hostInterface.getOutputDirectory(), true);
			fw.write("<br>Revisit State " + String.valueOf(currentState.getId() + "</br>\n\n"));
			fw.write("URL:\n" + currentState.getUrl() + "\n\n");
			fw.write("Path: " + getStatePath(context) + "\n\n");
			fw.write("Time taken: " + String.valueOf(difference) + " minutes \n\n");
			for(Map.Entry<StatementMetadata, Verdict> statement : verdicts.entrySet()) {
				fw.write("Statement:\n" + statement.getKey().toString() + "\n\n");
				fw.write("Verdict:\n" + statement.getValue().getValue().toString() + "\n\n");
				fw.write("Witness:\n" + statement.getValue().getWitnessFalse().toString() + "\n\n");
			}
			fw.write("JSON: \n" + content.toString() + "\n\n");
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
	
	private JsonElement serializePage(WebElement node, CrawlerContext context) {
		JsonElement content = serializePageContent(node, context, "//html[1]/body");
		content = serializeWindow(content, context);
		return content;
	}
	
	/**
	 * Serializes the current web page to evaluate in the interpreter.
	 * @param node  current node to serialize
	 * @param event  event that triggered the new state
	 * @return the serialized page
	 */
	private JsonElement serializePageContent(WebElement node, CrawlerContext context, String path) {
		JsonMap out = new JsonMap();
		
		WebElement target = null;
		
		if(!(node.equals(context.getBrowser().getWebElement(new Identification(Identification.How.xpath,path))))) {
			throw new CrawljaxException("The xpath " + path +  " doesn't match the current node in CornipicklePlugin");
		}
		
		try {
			target = context.getBrowser().getWebElement(context.getCrawlPath().last().getIdentification());
		}
		catch(NullPointerException e) {
			target = null;
		}
		catch(NoSuchElementException e) {
			target = null;
		}
		
		if(node.getTagName() != null) {
			LOG.debug("tag: " + node.getTagName() + " ");
			if(node.getAttribute("id") != null) {
				LOG.debug("id: " + node.getAttribute("id") + " ");
				if(node.getAttribute("class") != null) {
					LOG.debug("class: " + node.getAttribute("class") + " ");
				}
			}
		}
		
		if(includeInResult(node) == Include.INCLUDE || (target != null && node.equals(target))) {
			if(node.getTagName() != null) {
				Point pos = node.getLocation();
				out.put("tagname", node.getTagName().toLowerCase());
				out.put("cornipickleid", String.valueOf(this.m_cornipickleIdCounter++));
				out = isDefined("value") ? add(out, "value", setValue(node)) : out;
				out = isDefined("class") ? add(out, "class", node.getAttribute("class")) : out;
				out = isDefined("id") ? add(out, "id", node.getAttribute("id")) : out;
				out = isDefined("height") ? add(out, "height", node.getAttribute("clientHeight")) : out;
				out = isDefined("width") ? add(out, "width", node.getAttribute("clientWidth")) : out;
				out = isDefined("background") ? add(out, "background", getComputedValue(context,path,"background-color").trim()) : out;
				out = isDefined("color") ? add(out, "color", getComputedValue(context,path,"color")) : out;
				out = isDefined("border") ? add(out, "border", formatBorderString(context,path,node)) : out;
				out = isDefined("top") ? add(out, "top", String.valueOf(pos.getY())) : out;
				out = isDefined("left") ? add(out, "left", String.valueOf(pos.getX())) : out;
				out = isDefined("bottom") ? add(out, "bottom", add_dimensions(new String[]{String.valueOf(pos.getY()),node.getAttribute("clientHeight")})) : out;
				out = isDefined("right") ? add(out, "right", add_dimensions(new String[]{String.valueOf(pos.getX()),node.getAttribute("clientWidth")})) : out;
				out = isDefined("display") ? add(out, "display", getComputedValue(context,path,"display")) : out;
				out = isDefined("size") ? add(out, "size", node.getAttribute("size")) : out;
				out = isDefined("checked") ? add(out, "checked", String.valueOf(node.isSelected())) : out;
				out = isDefined("disabled") ? add(out, "disabled", String.valueOf(!node.isEnabled())) : out;
				out = isDefined("accesskey") ? add(out, "accesskey", node.getAttribute("accesskey")) : out;
				out = isDefined("min") ? add(out, "min", node.getAttribute("min")) : out;
				
				if(node.equals(target)) {
					out.put("event", serializeEvent(context));
				}
				
				if(node.getTagName().toLowerCase().equals("input")) {
					if(node.getAttribute("type").equals("text")) {
						JsonList children = new JsonList();
						JsonMap child = new JsonMap();
						child.put("tagname", "CDATA");
						child.put("text", node.getAttribute("value"));
						children.add(child);
						out.put("children", children);
						return out;
					}
				}
				else if(node.getTagName().toLowerCase().equals("button")) {
					JsonList children = new JsonList();
					JsonMap child = new JsonMap();
					child.put("tagname", "CDATA");
					child.put("text", node.getAttribute("innerHTML"));
					children.add(child);
					out.put("children", children);
					return out;
				}
			}
		}
		if(includeInResult(node) != Include.DONT_INCLUDE_RECURSIVE) {
			JsonList in_children = new JsonList();
			String text = node.getAttribute("textContent").replace(System.getProperty("line.separator"), "").trim();
			int childNumber = 0;
			for(WebElement child : node.findElements(new By.ByXPath("*"))) {
				childNumber++;
				String childContent = child.getAttribute("textContent").replace(System.getProperty("line.separator"), "").trim();
				int firstIndex = text.indexOf(childContent);
				String before = text.substring(0,firstIndex);
				String after = text.substring(firstIndex+childContent.length());
				text = before.concat(after);
				String newPath = path.concat("/*[" + String.valueOf(childNumber) + "]");
				JsonElement new_child = serializePageContent(child, context, newPath);
				if(!is_empty(new_child)) {
					in_children.add(new_child);
				}
			}
			if(!(text.equals(""))) {
				JsonMap child = new JsonMap();
				child.put("tagname", "CDATA");
				child.put("text", text);
				in_children.add(child);
			}
			if(in_children.size() > 0) {
				out.put("children", in_children);
			}
		}
		
		return out;
	}
	
	private JsonElement serializeWindow(JsonElement content, CrawlerContext context) {
		JsonMap window = new JsonMap();
		window.put("tagname", "window");
		window.put("URL", (String) context.getBrowser().executeJavaScript(
				"return window.location.host + window.location.pathname;"));
		window.put("aspect-ratio", String.valueOf((Double)context.getBrowser().executeJavaScript(
				"return window.document.documentElement.clientWidth / window.document.documentElement.clientHeight;")));
		window.put("orientation", getOrientation(context));
		window.put("width", String.valueOf((Long)context.getBrowser().executeJavaScript(
				"return window.document.documentElement.clientWidth;")));
		window.put("height", String.valueOf((Long)context.getBrowser().executeJavaScript(
				"return window.document.documentElement.clientHeight;")));
		window.put("device-width", String.valueOf((Long)context.getBrowser().executeJavaScript(
				"return window.screen.availWidth;")));
		window.put("device-height", String.valueOf((Long)context.getBrowser().executeJavaScript(
				"return window.screen.availHeight;")));
		window.put("device-aspect-ratio", String.valueOf((Double)context.getBrowser().executeJavaScript(
				"return window.screen.availWidth / window.screen.availHeight;")));
		window.put("mediaqueries", serializeMediaQueries(context));
		
		JsonList children = new JsonList();
		children.add(content);
		window.put("children", children);
		
		return window;
	}
	
	private String serializeEvent(CrawlerContext context) {
		EventType type = context.getCrawlPath().last().getEventType();
		if(type == EventType.click) {
			return "click";
		}
		else {
			return "hover";
		}
	}
	
	private JsonElement serializeMediaQueries(CrawlerContext context) {
		JsonMap out = new JsonMap();
		
		for(String att : this.m_attributes) {
			int indexOfUnderscore = att.indexOf("_");
			String query = "";
			String id = "";
			if(indexOfUnderscore != -1) {
				id = att.substring(0,indexOfUnderscore);
				query = att.substring(indexOfUnderscore + 1);
				if((boolean)context.getBrowser().executeJavaScript("return window.matchMedia(\"" + query + "\").matches;")) {
					out.put(id, "true");
				}
				else {
					out.put(id, "false");
				}
			}
		}
		
		return out;
	}
	
	private Include includeInResult(WebElement node) {
		if(node.getAttribute("class").contains("nocornipickle")) {
			return Include.DONT_INCLUDE_RECURSIVE;
		}
		if(node.getTagName() == null) {
			if(node.getAttribute("value").trim() == "") {
				return Include.DONT_INCLUDE_RECURSIVE;
			}
			else {
				return Include.INCLUDE;
			}
		}
		
		for(String tag : this.m_tagNames) {
			if(matchesSelector(node,tag)) {
				return Include.INCLUDE;
			}
		}
		
		return Include.DONT_INCLUDE;
	}
	
	private boolean matchesSelector(WebElement node, String selector) {
		Matcher mat = Pattern.compile("([\\w\\d]+){0,1}(\\.([\\w\\d]+)){0,1}(#([\\w\\d]+)){0,1}").matcher(selector);
		
		mat.matches();
		
		if(mat.group(1) != null) {
			if(node.getTagName() == null || !(node.getTagName().toLowerCase().equals(mat.group(1).toLowerCase()))) {
				return false;
			}
		}
		if(mat.group(3) != null) {
			if(node.getAttribute("class") == null) {
				return false;
			}
			String[] class_parts = node.getAttribute("class").split(" ");
			if(!arrayContains(class_parts,mat.group(3))) {
				return false;
			}
		}
		if(mat.group(5) != null) {
			if(node.getAttribute("id") != null) {
				if(!(node.getAttribute("id").equals(mat.group(5)))) {
					return false;
				}
			}
			else {
				return false;
			}
		}
		
		return true;
	}
	
	private boolean arrayContains(String[] class_parts, String class_name) {
		for(int i = 0; i < class_parts.length; i++) {
			if(class_parts[i].equals(class_name)) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isDefined(String property_name) {
		if(arrayContains(this.m_attributes.toArray(new String[this.m_attributes.size()]),property_name)) {
			return true;
		}
		else {
			return false;
		}
	}
	
	private JsonMap add(JsonMap out, String property_name, String property) {
		if(property != null && !(property.equals(""))) {
			out.put(property_name, property);
		}
		return out;
	}
	
	private String setValue(WebElement node) {
		if(node.getTagName().toLowerCase().equals("input") || node.getTagName().toLowerCase().equals("button")) {
			if(node.getAttribute("type").equals("range") || node.getAttribute("type").equals("number")) {
				return node.getAttribute("valueAsNumber");
			}
			else {
				return node.getAttribute("value");
			}
		}
		return "";
	}
	
	private boolean is_empty(JsonElement elem) {
		JsonMap map = (JsonMap)elem;
		if(map.size() > 0) {
			return false;
		}
		return true;
	}
	
	private String formatBorderString(CrawlerContext context, String path, WebElement node) {
		String s_top_style = getComputedValue(context,path,"border-top-style");
		String s_top_colour = getComputedValue(context,path,"border-top-color");
		String s_top_width = getComputedValue(context,path,"border-top-width");
		String out = s_top_style + " " + s_top_colour + " " + s_top_width;
		return out.trim();
	}
	
	private String add_dimensions(String[] dimensions) {
		int sum = 0;
		
		for(int i = 0; i < dimensions.length; i++) {
			String d = dimensions[i];
			sum += Integer.parseInt(d.replaceAll("px", ""));
		}
		return String.valueOf(sum);
	}
	
	private String getOrientation(CrawlerContext context) {
		return (String)context.getBrowser().executeJavaScript("switch(window.orientation)" +
	    "{" +
	      "case -90:" +
	      "case 90:" +
	        "return \"landscape\";" +
	      "default:" +
	        "return \"portrait\";" +
	    "};");
	}
	
	private String getComputedValue(CrawlerContext context, String path, String attribute) {
		return (String) context.getBrowser().executeJavaScript(
				"var elem = document.evaluate(\"" + path + "\", document, null, 9, null).singleNodeValue;" +
				"var res = null;" +
				"if(elem.currentStyle)" +
				"{" +
				"	res = elem.currentStyle[\"" + attribute + "\"];" +
				"}" +
				"else if(window.getComputedStyle)" +
				"{" +
				"	if(window.getComputedStyle.getPropertyValue)" +
				"	{" +
				"		res = window.getComputedStyle(elem,null).getPropertyValue(\"" + attribute + "\");" +
				"	}" +
				"	else" +
				"	{" +
				"		res = window.getComputedStyle(elem)[\"" + attribute + "\"];" +
				"	}" +
				"}" +
				"return res");
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