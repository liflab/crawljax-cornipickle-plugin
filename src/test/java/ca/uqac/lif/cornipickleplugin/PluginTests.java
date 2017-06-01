package ca.uqac.lif.cornipickleplugin;

import static org.junit.Assert.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.browser.EmbeddedBrowser.BrowserType;
import com.crawljax.condition.VisibleCondition;
import com.crawljax.core.CrawljaxRunner;
import com.crawljax.core.configuration.BrowserConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.configuration.InputSpecification;
import com.crawljax.core.plugin.HostInterfaceImpl;
import com.crawljax.core.plugin.descriptor.Parameter;
import com.crawljax.core.plugin.descriptor.PluginDescriptor;
import com.crawljax.core.state.Identification;

import ca.uqac.lif.cornipickle.CornipickleParser.ParseException;
import ca.uqac.lif.cornipickleplugin.CornipicklePlugin;

public class PluginTests {

	private static final Logger LOG = LoggerFactory.getLogger(PluginTests.class);
	
	@Test
	public void generalTest() throws ParseException {
		String URL = "http://www.xkcd.com";
		int MAX_DEPTH = 2;
		int MAX_NUMBER_STATES = 8;
		
		CrawljaxConfiguration.CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor(URL);
		builder.crawlRules().insertRandomDataInInputForms(false);
		
		builder.setBrowserConfig(new BrowserConfiguration(BrowserType.CHROME));

		builder.crawlRules().click("a");

		// except these
		builder.crawlRules().dontClick("a").underXPath("//DIV[@id='guser']");
		builder.crawlRules().dontClick("a").withText("Language Tools");

		// limit the crawling scope
		builder.setMaximumStates(MAX_NUMBER_STATES);
		builder.setMaximumDepth(MAX_DEPTH);

		PluginDescriptor descriptor = PluginDescriptor.forPlugin(CornipicklePlugin.class);
		Map<String, String> parameters = new HashMap<>();
		for(Parameter parameter : descriptor.getParameters()) {
			if(parameter.getId().equals("properties")) {
				//Put here the path to a .cp file containing your Cornipickle properties
				String path = this.getClass().getResource("resources/properties.cp").getPath();
				parameters.put(parameter.getId(), path);
			}
			else {
				parameters.put(parameter.getId(), "value");
			}
		}
		CornipicklePlugin plugin;
		plugin = new CornipicklePlugin(new HostInterfaceImpl(new File("."), parameters));
		builder.addPlugin(plugin); 

		builder.crawlRules().setInputSpec(getInputSpecification());

		CrawljaxRunner crawljax = new CrawljaxRunner(builder.build());
		crawljax.call();
		assert(true);
	}
	
	@Test
	public void beepStoreTestBug1() throws ParseException {
		
		String URL = "http://localhost/beepstore";
		int MAX_DEPTH = 20;
		int MAX_NUMBER_STATES = 200;
		
		CrawljaxConfiguration.CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor(URL);
		builder.crawlRules().insertRandomDataInInputForms(false);
		
		builder.setBrowserConfig(new BrowserConfiguration(BrowserType.CHROME));

		VisibleCondition condahome = new VisibleCondition(new Identification(Identification.How.id, "home"));
		VisibleCondition condwhatis = new VisibleCondition(new Identification(Identification.How.id, "what-is-the-beep-store"));
		VisibleCondition condsignin = new VisibleCondition(new Identification(Identification.How.id, "sign-in"));
		VisibleCondition condcontact = new VisibleCondition(new Identification(Identification.How.id, "contact-us"));
		VisibleCondition conditemsearch = new VisibleCondition(new Identification(Identification.How.id, "item-search"));
		VisibleCondition condcart = new VisibleCondition(new Identification(Identification.How.id, "cart-display"));
		VisibleCondition condfaults = new VisibleCondition(new Identification(Identification.How.id, "fault-parameters"));
		
		builder.crawlRules().click("a");
		builder.crawlRules().click("button");
		builder.crawlRules().dontClick("a").withAttribute("text", "Sign out");
		builder.crawlRules().dontClick("input").when(condfaults);
		builder.crawlRules().clickOnce(false);
		builder.crawlRules().dontClickChildrenOf("li");

		// limit the crawling scope
		builder.setMaximumStates(MAX_NUMBER_STATES);
		builder.setMaximumDepth(MAX_DEPTH);

		PluginDescriptor descriptor = PluginDescriptor.forPlugin(CornipicklePlugin.class);
		Map<String, String> parameters = new HashMap<>();
		for(Parameter parameter : descriptor.getParameters()) {
			if(parameter.getId().equals("properties")) {
				//Put here the path to a .cp file containing your Cornipickle properties
				String path = this.getClass().getResource("resources/propertiesbug1.cp").getPath();
				parameters.put(parameter.getId(), path);
			}
			else {
				parameters.put(parameter.getId(), "value");
			}
		}
		CornipicklePlugin plugin;
		plugin = new CornipicklePlugin(new HostInterfaceImpl(new File("."), parameters));
		builder.addPlugin(plugin); 

		builder.crawlRules().setInputSpec(getInputSpecification());

		CrawljaxRunner crawljax = new CrawljaxRunner(builder.build());
		crawljax.call();
		assert(true);
	}
	
	@Test
	public void beepStoreTestbug2() throws ParseException {
		String URL = "http://localhost/beepstore";
		int MAX_DEPTH = 20;
		int MAX_NUMBER_STATES = 200;
		
		CrawljaxConfiguration.CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor(URL);
		builder.crawlRules().insertRandomDataInInputForms(false);
		
		builder.setBrowserConfig(new BrowserConfiguration(BrowserType.CHROME));

		VisibleCondition condahome = new VisibleCondition(new Identification(Identification.How.id, "home"));
		VisibleCondition condwhatis = new VisibleCondition(new Identification(Identification.How.id, "what-is-the-beep-store"));
		VisibleCondition condsignin = new VisibleCondition(new Identification(Identification.How.id, "sign-in"));
		VisibleCondition condcontact = new VisibleCondition(new Identification(Identification.How.id, "contact-us"));
		VisibleCondition conditemsearch = new VisibleCondition(new Identification(Identification.How.id, "item-search"));
		VisibleCondition condcart = new VisibleCondition(new Identification(Identification.How.id, "cart-display"));
		VisibleCondition condfaults = new VisibleCondition(new Identification(Identification.How.id, "fault-parameters"));
		
		builder.crawlRules().click("button");
		builder.crawlRules().click("a").withText("Sign in");
		builder.crawlRules().click("a").withText("Search an item");
		builder.crawlRules().click("a").withAttribute("class", "button-item-search");
		builder.crawlRules().click("a").withAttribute("class", "button-create-cart");
		builder.crawlRules().click("a").withAttribute("class", "button-login");
		builder.crawlRules().clickOnce(false);

		// limit the crawling scope
		builder.setMaximumStates(MAX_NUMBER_STATES);
		builder.setMaximumDepth(MAX_DEPTH);

		PluginDescriptor descriptor = PluginDescriptor.forPlugin(CornipicklePlugin.class);
		Map<String, String> parameters = new HashMap<>();
		for(Parameter parameter : descriptor.getParameters()) {
			if(parameter.getId().equals("properties")) {
				//Put here the path to a .cp file containing your Cornipickle properties
				String path = this.getClass().getResource("resources/propertiesbug2.cp").getPath();
				parameters.put(parameter.getId(), path);
			}
			else {
				parameters.put(parameter.getId(), "value");
			}
		}
		CornipicklePlugin plugin;
		plugin = new CornipicklePlugin(new HostInterfaceImpl(new File("."), parameters));
		builder.addPlugin(plugin); 

		builder.crawlRules().setInputSpec(getInputSpecification());

		CrawljaxRunner crawljax = new CrawljaxRunner(builder.build());
		crawljax.call();
		assert(true);
	}
	
	private static InputSpecification getInputSpecification() {
		InputSpecification input = new InputSpecification();
		input.field("username").setValue("user");
		input.field("password").setValue("1234");
		return input;
	}
}
