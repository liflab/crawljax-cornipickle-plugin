package cornipickleplugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.browser.EmbeddedBrowser.BrowserType;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.CrawljaxRunner;
import com.crawljax.core.configuration.BrowserConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.configuration.InputSpecification;
import com.crawljax.core.plugin.HostInterfaceImpl;
import com.crawljax.core.plugin.OnNewStatePlugin;
import com.crawljax.core.plugin.descriptor.Parameter;
import com.crawljax.core.plugin.descriptor.PluginDescriptor;
import com.crawljax.core.state.StateVertex;

/**
 * Use the sample plugin in combination with Crawljax.
 */
public class Runner {

	private static final String URL = "http://www.apple.com";
	private static final int MAX_DEPTH = 2;
	private static final int MAX_NUMBER_STATES = 8;
	private static final Logger LOG = LoggerFactory.getLogger(Runner.class);

	/**
	 * Entry point
	 */
	public static void main(String[] args) {
		CrawljaxConfiguration.CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor(URL);
		builder.crawlRules().insertRandomDataInInputForms(false);
		
		builder.setBrowserConfig(new BrowserConfiguration(BrowserType.PHANTOMJS));

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
			parameters.put(parameter.getId(), "value");
		}
		builder.addPlugin(new CornipicklePlugin(new HostInterfaceImpl(new File("out"), parameters))); 

		builder.crawlRules().setInputSpec(getInputSpecification());

		CrawljaxRunner crawljax = new CrawljaxRunner(builder.build());
		crawljax.call();
	}

	private static InputSpecification getInputSpecification() {
		InputSpecification input = new InputSpecification();
		input.field("name").setValue("Crawl");
		return input;
	}

	private Runner() {
		// Utility class
	}
}
