/*******************************************************************************
 * #%L
 * Armedia JMeter Gherkin Plugin
 * %%
 * Copyright (C) 2020 Armedia, LLC
 * %%
 * This file is part of the Armedia JMeter Gherkin Plugin software.
 * 
 * If the software was purchased under a paid Armedia JMeter Gherkin Plugin license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 * 
 * Armedia JMeter Gherkin Plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Armedia JMeter Gherkin Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Armedia JMeter Gherkin Plugin. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.commons.jmeter.plugins.gherkin.config.gui;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import org.apache.jmeter.config.gui.AbstractConfigGui;
import org.apache.jmeter.gui.util.JSyntaxTextArea;
import org.apache.jmeter.gui.util.JTextScrollPane;
import org.apache.jmeter.testelement.TestElement;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.jmeter.plugins.gherkin.config.GherkinConfig;
import com.armedia.commons.jmeter.plugins.gherkin.config.GherkinConfig.Script;
import com.armedia.commons.jmeter.tools.JSR223Script;

public class GherkinConfigGui extends AbstractConfigGui implements ItemListener {
	private static final long serialVersionUID = 1L;

	private final Logger log = LoggerFactory.getLogger(getClass());

	private JCheckBox resetContextOnLoop;
	private JCheckBox dryRun;
	private JCheckBox failOnPending;
	private JComboBox<String> outputFormats;

	private JSyntaxTextArea packages;

	private final JFileChooser chooser = new JFileChooser();

	private JTextField compositesFile;
	private JSyntaxTextArea composites;

	private JComboBox<String> languages;
	private JCheckBox compileScript;
	private JSyntaxTextArea script;

	public GherkinConfigGui() {
		setLayout(new BorderLayout(0, 5));
		setBorder(makeBorder());

		Box box = Box.createVerticalBox();
		box.add(makeTitlePanel());
		box.add(createOutputFormatsPanel());
		add(box, BorderLayout.NORTH);
		add(createTabsPanel(), BorderLayout.CENTER);
	}

	private String renderLanguage(String name) {
		String desc = JSR223Script.LANGUAGES.get(name);
		if (desc == null) {
			desc = "engine unknown";
		}
		return renderLanguage(name, desc);
	}

	private String renderLanguage(String name, String description) {
		return name + "     (" + description + ")";
	}

	private String extractLanguage(String rendering) {
		if (rendering == null) { return null; }
		return rendering.trim().replaceAll("^\\s*(\\S*)\\s*.*$", "$1");
	}

	private JPanel createOutputFormatsPanel() {
		final JPanel outerPanel = new JPanel(new BorderLayout(5, 0));

		Box box = Box.createVerticalBox();

		this.dryRun = new JCheckBox("Dry run (parse and process the steps, but don't execute the code)");
		this.dryRun.setSelected(false);
		box.add(this.dryRun);

		this.failOnPending = new JCheckBox(
			"Fail on pending steps (fail if stories reference steps that aren't yet implemented)");
		this.failOnPending.setSelected(false);
		box.add(this.failOnPending);

		this.resetContextOnLoop = new JCheckBox("Reset the Gherkin context on every thread loop");
		this.resetContextOnLoop.setSelected(false);
		box.add(this.resetContextOnLoop);

		outerPanel.add(box, BorderLayout.NORTH);

		Vector<String> outputFormats = new Vector<>(GherkinConfig.OUTPUT_FORMATS);
		this.outputFormats = new JComboBox<>(outputFormats);
		this.outputFormats.setName(GherkinConfig.OUTPUT_FORMAT);
		final JLabel label = new JLabel("Output Format:");
		label.setLabelFor(this.outputFormats);

		outerPanel.add(label, BorderLayout.WEST);
		outerPanel.add(this.outputFormats, BorderLayout.CENTER);

		outerPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Story execution settings",
			TitledBorder.LEFT, TitledBorder.TOP));
		return outerPanel;
	}

	private JPanel createTabsPanel() {
		JTabbedPane tabs = new JTabbedPane();

		tabs.addTab("Steps", createStepScannerTab());
		tabs.addTab("Composite Steps", createCompositesTab());
		tabs.addTab("Initializer", createScriptTab());

		JPanel panel = new JPanel(new BorderLayout());
		panel.add(tabs, BorderLayout.CENTER);
		return panel;
	}

	private JPanel createStepScannerTab() {
		this.packages = JSyntaxTextArea.getInstance(25, 80, false);
		JScrollPane scanSpecPane = JTextScrollPane.getInstance(this.packages, true);
		this.packages.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
		this.packages.setLanguage("text");

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED),
			"Packages to scan for step implementations (use # for comments, leading and trailing spaces are ignored)",
			TitledBorder.LEFT, TitledBorder.TOP));

		panel.add(scanSpecPane, BorderLayout.CENTER);
		return panel;
	}

	private File chooseFile(String title, File currentFile) {
		this.chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
		if ((currentFile != null) && currentFile.exists()) {
			if (currentFile.isFile()) {
				currentFile = currentFile.getParentFile();
			}
			this.chooser.setCurrentDirectory(currentFile);
		}
		this.chooser.setDialogTitle(title);
		while (true) {
			switch (this.chooser.showDialog(null, "Select")) {
				case JFileChooser.APPROVE_OPTION:
					File target = this.chooser.getSelectedFile();
					try {
						target = target.getCanonicalFile();
					} catch (Exception e) {
						this.log.warn("Exception caught while trying to canonicalize the path [{}]", target, e);
					}
					return target.getAbsoluteFile();
				// fall-through

				default:
					return null;
			}
		}
	}

	private JPanel makeCompositesFileSelectorPanel() {
		this.compositesFile = new JTextField();
		final JLabel label = new JLabel("File Name:");
		label.setLabelFor(this.compositesFile);

		final JPanel panel = new JPanel(new BorderLayout(5, 0));
		panel.add(label, BorderLayout.WEST);
		panel.add(this.compositesFile, BorderLayout.CENTER);

		final JButton browseButton = new JButton("Browse...");
		panel.add(browseButton, BorderLayout.EAST);
		browseButton.addActionListener((ev) -> {
			File f = chooseFile("Select the file containing the composite Gherkin Steps",
				new File(this.compositesFile.getText()));
			if (f != null) {
				this.compositesFile.setText(f.getAbsolutePath());
			}
		});

		panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED),
			"Gherkin Composites file (overrides the Gherkin Composites defined below)", TitledBorder.LEFT,
			TitledBorder.TOP));
		return panel;
	}

	private JPanel createCompositesTab() {
		Box box = Box.createVerticalBox();
		box.add(makeCompositesFileSelectorPanel());

		this.composites = JSyntaxTextArea.getInstance(25, 80, false);
		JScrollPane scriptPane = JTextScrollPane.getInstance(this.composites, true);
		this.composites.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
		this.composites.setLanguage("text");

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED), "Gherkin Composites definitions",
			TitledBorder.LEFT, TitledBorder.TOP));

		panel.add(box, BorderLayout.NORTH);
		panel.add(scriptPane, BorderLayout.CENTER);
		return panel;
	}

	private JPanel createScriptLanguagesPanel() {
		Map<String, String> languages = JSR223Script.getLanguages();
		Vector<String> scriptLanguages = new Vector<>(languages.size());
		languages.forEach((n, d) -> scriptLanguages.add(renderLanguage(n, d)));
		Collections.sort(scriptLanguages);

		this.languages = new JComboBox<>(scriptLanguages);
		this.languages.setName(GherkinConfig.SCRIPT_LANGUAGE);
		this.languages.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent actionEvent) {
				JComboBox<?> source = JComboBox.class.cast(actionEvent.getSource());
				String language = StringUtils.defaultString(source.getSelectedItem(), null);
				setScriptLanguage(extractLanguage(language));
			}
		});

		final JLabel label = new JLabel("Script Language:");
		label.setLabelFor(this.languages);

		final JPanel panel = new JPanel(new BorderLayout(5, 0));
		panel.add(label, BorderLayout.WEST);
		panel.add(this.languages, BorderLayout.CENTER);

		/*
		panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED),
			"JSR223Script language (i.e. groovy, beanshell, javascript, jexl ...", TitledBorder.LEFT,
			TitledBorder.TOP));
		*/
		return panel;
	}

	private JPanel createScriptTab() {

		Box box = Box.createVerticalBox();
		box.add(createScriptLanguagesPanel());

		this.compileScript = new JCheckBox("Compile the script of possible");
		this.compileScript.setSelected(true);
		box.add(this.compileScript);

		this.script = JSyntaxTextArea.getInstance(25, 80, false);
		JScrollPane scriptPane = JTextScrollPane.getInstance(this.script, true);
		this.script.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
		setScriptLanguage("text");

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED),
			"Initialization JSR223Script (variables: gherkin ctx vars props log Label OUT ERR)", TitledBorder.LEFT,
			TitledBorder.TOP));

		panel.add(box, BorderLayout.NORTH);
		panel.add(scriptPane, BorderLayout.CENTER);
		return panel;
	}

	private void setScriptLanguage(String language) {
		String script = this.script.getText();
		this.script.setLanguage(language.toLowerCase());
		this.script.setText(script);
	}

	protected final GherkinConfig cast(TestElement element) {
		if (GherkinConfig.class.isInstance(element)) { return GherkinConfig.class.cast(element); }
		throw new ClassCastException("Can't convert " + element + " into " + GherkinConfig.class.getCanonicalName());
	}

	@Override
	public String getStaticLabel() {
		return "Gherkin Config";
	}

	@Override
	public String getLabelResource() {
		return getClass().getCanonicalName();
	}

	@Override
	public final void configure(TestElement element) {
		super.configure(element);
		configure(cast(element));
	}

	protected void configure(GherkinConfig cfg) {
		this.resetContextOnLoop.setSelected(cfg.isResetOnLoop());
		this.compileScript.setSelected(cfg.isCompileIfPossible());
		this.dryRun.setSelected(cfg.isDryRun());
		this.failOnPending.setSelected(cfg.isFailOnPending());
		Script script = cfg.getScript();
		this.script.setText(script.getScript());
		this.languages.setSelectedItem(renderLanguage(script.getLanguage()));
		this.outputFormats.setSelectedItem(cfg.getOutputFormat());
		this.compositesFile.setText(cfg.getCompositesFile());
		this.composites.setText(cfg.getComposites());
		this.packages.setText(cfg.getPackages());
	}

	@Override
	public void modifyTestElement(TestElement element) {
		configureTestElement(element);
	}

	@Override
	protected final void configureTestElement(TestElement element) {
		super.configureTestElement(element);
		configureTestElement(cast(element));
	}

	protected void configureTestElement(GherkinConfig config) {
		config.setResetOnLoop(this.resetContextOnLoop.isSelected());
		config.setCompileIfPossible(this.compileScript.isSelected());
		config.setDryRun(this.dryRun.isSelected());
		config.setFailOnPending(this.failOnPending.isSelected());
		String str = StringUtils.defaultString(this.languages.getSelectedItem(), null);
		config.setScript(extractLanguage(str), this.script.getText());
		str = StringUtils.defaultString(this.outputFormats.getSelectedItem(), null);
		config.setOutputFormat(str);
		config.setCompositesFile(this.compositesFile.getText());
		config.setComposites(this.composites.getText());
		config.setPackages(this.packages.getText());
	}

	@Override
	public TestElement createTestElement() {
		GherkinConfig element = new GherkinConfig();
		modifyTestElement(element);
		return element;
	}

	@Override
	public void clearGui() {
		super.clearGui();
		this.resetContextOnLoop.setSelected(false);
		this.compileScript.setSelected(true);
		this.dryRun.setSelected(false);
		this.failOnPending.setSelected(false);
		this.languages.setSelectedItem(renderLanguage(GherkinConfig.DEFAULT_SCRIPT_LANGUAGE));
		this.outputFormats.setSelectedItem(GherkinConfig.DEFAULT_OUTPUT_FORMAT);
		this.script.setText("");
	}

	@Override
	public void itemStateChanged(ItemEvent itemEvent) {
		// Item has been changed
	}
}
