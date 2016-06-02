package org.yocto.crops.poky.tests.ui;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.junit.SWTBotJunit4ClassRunner;
import org.eclipse.swtbot.swt.finder.widgets.SWTBotShell;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SWTBotJunit4ClassRunner.class)
public class IntegrationTestNewCProject {
	
	private static SWTWorkbenchBot bot;
	private static final String projName = "MyFirstTestProject"; //$NON-NLS-1$
	
	@BeforeClass
	public static void beforeClass() throws Exception {
		bot = new SWTWorkbenchBot();
		bot.viewByTitle("Welcome").close();
		bot.perspectiveByLabel("C/C++").activate();
	}
	
	@Test
	public void canCreateANewCProject() throws Exception {
		bot.menu("File").menu("New").menu("Project...").click();
		
		SWTBotShell shell = bot.shell("New Project");
		shell.activate();
		bot.tree().expandNode("C/C++").select("C Project");
		bot.button("Next >").click();
		
		bot.textWithLabel("Project name:").setText(projName);
		
		bot.button("Finish").click();
		bot.viewByTitle("Project Explorer");
		// FIXME: assert that the project is actually created, for later
	}
	
	@AfterClass
	public static void sleep() {
		bot.sleep(2000);
	}

}
