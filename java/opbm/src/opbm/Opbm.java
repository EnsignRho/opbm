/*
 * OPBM - Office Productivity Benchmark
 *
 * This class is the top-level class of the OPBM.  It creates a GUI, loads
 * necessary files, beings processing based on context, etc.
 *
 * Last Updated:  Sep 21, 2011
 *
 * by Van Smith
 * Cossatot Analytics Laboratories, LLC. (Cana Labs)
 *
 * (c) Copyright Cana Labs.
 * Free software licensed under the GNU GPL2.
 *
 * @version 1.2.0
 *
 */

package opbm;

import java.util.Collections;
import opbm.dialogs.OpbmInput;
import opbm.common.Xml;
import opbm.common.Macros;
import opbm.common.Settings;
import opbm.common.Commands;
import opbm.dialogs.resultsviewer.ResultsViewer;
import opbm.panels.right.PanelRightLookupbox;
import opbm.panels.right.PanelRight;
import opbm.panels.right.PanelRightListbox;
import opbm.panels.left.PanelLeft;
import opbm.panels.PanelFactory;
import opbm.panels.right.PanelRightItem;
import opbm.dialogs.DroppableFrame;
import opbm.benchmarks.Benchmarks;
import opbm.common.Tuple;
import opbm.common.Utils;
import java.io.*;
import java.awt.*;
import java.awt.Image.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.List;
import java.util.ArrayList;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import opbm.benchmarks.BenchmarkManifest;
import opbm.benchmarks.BenchmarkParams;
import opbm.common.ModalApp;
import opbm.dialogs.DeveloperWindow;
import opbm.dialogs.OpbmDialog;
import opbm.dialogs.SimpleWindow;
import org.xml.sax.SAXException;
import static java.awt.GraphicsDevice.WindowTranslucency.*;


/**
 * Primary Office Productivity Benchmark class deriving everything for the
 * OPBM application from an intelligent assembly of child classes.
 */
public final class Opbm extends	ModalApp
					 implements	AdjustmentListener,
								KeyListener,
								MouseWheelListener,
								ComponentListener
{
//////////
//
// NATIVE functions in opbm64.dll:
//
//////
	static {
		if (System.getProperty("sun.arch.data.model").equals("32"))
		{	// 32-bit JVM
			System.loadLibrary("opbm32");
			System.out.println("Running 32-bit JVM");

		} else {
			// 64-bit JVM
			System.loadLibrary("opbm64");
			System.out.println("Running 64-bit JVM");
		}
	}
	public native static void		sendWindowToForeground(String title);
	// Note:  All of these get__Directory() functions ALWAYS return a path ending in a backslash
	public native static String		getHarnessCSVDirectory();					// Returns c:\\users\\user\\documents\\obbm\\results\\csv\\
	public native static String		getHarnessXMLDirectory();					// Returns c:\\users\\user\\documents\\opbm\\results\\xml\
	public native static String		getHarnessTempDirectory();					// Returns c:\\users\\user\\documents\\opbm\\temp\\
	public native static String		getScriptCSVDirectory();					// Returns c:\\users\\user\\documents\\opbm\\scriptOutput\\
	public native static String		getScriptTempDirectory();					// Returns c:\\users\\user\\documents\\opbm\\scriptOutput\\temp\\
	public native static String		getSettingsDirectory();						// Returns c:\\users\\user\\documents\\opbm\\settings\\
	public native static String		getRunningDirectory();						// Returns c:\\users\\user\\documents\\opbm\\running\\
	public native static String		getCSIDLDirectory(String name);				// Returns directory specified by the CSIDL option
	public native static String		getCompressedPathname(String name);			// Returns the compressed pathname form, as in c:\\some\\..\\dir\\file.ext actually being c:\\dir\\file.ext
	// End Note
	public native static int		getComponentHWND(Component c);				// Returns the HWND for the specified component, or -1 if does not exist
	public native static int		setMinMaxResizeBoundaries(int hwnd, int minWidth, int minHeight, int maxWidth, int maxHeight, int desktopWidth, int desktopHeight);	// Sets a window to never be resized above or below these minimum widths/heights
	public native static int		setPersistAlwaysOnTop(int hwnd);			// Sets a window's status to be always on top, and to remain on top
	public native static void		snapshotProcesses();						// Takes a snapshot of the currently running processes
	public native static void		stopProcesses();							// Stops all processes that were not running when the snapshot was taken
	public native static String		GetRegistryKeyValue(String key);			// Requests the registry key value
	public native static String		SetRegistryKeyValueAsString(String key, String value);				// Writes the registry key and value as a REG_SZ
	public native static String		SetRegistryKeyValueAsDword(String key, int value);					// Writes the registry key and value as a REG_DWORD
	public native static String		SetRegistryKeyValueAsBinary(String key, String value, int length);	// Writes the registry key and value as a REG_BINARY
	public native static void		Office2010SaveKeys();						// Saves the current registry keys to a temporary area
	public native static void		Office2010InstallKeys();					// Installs registry keys required by OPBM
	public native static void		Office2010RestoreKeys();					// Restores the user's previous registry key settings
	public native static float		waitUntilSystemIdle(int percent, int durationMS, int timeoutMS);	// Waits up to timeoutMS for a period durationMS long of percent-or-lower total system activity




	/** Constructor creates ArrayList for m_leftPanels and m_navHistory, master
	 * Macros and Commands class objects.
	 *
	 * @param args Allows several switches:
	 *			-font			-- to change the default fonts
	 *			-atom:			-- Execute an atom
	 *			-atom(N):		-- Execute an atom N times
	 *			-trial			-- Execute a Trial Run of the entire benchmark suite
	 *			-official		-- Execute an Official Run of the entire benchmark suite
	 *			-skin			-- Load the simple, Skinned GUI
	 *			-simple			-- Load the Simple, skinned GUI
	 *			-developer		-- Load the Developer GUI
	 */
	@SuppressWarnings("LeakingThisInConstructor")
    public Opbm(String[] args)
	{
		m_opbm = this;
		System.out.println("Java version: " + System.getProperty("java.version"));
		System.out.println("JVM home: " + m_jvmHome);
		File f = new File(m_jvmHome);
		if (!f.exists())
		{	// Give a warning
			System.out.println("Warning: JVM home does not exist. Use -home:path override");
		}
                
                // Check to see if the current filepath is too long. -rcp 12/12/2011
                String currentFilepath = Utils.getCurrentDirectory();
                if (currentFilepath.length() >= 170)
                {
                    System.out.println("Filepath " + currentFilepath +" is too long. \nPlease move to a shorter filepath.");
                    // It is too long, tell the user and quit
                    JOptionPane.showMessageDialog(m_frameSimple, "Filepath " + currentFilepath +" is too long. \nPlease move to a shorter filepath.");
                    quit(-1);  
                          
                }


/*
 * Used for debugging, or reference.  This data comes from the opbm64.dll or opbm32.dll functions:
		System.out.println(" Harness CSV Directory: " + getHarnessCSVDirectory());
		System.out.println(" Harness XML Directory: " + getHarnessXMLDirectory());
		System.out.println("Harness Temp Directory: " + getHarnessTempDirectory());
		System.out.println("  Script CSV Directory: " + getScriptCSVDirectory());
		System.out.println(" Script Temp Directory: " + getScriptTempDirectory());
		System.out.println("     Running Directory: " + getRunningDirectory());
		System.out.println("    Settings Directory: " + getSettingsDirectory());
		System.out.println("    System32 Directory: " + getCSIDLDirectory("SYSTEM"));
 */

		// Make sure we're the only app running
		if (!isModalApp( getHarnessTempDirectory() + "opbm.dat", m_title ))
		{	// Already another app instance running
			System.out.println("Another process is running, bringing it to foreground.");
			sendWindowToForeground(m_title);
			quit(-1);
		}

		System.out.println("Detected auto-logon is " + (isAutoLogonEnabled() ? "enabled" : "disabled"));
		System.out.println("Detected UAC is " + (isUACEnabled() ? "enabled" : "disabled"));

		// Set the necessary startup variables
		m_args						= args;
		m_leftPanels				= new ArrayList<PanelLeft>(0);
		m_navHistory				= new ArrayList<PanelLeft>(0);
		m_editPanels				= new ArrayList<PanelRight>(0);
		m_rawEditPanels				= new ArrayList<PanelRight>(0);
		m_zoomFrames				= new ArrayList<JFrame>(0);
		m_rvFrames					= new ArrayList<DroppableFrame>(0);
		m_compilation				= new ArrayList<Xml>(0);
		m_tuples					= new ArrayList<Tuple>(0);
		m_macroMaster				= new Macros(this);
		m_benchmarkMaster			= new Benchmarks(this);
		m_settingsMaster			= new Settings(this);
		m_commandMaster				= new Commands(this, m_macroMaster, m_settingsMaster);
		m_executingFromCommandLine	= false;
		m_executingTrialRun			= false;
		m_executingOfficialRun		= false;
		m_executingBenchmarkRunName	= "";

		// If -font option is on command line, use slightly smaller fonts
		// REMEMBER I desire to change this later to use settings.xml file
		// to declare all fonts, then use -font Name to choose a font profile
		// from within the xml file, such as "-font Linux" for fonts that work
		// well with Linux.
		if (args.length != 0 && args[0].toLowerCase().contains("font"))
			m_fontOverride	= true;
		else
			m_fontOverride	= false;

        SwingUtilities.invokeLater(new Runnable()
		{
            @Override
            public void run()
			{
				// Show the GUI, which also loads the scripts.xml, edits.xml and panels.xml (essential files)
				createAndShowGUI();
				// This function exists outside the thead so it blocks the UI until everything is created

				// Create a non-edt thread to allow the GUI to continue starting up and displaying while processing
				Thread t = new Thread("OpbmCommandLineProcessingThread")
				{
					@Override
					public void run()
					{
						boolean isSilent;
						boolean errorsWereReported;
						boolean wasFound;
						List<String>	args	= new ArrayList<String>(0);
						List<Xml>		list	= new ArrayList<Xml>(0);
						List<Tuple>		todos	= new ArrayList<Tuple>(0);
						Xml target;
						String line, name, digits, group, command, temp;
						int i, j, count, iterations, runCount;
						BenchmarkManifest bm = new BenchmarkManifest(m_opbm, "compilation", "", true, false);
						OpbmDialog od;
						Tuple todo;

						// Used for the -noexit switch
						m_noExit = false;

						// Load the command line options, including those from files, into the execution sequence
						// Arguments get loaded into "List<String> args" rather than m_args[]
						// This allows command-line options that use @filename to be expanded into
						// their individual lines as additional command-line options.
						for (i = 0; i < m_args.length; i++)
						{
							if (m_args[i].startsWith("@"))
							{	// Load this file's entries
								Opbm.readTerminatedLinesFromFile(m_args[i].substring(1), args);

							} else {
								// Add this option
								args.add(m_args[i]);
							}
						}

						// Look for necessary-to-know-in-advance flags
						isSilent			= false;
						errorsWereReported	= false;
						for (i = 0; i < args.size(); i++)
						{
							line = args.get(i);
							if (line.toLowerCase().startsWith("-noexit"))
							{	// They don't want to exit when any automated runs are complete
								todos.add(new Tuple("switch", "noexit"));

							} else if (line.toLowerCase().startsWith("-silent")) {
								todos.add(new Tuple("switch", "silent"));

							} else if (line.toLowerCase().startsWith("-skin") || line.toLowerCase().startsWith("-simple")) {
								// They want to launch the simple skinned window
								todos.add(new Tuple("switch", "skin"));

							} else if (line.toLowerCase().startsWith("-developer")) {
								// They want to launch the developer window
								todos.add(new Tuple("switch", "developer"));

							} else if (line.toLowerCase().startsWith("-home:")) {
								// They are overriding the default java.home location for java.exe for the restarter
								temp = line.substring(6).replace("\"", "");
								File f = new File(temp);
								if (!f.exists())
								{	// The override location does not exist
									System.out.println("Error: Java.home command-line override \"" + temp + "\" does not exist.");
									errorsWereReported = true;
								} else {
									// It does exist, add it to the todos list
									todos.add(new Tuple("switch", "jvmhome", "temp"));
								}

							} else if (line.toLowerCase().startsWith("-restart")) {
								// They want to restart the prior benchmark, already in progress
								todos.add(new Tuple("switch", "restart"));

							} else {
								// We ignore the other options for now (they'll be processed below with their errors reported there)
							}
						}

						// If they specified any command line options, grab them
						for (i = 0; i < args.size(); i++)
						{
							line = args.get(i);
							if (line.toLowerCase().startsWith("-atom("))
							{	// It's an iterative atom count, at least it's supposed to be
								digits = Utils.extractOnlyNumbers(line.substring(6));
								list.clear();
								Xml.getNodeList(list, getScriptsXml(), "opbm.scriptdata.atoms.atom", false);
								if (!list.isEmpty())
								{
									wasFound = false;
									for (j = 0; j < list.size(); j++)
									{
										target	= list.get(j);
										name	= m_macroMaster.parseMacros(target.getAttribute("name"));
										if (name.replace(" ", "").equalsIgnoreCase(line.substring(6 + digits.length() + 2)))
										{	// This is the benchmark they want to run
											wasFound = true;
											todos.add(new Tuple("switch", "atom()", name, digits));
											break;
										}
									}
									if (!wasFound)
									{	// Display a message
										errorsWereReported = true;
										System.out.println("Unknown atom \"" + line.substring(6 + digits.length() + 2) + "\"");
										if (!isSilent)
											od = new OpbmDialog(m_opbm, true, "Unknown atom: " + line.substring(6 + digits.length() + 2), "Failure", OpbmDialog._OKAY_BUTTON, "cmdline", "");
									}

								} else {
									System.out.println("Fatal OPBM command line: Error loading scripts.xml to obtain list of Atoms");
									quit(-1);
								}

							} else if (line.toLowerCase().startsWith("-atom:")) {
								// It's an iterative atom count
								// Grab all of the atoms and iterate to find the name of the one we're after
								list.clear();
								Xml.getNodeList(list, getScriptsXml(), "opbm.scriptdata.atoms.atom", false);
								if (!list.isEmpty())
								{
									wasFound = false;
									for (j = 0; j < list.size(); j++)
									{
										target	= list.get(j);
										name	= m_macroMaster.parseMacros(target.getAttribute("name"));
										if (name.replace(" ", "").equalsIgnoreCase(line.substring(6)))
										{	// This is the benchmark they want to run
											wasFound = true;
											todos.add(new Tuple("switch", "atom", name));
											break;
										}
									}
									if (!wasFound)
									{	// Display a message
										errorsWereReported = true;
										System.out.println("Unknown atom: \"" + line.substring(6) + "\"");
										if (!isSilent)
											od = new OpbmDialog(m_opbm, true, "Unknown atom: " + line.substring(6), "Failure", OpbmDialog._OKAY_BUTTON, "cmdline", "");
									}

								} else {
									System.out.println("Fatal OPBM command line:  Error loading scripts.xml to obtain list of Atoms");
									quit(-1);
								}

							} else if (line.toLowerCase().startsWith("-molecule("))
							{	// It's an iterative molecule count, at least it's supposed to be
								digits = Utils.extractOnlyNumbers(line.substring(10));
								list.clear();
								Xml.getNodeList(list, getScriptsXml(), "opbm.scriptdata.molecules.molecule", false);
								if (!list.isEmpty())
								{
									wasFound = false;
									for (j = 0; j < list.size(); j++)
									{
										target	= list.get(j);
										name	= m_macroMaster.parseMacros(target.getAttribute("name"));
										if (name.replace(" ", "").equalsIgnoreCase(line.substring(10 + digits.length() + 2)))
										{	// This is the benchmark they want to run
											wasFound = true;
											todos.add(new Tuple("switch", "molecule()", name, digits));
											break;
										}
									}
									if (!wasFound)
									{	// Display a message
										errorsWereReported = true;
										System.out.println("Unknown molecule: \"" + line.substring(10 + digits.length() + 2) + "\"");
										if (!isSilent)
											od = new OpbmDialog(m_opbm, true, "Unknown molecule: " + line.substring(10 + digits.length() + 2), "Failure", OpbmDialog._OKAY_BUTTON, "cmdline", "");
									}

								} else {
									System.out.println("Fatal OPBM command line: Error loading scripts.xml to obtain list of molecules");
									quit(-1);
								}

							} else if (line.toLowerCase().startsWith("-molecule:")) {
								// It's an iterative molecule count
								// Grab all of the molecules and iterate to find the name of the one we're after
								list.clear();
								Xml.getNodeList(list, getScriptsXml(), "opbm.scriptdata.molecules.molecule", false);
								if (!list.isEmpty())
								{
									wasFound = false;
									for (j = 0; j < list.size(); j++)
									{
										target	= list.get(j);
										name	= m_macroMaster.parseMacros(target.getAttribute("name"));
										if (name.replace(" ", "").equalsIgnoreCase(line.substring(10)))
										{	// This is the benchmark they want to run
											wasFound = true;
											todos.add(new Tuple("switch", "molecule", name));
											break;
										}
									}
									if (!wasFound)
									{	// Display a message
										errorsWereReported = true;
										System.out.println("Unknown molecule: \"" + line.substring(10) + "\"");
										if (!isSilent)
											od = new OpbmDialog(m_opbm, true, "Unknown molecule: " + line.substring(10), "Failure", OpbmDialog._OKAY_BUTTON, "cmdline", "");
									}

								} else {
									System.out.println("Fatal OPBM command line:  Error loading scripts.xml to obtain list of molecules");
									quit(-1);
								}

							} else if (line.toLowerCase().startsWith("-scenario(")) {
								// It's an iterative scenario count, at least it's supposed to be
								digits = Utils.extractOnlyNumbers(line.substring(10));
								list.clear();
								Xml.getNodeList(list, getScriptsXml(), "opbm.scriptdata.scenarios.scenario", false);
								if (!list.isEmpty())
								{
									wasFound = false;
									for (j = 0; j < list.size(); j++)
									{
										target	= list.get(j);
										name	= m_macroMaster.parseMacros(target.getAttribute("name"));
										if (name.replace(" ", "").equalsIgnoreCase(line.substring(10 + digits.length() + 2)))
										{	// This is the benchmark they want to run
											wasFound = true;
											todos.add(new Tuple("switch", "scenario()", name, digits));
											break;
										}
									}
									if (!wasFound)
									{	// Display a message
										errorsWereReported = true;
										System.out.println("Unknown scenario: \"" + line.substring(10 + digits.length() + 2) + "\"");
										if (!isSilent)
											od = new OpbmDialog(m_opbm, true, "Unknown scenario: " + line.substring(10 + digits.length() + 2), "Failure", OpbmDialog._OKAY_BUTTON, "cmdline", "");
									}

								} else {
									System.out.println("Fatal OPBM command line: Error loading scripts.xml to obtain list of Scenarios");
									quit(-1);
								}

							} else if (line.toLowerCase().startsWith("-scenario:")) {
								// It's a scenario
								// Grab all of the scenarios and iterate to find the name of the one we're after
								list.clear();
								Xml.getNodeList(list, getScriptsXml(), "opbm.scriptdata.scenarios.scenario", false);
								if (!list.isEmpty())
								{
									wasFound = false;
									for (j = 0; j < list.size(); j++)
									{
										target	= list.get(j);
										name	= m_macroMaster.parseMacros(target.getAttribute("name"));
										if (name.replace(" ", "").equalsIgnoreCase(line.substring(10)))
										{	// This is the benchmark they want to run
											wasFound = true;
											todos.add(new Tuple("switch", "scenario", name));
											break;
										}
									}
									if (!wasFound)
									{	// Display a message
										errorsWereReported = true;
										System.out.println("Unknown scenario: \"" + line.substring(10) + "\"");
										if (!isSilent)
											od = new OpbmDialog(m_opbm, true, "Unknown scenario: " + line.substring(10), "Failure", OpbmDialog._OKAY_BUTTON, "cmdline", "");
									}

								} else {
									System.out.println("Fatal OPBM command line:  Error loading scripts.xml to obtain list of Scenarios");
									quit(-1);
								}

							} else if (line.toLowerCase().startsWith("-suite(")) {
								// It's an iterative scenario count, at least it's supposed to be
								digits = Utils.extractOnlyNumbers(line.substring(7));
								list.clear();
								Xml.getNodeList(list, getScriptsXml(), "opbm.scriptdata.suites.suite", false);
								if (!list.isEmpty())
								{
									wasFound = false;
									for (j = 0; j < list.size(); j++)
									{
										target	= list.get(j);
										name	= m_macroMaster.parseMacros(target.getAttribute("name"));
										if (name.replace(" ", "").equalsIgnoreCase(line.substring(7 + digits.length() + 2)))
										{	// This is the benchmark they want to run
											wasFound = true;
											todos.add(new Tuple("switch", "suite()", name, digits));
											break;
										}
									}
									if (!wasFound)
									{	// Display a message
										errorsWereReported = true;
										System.out.println("Unknown suite: \"" + line.substring(7 + digits.length() + 2) + "\"");
										if (!isSilent)
											od = new OpbmDialog(m_opbm, true, "Unknown suite: " + line.substring(7 + digits.length() + 2), "Failure", OpbmDialog._OKAY_BUTTON, "cmdline", "");
									}

								} else {
									System.out.println("Fatal OPBM command line: Error loading scripts.xml to obtain list of Suites");
									quit(-1);
								}

							} else if (line.toLowerCase().startsWith("-suite:")) {
								// It's a scenario
								// Grab all of the scenarios and iterate to find the name of the one we're after
								list.clear();
								Xml.getNodeList(list, getScriptsXml(), "opbm.scriptdata.suites.suite", false);
								if (!list.isEmpty())
								{
									wasFound = false;
									for (j = 0; j < list.size(); j++)
									{
										target	= list.get(j);
										name	= m_macroMaster.parseMacros(target.getAttribute("name"));
										if (name.replace(" ", "").equalsIgnoreCase(line.substring(7)))
										{	// This is the benchmark they want to run
											wasFound = true;
											todos.add(new Tuple("switch", "suite", name));
											break;
										}
									}
									if (!wasFound)
									{	// Display a message
										errorsWereReported = true;
										System.out.println("Unknown suite: \"" + line.substring(7) + "\"");
										if (!isSilent)
											od = new OpbmDialog(m_opbm, true, "Unknown suite: " + line.substring(7), "Failure", OpbmDialog._OKAY_BUTTON, "cmdline", "");
									}

								} else {
									System.out.println("Fatal OPBM command line:  Error loading scripts.xml to obtain list of Suites");
									quit(-1);
								}

							} else if (line.toLowerCase().equals("-trial")) {
								// They want to run a trial benchmark run
								todos.add(new Tuple("switch", "trial"));

							} else if (line.toLowerCase().equals("-official")) {
								// They want to run an official benchmark run
								todos.add(new Tuple("switch", "official"));

							} else if (line.toLowerCase().startsWith("-name:")) {
								// They are specifying a name for the run
								temp = line.substring(6);
								todos.add(new Tuple("switch", "name", temp));

							} else if (line.toLowerCase().startsWith("-noexit")) {
								// handled above in pre-this-loop processing
							} else if (line.toLowerCase().startsWith("-silent")) {
								// handled above in pre-this-loop processing
							} else if (line.toLowerCase().startsWith("-home:")) {
								// handled above in pre-this-loop processing
							} else if (line.toLowerCase().startsWith("-skin") || line.toLowerCase().startsWith("-simple")) {
								// handled above in pre-this-loop processing
							} else if (line.toLowerCase().startsWith("-developer")) {
								// handled above in pre-this-loop processing
							} else if (line.toLowerCase().startsWith("-restart")) {
								// handled above in pre-this-loop processing
							} else {
								// Ignore the unknown option
								errorsWereReported = true;
								System.out.println("Unknown option: \"" + line + "\"");
								if (!isSilent)
									od = new OpbmDialog(m_opbm, true, "Unknown command line option: " + line, "Failure", OpbmDialog._OKAY_BUTTON, "cmdline", "");
							}
						}

						// If there were failures reported in processing, exit
						if (errorsWereReported)
						{	// There were failures
							quit(-1);
						}

						// Process the parsed command line parameters in the order specified by the user
						runCount = 0;
						for (i = 0; i < todos.size(); i++)
						{	// Grab each item and process it in turn
							todo	= todos.get(i);
							group	= todo.getFirst(0);
							command	= (String)todo.getSecond(0);
							if (group.equalsIgnoreCase("switch"))
							{	// It's a command line switch read at the beginning, before the others
								if (command.equalsIgnoreCase("noexit"))
								{	// -noexit
									m_noExit = true;

								} else if (command.equalsIgnoreCase("silent")) {
									// -silent
									isSilent = true;

								} else if (command.equalsIgnoreCase("skin")) {
									// -skin or -simple
									showSimpleWindow();

								} else if (command.equalsIgnoreCase("developer")) {
									// -developer
									showDeveloperWindow();

								} else if (command.equalsIgnoreCase("jvmhome")) {
									// -home:c:\path\to\java\dot\exe\
									m_jvmHome = (String)todo.getThird(0);

								} else if (command.equalsIgnoreCase("restart")) {
									// -restart
									m_restartedManifest = true; //+rcp 12/19/2011
                                                                        m_benchmarkMaster.benchmarkManifestRestart();

								} else if (command.equalsIgnoreCase("atom()")) {
									// -atom(n):name
									m_executingFromCommandLine = true;
									name		= (String)todo.getThird(0);
									digits		= (String)todo.getFourth(0);
									iterations	= Integer.valueOf(digits);
									++runCount;
									System.out.println("Adding Atom \"" + name + "\" for " + digits + " iterations to compilation");
									bm.addToCompiledList("atom", name, iterations);

								} else if (command.equalsIgnoreCase("molecule()")) {
									// -molecule(n):name
									m_executingFromCommandLine = true;
									name		= (String)todo.getThird(0);
									digits		= (String)todo.getFourth(0);
									iterations	= Integer.valueOf(digits);
									++runCount;
									System.out.println("Adding Molecule \"" + name + "\" for " + digits + " iterations to compilation");
									bm.addToCompiledList("molecule", name, iterations);

								} else if (command.equalsIgnoreCase("scenario()")) {
									// -scenario(n):name
									m_executingFromCommandLine = true;
									name		= (String)todo.getThird(0);
									digits		= (String)todo.getFourth(0);
									iterations	= Integer.valueOf(digits);
									++runCount;
									System.out.println("Adding Scenario \"" + name + "\" for " + digits + " iterations to compilation");
									bm.addToCompiledList("scenario", name, iterations);

								} else if (command.equalsIgnoreCase("suite()")) {
									// -suite(n):name
									m_executingFromCommandLine = true;
									name		= (String)todo.getThird(0);
									digits		= (String)todo.getFourth(0);
									iterations	= Integer.valueOf(digits);
									++runCount;
									System.out.println("Adding Suite \"" + name + "\" for " + digits + " iterations to compilation");
									bm.addToCompiledList("suite", name, iterations);

								} else if (command.equalsIgnoreCase("atom")) {
									// -atom:name
									m_executingFromCommandLine = true;
									name = (String)todo.getThird(0);
									++runCount;
									System.out.println("Adding Atom \"" + name + "\" to compilation");
									bm.addToCompiledList("atom", name, 1);

								} else if (command.equalsIgnoreCase("molecule")) {
									// -molecule:name
									m_executingFromCommandLine = true;
									name = (String)todo.getThird(0);
									++runCount;
									System.out.println("Adding Molecule \"" + name + "\" to compilation");
									bm.addToCompiledList("molecule", name, 1);

								} else if (command.equalsIgnoreCase("scenario")) {
									// -scenario:name
									m_executingFromCommandLine = true;
									name = (String)todo.getThird(0);
									++runCount;
									System.out.println("Adding Scenario \"" + name + "\" to compilation");
									bm.addToCompiledList("scenario", name, 1);

								} else if (command.equalsIgnoreCase("suite")) {
									// -suite:name
									m_executingFromCommandLine = true;
									name = (String)todo.getThird(0);
									++runCount;
									System.out.println("Adding Suite \"" + name + "\" to compilation");
									bm.addToCompiledList("suite", name, 1);

								} else if (command.equalsIgnoreCase("trial")) {
									// -trial
									m_executingFromCommandLine = true;
									System.out.println("Beginning Trial Run");
									++runCount;
									trialRun(true);

								} else if (command.equalsIgnoreCase("official")) {
									// -official
									m_executingFromCommandLine = true;
									System.out.println("Beginning Official Run");
									++runCount;
									officialRun(true);

								} else if (command.equalsIgnoreCase("name")) {
									// -name:someName
									m_executingBenchmarkRunName = (String)todo.getThird(0);
									System.out.println("Benchmark given name: '" + m_executingBenchmarkRunName + "'");

								} else {
									System.out.println("Internal Opbm error: Command line switch was found with an unknown command: " + command);
									quit(-1);

								}
							}
						}
						// When we get here, everything they specified on the command line has been processed

						// They may have added items to the benchmark compilaiton to execute
						if (!bm.isCompilationEmpty())
						{	// There is a compilation, try to run it
							if (bm.buildCompilation())
								bm.run();
							else
								System.out.println("Fatal Error: Unable to run the prepared compilation.");
						}

						//if (!m_noExit && (runCount != 0 || (isSilent && errorsWereReported))) //-rcp 12/18/2011
                                                if (!m_noExit && (runCount != 0 || (isSilent && errorsWereReported) || (m_restartedManifest && m_executingFromCommandLine))) //+rcp 12/18/2011
						{	// If we get here, they want us to exit, or we did a run without any errors and we're ready to exit, or we had errors with a -silent switch
							quit(errorsWereReported ? -1 : 0);
						}
						// Finished with command-line things
						m_executingFromCommandLine = false;
					}
				};
				t.start();
            }
        });
	}

	public void trialRun(boolean fromCommandLine)
	{	// Execute a trial run benchmark
		m_benchmarkMaster.benchmarkTrialRun(fromCommandLine);
	}

	public void officialRun(boolean fromCommandLine)
	{	// Execute an official run benchmark
		m_benchmarkMaster.benchmarkOfficialRun(fromCommandLine);
	}

	public static void readTerminatedLinesFromFile(String			fileName,
												   List<String>		args)
	{
		File f;
		FileInputStream fi;
		String line;

		try {
			f  = new File(fileName);
			if (!f.exists())
			{	// It doesn't exist in the current directory, see if it exists in tests\
				f = new File("tests\\" + fileName);
				if (!f.exists())
				{	// Nope, unable to find this file
					System.out.println("OPBM command line:  Cannot find \"" + fileName + "\", or \"tests\\" + fileName + "\"");
					return;
				}
			}
			fi = new FileInputStream(f);
			BufferedReader d = new BufferedReader(new InputStreamReader(fi));

			// Read each line in turn
			line = d.readLine();
			while (line != null && !line.isEmpty())
			{
				args.add(line);
				line = d.readLine();
			}
			d.close();

		} catch (FileNotFoundException ex) {
		} catch (IOException ex) {
		}
	}

	public void setResultsViewerFilename(String resultsXmlFilename)
	{
		m_rvFilename = resultsXmlFilename;
	}

	public String getResultsViewerFilename()
	{
		if (m_rvFilename == null)
			return("");
		else
			return(m_rvFilename);
	}

	/**
	 * Creates the Results Viewer
	 */
	public ResultsViewer createAndShowResultsViewer(String resultsXmlFilename)
	{
		int count;
		m_rv = null;

		m_rvsync = 0;
		if (!resultsXmlFilename.isEmpty() && !m_opbm.willTerminateAfterRun())
		{	// We only process real files
			m_rvFilename = resultsXmlFilename;

			// Launch the Results Viewer in another thread (keeps GUI running)
			++m_rvsync;		// Raise the condition of this sync point's use
			Thread t = new Thread("results_viewer_loader")
			{
				@Override
				public void run()
				{
					m_rv = new ResultsViewer(m_opbm, true);
					--m_rvsync;

// REMEMBER the new "filter," tag from the scripts will create a compiled list of entries that are dynamic and based on scripts
// REMEMBER this hard-coded list needs to be removed
					// Add the filter tags
					m_rv.addFilterTag("Sample1",	"No");
					m_rv.addFilterTag("Sample2",	"No");

					// Attempt to create the viewer window, load the Xml file they specified, and render everything
					if (m_rv.load(m_rvFilename))
						m_rv.render();
				}
			};
			t.start();
		}
		count = 0;
		while (count < 50/* 50*200 = 10 seconds */ && m_rvsync != 0)
		{
			try
			{	// Wait for m_rv to be created and run() to to notify m_rvsync
			   Thread.sleep(200);

			} catch (InterruptedException ex) {
			}
		   ++count;
		}
		return(m_rv);
	}

	/**
	 * Prompts the user for the CV value that should appear in red, and then
	 * updates the value after the fact
	 */
	public void setResultsViewerCV()
	{
		OpbmInput.simpleInput(m_opbm, "Specify Value", "CV Percentage (should be \"3\" for 3%):", Utils.removeLeadingZeros(Utils.doubleToString(m_settingsMaster.getCVInRed() * 100.0, 3, 0)), "results_viewer_cv", "save_results_viewer_cv", true, true);
	}

	/**
	 * Creates the developer window to allow editing of scripts.xml and other
	 * xml files
	 */
	public void createDeveloperWindow()
	{
		m_frameDeveloper = new DeveloperWindow(this, false);
	}

	public void toggleDeveloperWindow()
	{
		if (m_frameDeveloper != null && m_frameDeveloper.isVisible())
		{	// Turn it off
			m_frameDeveloper.setVisible(false);

		} else {
			// Turn it on
			m_frameDeveloper.setVisible(true);

		}
	}

	public void showDeveloperWindow()
	{
		// Show the developer window
		if (m_frameDeveloper != null && !m_frameDeveloper.isVisible())
		{	// Turn it on
			m_frameDeveloper.setVisible(true);
		}

		// Hide the simple window
		if (m_frameSimple != null && m_frameSimple.isVisible())
		{	// Turn it off
			m_frameSimple.setVisible(false);
		}
	}

	public void hideDeveloperWindow()
	{
		// Show the developer window
		if (m_frameDeveloper != null && m_frameDeveloper.isVisible())
		{	// Turn it off
			m_frameDeveloper.setVisible(false);
		}
	}

	/**
	 * Creates the simple skinned interface, which allows for "Trial Run" and
	 * "Official Run", along with links to view previous entries
	 */
	public void createSimpleWindow()
	{
		m_frameSimple = new SimpleWindow(this, false);
	}

	public void toggleSimpleWindow()
	{
		if (m_frameSimple != null && m_frameSimple.isVisible())
		{	// Turn it off
			m_frameSimple.setVisible(false);

		} else {
			// Turn it on
			m_frameSimple.setVisible(true);

		}
	}

	public void showSimpleWindow()
	{
		// Hide the developer window
		if (m_frameDeveloper != null && m_frameDeveloper.isVisible())
		{	// Turn it off
			m_frameDeveloper.setVisible(false);
		}

		// Show the simple window
		if (m_frameSimple != null && !m_frameSimple.isVisible())
		{	// Turn it on
			m_frameSimple.setVisible(true);
		}
	}

	public void hideSimpleWindow()
	{
		// Hide the simple window
		if (m_frameSimple != null && m_frameSimple.isVisible())
		{	// Turn it off
			m_frameSimple.setVisible(false);
		}
	}

	public void showUserWindow()
	{
		// See which one should be visible
		if (m_frameSimple != null && m_settingsMaster.isSimpleSkin())
		{	// We're viewing the simple skin
			m_frameSimple.setVisible(true);

		}

		if (m_frameDeveloper != null && m_settingsMaster.isDeveloperSkin())
		{	// Viewing the developer window
			m_frameDeveloper.setVisible(true);
		}
	}

	public void hideUserWindow()
	{
		// See which one is visible
		if (m_frameSimple != null && m_settingsMaster.isSimpleSkin())
		{	// We're viewing the simple skin
			m_frameSimple.setVisible(false);

		}

		if (m_frameDeveloper != null && m_settingsMaster.isDeveloperSkin())
		{	// We're viewing the developer window
			m_frameDeveloper.setVisible(false);
		}
	}

	/** Self-explanatory.  Builds the GUI for OPBM using a four-panel design:
	 * 1)  Header
	 * 2)  Left panel for navigation
	 * 3)  Right panel for display and editing of controls
	 * 4)  Status bar for displaying tooltips and general information
	 *
	 */
    public void createAndShowGUI()
	{
		createDeveloperWindow();		// For "developer" skin setting in settings.xml
		createSimpleWindow();			// For "simple" skin setting in settings.xml
		showUserWindow();				// Display whichever one should be displayed

		// Load the XML panel content
		if (loadPanelsXml()) {
			// Once the Xml panel content is loaded, create all of the physical panels based on its instruction
			System.out.println("Loaded panels.xml");
			if (PanelFactory.createLeftPanelObjects(this, m_macroMaster, m_frameDeveloper.lblHeader, m_frameDeveloper.statusBar, m_frameDeveloper.panLeft, m_frameDeveloper)) {
				// All default panels are created, render the top-level item
				System.out.println("Created menus and navigation panels");
				if (navigateToLeftPanel("main")) {
					// If we get here, the main navigation panel is displayed and we're still good
					System.out.println("Found main panel");
					if (loadEditsXml()) {
						// We have our edits loaded, we're still good
						System.out.println("Loaded edits.xml");
						if (loadScriptsXml()) {
							// We have our scripts loaded, we're totally good
							System.out.println("Loaded scripts.xml");
							System.out.println("OPBM System Initialization completed successfully");
							// Normal system flow should reach this point
							m_frameDeveloper.statusBar.setText("Loaded panels.xml, edits.xml and scrips.xml okay.");

						} else {
							// Not found or not loaded properly, navigate to the raw editing options
							System.out.println("Unable to load scripts.xml");
							m_frameDeveloper.statusBar.setText("Error loading scripts.xml.  Please repair file manually. " + m_frameDeveloper.m_lastError);
							navigateToLeftPanel("XML File Maintenance");

						}

					} else {
						// Not found or not loaded properly, navigate to the raw editing options
						System.out.println("Unable to load edits.xml");
						m_frameDeveloper.statusBar.setText("Error loading edits.xml.  Please repair file manually. " + m_frameDeveloper.m_lastError);
						navigateToLeftPanel("XML File Maintenance");

					}

				} else {
					// If we get here, the "main" panel wasn'tup found
					System.out.println("Could not find main panel in panels.xml");
					// Display our default panel, which indicates the error condition
					m_frameDeveloper.panLeft.setVisible(true);
				}

			} else {
				// If we get here, the "main" panel wasn't found
				// Display our default panel, which indicates the error condition
				System.out.println("Unable to create main menus and navigation panels");
				m_frameDeveloper.panLeft.setVisible(true);
			}

		} else {
			// If we get here, the "main" panel wasn'tup found
			System.out.println("Unable to load panels.xml");
			// Display our default panel, which indicates the error condition
			m_frameDeveloper.statusBar.setText("Error loading panels.xml.  Please exit application and repair file manually. " + m_frameDeveloper.m_lastError);
			m_frameDeveloper.panLeft.setVisible(true);
		}
    }

	/** Called to navigate to a panel by name.  Appends new navigation to an
	 * internal navigation history for traversing backwards using successive
	 * "Back" commands.
	 *
	 * @param name name of panel to navigate to
	 * @return true of false if navigation was successful
	 */
	public boolean navigateToLeftPanel(String name)
	{
		PanelLeft p;
		int i, j;

		for (i = 0; i < m_leftPanels.size(); i++) {
			p = m_leftPanels.get(i);
			if (p.getName().equalsIgnoreCase(name)) {
				// This is the one, navigate here
				// Make the new one visible before making the old one invisible (so there's no flicker)
				for (j = 0; j < m_leftPanels.size(); j++) {
					if (j != i) {
						// Make this one invisible
						m_leftPanels.get(j).navigateAway();
					}
				}
				m_navHistory.add(p);
				p.setVisible(true);
				p.navigateTo(m_macroMaster);
				return(true);
			}
		}
		return(false);
	}

	/**
	 * When the left panel (menu) is updated, by toggling a macro or clicking
	 * on some link that will change the display, we need to rebuild it.
	 */
	public void refreshLeftPanelsAfterMacroUpdate()
	{
		int i;

		for (i = 0; i < m_leftPanels.size(); i++)
		{	// Update every menu in turn, so when they are redisplayed, they are updated
			m_leftPanels.get(i).refreshAfterMacroUpdate();
		}
	}

	/** Navigate backward in the chain of navigated panels.
	 *
	 * @return true or false if navigation backward was possible
	 */
	public boolean navigateBack()
	{
		PanelLeft p;
		int i;

		if (m_navHistory.isEmpty() || m_navHistory.size() == 1) {
			// Nothing to navigate back to
			return(false);
		}
		// Remove the last one
		m_navHistory.remove(m_navHistory.size() - 1);
		p = m_navHistory.get(m_navHistory.size() - 1);

		// Make sure everything else is hidden
		for (i = 0; i < m_leftPanels.size(); i++) {
			if (!m_leftPanels.get(i).equals(p)) {
				m_leftPanels.get(i).navigateAway();
			}
		}
		// Make the last one visible
		p.navigateTo(m_macroMaster);
		return(true);
	}

	/** Adds the specified <code>RightPanel</code> edit object to the list of
	 * known <code>RightPanel</code>s that have been created.
	 *
	 * @param pr <code>RightPanel</code> object to add
	 */
	public void addEditToRightPanelList(PanelRight pr) {
		m_editPanels.add(pr);
	}

	/** Adds the specified <code>RightPanel</code> rawedit object to the list of
	 * known <code>RightPanel</code>s that have been created.
	 *
	 * @param pr <code>RightPanel</code> object to add
	 */
	public void addRawEditToRightPanelList(PanelRight pr)
	{
		m_rawEditPanels.add(pr);
	}

	/** Called to navigate away from a named edit rightpanel.  Used to sync
	 * menu navigation with rightpanel objects associated with the menu's
	 * action, such as "Save and Close" or "Close".
	 *
	 * @param name rightpanel name to close
	 * @param destroy true or false should this rightpanel be destroyed?
	 * @return true or false if object was found and removed
	 */
	public boolean navigateAwayEdit(String name, boolean destroy)
	{
		PanelRight pr;
		int i;

		for (i = 0; i < m_editPanels.size(); i++) {
			pr = m_editPanels.get(i);
			if (pr.getName().equalsIgnoreCase(name)) {
				// This is the match
				pr.doPostCommand();
				pr.navigateAway();
				if (destroy) {
					m_editPanels.remove(i);
					m_editActive = null;
				}
				m_frameDeveloper.panRight.setVisible(true);
				return(true);
			}
		}
		return(false);
	}

	/**
	 * Called to save the contents of an edit, which saves everything on the
	 * specified edit physically to disk.
	 */
	public void editSave()
	{
		if (m_editActive != null)
			m_editActive.save(false);
	}

	/**
	 * Closes the edit without saving.
	 */
	public void editClose()
	{
		if (m_editActive != null) {
			navigateAwayEdit(m_editActive.getName(), true);
		}
	}

	/** Called to navigate away from a named rawedit rightpanel.  Used to sync
	 * menu navigation with rightpanel objects associated with the menu's
	 * action, such as "Save and Close" or "Close".
	 *
	 * @param name rightpanel name to close
	 * @param destroy true or false should this rightpanel be destroyed?
	 * @return true or false if object was found and removed
	 */
	public boolean navigateAwayRawEdit(String name, boolean destroy)
	{
		PanelRight pr;
		int i;

		for (i = 0; i < m_rawEditPanels.size(); i++) {
			pr = m_rawEditPanels.get(i);
			if (pr.getName().equalsIgnoreCase(name)) {
				// This is the match, do its closing command, and then navigate away
				pr.doPostCommand();
				pr.navigateAway();
				if (destroy) {
					m_rawEditPanels.remove(i);
					m_rawEditActive = null;
				}
				m_frameDeveloper.panRight.setVisible(true);
				return(true);
			}
		}
		return(false);
	}

	/** Identifies the active edit object as the current one being displayed.
	 * Used to allow the macro $active_edit$ to return a valid name.
	 *
	 * @param name name of <code>RightPanel</code> edit to activate
	 * @return true or false if object was found and set active
	 */
	public boolean navigateToEdit(String name)
	{
		PanelRight pr;
		int i;

		for (i = 0; i < m_editPanels.size(); i++) {
			pr = m_editPanels.get(i);
			if (pr.getName().equalsIgnoreCase(name)) {
				m_frameDeveloper.panRight.setVisible(false);
				pr.setVisible(true);
				pr.navigateTo();
				m_editActive = pr;
				return(true);
			}
		}
		return(false);
	}

	/** Identifies the active rawedit object as the current one being displayed.
	 * Used to allow the macro $active_rawedit$ to return a valid name.
	 *
	 * @param name name of <code>RightPanel</code> rawedit to activate
	 * @return true or false if object was found and set active
	 */
	public boolean navigateToRawEdit(String name)
	{
		PanelRight pr;
		int i;

		for (i = 0; i < m_rawEditPanels.size(); i++) {
			pr = m_rawEditPanels.get(i);
			if (pr.getName().equalsIgnoreCase(name)) {
				m_frameDeveloper.panRight.setVisible(false);
				pr.setVisible(true);
				pr.navigateTo();
				m_rawEditActive = pr;
				return(true);
			}
		}
		return(false);
	}

	/**
	 * Called to save the contents of a rawedit physically to disk.
	 */
	public void rawEditSave()
	{
		if (m_rawEditActive != null)
			m_rawEditActive.save(true);
	}

	/**
	 * Saves the contents of the rawedit physically to disk, then navigates away
	 * from the panel, closing it.
	 */
	public void rawEditSaveAndClose()
	{
		if (m_rawEditActive != null) {
			m_rawEditActive.save(true);
			navigateAwayRawEdit(m_rawEditActive.getName(), true);
		}
	}

	/**
	 * Closes the rawedit without saving.
	 */
	public void rawEditClose()
	{
		if (m_rawEditActive != null) {
			navigateAwayRawEdit(m_rawEditActive.getName(), true);
		}
	}


	/** Navigate backward in the chain to top-most panel (first menu).
	 *
	 * @return true or false if navigation home was possible
	 */
	public boolean navigateHome()
	{
		int i;

		if (m_navHistory.isEmpty()) {
			// Nothing to navigate to
			return(false);
		}

		// Remove all but the home one
		for (i = m_navHistory.size() - 1; i > 0; i--)
			m_navHistory.remove(i);

		// Make the home one visible
		m_navHistory.get(0).navigateTo(m_macroMaster);
		return(true);
	}

	/** Called to obtain the name of the panel immediately previous to the
	 * current one.  Used for building contextual tooltip texts based on
	 * "wherever we are".
	 *
	 * @return Name of the immediately previous panel
	 */
	public String previousPanel()
	{
		if (m_navHistory.size() <= 1) {
			// Nothing to navigate back to before this one, so no valid name
			return("previous");
		}
		return(m_navHistory.get(m_navHistory.size() - 2).getName());
	}

	/** Called to obtain the name of the panel immediately at the start of
	 * the navigation list.  Used for building contextual tooltip texts based
	 * on "going to home from wherever we are".
	 *
	 * @return Name of home panel
	 */
	public String homePanel()
	{
		return(m_navHistory.get(0).getName());
	}

	/**
	 * Loads the panels.xml file as a W3C DOM object, converts it to a
	 * logically structured link-list of Xml class objects which are far
	 * easier to navigate generally.
	 *
	 * @return true of false if the panels.xml file was loaded
	 */
	private boolean loadPanelsXml()
	{
		m_panelXml = loadXml("panels.xml", this);
		return(m_panelXml != null);
	}

	/**
	 * Loads the edits.xml file as a W3C DOM object, converts it to a
	 * logically structured link-list of Xml class objects which are far
	 * easier to navigate generally.
	 *
	 * @return true of false if the edits.xml file was loaded
	 */
	private boolean loadEditsXml()
	{
		m_editXml = loadXml("edits.xml", this);
		return(m_editXml != null);
	}

	/**
	 * Loads the scripts.xml file as a W3C DOM object, converts it to a
	 * logically structured link-list of Xml class objects which are far
	 * easier to navigate generally.
	 *
	 * @return true of false if the scripts.xml file was loaded
	 */
	private boolean loadScriptsXml()
	{
		int deletedCount;
		String scriptsXmlFilename;

		scriptsXmlFilename = Opbm.locateFile("scripts.xml");
		m_scriptsXml = loadXml(scriptsXmlFilename, this);
		if (m_scriptsXml != null)
		{	// When scripts.xml is loaded, remove all atomuuids which may have existed previously from an impolite exit (or debugging by the developer)
			deletedCount = m_scriptsXml.deleteAllAttributesWithThisName("atomuuid");
			if (deletedCount != 0)
				m_scriptsXml.saveNode(scriptsXmlFilename);
		}
		return(m_scriptsXml != null);
	}

	/**
	 * Physically loads the specified XML file as a W3C DOM object, converts
	 * it to a logically structured link-list of Xml class objects which are far
	 * easier to navigate generally.
	 *
	 * @return valid or null if the file was loaded
	 */
	public static Xml loadXml(String	fileName,
							  Opbm		opbm)
	{
		Xml root;
		File panelsXmlFile;
		String fileToProcess;
		DocumentBuilderFactory factory;
		DocumentBuilder bldPanelsXml;
		Document docPanelsXml;

		root			= null;
		// Remove any double-quotes from the filename, often used around "c:\paths that contain\spaces\file.xml"
		fileName		= fileName.replace("\"", "");
		// If the filename contains a \ or / character, we assume it's a fully qualified path, otherwise we search our path for it
		panelsXmlFile	= new File(fileName.contains("\\") || fileName.contains("/") ? fileName : locateFile(fileName));
		fileToProcess	= panelsXmlFile.getAbsolutePath();
		try {
			factory			= DocumentBuilderFactory.newInstance();
			factory.setIgnoringComments(true);
			factory.setIgnoringElementContentWhitespace(true);
			bldPanelsXml	= factory.newDocumentBuilder();
			docPanelsXml	= bldPanelsXml.parse(fileToProcess);

			// Convert Java's w3c dom model to something more straight-forward and usable
			root			= Xml.processW3cNodesIntoXml(null, docPanelsXml.getChildNodes(), opbm);

		} catch (ParserConfigurationException ex) {
			m_lastLoadXmlError = ex.getMessage();
		} catch (SAXException ex) {
			m_lastLoadXmlError = ex.getMessage();
		} catch (IOException ex) {
			m_lastLoadXmlError = ex.getMessage();
		}
		return(root);	// success if root was defined
	}

	/**
	 * Attempts to locate the specified filename.  Files are located in one
	 * of a few places for this app.
	 *
	 * @param fileName file being searched for
	 * @return path of file if found, or original filename if not found
	 */
	public static String locateFile(String fileName)
	{
		File f1, f2, f3, f4, f5, f6, f7, f8, f9, f10, f11, f12, f13;
		String path;

		// Remove any quotes before looking
		fileName = fileName.replace("\"", "");

		f1	= new File("./"								+ fileName);
		f2	= new File("./resources/"					+ fileName);
		f3	= new File("./src/resources/"				+ fileName);
		f4	= new File(getSettingsDirectory()			+ fileName);
		f5	= new File("./resources/xmls/"				+ fileName);
		f6	= new File("./src/resources/xmls/"			+ fileName);
		f7	= new File("./resources/backgrounds/"		+ fileName);
		f8	= new File("./src/resources/backgrounds/"	+ fileName);
		f9	= new File("./resources/graphics/"			+ fileName);
		f10	= new File("./src/resources/graphics/"		+ fileName);
		f11	= new File("./resources/masks/"				+ fileName);
		f12	= new File("./src/resources/masks/"			+ fileName);
		f13	= new File("./src/resources/masks/simple/"	+ fileName);

		do {
			if (f1.exists()) {
				path = f1.getAbsolutePath();
				break;
			}
			if (f2.exists()) {
				path = f2.getAbsolutePath();
				break;
			}
			if (f3.exists()) {
				path = f3.getAbsolutePath();
				break;
			}
			if (f4.exists()) {
				path = f4.getAbsolutePath();
				break;
			}
			if (f5.exists()) {
				path = f5.getAbsolutePath();
				break;
			}
			if (f6.exists()) {
				path = f6.getAbsolutePath();
				break;
			}
			if (f7.exists()) {
				path = f7.getAbsolutePath();
				break;
			}
			if (f8.exists()) {
				path = f8.getAbsolutePath();
				break;
			}
			if (f9.exists()) {
				path = f9.getAbsolutePath();
				break;
			}
			if (f10.exists()) {
				path = f10.getAbsolutePath();
				break;
			}
			if (f11.exists()) {
				path = f11.getAbsolutePath();
				break;
			}
			if (f12.exists()) {
				path = f12.getAbsolutePath();
				break;
			}
			if (f13.exists()) {
				path = f13.getAbsolutePath();
				break;
			}
			// We didn't find the file.
			// Default to the regular filename (for error reporting)
			path = fileName;
			break;
		} while (false);
		// Regardless of whether it was found or not, return the result
		return(path);
	}

	/**
	 * Called to edit data in the scripts.xml file.  Lists all options at the
	 * specified level (by name), and presents an edit screen as defined by the
	 * loaded edits from edits.xml.
	 *
	 * @param name name of opbm.scriptdata.* to list and edit
	 * @return true of false if edit was found in edits.xml
	 */
	public boolean edit(String name)
	{
		PanelRight panel = PanelFactory.createRightPanelFromEdit(name, this, m_macroMaster, m_commandMaster, m_frameDeveloper.lblHeader, m_frameDeveloper.statusBar, m_frameDeveloper.panRight, m_frameDeveloper, "", "");
		if (panel == null) {
			m_frameDeveloper.statusBar.setText("Error: Unable to edit " + name + ".");
			return(false);

		} else {
			// It was found and created
			addEditToRightPanelList(panel);
			panel.doPreCommand();
			navigateToEdit(panel.getName());

		}
		return(true);
	}

	/**
	 * Called to raw edit the file specified.  Presents the user with a full
	 * page editbox of its contents.
	 *
	 * @param fileName name of file to edit
	 * @return true of false if file was found
	 */
	public boolean rawedit(String fileName)
	{
		PanelRight panel = PanelFactory.createRightPanelFromRawEdit(fileName, this, m_macroMaster, m_commandMaster, m_frameDeveloper.lblHeader, m_frameDeveloper.statusBar, m_frameDeveloper.panRight, m_frameDeveloper);
		if (panel == null) {
			m_frameDeveloper.statusBar.setText("Error: Unable to load " + fileName + " for editing.");
			return(false);

		} else {
			// It was found and created
			addRawEditToRightPanelList(panel);

			// Populate its contents
			panel.load(true);
			panel.doPreCommand();
			navigateToRawEdit(panel.getName());

		}
		return(true);
	}

	/** Called to resize everything when the user resizes the window.
	 *
	 */
	public void resizeEverything()
	{
		int i;

		m_frameDeveloper.resizeEverything();

		// Resize the navigation panels
		for (i = 0; i < m_leftPanels.size(); i++) {
			m_leftPanels.get(i).afterWindowResize(m_frameDeveloper.panRight.getX() - 1,
												  m_frameDeveloper.panRight.getHeight());
		}

		// Resize the active edits (if any)
		if (m_rawEditActive != null) {
			m_rawEditActive.afterWindowResize(m_frameDeveloper.panRight.getWidth(),
											  m_frameDeveloper.panRight.getHeight());
		}

		if (m_editActive != null) {
			m_editActive.afterWindowResize(m_frameDeveloper.panRight.getWidth(),
										   m_frameDeveloper.panRight.getHeight());
		}
		m_frameDeveloper.repaint();
	}

	/** Called when the scrollbar position moves.
	 *
	 * @param ae system adjustment event
	 */
	@Override
	public void adjustmentValueChanged(AdjustmentEvent ae) {
		// Called when scrollbar scrolls
	}

	/** Not used.  Required for use of ComponentListener.
	 *
	 */
	@Override
	public void componentHidden(ComponentEvent evt) {
	}
	/** Not used.  Required for use of ComponentListener.
	 *
	 */
	@Override
	public void componentShown(ComponentEvent evt) {
	}
	/** Not used.  Required for use of ComponentListener.
	 *
	 */
	@Override
	public void componentMoved(ComponentEvent evt) {
	}

	/** Called when the base newFrame is resized.  Calls
	 * <code>resizeEverything()</code>
	 *
	 */
	@Override
	public void componentResized(ComponentEvent evt)
	{
	// Called when the newFrame (window) is resized
		if (evt.getComponent().equals(m_frameDeveloper))
		{	// Resize everything on the developer window
			m_frameDeveloper.componentResized(evt);
			resizeEverything();
		}
	}

	/** Not used.  Required definition for KeyListener.
	 *
	 * @param e system key event
	 */
	@Override
	public void keyTyped(KeyEvent e) {
    }

	/** Not currently used.  Required definition for KeyListener.
	 *
	 * @param e system key event
	 */
	@Override
    public void keyPressed(KeyEvent e) {
    }

	/** Not used.  Required definition for KeyListener.
	 *
	 * @param e system key event
	 */
	@Override
    public void keyReleased(KeyEvent e) {
    }

	/** Called when the user wheels over the newFrame.
	 *
	 * @param e system mouse wheel event
	 */
	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
	// For mouse-wheel events
		if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
//			int newPosition = scrollbarV.getValue() + e.getUnitsToScroll() * 16;
			if (e.getUnitsToScroll() < 0) {
				// Moving down (toward 0)
//				scrollbarV.setValue(Math.max(newPosition, scrollbarV.getMinimum()));
			}
			else {
				// Moving up (toward largest value)
//				scrollbarV.setValue(Math.min(newPosition, scrollbarV.getMaximum()));

			}
		}
	}

	public void listBoxAddCommand()
	{
		if (m_editActive != null)
			m_editActive.listBoxAddCommand();
	}

	public void listBoxDeleteCommand()
	{
		if (m_editActive != null)
			m_editActive.listBoxDeleteCommand();
	}

	public void listBoxCloneCommand()
	{
		if (m_editActive != null)
			m_editActive.listBoxCloneCommand();
	}

	public void listBoxCommand(String				command,
							   PanelRightListbox	source)
	{
		if (m_editActive != null)
			m_editActive.listboxCommand(command, source);
	}

	/**
	 * Called when the user clicks on the add button on a
	 * <code>_TYPE_LOOKUPBOX</code>
	 *
	 * @param whereFrom name of the lookupbox control being added from
	 * @param whereTo name of the listbox or lookupbox being added to
	 */
	public void lookupboxAddCommand(PanelRightLookupbox source,
									String				whereTo,
									String				after,
									String				whereFrom)
	{
		if (m_editActive != null)
			m_editActive.lookupboxAddCommand(source, whereTo, after, whereFrom);
	}

	public void lookupboxCloneCommand(PanelRightLookupbox source)
	{
		if (m_editActive != null)
			m_editActive.lookupboxCloneCommand(source);
	}

	public void lookupboxCommand(String					command,
								 PanelRightLookupbox	source)
	{
		if (m_editActive != null)
			m_editActive.lookupboxCommand(command, source);
	}

	public void lookupboxZoomCommand(PanelRightLookupbox	source,
									 String					editName,
									 String					zoomFields,
									 String					dataSource)
	{
		if (m_editActive != null)
			m_editActive.lookupboxZoomCommand(source, editName, zoomFields, dataSource);
	}

	public Xml getListboxOrLookupboxSelectedItem(PanelRightLookupbox source)
	{
		if (m_editActive != null)
			return(m_editActive.getLookupboxSelectedItemByObject(source));
		else
			return(null);
	}

	/**
	 * Called after a zoom window is created to input some custom data.  Saves
	 * the input.
	 * @param uuid uuid from when the zoom window was created
	 */
	public void saveCustom(String uuid)
	{
		Tuple t;

		t = findTuple(uuid);
		if (t != null)
		{	// If this is a custom command, call it
			if (m_editActive != null)
				m_editActive.saveCustomInputCommand(t);
		}
	}

	/**
	 * Called after a zoom window is created to input some custom data.  Cancels
	 * the input.
	 * @param uuid uuid from when the zoom window was created
	 */
	public void cancelCustom(String uuid)
	{
		Tuple t;

		t = findTuple(uuid);
		if (t != null)
		{	// If this is a custom command, call it
			if (m_editActive != null)
				m_editActive.cancelCustomInputCommand(t);
		}
	}

	/**
	 * Search for the specified named lookupbox, and tell it to update itself.
	 * @param name
	 */
	public void lookupboxUpdateCommand(String name)
	{
		if (m_editActive != null)
		{
			m_editActive.lookupboxUpdateCommand(name);
		}
	}

	/**
	 * Search for the specified named compilation, and tell it to execute the
	 * in command on its linked object
	 * @param name
	 */
	public void compilationInCommand(String		compilationName,
									 String		dataSourceName)
	{
		if (m_editActive != null)
		{
			m_editActive.compilationInCommand(compilationName, dataSourceName);
		}
	}

	/**
	 * Creates a managed window relative to the <code>PanelRight</code> panel
	 * for size, color, etc.
	 *
	 * @param panelRef <code>PanelRight</code> with which to copy attributes from
	 * @param name window title
	 * @return new <code>DroppableFrame</code>
	 */
	public DroppableFrame createZoomWindow(PanelRight	panelRef,
										   String		name)
	{
		int actualWidth, actualHeight;
		Dimension size;

		DroppableFrame fr = new DroppableFrame(this, true, false);
		fr.setTitle("Zoom: " + name);
        fr.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		fr.setSize(panelRef.getWidth(), panelRef.getHeight());

		fr.pack();
        Insets fi = fr.getInsets();
		actualWidth		= panelRef.getWidth()  + fi.left + fi.right;
		actualHeight	= panelRef.getHeight() + fi.top  + fi.bottom;
        size = new Dimension(actualWidth, actualHeight);
        fr.setMinimumSize(size);
        fr.setMaximumSize(size);
        fr.setPreferredSize(size);
        fr.setSize(size);

        fr.setLocationRelativeTo(null);		// Center window on desktop
        fr.setLayout(null);					// We handle all redraws

		Container c = fr.getContentPane();
        c.setBackground(panelRef.getBackColor());
		c.setForeground(panelRef.getForeColor());

		return(fr);
	}

	public void addZoomWindow(JFrame fr)
	{
		if (!m_zoomFrames.contains(fr))
			m_zoomFrames.add(fr);
	}

	public void removeZoomWindow(JFrame fr)
	{
		if (fr != null)
		{
			fr.dispose();
		}
		m_zoomFrames.remove(fr);
	}

	public void closeAllZoomWindows()
	{
		int i;

		for (i = m_zoomFrames.size() - 1; i >= 0; i--)
		{
			m_zoomFrames.get(i).dispose();
		}
	}

	public void addTuple(Tuple t)
	{
		m_tuples.add(t);
	}

	public Tuple findTuple(String uuid)
	{
		int i;

		for (i = 0; i < m_tuples.size(); i++)
		{
			if (m_tuples.get(i).getUUID().equalsIgnoreCase(uuid))
				return(m_tuples.get(i));
		}
		return(null);
	}

	public Tuple deleteTuple(String uuid)
	{
		int i;

		for (i = 0; i < m_tuples.size(); i++)
		{
			if (m_tuples.get(i).getUUID().equalsIgnoreCase(uuid))
				m_tuples.remove(i);
		}
		return(null);
	}

	/** Called from various locations to process a command through the Macros
	 * <code>processCommand</code> feature.
	 *
	 * Note:  May not be needed in the near future as current plans include
	 * everything including a passed parameter to reach their own commandMaster
	 * variable.
	 *
	 * @param commandOriginal command to execute
	 * @param p1 first parameter of command
	 * @param p2 second parameter of command
	 * @param p3 third parameter of command
	 * @param p4 fourth parameter of command
	 * @param p5 fifth parameter of command
	 * @param p6 sixth parameter of command
	 * @param p7 seventh parameter of command
	 * @param p8 eighth parameter of command
	 * @param p9 ninth parameter of command
	 * @param p10 tenth parameter of command
	 */
	public void processCommand(String commandOriginal,
							   String p1,
							   String p2,
							   String p3,
							   String p4,
							   String p5,
							   String p6,
							   String p7,
							   String p8,
							   String p9,
							   String p10)
	{
		m_commandMaster.processCommand(this, commandOriginal, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10);
	}

	/**
	 * Initializes the response entry in the dialog tuple, so it can be accessed
	 * by the readDialog() code, or for other purposes
	 * @param id identifier to associate with this dialog input
	 * @param triggerCommand triggers the command specified once the
	 * OpbmInput dialog sets something
	 * @param od the OpenDialog which this relates to, or null
	 * @param oi the OpenInput which this relates to, or null
	 */
	public void initializeDialogResponse(String			id,
										 String			triggerCommand,
										 OpbmDialog		od,
										 OpbmInput		oi)
	{
		int i;

		if (m_dialogTuple == null)
			m_dialogTuple = new Tuple(this);

		for (i = 0; i < m_dialogTuple.size(); i++)
		{
			if (m_dialogTuple.getFirst(i).equalsIgnoreCase(id))
			{	// Found it
				m_dialogTuple.setSecond(i, "Unanswered");
				m_dialogTuple.setThird(i, "");
				m_dialogTuple.setFifth(i, od);
				m_dialogTuple.setSixth(i, oi);
				m_dialogTuple.setTriggerCommand(i, triggerCommand);
				return;
			}
		}
		// If we get here, it wasn't found, add it
		i = m_dialogTuple.add(id, "Unanswered", "", "", od, oi);
		m_dialogTuple.setTriggerCommand(i, triggerCommand);	// command to execute
		m_dialogTuple.setTriggerFilters(i, "3");			// when 3rd item is updated
	}

	/**
	 * When the dialog box closes, it sets the userAction (which button was
	 * pressed)
	 * @param id identifier associated with the dialog
	 * @param userAction user action (text on the button, generally speaking)
	 * @param od the OpenDialog which this relates to, or null
	 * @param oi the OpenInput which this relates to, or null
	 */
	public void setDialogResponse(String		id,
								  String		userAction,
								  OpbmDialog	od,
								  OpbmInput		oi)
	{
		int i;

		if (m_dialogTuple == null)
			initializeDialogResponse(id, "", od, oi);

		for (i = 0; i < m_dialogTuple.size(); i++)
		{
			if (m_dialogTuple.getFirst(i).equalsIgnoreCase(id))
			{	// Found it
				m_dialogTuple.setSecond(i, userAction);
				return;
			}
		}
		// If we get here, it wasn't found, add it, and try again
		initializeDialogResponse(id, "", od, oi);
		setDialogResponse(id, userAction, od, oi);
	}

	/**
	 * When the input box closes, it sets the user action (which button was
	 * pressed) and the data that was in the input box when it was pressed
	 * @param id identifier associated with this input
	 * @param userAction user action (text on the button, generally speaking)
	 * @param data whatever the user had input in the input box when the button
	 * was pressed
	 * @param od the OpenDialog which this relates to, or null
	 * @param oi the OpenInput which this relates to, or null
	 */
	public void setDialogResponse(String		id,
								  String		userAction,
								  String		data,
								  OpbmDialog	od,
								  OpbmInput		oi)
	{
		int i;

		if (m_dialogTuple == null)
			initializeDialogResponse(id, "", od, oi);

		for (i = 0; i < m_dialogTuple.size(); i++)
		{
			if (m_dialogTuple.getFirst(i).equalsIgnoreCase(id))
			{	// Found it
				m_dialogTuple.setSecond(i, userAction);
				m_dialogTuple.setThird(i, data);
				m_dialogTuple.setFourth(i, "");
				m_dialogTuple.setFifth(i, od);
				m_dialogTuple.setSixth(i, oi);
				return;
			}
		}
		// If we get here, it wasn't found, add it, and try again
		initializeDialogResponse(id, "", od, oi);
		setDialogResponse(id, userAction, data, od, oi);
	}

	/**
	 * Closes the specified dialog/input window
	 * @param id window id to close
	 */
	public void closeDialogWindow(String id)
	{
		int i;
		OpbmDialog od;
		OpbmInput oi;

		if (m_dialogTuple != null)
		{
			for (i = 0; i < m_dialogTuple.size(); i++)
			{
				if (m_dialogTuple.getFirst(i).equalsIgnoreCase(id))
				{	// Found it
					try {
						od = (OpbmDialog)m_dialogTuple.getFifth(i);
						if (od != null)
							od.dispose();
					} catch (Throwable t) {
					}

					try {
						oi = (OpbmInput)m_dialogTuple.getSixth(i);
						if (oi != null)
							oi.dispose();
					} catch (Throwable t) {
					}
				}
			}
			// Not found
		}
	}

	/**
	 * Returns the user's response (which button they pressed)
	 * @param id
	 * @return
	 */
	public String getDialogResponse(String id)
	{
		String result;
		int i;

		for (i = 0; i < m_dialogTuple.size(); i++)
		{
			if (m_dialogTuple.getFirst(i).equalsIgnoreCase(id))
			{	// Found it
				result = (String)m_dialogTuple.getSecond(i);
				return(result == null ? "" : result);
			}
		}
		// Not found
		return("--not found--");
	}

	/**
	 * Returns the user's response (which button they pressed)
	 * @param id
	 * @return
	 */
	public void clearDialogResponse(String id)
	{
		int i;

		for (i = 0; i < m_dialogTuple.size(); i++)
		{
			if (m_dialogTuple.getFirst(i).equalsIgnoreCase(id))
			{	// Found it
				m_dialogTuple.remove(i);
				return;
			}
		}
	}

	/**
	 * Returns the data item (input box) text that was recorded when the user
	 * pressed the button
	 * @param id
	 * @return
	 */
	public String getDialogResponseData(String id)
	{
		int i;

		for (i = 0; i < m_dialogTuple.size(); i++)
		{
			if (m_dialogTuple.getFirst(i).equalsIgnoreCase(id))
			{	// Found it
				return((String)m_dialogTuple.getThird(i));
			}
		}
		// Not found
		return("--not found--");
	}

	/**
	 * Returns the OpbmDialog window associated with the id
	 * pressed the button
	 * @param id
	 * @return
	 */
	public OpbmDialog getDialogOpbmDialogWindow(String id)
	{
		int i;

		for (i = 0; i < m_dialogTuple.size(); i++)
		{
			if (m_dialogTuple.getFirst(i).equalsIgnoreCase(id))
			{	// Found it
				return((OpbmDialog)m_dialogTuple.getFifth(i));
			}
		}
		// Not found
		return(null);
	}

	/**
	 * Returns the OpbmInput window associated with the id
	 * pressed the button
	 * @param id
	 * @return
	 */
	public OpbmDialog getDialogOpbmInputWindow(String id)
	{
		int i;

		for (i = 0; i < m_dialogTuple.size(); i++)
		{
			if (m_dialogTuple.getFirst(i).equalsIgnoreCase(id))
			{	// Found it
				return((OpbmDialog)m_dialogTuple.getSixth(i));
			}
		}
		// Not found
		return(null);
	}

	public void setTrialRun()
	{
		m_executingTrialRun		= true;
		m_executingOfficialRun	= false;
	}

	public void setOfficialRun()
	{
		m_executingTrialRun		= false;
		m_executingOfficialRun	= true;
	}

	public void setRunFinished()
	{
		m_executingTrialRun		= false;
		m_executingOfficialRun	= false;
	}

	public String getRunType()
	{
		if (m_executingOfficialRun)
			return("official");

		else if (m_executingTrialRun)
			return("trial");

		else
			return("manual");
	}

	// +rcp 12/18/2011
        public void setExecutingFromCommandLine(Boolean automated)
	{
            m_executingFromCommandLine = automated;
        }
        //+rcp

        // +rcp 12/18/2011
        public boolean getExecutingFromCommandLine()
        {
            return(m_executingFromCommandLine);
        }
        //+rcp

        
        
        public void setRunName(String name)
	{
		m_executingBenchmarkRunName = name;
	}

	public String getRunName()
	{
		return(m_executingBenchmarkRunName);
	}

	public boolean isExecutingTrialRun()
	{
		return(m_executingTrialRun);
	}

	public boolean isExecutionOfficialRun()
	{
		return(m_executingOfficialRun);
	}

	/**
	 * Open a web browser with the specified parameters
	 *
	 * @param p1 first parameter (typically the url)
	 * @param p2 second parameter (typically ignored)
	 * @param p3 third parameter
	 * @param p4 fourth parameter
	 * @param p5 fifth parameter
	 * @param p6 sixth parameter
	 * @param p7 seventh parameter
	 * @param p8 eighth parameter
	 * @param p9 ninth parameter
	 * @param p10 tenth parameter
	 */
	public void webBrowser(String p1,
						   String p2,
						   String p3,
						   String p4,
						   String p5,
						   String p6,
						   String p7,
						   String p8,
						   String p9,
						   String p10)
	{
		try {

			Utils.launchWebBrowser((p1 + p2 + p3 + p4 + p5 + p6 + p7 + p8 + p9 + p10).trim());

		} catch (Exception ex1) {
			try {

				Utils.launchWebBrowser(p1);

			} catch (Exception ex2) {
			}
		}
	}

	public Settings getSettingsMaster()
	{
		return(m_settingsMaster);
	}

	public Macros getMacroMaster()
	{
		return(m_macroMaster);
	}

	public Benchmarks getBenchmarkMaster()
	{
		return(m_benchmarkMaster);
	}

	public Commands getCommandMaster()
	{
		return(m_commandMaster);
	}

	/**
	 * Takes the
	 */
	public void translateManifestToResults(String fileName)
	{
		BenchmarkManifest bm;
		OpbmDialog od;

		bm = new BenchmarkManifest(m_opbm);
		bm.reloadManifestAndComputeResultsXml(fileName);
		if (bm.isManifestInError())
			od = new OpbmDialog(m_opbm, true, "There was an error while processing manifest.xml", "Error", OpbmDialog._CANCEL_BUTTON, "", "");
		else
			createAndShowResultsViewer(Opbm.getHarnessXMLDirectory() + getResultsViewerFilename());
	}

	public void benchmarkLaunchTrialRun(boolean automated)
	{
		m_benchmarkMaster.benchmarkLaunchTrialRun(automated);
	}

	public void benchmarkLaunchOfficialRun(boolean automated)
	{
		m_benchmarkMaster.benchmarkLaunchOfficialRun(automated);
	}


	/**
	 * Prepares everything to run a suite benchmark
	 */
	public void benchmarkRunSuite(Xml				suite,
								  int				iterations,
								  boolean			openInNewThread,
								  PanelRightItem	pri,
								  Opbm				opbm,
								  Macros			macroMaster,
								  Settings			settingsMaster,
								  String			p1,
								  String			p2,
								  String			p3,
								  String			p4,
								  String			p5,
								  String			p6,
								  String			p7,
								  String			p8,
								  String			p9,
								  String			p10)
	{
		Tuple tup = new Tuple();
		tup.add("type",					"suite");
		tup.add("m_bm",					new BenchmarkManifest(opbm, "compilation", "", false, false));
		tup.add("suite",				suite == null ? m_benchmarkMaster.loadEntryFromPanelRightItem(pri, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10) : suite);
		tup.add("scenario",				null);
		tup.add("molecule",				null);
		tup.add("atom",					null);
		tup.add("iterations",			iterations);
		tup.add("openInNewThread",		openInNewThread);
		tup.add("pri",					pri);
		tup.add("opbm",					opbm);
		tup.add("macroMaster",			macroMaster);
		tup.add("settingsMaster",		settingsMaster);
		tup.add("p1",					p1);
		tup.add("p2",					p2);
		tup.add("p3",					p3);
		tup.add("p4",					p4);
		tup.add("p5",					p5);
		tup.add("p6",					p6);
		tup.add("p7",					p7);
		tup.add("p8",					p8);
		tup.add("p9",					p9);
		tup.add("p10",					p10);
		m_benchmarkRun_tup = tup;

		Thread t = new Thread("BenchmarkRunSuite_Setup")
		{
			@Override
			public void run()
			{	// In the thread
				int iterations;
				Tuple tup = m_benchmarkRun_tup;

				iterations = (Integer)tup.getSecond("iterations");
				if (iterations == 0)
				{	// Ask the user how many iterations they want
					OpbmInput oi = new OpbmInput(m_opbm, true, "Iteration Count for Suite: " + ((Xml)tup.getSecond("suite")).getAttribute("name"), "Please specify the iteration count (1 to N):", "1", OpbmInput._ACCEPT_CANCEL, "iteration_count_suite", "", true);
					Tuple input = oi.readInput();
					if (!((String)input.getSecond("action")).toLowerCase().contains("accept"))
					{	// They did not click the accept button, so they are canceling
						return;
					}
					// If we get here, we have a value
					iterations	= Utils.getValueOf((String)input.getSecond("value"), 0, 0, Integer.MAX_VALUE);
					if (iterations == 0)
					{	// They did not specify a valid value
						return;
					}
					tup.setSecond("iterations", iterations);
				}
				//else
				// Just run with the specified number of iterations

				if ((Boolean)tup.getSecond("openInNewThread"))
				{	// Since the benchmark uses an overlay heads-up-display,
					// it needs to be off the EDT thread, so we give it its own thread.
					m_benchmarkRun_tup = tup;
					Thread t = new Thread("OPBM_Benchmark_Thread")
					{
						@Override
						public void run()
						{
							runAtomMoleculeScenarioSuite(m_benchmarkRun_tup);
						}
					};
					t.start();

				} else {
					runAtomMoleculeScenarioSuite(tup);
				}
			}
		};
		t.start();
	}

	/**
	 * Prepares everything to run a scenario benchmark
	 */
	public void benchmarkRunScenario(Xml			scenario,
									 int			iterations,
									 boolean		openInNewThread,
									 PanelRightItem	pri,
									 Opbm			opbm,
									 Macros			macroMaster,
									 Settings		settingsMaster,
									 String			p1,
									 String			p2,
									 String			p3,
									 String			p4,
									 String			p5,
									 String			p6,
									 String			p7,
									 String			p8,
									 String			p9,
									 String			p10)
	{
		Tuple tup = new Tuple();
		tup.add("type",					"scenario");
		tup.add("m_bm",					new BenchmarkManifest(opbm, "compilation", "", false, false));
		tup.add("suite",				null);
		tup.add("scenario",				scenario == null ? m_benchmarkMaster.loadEntryFromPanelRightItem(pri, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10) : scenario);
		tup.add("molecule",				null);
		tup.add("atom",					null);
		tup.add("iterations",			iterations);
		tup.add("openInNewThread",		openInNewThread);
		tup.add("pri",					pri);
		tup.add("opbm",					opbm);
		tup.add("macroMaster",			macroMaster);
		tup.add("settingsMaster",		settingsMaster);
		tup.add("p1",					p1);
		tup.add("p2",					p2);
		tup.add("p3",					p3);
		tup.add("p4",					p4);
		tup.add("p5",					p5);
		tup.add("p6",					p6);
		tup.add("p7",					p7);
		tup.add("p8",					p8);
		tup.add("p9",					p9);
		tup.add("p10",					p10);
		m_benchmarkRun_tup = tup;

		Thread t = new Thread("BenchmarkRunScenario_Setup")
		{
			@Override
			public void run()
			{	// In the thread
				int iterations;
				Tuple tup = m_benchmarkRun_tup;

				iterations = (Integer)tup.getSecond("iterations");
				if (iterations == 0)
				{	// Ask the user how many iterations they want
					OpbmInput oi = new OpbmInput(m_opbm, true, "Iteration Count for Scenario: " + ((Xml)tup.getSecond("scenario")).getAttribute("name"), "Please specify the iteration count (1 to N):", "1", OpbmInput._ACCEPT_CANCEL, "iteration_count_suite", "", true);
					Tuple input = oi.readInput();
					if (!((String)input.getSecond("action")).toLowerCase().contains("accept"))
					{	// They did not click the accept button, so they are canceling
						return;
					}
					// If we get here, we have a value
					iterations	= Utils.getValueOf((String)input.getSecond("value"), 0, 0, Integer.MAX_VALUE);
					if (iterations == 0)
					{	// They did not specify a valid value
						return;
					}
					tup.setSecond("iterations", iterations);
				}
				//else
				// Just run with the specified number of iterations

				if ((Boolean)tup.getSecond("openInNewThread"))
				{	// Since the benchmark uses an overlay heads-up-display,
					// it needs to be off the EDT thread, so we give it its own thread.
					m_benchmarkRun_tup = tup;
					Thread t = new Thread("OPBM_Benchmark_Thread")
					{
						@Override
						public void run()
						{
							runAtomMoleculeScenarioSuite(m_benchmarkRun_tup);
						}
					};
					t.start();

				} else {
					runAtomMoleculeScenarioSuite(tup);
				}
			}
		};
		t.start();
	}

	/**
	 * Prepares everything to run a molecule benchmark
	 */
	public void benchmarkRunMolecule(Xml			molecule,
									 int			iterations,
									 boolean		openInNewThread,
									 PanelRightItem	pri,
									 Opbm			opbm,
									 Macros			macroMaster,
									 Settings		settingsMaster,
									 String			p1,
									 String			p2,
									 String			p3,
									 String			p4,
									 String			p5,
									 String			p6,
									 String			p7,
									 String			p8,
									 String			p9,
									 String			p10)
	{
		Tuple tup = new Tuple();
		tup.add("type",					"molecule");
		tup.add("m_bm",					new BenchmarkManifest(opbm, "compilation", "", false, false));
		tup.add("suite",				null);
		tup.add("scenario",				null);
		tup.add("molecule",				molecule == null ? m_benchmarkMaster.loadEntryFromPanelRightItem(pri, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10) : molecule);
		tup.add("atom",					null);
		tup.add("iterations",			iterations);
		tup.add("openInNewThread",		openInNewThread);
		tup.add("pri",					pri);
		tup.add("opbm",					opbm);
		tup.add("macroMaster",			macroMaster);
		tup.add("settingsMaster",		settingsMaster);
		tup.add("p1",					p1);
		tup.add("p2",					p2);
		tup.add("p3",					p3);
		tup.add("p4",					p4);
		tup.add("p5",					p5);
		tup.add("p6",					p6);
		tup.add("p7",					p7);
		tup.add("p8",					p8);
		tup.add("p9",					p9);
		tup.add("p10",					p10);
		m_benchmarkRun_tup = tup;

		Thread t = new Thread("BenchmarkRunMolecule_Setup")
		{
			@Override
			public void run()
			{	// In the thread
				int iterations;
				Tuple tup = m_benchmarkRun_tup;

				iterations = (Integer)tup.getSecond("iterations");
				if (iterations == 0)
				{	// Ask the user how many iterations they want
					OpbmInput oi = new OpbmInput(m_opbm, true, "Iteration Count for Molecule: " + ((Xml)tup.getSecond("molecule")).getAttribute("name"), "Please specify the iteration count (1 to N):", "1", OpbmInput._ACCEPT_CANCEL, "iteration_count_suite", "", true);
					Tuple input = oi.readInput();
					if (!((String)input.getSecond("action")).toLowerCase().contains("accept"))
					{	// They did not click the accept button, so they are canceling
						return;
					}
					// If we get here, we have a value
					iterations	= Utils.getValueOf((String)input.getSecond("value"), 0, 0, Integer.MAX_VALUE);
					if (iterations == 0)
					{	// They did not specify a valid value
						return;
					}
					tup.setSecond("iterations", iterations);
				}
				//else
				// Just run with the specified number of iterations

				if ((Boolean)tup.getSecond("openInNewThread"))
				{	// Since the benchmark uses an overlay heads-up-display,
					// it needs to be off the EDT thread, so we give it its own thread.
					m_benchmarkRun_tup = tup;
					Thread t = new Thread("OPBM_Benchmark_Thread")
					{
						@Override
						public void run()
						{
							runAtomMoleculeScenarioSuite(m_benchmarkRun_tup);
						}
					};
					t.start();

				} else {
					runAtomMoleculeScenarioSuite(tup);
				}
			}
		};
		t.start();
	}

	/**
	 * Prepares everything to run an atom benchmark
	 */
	public void benchmarkRunAtom(Xml			atom,
								 int			iterations,
								 boolean		openInNewThread,
								 PanelRightItem	pri,
								 Opbm			opbm,
								 Macros			macroMaster,
								 Settings		settingsMaster,
								 String			p1,
								 String			p2,
								 String			p3,
								 String			p4,
								 String			p5,
								 String			p6,
								 String			p7,
								 String			p8,
								 String			p9,
								 String			p10)
	{
		Tuple tup = new Tuple();
		tup.add("type",					"atom");
		tup.add("m_bm",					new BenchmarkManifest(opbm, "compilation", "", false, false));
		tup.add("suite",				null);
		tup.add("scenario",				null);
		tup.add("molecule",				null);
		tup.add("atom",					atom == null ? m_benchmarkMaster.loadEntryFromPanelRightItem(pri, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10) : atom);
		tup.add("iterations",			iterations);
		tup.add("openInNewThread",		openInNewThread);
		tup.add("pri",					pri);
		tup.add("opbm",					opbm);
		tup.add("macroMaster",			macroMaster);
		tup.add("settingsMaster",		settingsMaster);
		tup.add("p1",					p1);
		tup.add("p2",					p2);
		tup.add("p3",					p3);
		tup.add("p4",					p4);
		tup.add("p5",					p5);
		tup.add("p6",					p6);
		tup.add("p7",					p7);
		tup.add("p8",					p8);
		tup.add("p9",					p9);
		tup.add("p10",					p10);
		m_benchmarkRun_tup = tup;

		Thread t = new Thread("BenchmarkRunAtom_Setup")
		{
			@Override
			public void run()
			{	// In the thread
				int iterations;
				Tuple tup = m_benchmarkRun_tup;

				iterations = (Integer)tup.getSecond("iterations");
				if (iterations == 0)
				{	// Ask the user how many iterations they want
					OpbmInput oi = new OpbmInput(m_opbm, true, "Iteration Count for Atom: " + ((Xml)tup.getSecond("atom")).getAttribute("name"), "Please specify the iteration count (1 to N):", "1", OpbmInput._ACCEPT_CANCEL, "iteration_count_suite", "", true);
					Tuple input = oi.readInput();
					if (!((String)input.getSecond("action")).toLowerCase().contains("accept"))
					{	// They did not click the accept button, so they are canceling
						return;
					}
					// If we get here, we have a value
					iterations	= Utils.getValueOf((String)input.getSecond("value"), 0, 0, Integer.MAX_VALUE);
					if (iterations == 0)
					{	// They did not specify a valid value
						return;
					}
					tup.setSecond("iterations", iterations);
				}
				//else
				// Just run with the specified number of iterations
				tup.add("iterations", iterations);

				if ((Boolean)tup.getSecond("openInNewThread"))
				{	// Since the benchmark uses an overlay heads-up-display,
					// it needs to be off the EDT thread, so we give it its own thread.
					m_benchmarkRun_tup = tup;
					Thread t = new Thread("OPBM_Benchmark_Thread")
					{
						@Override
						public void run()
						{
							runAtomMoleculeScenarioSuite(m_benchmarkRun_tup);
						}
					};
					t.start();

				} else {
					runAtomMoleculeScenarioSuite(tup);
				}
			}
		};
		t.start();
	}


	// These variables are used only by the few methods above, and this one below
	private BenchmarkManifest	m_benchmarkRun_bm		= null;
	private Tuple				m_benchmarkRun_tup		= null;

	/**
	 * Physically completes the initial setup and execution of a compilation
	 * run for atom, molecule, scenario or suite
	 * @param tup the Tuple holding data for the compilation run
	 */
	public void runAtomMoleculeScenarioSuite(Tuple tup)
	{
		BenchmarkManifest	bm						= (BenchmarkManifest)	tup.getSecond("m_bm");
		Xml					atom					= (Xml)					tup.getSecond("atom");
		Xml					molecule				= (Xml)					tup.getSecond("molecule");
		Xml					scenario				= (Xml)					tup.getSecond("scenario");
		Xml					suite					= (Xml)					tup.getSecond("suite");
		int					iterations				= (Integer)				tup.getSecond("iterations");
		boolean				openInNewThread			= (Boolean)				tup.getSecond("openInNewThread");
		PanelRightItem		pri						= (PanelRightItem)		tup.getSecond("pri");
		Opbm				opbm					= (Opbm)				tup.getSecond("opbm");
		Macros				macroMaster				= (Macros)				tup.getSecond("macroMaster");
		Settings			settingsMaster			= (Settings)			tup.getSecond("settings");
		String				p1						= (String)				tup.getSecond("p1");
		String				p2						= (String)				tup.getSecond("p2");
		String				p3						= (String)				tup.getSecond("p3");
		String				p4						= (String)				tup.getSecond("p4");
		String				p5						= (String)				tup.getSecond("p5");
		String				p6						= (String)				tup.getSecond("p6");
		String				p7						= (String)				tup.getSecond("p7");
		String				p8						= (String)				tup.getSecond("p8");
		String				p9						= (String)				tup.getSecond("p9");
		String				p10						= (String)				tup.getSecond("p10");

		// m_benchmarkRun_bm is used once the benchmark is finished to find out which one is running
		m_benchmarkRun_bm = bm;
		if (((String)tup.getSecond("type")).equalsIgnoreCase("atom"))
		{	// They are running an atom
			atom = m_benchmarkMaster.loadEntryFromPanelRightItem(pri, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10);
			bm.addToCompiledList("atom", atom.getAttribute("name"), iterations);
			m_executingBenchmarkRunName = "Atom " + atom.getAttribute("name");

		} else if (((String)tup.getSecond("type")).equalsIgnoreCase("molecule"))
		{	// Running a molecule
			molecule = m_benchmarkMaster.loadEntryFromPanelRightItem(pri, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10);
			bm.addToCompiledList("molecule", molecule.getAttribute("name"), iterations);
			m_executingBenchmarkRunName = "Molecule " + molecule.getAttribute("name");

		} else if (((String)tup.getSecond("type")).equalsIgnoreCase("scenario"))
		{	// Running a scenario
			scenario = m_benchmarkMaster.loadEntryFromPanelRightItem(pri, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10);
			bm.addToCompiledList("scenario", scenario.getAttribute("name"), iterations);
			m_executingBenchmarkRunName = "Scenario " + scenario.getAttribute("name");

		} else if (((String)tup.getSecond("type")).equalsIgnoreCase("suite"))
		{	// Running a suite
			suite = m_benchmarkMaster.loadEntryFromPanelRightItem(pri, p1, p2, p3, p4, p5, p6, p7, p8, p9, p10);
			bm.addToCompiledList("suite", suite.getAttribute("name"), iterations);
			m_executingBenchmarkRunName = "Suite " + suite.getAttribute("name");

		} else {
			// Unknown type
			System.out.println("Unrecognized benchmark run type encountered (\"" + (String)tup.getSecond("type") + "\"), should be atom, molecule, scenario or suite.");
			return;
		}

		// Build the single-entry compilation, and run it
		if (bm != null && bm.buildCompilation())
			bm.run();
	}

	// Used only by benchmarkRunCompilation():
	private Opbm				m_brc_opbm;
	private BenchmarkManifest	m_brc_bm;

	/**
	 * Called to run whatever's in the m_compilation Xml list.
	 */
	public void benchmarkRunCompilation()
	{
		int i, iterations;
		String type, name;
		Xml candidate;
		BenchmarkManifest bm;

		if (!m_compilation.isEmpty())
		{	// Create our new compilation
			bm = new BenchmarkManifest(m_opbm, "compilation", "", true, false);

			// Add items to it
			for (i = 0; i < m_compilation.size(); i++)
			{	// Grab the entry
				candidate = m_compilation.get(i);

				// See what type it is
				type		= candidate.getName();
				name		= candidate.getAttribute("name");
				iterations	= Utils.getValueOf(candidate.getAttribute("iterations"), 0, 0, Integer.MAX_VALUE);
				if (iterations != 0)
				{
					if (type.equalsIgnoreCase("atom"))
					{	// It's an atom, add it
						bm.addToCompiledList("atom", name, iterations);

					} else if (type.equalsIgnoreCase("molecule")) {
						// It's a molecule, add it
						bm.addToCompiledList("molecule", name, iterations);

					} else if (type.equalsIgnoreCase("scenario")) {
						// It's a scenario, add it
						bm.addToCompiledList("scenario", name, iterations);

					} else if (type.equalsIgnoreCase("suite")) {
						// It's a suite, add it
						bm.addToCompiledList("suite", name, iterations);

					}
				}
			}

			if (!bm.isCompilationEmpty())
			{	// There is a compilation, try to run it
				m_brc_bm	= bm;
				m_brc_opbm	= this;
				Thread t = new Thread("OPBM_Compilation_Benchmark_Thread")
				{
					@Override
					public void run()
					{
						OpbmDialog od;

						// They may have added items to the benchmark compilaiton to execute
						if (m_brc_bm.buildCompilation())
							m_brc_bm.run();
						else
							od = new OpbmDialog(m_brc_opbm, true, "Error building the compilation.", "Failure", OpbmDialog._CANCEL_BUTTON, "", "");
					}
				};
				t.start();
			}
		}
	}

	/** Calls <code>Macros.parseMacros()</code>
	 *
	 * Note:  May no longer be needed as everything now includes a passed
	 * parameter to reach their own Macros class (macroMaster variable).
	 *
	 * @param candidate string which may contain macros to expand
	 * @return string with any macros expanded or replaced
	 */
	public String expandMacros(String candidate)
	{
		return(m_macroMaster.parseMacros(candidate));
	}

	/** Adds a panel to the m_leftPanels ArrayList.
	 *
	 */
	public void addPanelLeft(PanelLeft p)
	{
		m_leftPanels.add(p);
	}

	/** Returns the root of the loaded panels.xml file in its logically
	 * structured link-list Xml class hierarchy.
	 *
	 * @return Xml root for panels.xml
	 */
	public Xml getPanelsXml()
	{
		return(m_panelXml);
	}

	/** Returns the root of the loaded edits.xml file in its logically
	 * structured link-list Xml class hierarchy.
	 *
	 * @return Xml root for edits.xml
	 */
	public Xml getEditsXml()
	{
		return(m_editXml);
	}

	/** Returns the root of the loaded scripts.xml file in its logically
	 * structured link-list Xml class hierarchy.
	 *
	 * @return Xml root for scripts.xml
	 */
	public Xml getScriptsXml()
	{
		return(m_scriptsXml);
	}

	public PanelRight getActiveEdit()
	{
		return(m_editActive);
	}

	public PanelRight getActiveRawEdit()
	{
		return(m_rawEditActive);
	}

	public void updateEditListboxesAndLookupboxes()
	{
		if (m_editActive != null) {
			m_editActive.updateEditListboxesAndLookupboxes();
		}
	}

	/**
	 * Indicates whether or not the -font command line switch was used.
	 *
	 * @return true or false, should font be overridden?
	 */
	public boolean isFontOverride()
	{
		return(m_fontOverride);
	}

	/**
	 * Examines the registry keys to see if the user has a default password
	 * setup in the appropriate registry key, and if not then returns false.
	 * @return true or false, is auto logon enabled?
	 */
	public static boolean isAutoLogonEnabled()
	{
		String value;
		value = Opbm.GetRegistryKeyValue("HKLM\\SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion\\Winlogon\\AutoAdminLogon");
		return(value.equals("1"));
	}

	/**
	 * Examines the registry keys to see if User Account Control is enabled,
	 * which will prevent unsigned executables from running.
	 * @return
	 */
	public static boolean isUACEnabled()
	{
		String value;

		value = Opbm.GetRegistryKeyValue("HKLM\\SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Policies\\System\\EnableLUA");
		try {
			if (Integer.valueOf(value) != 0)
				return(true);	// UAC is enabled
		} catch (Throwable t) {
			// Probably a number exception, but should not happen as this registry key is a DWORD
		}
		// If we get here, not enabled or an error (which we then assume not enabled)
		return(false);
	}

	/**
	 * Debugging tool, enable or disable debugging
	 * @param enabled
	 */
	public static void setBreakpointsEnabled(boolean enabled)
	{
		m_breakpointsEnabled = enabled;
	}

	/**
	 * Debugging too, queries current setting of breakpoints
	 * @return
	 */
	public static boolean areBreakpointsEnabled()
	{
		return(m_breakpointsEnabled);
	}

	/**
	 * Returns the JFrame of the main GUI
	 * @return
	 */
	public DroppableFrame getGUIFrame()
	{
		return(m_frameDeveloper);
	}

	/**
	 * Adds the frame to a list so that it can be closed when starting a new
	 * benchmark run
	 * @param frame
	 */
	public void addResultsViewerToQueue(DroppableFrame frame)
	{
		int i;

		for (i = 0; i < m_rvFrames.size(); i++)
		{
			if (m_rvFrames.get(i).equals(frame))
			{	// It's already in our list
				return;
			}
		}
		// If we get here, it's not in our list, add it
		m_rvFrames.add(frame);
	}

	/**
	 * Removes the entry from the queue
	 * @param frame
	 */
	public void removeResultsViewerFromQueue(DroppableFrame frame)
	{
		int i;

		for (i = 0; i < m_rvFrames.size(); i++)
		{
			if (m_rvFrames.get(i).equals(frame))
			{	// It's here, delete it
				m_rvFrames.remove(i);
			}
		}
	}

	/**
	 * Called when a benchmark run starts to close all the results viewer windows
	 */
	public void closeAllResultsViewerWindowsInQueue()
	{
		int i;

		for (i = m_rvFrames.size() - 1; i >= 0; i--)
		{	// Close and remove them from this list (so they won't show up again and will be garbage collected)
			m_rvFrames.get(i).dispose();
			m_rvFrames.remove(i);
		}
	}

	/**
	 * Called when a benchmark run starts to hide all the results viewer windows
	 */
	public void hideAllResultsViewerWindowsInQueue()
	{
		int i;

		for (i = 0; i < m_rvFrames.size(); i++)
			m_rvFrames.get(i).setVisible(false);
	}

	/**
	 * Called when a benchmark ends to restore all the results viewer windows
	 */
	public void showAllResultsViewerWindowsInQueue()
	{
		int i;

		for (i = 0; i < m_rvFrames.size(); i++)
			m_rvFrames.get(i).setVisible(true);
	}

	/**
	 * Returns the compilation list
	 */
	public List<Xml> getCompilationXml()
	{
		return(m_compilation);
	}

	/**
	 * The highlighted entry is maintained globally, so it can be accessed from
	 * multiple different edit screens.
	 * @return the current highlighted entry
	 */
	public int getCompilationHighlightEntry()
	{
		if (m_compilationHighlightEntry > m_compilation.size() - 1)
			m_compilationHighlightEntry = m_compilation.size() - 1;

		return(m_compilationHighlightEntry);
	}

	/**
	 * Deletes the highlighted entry in the list
	 */
	public void deleteCompilationHighlightedEntries(JList listbox)
	{
		int i;
		int[] indices = listbox.getSelectedIndices();

		// Delete every entry that's highlighted in the listbox,
		// which exists in a 1:1 ratio/relationship to the m_compilation list
		for (i = indices.length - 1; i >= 0; i--)
		{	// If this entry is selected, delete it
			m_compilation.remove(indices[i]);
		}
	}

	/**
	 * Move the compilation highlighted entry up one position
	 */
	public void moveCompilationHighlightedEntryUpOne()
	{
		if (m_compilationHighlightEntry > 0)
			Collections.swap(m_compilation, m_compilationHighlightEntry, m_compilationHighlightEntry - 1);
	}

	/**
	 * Move the compilation highlighted entry down one position
	 */
	public void moveCompilationHighlightedEntryDownOne()
	{
		if (m_compilationHighlightEntry < m_compilation.size() - 1)
			Collections.swap(m_compilation, m_compilationHighlightEntry + 1, m_compilationHighlightEntry);
	}

	/**
	 * Removes everything from the compilation list
	 */
	public void clearCompilation()
	{
		m_compilation.clear();
	}

	/**
	 * Asks the user for the specified entry's iteration count
	 */
	public void compilationUpdateIterations()
	{
		String tag, name, iterations;
		Xml toUpdate, iterationNode;

		toUpdate		= m_compilation.get(m_compilationHighlightEntry);
		tag				= toUpdate.getName();
		name			= toUpdate.getAttribute("name");
		iterationNode	= toUpdate.getAttributeNode("iterations");
		iterations		= iterationNode.getText();

		// Ask the user how many iterations
		OpbmInput oi = new OpbmInput(m_opbm, true, "Iteration Count for " + Utils.toProper(tag) + ": " + Utils.toProper(name), "Please specify the iteration count (1 to N):", iterations, OpbmInput._ACCEPT_CANCEL, "iteration_count_compilation", "", true);
		Tuple input = oi.readInput();
		if (!((String)input.getSecond("action")).toLowerCase().contains("accept"))
		{	// They did not click the accept button, so they are canceling
			return;
		}
		// If we get here, we have a value
		iterations	= (String)input.getSecond("value");
		if (Utils.getValueOf(iterations, 0, 0, Integer.MAX_VALUE) == 0)
		{	// They did not specify a valid value
			return;
		}

		// We're good, Insert it where they are
		iterationNode.setText(iterations);
	}

	/**
	 * The highlighted entry is maintained globally, so it can be accessed from
	 * multiple different edit screens.
	 * @param entry the new entry position to set
	 */
	public void setCompilationHighlightEntry(int entry)
	{
		entry = Math.min(entry, m_compilation.size());
		m_compilationHighlightEntry = entry;
	}

	/**
	 * Inserts the specified atom/molecule/scenario/suite by name and iteration
	 * count into the list
	 * @param type
	 * @param name
	 * @param iterations
	 */
	public void insertToCompilationXml(String	type,
									   String	name,
									   int		iterations)
	{
		Xml toAdd;

		toAdd = new Xml(type);
		toAdd.appendAttribute("name",			name);
		toAdd.appendAttribute("iterations",		Integer.toString(iterations));

		if (m_compilation.isEmpty())
		{	// Add at the beginning
			m_compilation.add(toAdd);
			m_compilationHighlightEntry = 0;

		} else {
			if (m_compilationHighlightEntry + 1 > m_compilation.size())
			{	// Append to the end
				m_compilation.add(toAdd);
				m_compilationHighlightEntry = m_compilation.size() - 1;

			} else {
				// Insert where we are
				m_compilation.add(m_compilationHighlightEntry + 1, toAdd);
				++m_compilationHighlightEntry;
			}
		}
	}

	/**
	 * Asks the user for a directory, and loads all results*.xml files, adding
	 * up every timing point contained within, and producing an average and
	 * output file called results_averages.xml in the same directory.
	 *
	 * This method is only used by the developer.
	 *
	 */
	public void computeResultsXmlAverages()
	{
		int i, j, totalFiles, totalEntries;
		String fileName, directoryName, entry, atomName, timingName, timingSeconds, outputLine;
		JFileChooser chooser;
		String[] files;
		File candidate;
		Xml results, timing;
		List<Xml> resultsData;
		List<Xml> resultItems;
		Tuple compiledResults;
		Tuple detailedResults;
		BenchmarkParams bp;
		OpbmDialog od;
		FilenameFilter fileFilter;
		javax.swing.filechooser.FileFilter directoryFilter;
		List<String> output;


		// Include only those files which are results*.xml
		fileFilter = new FilenameFilter()
		{
			@Override
			public boolean accept(File dir, String name)
			{
				return(name.toLowerCase().startsWith("results") && name.toLowerCase().endsWith(".xml"));
			}
		};

		// Include only those files which are results*.xml
		directoryFilter = new javax.swing.filechooser.FileFilter() {
			@Override
			public boolean accept(File file)
			{
				return(file.isDirectory());
			}
			@Override
			public String getDescription() {
				return("Directory");
			}
		};


		// Ask the user for the directory
		chooser = new JFileChooser();
		chooser.setFileFilter(directoryFilter);
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		if (chooser.showOpenDialog(m_frameDeveloper) == JFileChooser.APPROVE_OPTION)
		{
			directoryName	= Utils.verifyPathEndsInBackslash(chooser.getSelectedFile().getAbsolutePath());
			candidate		= new File(directoryName);
			System.out.println("Loading results*.xml files from " + directoryName + " for average.");

			// Get a list of all matching files in the list
			files = candidate.list(fileFilter);
			if (files != null)
			{	// Process each file in the list
				bp = new BenchmarkParams();

				// Create the master list to be used for all data gathering
				// resultsData (allocated below, once for each file)
				// holds a full list of pointers to each score line

				compiledResults = new Tuple(this);
				// The tuple array contains one entry per file, with these data items in these element locations:
				//		first	= fileName
				//		second	= resultsData ArrayList of raw Xml entries for the file
				//		third	= Tuple containing:
				//					first	= name of timing item
				//					second	= timing
				//					third	= results entry for this item

				// Iterate through every file
				totalFiles		= 0;
				totalEntries	= 0;
				for (i = 0; i < files.length; i++)
				{	// Get filename of file or directory
					fileName	= directoryName + files[i];
					candidate	= new File(fileName);
					if (candidate.exists() && candidate.isFile() && candidate.canRead())
					{	// Try to load as an Xml
						results = loadXml(fileName, this);
						if (results != null)
						{	// Process its data elements
							++totalFiles;

							// Grab the list of nodes
							resultsData = new ArrayList<Xml>(0);			// Holds the list of opbm.rawdata.run.results nodes in the file
							Xml.getNodeList(resultsData, results, "opbm.rawdata.run.results", false);
							if (!resultsData.isEmpty())
							{	// There is at least one entry in this file

								// Create the entries for this listing
								resultItems		= new ArrayList<Xml>(0);	// Holds the scoring data lines from each result*.xml file that's loaded
								detailedResults	= new Tuple(this);			// Holds the detailed items for each Xml entry broken out
								compiledResults.add(fileName, resultItems, detailedResults);
								for (j = 0; j < resultsData.size(); j++)
								{	// Grab all of the named elements that are not total lines
									results = resultsData.get(j);
									timing = results.getChildNode("timing");
									while (timing != null)
									{	// Process every entry that's not a timing line
										entry = timing.getText();
										if (!entry.toLowerCase().startsWith("total,"))
										{	// Save this entry
											++totalEntries;
											resultItems.add(timing);
											bp.extractTimingLineElements(timing.getText());
											detailedResults.add(bp.m_timingName, Double.toString(bp.m_timingInSeconds), results);
										}

										// Move to next sibling
										timing = timing.getNext();
									}
									// When we get here, this entry's timing elements have been exhausted
									// Proceed to the next results entry
								}
								// When we get here, all results for this file have been consumed
								// We don't clear up the results Xml that was loaded, because
								// it has elements referenced above

							} else {
								System.out.println("No opbm.rawdata.run.results elements found in " + fileName);
							}
							// When we get here, we're ready to try the next file

						} else {
							System.out.println("Unable to load " + fileName);
						}

					} else {
						System.out.println("Ignoring " + fileName + ", not found.");
					}
					// When we get here, we're ready to try the next file
				}
				// When we get here, we've tried all files
				if (totalFiles == 0)
				{	// Nothing to do
					od = new OpbmDialog(m_opbm, true, "Directory had no files with opbm.rawdata.run.results elements.", "Error", OpbmDialog._OKAY_BUTTON, "", "");
					return;
				}
				// If we get here, there is some data to process
				output = new ArrayList<String>(0);
				// Generate an output of everything we have
				for (i = 0; i < compiledResults.size(); i++)
				{	// Generate output from each entry
					fileName		= compiledResults.getFirst(i);
					resultItems		= (List<Xml>)compiledResults.getSecond(i);
					detailedResults	= (Tuple)compiledResults.getThird(i);
					for (j = 0; j < detailedResults.size(); j++)
					{	// Build the line for this entry
						timingName		= (String)detailedResults.getFirst(j);
						timingSeconds	= (String)detailedResults.getSecond(j);
						atomName		= ((Xml)detailedResults.getThird(j)).getAttribute("name");
						outputLine		= atomName + "," + timingName + "," + timingSeconds;
						output.add(outputLine);
					}
					// When we get here, all entries for this file are generated
					// Proceed to the next file
				}
				// When we get here, the output list is populated
				fileName = directoryName + "output.csv";
				Utils.writeTerminatedLinesToFile(fileName, output);
				od = new OpbmDialog(m_opbm, true, "Compiled results to " + fileName, "Success", OpbmDialog._OKAY_BUTTON, "", "");
				return;
			}
			// Directory contains no results*.xml files
			od = new OpbmDialog(m_opbm, true, "Directory contains no results*.xml files", "Error", OpbmDialog._OKAY_BUTTON, "", "");
			return;
		}
	}

	/**
	 * Gathers debug info in the form of all the XML files used by OPBM
	 * (settings, manifest, results, scripts, edits and panels.xml), and
	 * records information about the Java version, system variables, etc.
	 */
	public void gatherDebugInfo()
	{
		Thread t = new Thread("GatherDebugInfoThread")
		{
			@Override
			public void run()
			{	// Display a dialog box so the user doesn't click the button twice
				String uuid;
				uuid = OpbmDialog.simpleDialog(m_opbm, "Creating Debug Info...please wait", "Gather Debug Info", 0, false, true);

				Xml root				= new Xml("opbm");
				root.appendChild(new Xml("version", m_version));
				Xml debugInfo			= root.appendChild(new Xml("debugInfo"));
				Xml machineInfo			= root.appendChild(new Xml("machineInfo"));
				Xml settings			= debugInfo.appendChild(new Xml("settingsDotXml"));
				Xml manifest			= debugInfo.appendChild(new Xml("manifestDotXml"));
				Xml manifestFile		= Opbm.loadXml(Opbm.getRunningDirectory() + "manifest.xml", m_opbm);
				Xml results				= debugInfo.appendChild(new Xml("resultsDotXml"));
				String resultsXmlFile	= Opbm.getHarnessXMLDirectory() + Utils.getMostRecentResultsXmlFile(m_opbm);
				Xml resultsFile			= Opbm.loadXml(resultsXmlFile, m_opbm);
				Xml scripts				= debugInfo.appendChild(new Xml("scriptsDotXml"));
				Xml edits				= debugInfo.appendChild(new Xml("editsDotXml"));
				Xml panels				= debugInfo.appendChild(new Xml("panelsDotXml"));

				Utils.appendJavaInfo(machineInfo);

				settings.appendChild(getSettingsMaster().getSettingsXml().cloneNode(true));
				scripts.appendChild(getScriptsXml().cloneNode(true));
				edits.appendChild(getEditsXml().cloneNode(true));
				panels.appendChild(getPanelsXml().cloneNode(true));

				if (manifestFile != null)
				{	// Manifest.xml file was loaded
					manifest.appendChild(manifestFile.cloneNode(true));
				} else {
					// Manifest.xml file was not found
					Xml error = manifest.appendChild(new Xml("error"));
					error.addAttribute("fileNotFound", "manifest.xml");
				}

				if (resultsFile != null)
				{	// Results.xml file was loaded
					results.appendAttribute("filename", resultsXmlFile);
					results.appendChild(resultsFile.cloneNode(true));
				} else {
					// Results.xml file was not found
					Xml error = manifest.appendChild(new Xml("error"));
					error.addAttribute("fileNotFound", "[A valid results*.xml file]");
				}

				String filename	 = Utils.convertFilenameToLettersAndNumbersOnly("debugInfo_" + Utils.getDateTimeAs_Mmm_DD_YYYY_at_HH_MM_SS()) + ".xml";
				root.saveNode(Opbm.getHarnessTempDirectory() + filename);
				try {
					Runtime.getRuntime().exec("explorer.exe /n, /select, " + Opbm.getHarnessTempDirectory() + filename);
				} catch (Throwable t) {
				}

				// Remove the dialog box
				closeDialogWindow(uuid);
			}
		};
		t.start();
	}

	/**
	 * When a command line sequence is run, this variable is set high.
	 * @return yes or no if we're running a sequence from the command line
	 */
	public boolean isExecutingFromCommandLine()
	{
		return(m_executingFromCommandLine);
	}

	/**
	 * Based on a combination of factors.  If running a manual benchmark,
	 * then will not be terminating afterward.  If running from the command
	 * line then they will be terminating unless they've specified the -noexit
	 * command line parameter.
	 * @return
	 */
	public boolean willTerminateAfterRun()
	{
		if (m_benchmarkRun_bm != null && m_benchmarkRun_bm.didOriginallyLaunchFromCommandLine())
		{
			return(true);
		} else {
			return(false);
		}
	}

	/**
	 * Returns the encoded final static version string, which is the build
	 * date and time.
	 */
	public String getVersion()
	{
		return(m_version);
	}

	/**
	 * Returns the encoded final static title string, which includes the build
	 * date and time.
	 */
	public String getAppTitle()
	{
		return(m_title);
	}

	/**
	 * This is the common way to exit the system politely, when OPBM wants to
	 * shut down normally and cleanly, and not leave the registry affected by
	 * any previous setting which may have resulted from a failure.
	 * @param returnCode
	 */
	public static void quit(int returnCode)
	{
		// Remove any restarter registry keys and opbmpostboot executable keys that may have been deposited earlier
		// All other exits are handled elsewhere, and leave any registry keys as they were
		Opbm.SetRegistryKeyValueAsString("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\RunOnce\\opbm", "");
		Opbm.SetRegistryKeyValueAsString("HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\RunOnce\\opbmpostboot", "");

		// Return the code they specified
		System.exit(returnCode);
	}

	/**
	 * Main app entry point
	 * @param args command line parameters
	 */
    public static void main(String[] args)
	{
		if (System.getProperty("java.version").substring(0,3).compareTo("1.7") >= 0)
		{	// Switching to 1.7.0 changed the way translucency is handled
			GraphicsEnvironment	ge		= GraphicsEnvironment.getLocalGraphicsEnvironment();
			GraphicsDevice		grfx	= ge.getDefaultScreenDevice();
			if (!grfx.isWindowTranslucencySupported(TRANSLUCENT))
			{	// Tell the user that translucency is not supported
				System.out.println("Translucency is not supported by this graphical environment");
			}
		}
		// Launch the system
		Opbm o = new Opbm(args);
	}


	private Opbm					m_opbm;										// Master instance created in main()
	private String[]				m_args;										// Holds the command line arguments for processing after the invokeLater() runnable
	private	Xml						m_editXml;									// Root node of the edits.xml data that is loaded at startup
	private	Xml						m_panelXml;									// Root node of the panels.xml data that is loaded at startup
	private	Xml						m_scriptsXml;								// Root node of the scripts.xml data that is loaded at startup
	private List<PanelLeft>			m_leftPanels;								// Raw pool of all loaded left-side panels, whether they are displayed or not
	private List<PanelLeft>			m_navHistory;								// Chain of panels as they're navigated to/through in real-time
	private List<PanelRight>		m_editPanels;								// Raw pool of edit rightpanels as created
	private List<PanelRight>		m_rawEditPanels;							// Raw pool of rawedit rightpanels as created
	private List<Tuple>				m_tuples;									// Master list of tuples used throughout the system, referenced by name
	private PanelRight				m_editActive;								// Holds current active edit <code>PanelRight</code> object being displayed.  Used to make $active_edit$ macro work.
	private PanelRight				m_rawEditActive;							// Holds current active_rawedit <code>PanelRight</code> object being displayed.  Used to make $active_rawedit$ macro work.
	private Macros					m_macroMaster;								// Handles all macros used in this system, updated with live data while running.  Note:  Does not need to be a non-static class, but was setup that way for possible future extensibility.
	private Benchmarks				m_benchmarkMaster;							// Handles all benchmark runs
	private Settings				m_settingsMaster;							// Handles all settings processing.
	private Commands				m_commandMaster;							// Handles all command processing.  Note:  Does not need to be a non-static class, but was setup that way for possible future extensibility.
	private boolean					m_fontOverride;								// Command line switch "-font" can be used to indiate not running in Windows.  Used to switch default font from Calibri to Arial.

	/**
	 * Reference to the main newFrame used for visualization in this application.
	 * Refer to the <code>DroppableFrame</code> class.  This newFrame supports
	 * four primary components:
	 *
	 *		1)  Header
	 *		2)	Left-panel
	 *		3)	Right-panel
	 *		4)  Status bar
	 *
	 * These elements are always present, though they change through the real-
	 * time use of the system by users.
	 */
    private DeveloperWindow			m_frameDeveloper;							// The complex developer screen
	private SimpleWindow			m_frameSimple;								// The skinned simple user screen

	private List<JFrame>			m_zoomFrames;								// Zoom button and editing windows (that popup when new entries are added) are all called zoom windows, this holds a list of all that are open at the time
	private List<DroppableFrame>	m_rvFrames;									// Holds list of open results viewer windows, closed automatically at benchmark run
	private List<Xml>				m_compilation;								// Holds the built on-the-fly compilation list to execute
	private int						m_compilationHighlightEntry;				// Holds the global offset into the m_compilation list for the highlighted item
	private ResultsViewer			m_rv;										// What was the last loaded results viewer instance?  (multiple instances of the results viewer are possible)
	private String					m_rvFilename;								// What was the last loaded results viewer filename?
	private boolean					m_executingFromCommandLine;					// Are we executing from the command line?
	private boolean					m_executingTrialRun;						// Are we executing a trial run?
	private boolean					m_executingOfficialRun;						// Are we executing an official run?
	private String					m_executingBenchmarkRunName;				// The name of the run assigned on the command line
	private boolean					m_noExit;									// Was the -noexit command line option specified?
	private Tuple					m_dialogTuple;								// Holds references (by id, in getFirst(n)) for all responses and last actions (see getDialog* and setDialog* responses)
        private boolean                                 m_restartedManifest = false;                            //Defines a restarted manifest +rcp 12/18/2011

	// Static variables used throughout
	public static boolean			m_breakpointsEnabled;						// Used for debugging (An internal debugger flag, to determine if certain breakpoints used during development should be stopped at or not)
	public static boolean			m_debugSimulateRunAtomMode;					// Used for debugging (An internal debugger flag, to bypass normal operations and simulate successes during testing)
	public static double			m_debugSimulateRunAtomModeFailurePercent;	// Used for debugging (An internal debugger value, to determine how many tests (on average) should fail)
	public static String			m_lastLoadXmlError;							// Used during load, to help determine the cause of the requisite xml file which failed to load
	public static String			m_jvmHome					= Utils.getPathToJavaDotExe();

	// Synchronization items used for various wait-until-all-parts-are-completed operations
	public volatile static int		m_rvsync					= 0;	// Used by createAndShowResultsViewer

	// Used for the build-date and time
	public final static String		m_version					= "-- 1.3.1 -- DEV BRANCH BUILD -- UNSTABLE -- Built 2011.12.17 09:01AM";
	public final static String		m_title						= "OPBM - Office Productivity Benchmark - " + m_version;
}
