package cornipickleplugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.regex.Pattern;

import com.crawljax.core.CandidateElement;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.plugin.GeneratesOutput;
import com.crawljax.core.plugin.HostInterface;
import com.crawljax.core.plugin.HostInterfaceImpl;
import com.crawljax.core.plugin.OnNewStatePlugin;
import com.crawljax.core.state.CrawlPath;
import com.crawljax.core.state.Identification;
import com.crawljax.core.state.StateVertex;
import com.google.common.collect.ImmutableList;

import ca.uqac.lif.cornipickle.CornipickleParser.ParseException;
import ca.uqac.lif.cornipickle.Interpreter;
import ca.uqac.lif.json.JsonElement;
import ca.uqac.lif.json.JsonList;
import ca.uqac.lif.json.JsonMap;

import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin for Crawljax that tests every new state with Cornipickle
 * @author fguerin
 *
 */
public class CornipicklePlugin implements OnNewStatePlugin, GeneratesOutput {
	private HostInterface m_hostInterface;
	
	private String m_outputFolder;
	
	private Interpreter m_corniInterpreter;
	
	private Set<String> m_attributes;
	
	private Set<String> m_tagNames;
	
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
	}
	
	/* 
	 * Function executed everytime a new state is found
	 * @see com.crawljax.core.plugin.OnNewStatePlugin#onNewState(com.crawljax.core.CrawlerContext, com.crawljax.core.state.StateVertex)
	 */
	@Override
	public void onNewState(CrawlerContext context, StateVertex newState) {
		context.getBrowser().getWebElement(new Identification(Identification.How.tag,"body"));
		CrawlPath path;
		try {
			path = context.getCrawlPath();
		}
		catch(RuntimeException e) {
			
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
	
	/**
	 * Serializes the current web page to evaluate in the interpreter.
	 * @param node  current node to serialize
	 * @param event  event that triggered the new state
	 * @return the serialized page
	 */
	private JsonElement serializePageContent(WebElement node, String event) {
		JsonMap out = new JsonMap();
		
		
		
		return out;
	}
	
	private Include includeInResult(WebElement node) {
		if(node.getTagName().equals("")) {
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
		String[] mat = Pattern.compile("([\\w\\d]+){0,1}(\\.([\\w\\d]+)){0,1}(#([\\w\\d]+)){0,1}").split(selector);
		String tag_name = mat[1];
		String class_name = mat[3];
		String id_name = mat[5];
	
		return false;
	}
}