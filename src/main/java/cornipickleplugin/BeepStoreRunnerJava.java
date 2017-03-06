package cornipickleplugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.browser.EmbeddedBrowser.BrowserType;
import com.crawljax.condition.VisibleCondition;
import com.crawljax.core.CrawljaxRunner;
import com.crawljax.core.configuration.BrowserConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.configuration.InputSpecification;
import com.crawljax.core.plugin.HostInterfaceImpl;
import com.crawljax.core.plugin.descriptor.PluginDescriptor;
import com.crawljax.core.state.Identification;

public class BeepStoreRunnerJava {
	private static final String URL = "http://localhost/beepstore";
	private static final int MAX_DEPTH = 20;
	private static final int MAX_NUMBER_STATES = 200;
	private static final Logger LOG = LoggerFactory.getLogger(Runner.class);

	public static void main(String[] args) {
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

		PluginDescriptor descriptor = PluginDescriptor.forPlugin(JavaPlugin.class);
		Map<String, String> parameters = new HashMap<>();
		JavaPlugin plugin;
		plugin = new JavaPlugin(new HostInterfaceImpl(new File("."), parameters));
		builder.addPlugin(plugin); 

		builder.crawlRules().setInputSpec(getInputSpecification());

		CrawljaxRunner crawljax = new CrawljaxRunner(builder.build());
		crawljax.call();

	}
	
	private static InputSpecification getInputSpecification() {
		InputSpecification input = new InputSpecification();
		input.field("username").setValue("user");
		input.field("password").setValue("1234");
		return input;
	}

	private BeepStoreRunnerJava() {
		// Utility class
	}

}
