package cornipickleplugin;

import java.io.File;
import java.io.FileWriter;
import java.util.Set;

import com.crawljax.core.CandidateElement;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.plugin.GeneratesOutput;
import com.crawljax.core.plugin.HostInterface;
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
	
	private static final Logger LOG = LoggerFactory.getLogger(CornipicklePlugin.class);

	/**
	 * Constructor for the plugin
	 * @param hostInterface
	 */
	public CornipicklePlugin(HostInterface hostInterface) {
		this.m_hostInterface = hostInterface;
		this.m_outputFolder = hostInterface.getOutputDirectory().getAbsolutePath();
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
	public boolean setProperties(String properties) {
		boolean success = true;
		try {
			this.m_corniInterpreter.clear();
			this.m_corniInterpreter.parseProperties(properties);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			success = false;
		}
		
		this.m_attributes = m_corniInterpreter.getAttributes();
		this.m_tagNames = m_corniInterpreter.getTagNames();
		
		return success;
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
}