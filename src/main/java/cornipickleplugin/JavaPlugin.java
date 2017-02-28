package cornipickleplugin;

import com.crawljax.browser.EmbeddedBrowser;
import com.crawljax.core.CrawlerContext;
import com.crawljax.core.plugin.GeneratesOutput;
import com.crawljax.core.plugin.HostInterface;
import com.crawljax.core.plugin.HostInterfaceImpl;
import com.crawljax.core.plugin.OnNewStatePlugin;
import com.crawljax.core.plugin.OnRevisitStatePlugin;
import com.crawljax.core.state.Eventable;
import com.crawljax.core.state.Identification;
import com.crawljax.core.state.StateVertex;

import java.io.FileWriter;
import java.io.IOException;
import java.util.regex.Pattern;

import org.openqa.selenium.WebElement;

public class JavaPlugin implements OnNewStatePlugin, OnRevisitStatePlugin, GeneratesOutput{
	
	private HostInterface m_hostInterface;
	private String m_outputFolder;
	
	private enum Verdict {TRUE, FALSE, INCONCLUSIVE};
	private Verdict m_verdict;
	
	@Override
	public void onNewState(CrawlerContext context, StateVertex newState) {
		double begin = (double)System.currentTimeMillis();
		
		if(m_verdict == Verdict.INCONCLUSIVE) {
			EmbeddedBrowser browser = context.getBrowser();

			Identification identificationActionBand = new Identification(Identification.How.id, "action-band");
			boolean signedIn = false;

			Identification identificationSignInDiv = new Identification(Identification.How.id, "sign-in");
			boolean currentlyInLoginPage = false;
			
			if(browser.elementExists(identificationActionBand)) {
				WebElement actionBand = browser.getWebElement(identificationActionBand);
				if(Pattern.matches("^Welcome.*", actionBand.getText())) {
					signedIn = true;
				}
			}
			
			if(browser.elementExists(identificationSignInDiv)) {
				WebElement signInDiv = browser.getWebElement(identificationSignInDiv);
				if(signInDiv.isDisplayed()) {
					currentlyInLoginPage = true;
				}
			}
			
			if(signedIn) {
				if(currentlyInLoginPage) {
					m_verdict = Verdict.FALSE;
				}
			}
		}
		
		double end = (double)System.currentTimeMillis();
		double difference = (end - begin);
		
		output(context, newState, difference);
	}
	
	public JavaPlugin() {
		this.m_hostInterface = new HostInterfaceImpl(null, null);
		this.m_outputFolder = "";
		this.m_verdict = Verdict.INCONCLUSIVE;
	}
	
	public JavaPlugin(HostInterface hostInterface) {
		this.m_hostInterface = hostInterface;
		this.m_outputFolder = hostInterface.getOutputDirectory().getAbsolutePath();
		this.m_verdict = Verdict.INCONCLUSIVE;
		
		try {
			FileWriter fw = new FileWriter(hostInterface.getOutputDirectory(), false);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void onRevisitState(CrawlerContext context, StateVertex currentState) {
		double begin = (double)System.currentTimeMillis();
		
		if(m_verdict == Verdict.INCONCLUSIVE) {
			EmbeddedBrowser browser = context.getBrowser();
			
			WebElement actionBand = null;
			Identification identificationActionBand = new Identification(Identification.How.id, "action-band");
			boolean signedIn = false;
			
			WebElement signInDiv = null;
			Identification identificationSignInDiv = new Identification(Identification.How.xpath, "//*[@id=\"sign-in\"]");
			boolean currentlyInLoginPage = false;
			
			if(browser.elementExists(identificationActionBand)) {
				actionBand = browser.getWebElement(identificationActionBand);
				if(Pattern.matches("^Welcome.*", actionBand.getText())) {
					signedIn = true;
				}
			}
			
			if(browser.elementExists(identificationSignInDiv)) {
				signInDiv = browser.getWebElement(identificationSignInDiv);
				if(signInDiv.isDisplayed()) {
					currentlyInLoginPage = true;
				}
			}
			
			if(signedIn) {
				if(currentlyInLoginPage) {
					m_verdict = Verdict.FALSE;
				}
			}
		}
		
		double end = (double)System.currentTimeMillis();
		double difference = (end - begin);
		
		output(context, currentState, difference);
	}

	@Override
	public String getOutputFolder() {
		return this.m_outputFolder;
	}

	@Override
	public void setOutputFolder(String arg0) {
		this.m_outputFolder = arg0;
	}
	
	public void output(CrawlerContext context, StateVertex newState, double difference) {
		try {
			FileWriter fw = new FileWriter(this.m_hostInterface.getOutputDirectory(), true);
			fw.write("<br>New State " + String.valueOf(newState.getId() + "</br>\n\n"));
			fw.write("URL:\n" + newState.getUrl() + "\n\n");
			fw.write("Path: " + getStatePath(context) + "\n\n");
			fw.write("Time taken: " + String.valueOf(difference) + " milliseconds \n\n");
			fw.write("Verdict: " + m_verdict.toString() + "\n\n");
			fw.write("----------------------------------------------------------------------------\n\n");
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
}
