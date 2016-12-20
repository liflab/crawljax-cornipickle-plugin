package cornipickleplugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.crawljax.browser.EmbeddedBrowser.BrowserType;
import com.crawljax.core.CrawljaxRunner;
import com.crawljax.core.configuration.BrowserConfiguration;
import com.crawljax.core.configuration.CrawljaxConfiguration;
import com.crawljax.core.configuration.InputSpecification;
import com.crawljax.core.plugin.HostInterfaceImpl;
import com.crawljax.core.plugin.descriptor.Parameter;
import com.crawljax.core.plugin.descriptor.PluginDescriptor;

import ca.uqac.lif.cornipickle.CornipickleParser.ParseException;

public class BeepStoreRunner {

	private static final String URL = "http://localhost/beepstore";
	private static final int MAX_DEPTH = 2;
	private static final int MAX_NUMBER_STATES = 8;
	private static final Logger LOG = LoggerFactory.getLogger(Runner.class);
	
	public static void main(String[] args) {
		CrawljaxConfiguration.CrawljaxConfigurationBuilder builder = CrawljaxConfiguration.builderFor(URL);
		builder.crawlRules().insertRandomDataInInputForms(false);
		
		builder.setBrowserConfig(new BrowserConfiguration(BrowserType.CHROME));

		builder.crawlRules().click("a");
		builder.crawlRules().click("button");

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
				parameters.put(parameter.getId(), "/home/fguerin/Documents/crawljax-cornipickle-plugin/properties.cp");
			}
			else {
				parameters.put(parameter.getId(), "value");
			}
		}
		CornipicklePlugin plugin;
		try {
			plugin = new CornipicklePlugin(new HostInterfaceImpl(new File("out.txt"), parameters));
			builder.addPlugin(plugin); 

			builder.crawlRules().setInputSpec(getInputSpecification());

			CrawljaxRunner crawljax = new CrawljaxRunner(builder.build());
			crawljax.call();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static InputSpecification getInputSpecification() {
		InputSpecification input = new InputSpecification();
		input.field("fault-leave-login-link").setValue(true);
		input.field("username").setValue("user");
		input.field("password").setValue("1234");
		return input;
	}

	private BeepStoreRunner() {
		// Utility class
	}

}
